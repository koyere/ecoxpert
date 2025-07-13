package me.koyere.ecoxpert.core.dependencies;

/**
 * Information about a dependency including download details and verification
 */
public class DependencyInfo {
    private final String name;
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final String downloadUrl;
    private final String sha256Checksum;
    private final boolean required;
    private final String fallbackClass;
    
    public DependencyInfo(String name, String groupId, String artifactId, String version,
                         String downloadUrl, String sha256Checksum, boolean required, String fallbackClass) {
        this.name = name;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.downloadUrl = downloadUrl;
        this.sha256Checksum = sha256Checksum;
        this.required = required;
        this.fallbackClass = fallbackClass;
    }
    
    public String getName() {
        return name;
    }
    
    public String getGroupId() {
        return groupId;
    }
    
    public String getArtifactId() {
        return artifactId;
    }
    
    public String getVersion() {
        return version;
    }
    
    public String getDownloadUrl() {
        return downloadUrl;
    }
    
    public String getSha256Checksum() {
        return sha256Checksum;
    }
    
    public boolean isRequired() {
        return required;
    }
    
    public String getFallbackClass() {
        return fallbackClass;
    }
    
    public String getFileName() {
        return artifactId + "-" + version + ".jar";
    }
    
    public String getCoordinates() {
        return groupId + ":" + artifactId + ":" + version;
    }
}