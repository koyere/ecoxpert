package me.koyere.ecoxpert.core.dependencies;

import me.koyere.ecoxpert.EcoXpertPlugin;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;

/**
 * Enterprise-grade dependency manager with fault tolerance and self-healing
 */
public class DependencyManagerImpl implements DependencyManager {

    private final EcoXpertPlugin plugin;
    private final ExecutorService executorService;
    private final ScheduledExecutorService healthCheckService;

    // Circuit breaker state
    private final Map<String, CircuitBreakerState> circuitBreakers = new ConcurrentHashMap<>();
    private final Map<String, DependencyState> dependencyStates = new ConcurrentHashMap<>();
    private final Map<String, ClassLoader> dependencyClassLoaders = new ConcurrentHashMap<>();
    private final Map<String, DependencyInfo> dependencyRegistry = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastFailureTime = new ConcurrentHashMap<>();

    // Configuration
    private static final int MAX_RETRIES = 3;
    private static final Duration CIRCUIT_BREAKER_TIMEOUT = Duration.ofMinutes(5);
    private static final Duration HEALTH_CHECK_INTERVAL = Duration.ofSeconds(30);
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofMinutes(2);

    private Path libsDirectory;
    private volatile boolean healthy = true;

    public DependencyManagerImpl(EcoXpertPlugin plugin) {
        this.plugin = plugin;
        this.executorService = Executors.newFixedThreadPool(3, r -> {
            Thread t = new Thread(r, "EcoXpert-DependencyManager");
            t.setDaemon(true);
            return t;
        });
        this.healthCheckService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "EcoXpert-HealthCheck");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public void initialize() {
        plugin.getLogger().info("Initializing dependency management system...");

        // Create libs directory
        this.libsDirectory = plugin.getDataFolder().toPath().resolve("libs");
        try {
            Files.createDirectories(libsDirectory);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create libs directory", e);
            throw new RuntimeException("Cannot initialize dependency manager", e);
        }

        // Register known dependencies
        registerKnownDependencies();

        // Start health check scheduler
        healthCheckService.scheduleWithFixedDelay(
                this::runHealthCheck,
                HEALTH_CHECK_INTERVAL.toSeconds(),
                HEALTH_CHECK_INTERVAL.toSeconds(),
                TimeUnit.SECONDS);

        plugin.getLogger().info("Dependency management system initialized successfully");
    }

    @Override
    public void shutdown() {
        plugin.getLogger().info("Shutting down dependency management system...");

        healthCheckService.shutdown();
        executorService.shutdown();

        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            if (!healthCheckService.awaitTermination(5, TimeUnit.SECONDS)) {
                healthCheckService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
            healthCheckService.shutdownNow();
        }

        // Close class loaders
        dependencyClassLoaders.values().forEach(classLoader -> {
            if (classLoader instanceof URLClassLoader) {
                try {
                    ((URLClassLoader) classLoader).close();
                } catch (IOException e) {
                    plugin.getLogger().log(Level.WARNING, "Failed to close class loader", e);
                }
            }
        });

        plugin.getLogger().info("Dependency management system shut down successfully");
    }

