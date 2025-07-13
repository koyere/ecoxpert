package me.koyere.ecoxpert.core.dependencies;

import java.util.concurrent.CompletableFuture;
import java.util.Set;

/**
 * Manages plugin dependencies with automatic downloading, verification, and fallback handling
 * 
 * Provides enterprise-grade dependency management with:
 * - Automatic dependency downloading from Maven repositories
 * - SHA-256 checksum verification for security
 * - Circuit breaker pattern for fault tolerance
 * - Exponential backoff retry policy
 * - Graceful degradation with fallback options
 * - Health monitoring and self-healing capabilities
 */
public interface DependencyManager {
    
    /**
     * Initialize the dependency manager
     */
    void initialize();
    
    /**
     * Shutdown the dependency manager and cleanup resources
     */
    void shutdown();
    
    /**
     * Ensure a dependency is available, downloading if necessary
     * 
     * @param dependencyName Name of the dependency
     * @return CompletableFuture with the result of the operation
     */
    CompletableFuture<DependencyResult> ensureDependency(String dependencyName);
    
    /**
     * Ensure multiple dependencies are available
     * 
     * @param dependencyNames Set of dependency names
     * @return CompletableFuture with results for all dependencies
     */
    CompletableFuture<Void> ensureAllDependencies(Set<String> dependencyNames);
    
    /**
     * Get the current state of a dependency
     * 
     * @param dependencyName Name of the dependency
     * @return Current state of the dependency
     */
    DependencyState getDependencyState(String dependencyName);
    
    /**
     * Check if a dependency is available and loaded
     * 
     * @param dependencyName Name of the dependency
     * @return true if dependency is loaded and ready to use
     */
    boolean isDependencyAvailable(String dependencyName);
    
    /**
     * Check if a dependency is using fallback mode
     * 
     * @param dependencyName Name of the dependency
     * @return true if dependency is in fallback mode
     */
    boolean isUsingFallback(String dependencyName);
    
    /**
     * Get the class loader for a specific dependency
     * 
     * @param dependencyName Name of the dependency
     * @return ClassLoader for the dependency, or null if not loaded
     */
    ClassLoader getDependencyClassLoader(String dependencyName);
    
    /**
     * Force a health check of all dependencies
     * 
     * @return CompletableFuture that completes when health check is done
     */
    CompletableFuture<Void> performHealthCheck();
    
    /**
     * Get health status of dependency system
     * 
     * @return true if all critical dependencies are healthy
     */
    boolean isHealthy();
    
    /**
     * Clear cache and force re-download of a dependency
     * 
     * @param dependencyName Name of the dependency
     * @return CompletableFuture with the result of the operation
     */
    CompletableFuture<DependencyResult> forceRefresh(String dependencyName);
}