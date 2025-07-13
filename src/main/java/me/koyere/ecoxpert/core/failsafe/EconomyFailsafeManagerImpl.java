package me.koyere.ecoxpert.core.failsafe;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.core.data.DataManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

/**
 * Enterprise-grade failsafe manager for economy operations
 * 
 * Provides comprehensive data protection with automatic backup,
 * corruption detection, transaction logging, and emergency recovery.
 */
@Singleton
public class EconomyFailsafeManagerImpl implements EconomyFailsafeManager {
    
    private final EcoXpertPlugin plugin;
    private final DataManager dataManager;
    private final ExecutorService executorService;
    private final ScheduledExecutorService scheduledService;
    
    // Read-only mode state
    private final AtomicBoolean readOnlyMode = new AtomicBoolean(false);
    private final AtomicReference<String> readOnlyReason = new AtomicReference<>();
    
    // Checkpoint system
    private final Map<String, CheckpointData> checkpoints = new ConcurrentHashMap<>();
    private final Queue<TransactionRecord> transactionQueue = new ConcurrentLinkedQueue<>();
    
    // Configuration
    private static final int MAX_BACKUPS = 10;
    private static final int MAX_TRANSACTION_QUEUE = 10000;
    private static final long BACKUP_INTERVAL_HOURS = 6;
    private static final long HEALTH_CHECK_INTERVAL_MINUTES = 5;
    
    private Path backupDirectory;
    private Path transactionLogFile;
    private volatile boolean initialized = false;
    private volatile FailsafeHealth lastHealthStatus;
    
