package me.koyere.ecoxpert.core.dependencies;

/**
 * Result of a dependency operation
 */
public class DependencyResult {
    private final DependencyState state;
    private final String message;
    private final Throwable exception;
    private final boolean usingFallback;
    
    private DependencyResult(DependencyState state, String message, Throwable exception, boolean usingFallback) {
        this.state = state;
        this.message = message;
        this.exception = exception;
        this.usingFallback = usingFallback;
    }
    
    public static DependencyResult success(DependencyState state) {
        return new DependencyResult(state, "Success", null, false);
    }
    
    public static DependencyResult success(DependencyState state, String message) {
        return new DependencyResult(state, message, null, false);
    }
    
    public static DependencyResult failure(DependencyState state, String message) {
        return new DependencyResult(state, message, null, false);
    }
    
    public static DependencyResult failure(DependencyState state, String message, Throwable exception) {
        return new DependencyResult(state, message, exception, false);
    }
    
    public static DependencyResult fallback(String message) {
        return new DependencyResult(DependencyState.FALLBACK, message, null, true);
    }
    
    public DependencyState getState() {
        return state;
    }
    
    public String getMessage() {
        return message;
    }
    
    public Throwable getException() {
        return exception;
    }
    
    public boolean isSuccess() {
        return state == DependencyState.LOADED || state == DependencyState.FALLBACK;
    }
    
    public boolean isUsingFallback() {
        return usingFallback;
    }
    
    @Override
    public String toString() {
        return "DependencyResult{" +
                "state=" + state +
                ", message='" + message + '\'' +
                ", usingFallback=" + usingFallback +
                '}';
    }
}