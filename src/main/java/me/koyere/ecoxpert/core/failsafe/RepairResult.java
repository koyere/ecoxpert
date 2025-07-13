package me.koyere.ecoxpert.core.failsafe;

import java.util.List;

/**
 * Result of data corruption detection and repair operations
 */
public class RepairResult {
    private final boolean corruptionDetected;
    private final boolean repairSuccessful;
    private final List<String> issuesFound;
    private final List<String> actionsPerformed;
    private final String summary;
    
    public RepairResult(boolean corruptionDetected, boolean repairSuccessful,
                       List<String> issuesFound, List<String> actionsPerformed, String summary) {
        this.corruptionDetected = corruptionDetected;
        this.repairSuccessful = repairSuccessful;
        this.issuesFound = List.copyOf(issuesFound);
        this.actionsPerformed = List.copyOf(actionsPerformed);
        this.summary = summary;
    }
    
    public boolean isCorruptionDetected() {
        return corruptionDetected;
    }
    
    public boolean isRepairSuccessful() {
        return repairSuccessful;
    }
    
    public List<String> getIssuesFound() {
        return issuesFound;
    }
    
    public List<String> getActionsPerformed() {
        return actionsPerformed;
    }
    
    public String getSummary() {
        return summary;
    }
}