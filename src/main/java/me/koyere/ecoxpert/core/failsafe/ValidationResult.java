package me.koyere.ecoxpert.core.failsafe;

import java.util.List;

/**
 * Result of economy data validation and consistency checks
 */
public class ValidationResult {
    private final boolean valid;
    private final List<String> errors;
    private final List<String> warnings;
    private final ValidationSeverity severity;
    private final String summary;
    
    public ValidationResult(boolean valid, List<String> errors, List<String> warnings, 
                          ValidationSeverity severity, String summary) {
        this.valid = valid;
        this.errors = List.copyOf(errors);
        this.warnings = List.copyOf(warnings);
        this.severity = severity;
        this.summary = summary;
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public List<String> getErrors() {
        return errors;
    }
    
    public List<String> getWarnings() {
        return warnings;
    }
    
    public ValidationSeverity getSeverity() {
        return severity;
    }
    
    public String getSummary() {
        return summary;
    }
    
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
    
    public enum ValidationSeverity {
        HEALTHY,    // No issues found
        WARNING,    // Minor issues that don't affect functionality
        ERROR,      // Serious issues that may affect functionality
        CRITICAL    // Severe issues requiring immediate attention
    }
}