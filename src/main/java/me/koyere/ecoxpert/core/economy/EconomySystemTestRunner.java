package me.koyere.ecoxpert.core.economy;

import me.koyere.ecoxpert.EcoXpertPlugin;
import me.koyere.ecoxpert.economy.EconomyManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;

/**
 * Economy System Test Runner
 * 
 * Safe testing utility to verify economy system functionality
 * without disrupting server operations.
 */
public class EconomySystemTestRunner {
    
    private final EcoXpertPlugin plugin;
    
    public EconomySystemTestRunner(EcoXpertPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Run comprehensive economy system tests
     * 
     * @return TestResults with detailed information
     */
    public TestResults runSafeTests() {
        TestResults.Builder results = new TestResults.Builder();
        
        try {
            // Test 1: Conflict Detection
            results.addTest("ConflictDetection", testConflictDetection());
            
            // Test 2: Mode Manager Initialization
            results.addTest("ModeManagerInit", testModeManagerInitialization());
            
            // Test 3: Vault Provider Safety
            results.addTest("VaultProviderSafety", testVaultProviderSafety());
            
            // Test 4: Service Registry Integration
            results.addTest("ServiceRegistryIntegration", testServiceRegistryIntegration());
            
            plugin.getLogger().info("Economy system tests completed");
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error during economy system testing: " + e.getMessage());
            results.addTest("TestRunner", TestResult.failure("Test runner error: " + e.getMessage()));
        }
        
        return results.build();
    }
    
    /**
     * Test conflict detection functionality
     */
    private TestResult testConflictDetection() {
        try {
            EconomyConflictDetector detector = new EconomyConflictDetector(plugin);
            
            // Test provider detection
            EconomyConflictDetector.EconomyProviderStatus status = detector.detectEconomyProvider();
            if (status == null) {
                return TestResult.failure("Provider detection returned null");
            }
            
            // Test plugin detection
            EconomyConflictDetector.InstalledEconomyPlugins plugins = detector.detectInstalledEconomyPlugins();
            if (plugins == null) {
                return TestResult.failure("Plugin detection returned null");
            }
            
            // Test safety check
            boolean canRegister = detector.canSafelyRegister();
            
            return TestResult.success(String.format(
                "Detection successful - Status: %s, Plugins: %d, CanRegister: %s",
                status.getStatus(), plugins.getInstalledCount(), canRegister
            ));
            
        } catch (Exception e) {
            return TestResult.failure("Conflict detection test failed: " + e.getMessage());
        }
    }
    
    /**
     * Test mode manager initialization
     */
    private TestResult testModeManagerInitialization() {
        try {
            // Get economy manager from registry (safely)
            EconomyManager economyManager = plugin.getServiceRegistry().getInstance(EconomyManager.class);
            if (economyManager == null) {
                return TestResult.failure("EconomyManager not available in registry");
            }
            
            // Get vault provider from registry (safely)
            var vaultProvider = plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.economy.VaultEconomyProvider.class);
            if (vaultProvider == null) {
                return TestResult.failure("VaultEconomyProvider not available in registry");
            }
            
            // Test mode manager creation (but don't initialize to avoid conflicts)
            EconomyModeManager modeManager = new EconomyModeManager(plugin, vaultProvider);
            if (modeManager == null) {
                return TestResult.failure("Failed to create EconomyModeManager");
            }
            
            // Verify initial state
            if (modeManager.isInitialized()) {
                return TestResult.failure("ModeManager should not be initialized on creation");
            }
            
            return TestResult.success("ModeManager created successfully - ready for initialization");
            
        } catch (Exception e) {
            return TestResult.failure("Mode manager test failed: " + e.getMessage());
        }
    }
    
    /**
     * Test vault provider safety features
     */
    private TestResult testVaultProviderSafety() {
        try {
            var vaultProvider = plugin.getServiceRegistry().getInstance(me.koyere.ecoxpert.economy.VaultEconomyProvider.class);
            if (vaultProvider == null) {
                return TestResult.failure("VaultEconomyProvider not available");
            }
            
            // Test if it's our implementation with safety features
            if (!(vaultProvider instanceof me.koyere.ecoxpert.economy.VaultEconomyProviderImpl)) {
                return TestResult.failure("VaultProvider is not our enhanced implementation");
            }
            
            me.koyere.ecoxpert.economy.VaultEconomyProviderImpl impl = 
                (me.koyere.ecoxpert.economy.VaultEconomyProviderImpl) vaultProvider;
            
            // Test basic methods exist and work
            boolean registered = impl.isRegistered();
            String name = impl.getName();
            
            return TestResult.success(String.format(
                "VaultProvider basic features working - Registered: %s, Name: %s",
                registered, name
            ));
            
        } catch (Exception e) {
            return TestResult.failure("Vault provider safety test failed: " + e.getMessage());
        }
    }
    
    /**
     * Test service registry integration
     */
    private TestResult testServiceRegistryIntegration() {
        try {
            var serviceRegistry = plugin.getServiceRegistry();
            if (serviceRegistry == null) {
                return TestResult.failure("ServiceRegistry not available");
            }
            
            // Test that all required services are available
            EconomyManager economyManager = serviceRegistry.getInstance(EconomyManager.class);
            var vaultProvider = serviceRegistry.getInstance(me.koyere.ecoxpert.economy.VaultEconomyProvider.class);
            
            if (economyManager == null) {
                return TestResult.failure("EconomyManager not available in registry");
            }
            
            if (vaultProvider == null) {
                return TestResult.failure("VaultEconomyProvider not available in registry");
            }
            
            return TestResult.success("All required services available in registry");
            
        } catch (Exception e) {
            return TestResult.failure("Service registry test failed: " + e.getMessage());
        }
    }
    
    /**
     * Test result representation
     */
    public static class TestResult {
        private final boolean success;
        private final String message;
        
        private TestResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public static TestResult success(String message) {
            return new TestResult(true, message);
        }
        
        public static TestResult failure(String message) {
            return new TestResult(false, message);
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        
        @Override
        public String toString() {
            return (success ? "✓ PASS" : "✗ FAIL") + ": " + message;
        }
    }
    
    /**
     * Collection of test results
     */
    public static class TestResults {
        private final java.util.Map<String, TestResult> results;
        private final int totalTests;
        private final int passedTests;
        
        private TestResults(java.util.Map<String, TestResult> results) {
            this.results = new java.util.HashMap<>(results);
            this.totalTests = results.size();
            this.passedTests = (int) results.values().stream().filter(TestResult::isSuccess).count();
        }
        
        public TestResult getResult(String testName) {
            return results.get(testName);
        }
        
        public java.util.Set<String> getTestNames() {
            return results.keySet();
        }
        
        public int getTotalTests() { return totalTests; }
        public int getPassedTests() { return passedTests; }
        public int getFailedTests() { return totalTests - passedTests; }
        
        public boolean allPassed() {
            return passedTests == totalTests;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Economy System Test Results ===\n");
            sb.append(String.format("Total: %d, Passed: %d, Failed: %d\n", 
                totalTests, passedTests, getFailedTests()));
            
            for (java.util.Map.Entry<String, TestResult> entry : results.entrySet()) {
                sb.append(String.format("%s: %s\n", entry.getKey(), entry.getValue()));
            }
            
            sb.append("=== End Test Results ===");
            return sb.toString();
        }
        
        public static class Builder {
            private final java.util.Map<String, TestResult> results = new java.util.HashMap<>();
            
            public Builder addTest(String name, TestResult result) {
                results.put(name, result);
                return this;
            }
            
            public TestResults build() {
                return new TestResults(results);
            }
        }
    }
}