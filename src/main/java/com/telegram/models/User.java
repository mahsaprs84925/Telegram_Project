package com.telegram.models;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a user in the Telegram clone application
 */
public class User {
    private String userId;
    private String username;
    private String password;
    private String profileName;
    private String profilePicturePath;
    private UserStatus status;
    private String bio;
    private LocalDateTime lastSeen;
    private LocalDateTime createdAt;
    private boolean showLastSeen = true; // Privacy setting for last seen status
    private boolean showTypingIndicators = true; // Privacy setting for typing indicators
    
    public enum UserStatus {
        ONLINE, OFFLINE, TYPING
    }
    
    // Constructors
    public User() {
        this.userId = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
        this.lastSeen = LocalDateTime.now();
        this.status = UserStatus.OFFLINE;
        this.bio = "";
    }
    
    public User(String username, String password, String profileName) {
        this();
        this.username = username;
        this.password = password;
        this.profileName = profileName;
    }
    
    public User(String userId, String username, String profileName, String profilePicturePath, 
                UserStatus status, String bio, LocalDateTime lastSeen, LocalDateTime createdAt) {
        this.userId = userId;
        this.username = username;
        this.profileName = profileName;
        this.profilePicturePath = profilePicturePath;
        this.status = status;
        this.bio = bio;
        this.lastSeen = lastSeen;
        this.createdAt = createdAt;
        // Initialize privacy settings with default values
        this.showLastSeen = true;
        this.showTypingIndicators = true;
    }
    
    // Getters and Setters
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getProfileName() {
        return profileName;
    }
    
    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }
    
    public String getProfilePicturePath() {
        return profilePicturePath;
    }
    
    public void setProfilePicturePath(String profilePicturePath) {
        this.profilePicturePath = profilePicturePath;
    }
    
    public UserStatus getStatus() {
        return status;
    }
    
    public void setStatus(UserStatus status) {
        this.status = status;
        this.lastSeen = LocalDateTime.now();
    }
    
    public String getBio() {
        return bio;
    }
    
    public void setBio(String bio) {
        this.bio = bio;
    }
    
    public LocalDateTime getLastSeen() {
        return lastSeen;
    }
    
    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public boolean isShowLastSeen() {
        return showLastSeen;
    }
    
    public void setShowLastSeen(boolean showLastSeen) {
        this.showLastSeen = showLastSeen;
    }
    
    public boolean isShowTypingIndicators() {
        return showTypingIndicators;
    }
    
    public void setShowTypingIndicators(boolean showTypingIndicators) {
        this.showTypingIndicators = showTypingIndicators;
    }
    
    // Convenience methods
    public String getDisplayName() {
        return profileName != null && !profileName.trim().isEmpty() ? profileName : username;
    }
    
    public String getPhoneNumber() {
        // For now, return null as phone number is not implemented
        // This can be added as a field later if needed
        return null;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return userId.equals(user.userId);
    }
    
    @Override
    public int hashCode() {
        return userId.hashCode();
    }
    
    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", username='" + username + '\'' +
                ", profileName='" + profileName + '\'' +
                ", status=" + status +
                ", bio='" + bio + '\'' +
                ", lastSeen=" + lastSeen +
                '}';
    }
}