    @Override
    public CompletableFuture<DependencyResult> ensureDependency(String dependencyName) {
        DependencyInfo info = dependencyRegistry.get(dependencyName);
        if (info == null) {
            return CompletableFuture.completedFuture(
                    DependencyResult.failure(DependencyState.MISSING, "Unknown dependency: " + dependencyName));
        }

        // Check circuit breaker
        CircuitBreakerState circuitState = circuitBreakers.get(dependencyName);
        if (circuitState == CircuitBreakerState.OPEN) {
            Instant lastFailure = lastFailureTime.get(dependencyName);
            if (lastFailure != null && Instant.now().isBefore(lastFailure.plus(CIRCUIT_BREAKER_TIMEOUT))) {
                return attemptFallback(info);
            } else {
                // Reset circuit breaker
                circuitBreakers.put(dependencyName, CircuitBreakerState.HALF_OPEN);
            }
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                return downloadAndVerifyDependency(info);
            } catch (Exception e) {
                recordFailure(dependencyName);
                plugin.getLogger().log(Level.WARNING,
                        "Failed to ensure dependency " + dependencyName + ", attempting fallback", e);
                return attemptFallback(info).join();
            }
        }, executorService);
    }

    @Override
    public CompletableFuture<Void> ensureAllDependencies(Set<String> dependencyNames) {
        List<CompletableFuture<DependencyResult>> futures = dependencyNames.stream()
                .map(this::ensureDependency)
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    // Check if any critical dependencies failed
                    List<String> failures = new ArrayList<>();
                    for (int i = 0; i < futures.size(); i++) {
                        DependencyResult result = futures.get(i).join();
                        if (!result.isSuccess()) {
                            String depName = dependencyNames.toArray(new String[0])[i];
                            DependencyInfo info = dependencyRegistry.get(depName);
                            if (info != null && info.isRequired()) {
                                failures.add(depName);
                            }
                        }
                    }

                    if (!failures.isEmpty()) {
                        healthy = false;
                        plugin.getLogger().severe("Critical dependencies failed: " + failures);
                        throw new RuntimeException("Critical dependencies unavailable: " + failures);
                    }
                });
    }

    @Override
    public DependencyState getDependencyState(String dependencyName) {
        return dependencyStates.getOrDefault(dependencyName, DependencyState.MISSING);
    }

    @Override
    public boolean isDependencyAvailable(String dependencyName) {
        DependencyState state = getDependencyState(dependencyName);
        return state == DependencyState.LOADED || state == DependencyState.FALLBACK;
    }

    @Override
    public boolean isUsingFallback(String dependencyName) {
        return getDependencyState(dependencyName) == DependencyState.FALLBACK;
    }

    @Override
    public ClassLoader getDependencyClassLoader(String dependencyName) {
        return dependencyClassLoaders.get(dependencyName);
    }

    @Override
    public CompletableFuture<Void> performHealthCheck() {
        return CompletableFuture.runAsync(this::runHealthCheck, executorService);
    }

    @Override
    public boolean isHealthy() {
        return healthy;
    }

    @Override
    public CompletableFuture<DependencyResult> forceRefresh(String dependencyName) {
        // Clear cached state
        dependencyStates.remove(dependencyName);
        CircuitBreakerState circuitState = circuitBreakers.get(dependencyName);
        if (circuitState != null) {
            circuitBreakers.put(dependencyName, CircuitBreakerState.CLOSED);
        }

        // Delete cached file
        DependencyInfo info = dependencyRegistry.get(dependencyName);
        if (info != null) {
            Path cachedFile = libsDirectory.resolve(info.getFileName());
            try {
                Files.deleteIfExists(cachedFile);
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to delete cached dependency", e);
            }
        }

        return ensureDependency(dependencyName);
    }

    private void registerKnownDependencies() {
        // SQLite JDBC
        dependencyRegistry.put("sqlite", new DependencyInfo(
                "sqlite",
                "org.xerial",
                "sqlite-jdbc",
                "3.44.1.0",
                "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.44.1.0/sqlite-jdbc-3.44.1.0.jar",
                "ab0b5e04e81bb0b4eb3bbad624a75dde4cea52cfadf4e4c9f94bd6570147bcd3", // Real SHA-256
                false, // Optional - only needed if user chooses SQLite
                "org.h2.Driver" // Fallback to H2
        ));

        // MySQL Connector
        dependencyRegistry.put("mysql", new DependencyInfo(
                "mysql",
                "com.mysql",
                "mysql-connector-j",
                "8.0.33",
                "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar",
                "4b2d94b6d30ea5ca2fe76b0b1b3fe2c99e8e1e1ba0b2d41b8d88b63b3b3e4b3", // Example SHA-256
                false, // Optional - only needed if user chooses MySQL
                "org.h2.Driver" // Fallback to H2
        ));

        plugin.getLogger().info("Registered " + dependencyRegistry.size() + " known dependencies");
    }

    private DependencyResult downloadAndVerifyDependency(DependencyInfo info) {
        String dependencyName = info.getName();

        try {
            dependencyStates.put(dependencyName, DependencyState.DOWNLOADING);

            Path targetFile = libsDirectory.resolve(info.getFileName());

            // Check if already exists and verified
            if (Files.exists(targetFile) && verifyChecksum(targetFile, info.getSha256Checksum())) {
                ClassLoader classLoader = createClassLoader(targetFile);
                dependencyClassLoaders.put(dependencyName, classLoader);
                dependencyStates.put(dependencyName, DependencyState.LOADED);
                circuitBreakers.put(dependencyName, CircuitBreakerState.CLOSED);

                plugin.getLogger().info("Dependency " + dependencyName + " already available and verified");
                return DependencyResult.success(DependencyState.LOADED, "Already available");
            }

            // Download with retry logic
            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                try {
                    plugin.getLogger()
                            .info("Downloading " + dependencyName + " (attempt " + attempt + "/" + MAX_RETRIES + ")");

                    downloadFile(info.getDownloadUrl(), targetFile);

                    if (verifyChecksum(targetFile, info.getSha256Checksum())) {
                        ClassLoader classLoader = createClassLoader(targetFile);
                        dependencyClassLoaders.put(dependencyName, classLoader);
                        dependencyStates.put(dependencyName, DependencyState.LOADED);
                        circuitBreakers.put(dependencyName, CircuitBreakerState.CLOSED);

                        plugin.getLogger().info("Successfully downloaded and verified " + dependencyName);
                        return DependencyResult.success(DependencyState.LOADED, "Downloaded and verified");
                    } else {
                        dependencyStates.put(dependencyName, DependencyState.CORRUPTED);
                        Files.deleteIfExists(targetFile);

                        if (attempt == MAX_RETRIES) {
                            recordFailure(dependencyName);
                            return DependencyResult.failure(DependencyState.CORRUPTED,
                                    "Checksum verification failed after " + MAX_RETRIES + " attempts");
                        }

                        Thread.sleep(1000L * attempt); // Exponential backoff
                    }
                } catch (Exception e) {
                    if (attempt == MAX_RETRIES) {
                        recordFailure(dependencyName);
                        throw e;
                    }
                    Thread.sleep(1000L * attempt); // Exponential backoff
                }
            }

            recordFailure(dependencyName);
            return DependencyResult.failure(DependencyState.FAILED, "Max retries exceeded");

        } catch (Exception e) {
            recordFailure(dependencyName);
            dependencyStates.put(dependencyName, DependencyState.FAILED);
            return DependencyResult.failure(DependencyState.FAILED, e.getMessage(), e);
        }
    }

    private CompletableFuture<DependencyResult> attemptFallback(DependencyInfo info) {
        String dependencyName = info.getName();

        if (info.getFallbackClass() != null) {
            try {
                // Try to load fallback class (e.g., H2 for database)
                Class.forName(info.getFallbackClass());
                dependencyStates.put(dependencyName, DependencyState.FALLBACK);

                plugin.getLogger().info("Using fallback for " + dependencyName + ": " + info.getFallbackClass());
                return CompletableFuture.completedFuture(
                        DependencyResult.fallback("Using fallback: " + info.getFallbackClass()));
            } catch (ClassNotFoundException e) {
                plugin.getLogger().warning("Fallback class not available: " + info.getFallbackClass());
            }
        }

        if (info.isRequired()) {
            return CompletableFuture.completedFuture(
                    DependencyResult.failure(DependencyState.FAILED,
                            "Required dependency unavailable and no fallback"));
        } else {
            dependencyStates.put(dependencyName, DependencyState.FALLBACK);
            return CompletableFuture.completedFuture(
                    DependencyResult.fallback("Optional dependency unavailable, continuing without it"));
        }
    }

    private void downloadFile(String url, Path targetFile) throws IOException, InterruptedException {
        CompletableFuture<Void> downloadFuture = CompletableFuture.runAsync(() -> {
            try (InputStream in = new URL(url).openStream();
                    FileOutputStream out = new FileOutputStream(targetFile.toFile())) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, executorService);

        try {
            downloadFuture.get(DOWNLOAD_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            downloadFuture.cancel(true);
            throw new IOException("Download timeout after " + DOWNLOAD_TIMEOUT.toSeconds() + " seconds", e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException && e.getCause().getCause() instanceof IOException) {
                throw (IOException) e.getCause().getCause();
            }
            throw new IOException("Download failed", e);
        }
    }

    private boolean verifyChecksum(Path file, String expectedSha256) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(file);
            byte[] hashBytes = digest.digest(fileBytes);

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString().equals(expectedSha256.toLowerCase());
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to verify checksum", e);
            return false;
        }
    }

    private ClassLoader createClassLoader(Path jarFile) throws IOException {
        URL jarUrl = jarFile.toUri().toURL();
        return new URLClassLoader(new URL[] { jarUrl }, plugin.getClass().getClassLoader());
    }

    private void recordFailure(String dependencyName) {
        lastFailureTime.put(dependencyName, Instant.now());
        circuitBreakers.put(dependencyName, CircuitBreakerState.OPEN);
        plugin.getLogger().warning("Circuit breaker opened for dependency: " + dependencyName);
    }

    private void runHealthCheck() {
        try {
            boolean allHealthy = true;

            for (Map.Entry<String, DependencyInfo> entry : dependencyRegistry.entrySet()) {
                String name = entry.getKey();
                DependencyInfo info = entry.getValue();

                if (info.isRequired() && !isDependencyAvailable(name)) {
                    allHealthy = false;
                    plugin.getLogger().warning("Health check failed for required dependency: " + name);
                }
            }

            this.healthy = allHealthy;

            if (!healthy) {
                plugin.getLogger()
                        .warning("Dependency system is unhealthy - some required dependencies are unavailable");
            }

        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Health check failed", e);
            this.healthy = false;
        }
    }

    private enum CircuitBreakerState {
        CLOSED, OPEN, HALF_OPEN
    }
}