    @Inject
    public EconomyFailsafeManagerImpl(EcoXpertPlugin plugin, DataManager dataManager) {
        this.plugin = plugin;
        this.dataManager = dataManager;
        this.executorService = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "EcoXpert-Failsafe");
            t.setDaemon(true);
            return t;
        });
        this.scheduledService = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "EcoXpert-FailsafeScheduler");
            t.setDaemon(true);
            return t;
        });
    }
    
    @Override
    public void initialize() {
        if (initialized) {
            return;
        }
        
        try {
            plugin.getLogger().info("Initializing economy failsafe system...");
            
            // Create backup directory
            this.backupDirectory = plugin.getDataFolder().toPath().resolve("backups");
            Files.createDirectories(backupDirectory);
            
            // Initialize transaction log
            this.transactionLogFile = plugin.getDataFolder().toPath().resolve("transaction.log");
            
            // Clean old backups
            cleanOldBackups();
            
            // Schedule automatic backups
            scheduledService.scheduleAtFixedRate(
                this::performScheduledBackup,
                BACKUP_INTERVAL_HOURS,
                BACKUP_INTERVAL_HOURS,
                TimeUnit.HOURS
            );
            
            // Schedule health checks
            scheduledService.scheduleAtFixedRate(
                this::performScheduledHealthCheck,
                HEALTH_CHECK_INTERVAL_MINUTES,
                HEALTH_CHECK_INTERVAL_MINUTES,
                TimeUnit.MINUTES
            );
            
            // Schedule transaction log flushing
            scheduledService.scheduleAtFixedRate(
                this::flushTransactionLog,
                30, 30, TimeUnit.SECONDS
            );
            
            // Initial health check
            performHealthCheck().join();
            
            this.initialized = true;
            plugin.getLogger().info("Economy failsafe system initialized successfully");
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize failsafe system", e);
            throw new RuntimeException("Failsafe initialization failed", e);
        }
    }
    
    @Override
    public void shutdown() {
        if (!initialized) {
            return;
        }
        
        plugin.getLogger().info("Shutting down economy failsafe system...");
        
        // Flush remaining transactions
        flushTransactionLog();
        
        // Shutdown executors
        scheduledService.shutdown();
        executorService.shutdown();
        
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            if (!scheduledService.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
            scheduledService.shutdownNow();
        }
        
        this.initialized = false;
        plugin.getLogger().info("Economy failsafe system shut down successfully");
    }
    
    @Override
    public CompletableFuture<Void> createBackup(String reason) {
        return CompletableFuture.runAsync(() -> {
            try {
                String timestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
                    .format(Instant.now().atZone(java.time.ZoneOffset.UTC));
                String backupName = "economy_" + timestamp + ".backup";
                Path backupFile = backupDirectory.resolve(backupName);
                
                plugin.getLogger().info("Creating economy backup: " + backupName + " (Reason: " + reason + ")");
                
                // Create backup using database export
                dataManager.exportDatabase(backupFile).join();
                
                // Write metadata
                Path metadataFile = backupDirectory.resolve(backupName + ".meta");
                Properties metadata = new Properties();
                metadata.setProperty("timestamp", Instant.now().toString());
                metadata.setProperty("reason", reason);
                metadata.setProperty("plugin_version", plugin.getDescription().getVersion());
                
                try (FileOutputStream out = new FileOutputStream(metadataFile.toFile())) {
                    metadata.store(out, "EcoXpert Backup Metadata");
                }
                
                // Clean old backups after successful backup
                cleanOldBackups();
                
                plugin.getLogger().info("Economy backup created successfully: " + backupName);
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create backup", e);
                throw new RuntimeException("Backup creation failed", e);
            }
        }, executorService);
    }
    
    @Override
    public CompletableFuture<Void> restoreFromBackup() {
        return CompletableFuture.runAsync(() -> {
            try {
                // Find most recent backup
                Optional<Path> latestBackup = findLatestBackup();
                if (latestBackup.isEmpty()) {
                    throw new RuntimeException("No backups available for restore");
                }
                
                restoreFromBackupFile(latestBackup.get());
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to restore from backup", e);
                throw new RuntimeException("Backup restore failed", e);
            }
        }, executorService);
    }
    
    @Override
    public CompletableFuture<Void> restoreFromBackup(String backupId) {
        return CompletableFuture.runAsync(() -> {
            try {
                Path backupFile = backupDirectory.resolve(backupId);
                if (!Files.exists(backupFile)) {
                    throw new RuntimeException("Backup not found: " + backupId);
                }
                
                restoreFromBackupFile(backupFile);
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to restore from specific backup", e);
                throw new RuntimeException("Backup restore failed", e);
            }
        }, executorService);
    }
    
    @Override
    public CompletableFuture<ValidationResult> performConsistencyCheck() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<String> errors = new ArrayList<>();
                List<String> warnings = new ArrayList<>();
                
                // Check database connectivity
                if (!dataManager.isHealthy()) {
                    errors.add("Database connectivity issues detected");
                }
                
                // Check for negative balances
                // TODO: Implement specific balance validation queries
                
                // Check transaction log integrity
                if (!Files.exists(transactionLogFile)) {
                    warnings.add("Transaction log file not found");
                } else {
                    // Check if log file is readable
                    try {
                        Files.size(transactionLogFile);
                    } catch (IOException e) {
                        errors.add("Transaction log file is corrupted or unreadable");
                    }
                }
                
                // Check backup integrity
                try {
                    List<Path> backups = findAllBackups();
                    if (backups.isEmpty()) {
                        warnings.add("No backups available");
                    }
                } catch (Exception e) {
                    errors.add("Backup directory issues: " + e.getMessage());
                }
                
                ValidationResult.ValidationSeverity severity;
                boolean valid = errors.isEmpty();
                
                if (!errors.isEmpty()) {
                    severity = ValidationResult.ValidationSeverity.CRITICAL;
                } else if (!warnings.isEmpty()) {
                    severity = ValidationResult.ValidationSeverity.WARNING;
                } else {
                    severity = ValidationResult.ValidationSeverity.HEALTHY;
                }
                
                String summary = String.format("Consistency check completed: %d errors, %d warnings", 
                    errors.size(), warnings.size());
                
                return new ValidationResult(valid, errors, warnings, severity, summary);
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Consistency check failed", e);
                return new ValidationResult(false, 
                    List.of("Consistency check failed: " + e.getMessage()), 
                    List.of(), 
                    ValidationResult.ValidationSeverity.CRITICAL,
                    "Consistency check failed due to system error");
            }
        }, executorService);
    }
    
    @Override
    public void enterReadOnlyMode(String reason) {
        if (readOnlyMode.compareAndSet(false, true)) {
            readOnlyReason.set(reason);
            plugin.getLogger().warning("ECONOMY ENTERING READ-ONLY MODE: " + reason);
            
            // Create emergency backup
            createBackup("Emergency backup before read-only mode: " + reason);
            
            // Notify administrators (if notification system exists)
            // TODO: Implement admin notification system
        }
    }
    
    @Override
    public void exitReadOnlyMode() {
        if (readOnlyMode.compareAndSet(true, false)) {
            String reason = readOnlyReason.getAndSet(null);
            plugin.getLogger().info("Economy exiting read-only mode (was: " + reason + ")");
        }
    }
    
    @Override
    public boolean isReadOnlyMode() {
        return readOnlyMode.get();
    }
    
    @Override
    public String getReadOnlyReason() {
        return readOnlyReason.get();
    }
    
    @Override
    public CompletableFuture<Void> recordTransaction(TransactionRecord transaction) {
        return CompletableFuture.runAsync(() -> {
            // Add to queue for batch processing
            transactionQueue.offer(transaction);
            
            // Prevent queue overflow
            while (transactionQueue.size() > MAX_TRANSACTION_QUEUE) {
                transactionQueue.poll();
            }
        }, executorService);
    }
    
    @Override
    public CompletableFuture<TransactionHistory> getTransactionHistory(UUID playerUuid, int limit) {
        // TODO: Implement transaction history retrieval from log files
        return CompletableFuture.completedFuture(
            new TransactionHistory(playerUuid, List.of(), 0, false)
        );
    }
    
    @Override
    public CompletableFuture<RepairResult> detectAndRepairCorruption() {
        return CompletableFuture.supplyAsync(() -> {
            List<String> issuesFound = new ArrayList<>();
            List<String> actionsPerformed = new ArrayList<>();
            boolean corruptionDetected = false;
            boolean repairSuccessful = true;
            
            try {
                // Perform validation first
                ValidationResult validation = performConsistencyCheck().join();
                
                if (!validation.isValid()) {
                    corruptionDetected = true;
                    issuesFound.addAll(validation.getErrors());
                    
                    // Attempt automatic repair
                    if (validation.getSeverity() == ValidationResult.ValidationSeverity.CRITICAL) {
                        plugin.getLogger().warning("Critical corruption detected, entering read-only mode");
                        enterReadOnlyMode("Automatic corruption detection");
                        actionsPerformed.add("Entered read-only mode due to critical corruption");
                        
                        // Consider automatic restore from backup
                        try {
                            Optional<Path> backup = findLatestBackup();
                            if (backup.isPresent()) {
                                plugin.getLogger().info("Attempting automatic restore from latest backup");
                                restoreFromBackupFile(backup.get());
                                actionsPerformed.add("Restored from latest backup: " + backup.get().getFileName());
                                exitReadOnlyMode();
                                actionsPerformed.add("Exited read-only mode after successful restore");
                            } else {
                                repairSuccessful = false;
                                actionsPerformed.add("No backups available for automatic restore");
                            }
                        } catch (Exception e) {
                            repairSuccessful = false;
                            actionsPerformed.add("Automatic restore failed: " + e.getMessage());
                        }
                    }
                }
                
                String summary = corruptionDetected 
                    ? (repairSuccessful ? "Corruption detected and repaired" : "Corruption detected but repair failed")
                    : "No corruption detected";
                
                return new RepairResult(corruptionDetected, repairSuccessful, issuesFound, actionsPerformed, summary);
                
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Corruption detection failed", e);
                return new RepairResult(false, false, 
                    List.of("Corruption detection failed: " + e.getMessage()),
                    List.of(),
                    "Detection process failed");
            }
        }, executorService);
    }
    
    @Override
    public CompletableFuture<String> createCheckpoint(String operationId) {
        return CompletableFuture.supplyAsync(() -> {
            String checkpointId = UUID.randomUUID().toString();
            CheckpointData checkpoint = new CheckpointData(operationId, Instant.now());
            checkpoints.put(checkpointId, checkpoint);
            
            // Limit checkpoint cache size
            if (checkpoints.size() > 100) {
                // Remove oldest checkpoints
                checkpoints.entrySet().removeIf(entry -> 
                    entry.getValue().timestamp.isBefore(Instant.now().minusSeconds(3600)));
            }
            
            return checkpointId;
        }, executorService);
    }
    
    @Override
    public CompletableFuture<Void> rollbackToCheckpoint(String checkpointId) {
        return CompletableFuture.runAsync(() -> {
            CheckpointData checkpoint = checkpoints.get(checkpointId);
            if (checkpoint == null) {
                throw new RuntimeException("Checkpoint not found: " + checkpointId);
            }
            
            // TODO: Implement actual rollback logic based on checkpoint data
            plugin.getLogger().info("Rolling back to checkpoint: " + checkpointId + 
                " (Operation: " + checkpoint.operationId + ")");
            
        }, executorService);
    }
    
    @Override
    public CompletableFuture<Void> commitCheckpoint(String checkpointId) {
        return CompletableFuture.runAsync(() -> {
            checkpoints.remove(checkpointId);
        }, executorService);
    }
    
    @Override
    public FailsafeHealth getHealthStatus() {
        return lastHealthStatus != null ? lastHealthStatus : 
            new FailsafeHealth(false, Instant.EPOCH, Map.of(), "Not initialized");
    }
    
    @Override
    public CompletableFuture<FailsafeHealth> performHealthCheck() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, String> metrics = new HashMap<>();
            boolean healthy = true;
            String status = "Healthy";
            
            try {
                // Check if initialized
                if (!initialized) {
                    healthy = false;
                    status = "Not initialized";
                } else {
                    // Check read-only mode
                    if (isReadOnlyMode()) {
                        healthy = false;
                        status = "Read-only mode: " + getReadOnlyReason();
                    }
                    
                    // Check database health
                    boolean dbHealthy = dataManager.isHealthy();
                    metrics.put("database_healthy", String.valueOf(dbHealthy));
                    if (!dbHealthy) {
                        healthy = false;
                        status = "Database unhealthy";
                    }
                    
                    // Check backup directory
                    boolean backupDirExists = Files.exists(backupDirectory) && Files.isDirectory(backupDirectory);
                    metrics.put("backup_directory_exists", String.valueOf(backupDirExists));
                    if (!backupDirExists) {
                        healthy = false;
                        status = "Backup directory missing";
                    }
                    
                    // Check transaction queue size
                    int queueSize = transactionQueue.size();
                    metrics.put("transaction_queue_size", String.valueOf(queueSize));
                    if (queueSize > MAX_TRANSACTION_QUEUE * 0.8) {
                        status = "Transaction queue near capacity";
                    }
                    
                    // Check checkpoint count
                    int checkpointCount = checkpoints.size();
                    metrics.put("active_checkpoints", String.valueOf(checkpointCount));
                }
                
            } catch (Exception e) {
                healthy = false;
                status = "Health check failed: " + e.getMessage();
                plugin.getLogger().log(Level.WARNING, "Failsafe health check failed", e);
            }
            
            FailsafeHealth health = new FailsafeHealth(healthy, Instant.now(), metrics, status);
            this.lastHealthStatus = health;
            return health;
        }, executorService);
    }
    
    // Private helper methods
    
    private void restoreFromBackupFile(Path backupFile) throws Exception {
        plugin.getLogger().warning("RESTORING ECONOMY FROM BACKUP: " + backupFile.getFileName());
        
        // Enter read-only mode during restore
        enterReadOnlyMode("Restore operation in progress");
        
        try {
            // Import database from backup
            dataManager.importDatabase(backupFile).join();
            
            plugin.getLogger().info("Economy restore completed successfully");
            
        } finally {
            // Exit read-only mode
            exitReadOnlyMode();
        }
    }
    
    private Optional<Path> findLatestBackup() throws IOException {
        return Files.list(backupDirectory)
            .filter(path -> path.getFileName().toString().endsWith(".backup"))
            .max(Comparator.comparing(path -> {
                try {
                    return Files.getLastModifiedTime(path);
                } catch (IOException e) {
                    return java.nio.file.attribute.FileTime.fromMillis(0);
                }
            }));
    }
    
    private List<Path> findAllBackups() throws IOException {
        return Files.list(backupDirectory)
            .filter(path -> path.getFileName().toString().endsWith(".backup"))
            .sorted(Comparator.comparing(path -> {
                try {
                    return Files.getLastModifiedTime(path);
                } catch (IOException e) {
                    return java.nio.file.attribute.FileTime.fromMillis(0);
                }
            }))
            .toList();
    }
    
    private void cleanOldBackups() {
        try {
            List<Path> backups = findAllBackups();
            if (backups.size() > MAX_BACKUPS) {
                List<Path> toDelete = backups.subList(0, backups.size() - MAX_BACKUPS);
                for (Path backup : toDelete) {
                    Files.deleteIfExists(backup);
                    Files.deleteIfExists(backupDirectory.resolve(backup.getFileName() + ".meta"));
                }
                plugin.getLogger().info("Cleaned " + toDelete.size() + " old backups");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to clean old backups", e);
        }
    }
    
    private void performScheduledBackup() {
        try {
            createBackup("Scheduled automatic backup").join();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Scheduled backup failed", e);
        }
    }
    
    private void performScheduledHealthCheck() {
        try {
            FailsafeHealth health = performHealthCheck().join();
            if (!health.isHealthy()) {
                plugin.getLogger().warning("Failsafe health check failed: " + health.getStatus());
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Scheduled health check failed", e);
        }
    }
    
    private void flushTransactionLog() {
        if (transactionQueue.isEmpty()) {
            return;
        }
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(transactionLogFile.toFile(), true))) {
            TransactionRecord transaction;
            while ((transaction = transactionQueue.poll()) != null) {
                // Write transaction in JSON-like format for easy parsing
                writer.println(String.format("{\"id\":\"%s\",\"timestamp\":\"%s\",\"type\":\"%s\",\"amount\":\"%s\",\"success\":%b}",
                    transaction.getTransactionId(),
                    transaction.getTimestamp().toString(),
                    transaction.getType(),
                    transaction.getAmount().toString(),
                    transaction.isSuccessful()));
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to flush transaction log", e);
        }
    }
    
    private static class CheckpointData {
        final String operationId;
        final Instant timestamp;
        
        CheckpointData(String operationId, Instant timestamp) {
            this.operationId = operationId;
            this.timestamp = timestamp;
        }
    }
}