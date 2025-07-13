package me.koyere.ecoxpert.core.dependencies;

/**
 * Represents the current state of a dependency
 */
public enum DependencyState {
    /**
     * Dependency is missing and needs to be downloaded
     */
    MISSING,
    
    /**
     * Dependency is currently being downloaded
     */
    DOWNLOADING,
    
    /**
     * Download failed after retries
     */
    FAILED,
    
    /**
     * Dependency downloaded but not yet verified
     */
    DOWNLOADED,
    
    /**
     * Dependency loaded and verified successfully
     */
    LOADED,
    
    /**
     * Dependency verification failed (checksum mismatch)
     */
    CORRUPTED,
    
    /**
     * Dependency unavailable, using fallback
     */
    FALLBACK
}