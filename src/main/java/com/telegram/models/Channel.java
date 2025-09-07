package com.telegram.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a channel for broadcasting messages
 */
public class Channel {
    private String channelId;
    private String channelName;
    private String ownerId;
    private String description;
    private String profilePicturePath;
    private List<String> subscriberIds;
    private List<String> adminIds;
    private List<Message> messageHistory;
    private LocalDateTime createdAt;
    private LocalDateTime lastActivity;
    private boolean isPrivate;
    private String inviteLink;
    
    // Constructors
    public Channel() {
        this.channelId = UUID.randomUUID().toString();
        this.subscriberIds = new ArrayList<>();
        this.adminIds = new ArrayList<>();
        this.messageHistory = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
        this.isPrivate = false;
        this.description = "";
        this.inviteLink = ""; // Will be generated when channel name is set
    }
    
    public Channel(String channelName, String ownerId) {
        this();
        this.channelName = channelName;
        this.ownerId = ownerId;
        this.subscriberIds.add(ownerId);
        this.adminIds.add(ownerId);
        this.inviteLink = generateInviteLink(); // Generate after setting channel name
    }
    
    public Channel(String channelId, String channelName, String ownerId, String description,
                  String profilePicturePath, boolean isPrivate, LocalDateTime createdAt, 
                  LocalDateTime lastActivity) {
        this.channelId = channelId;
        this.channelName = channelName;
        this.ownerId = ownerId;
        this.description = description;
        this.profilePicturePath = profilePicturePath;
        this.isPrivate = isPrivate;
        this.subscriberIds = new ArrayList<>();
        this.adminIds = new ArrayList<>();
        this.messageHistory = new ArrayList<>();
        this.createdAt = createdAt;
        this.lastActivity = lastActivity;
        this.inviteLink = generateInviteLink();
    }
    
    // Methods
    public void addMessage(Message message) {
        messageHistory.add(message);
        lastActivity = LocalDateTime.now();
    }
    
    public boolean subscribe(String userId) {
        if (subscriberIds.contains(userId)) {
            return false;
        }
        subscriberIds.add(userId);
        return true;
    }
    
    public boolean unsubscribe(String userId) {
        if (userId.equals(ownerId)) {
            return false; // Owner cannot unsubscribe
        }
        adminIds.remove(userId);
        return subscriberIds.remove(userId);
    }
    
    public boolean addAdmin(String userId) {
        if (!subscriberIds.contains(userId) || adminIds.contains(userId)) {
            return false;
        }
        adminIds.add(userId);
        return true;
    }
    
    public boolean removeAdmin(String userId) {
        if (userId.equals(ownerId)) {
            return false; // Cannot remove owner from admins
        }
        return adminIds.remove(userId);
    }
    
    public boolean isAdmin(String userId) {
        return adminIds.contains(userId);
    }
    
    public boolean isSubscriber(String userId) {
        return subscriberIds.contains(userId);
    }
    
    public boolean isOwner(String userId) {
        return ownerId.equals(userId);
    }
    
    public boolean canPost(String userId) {
        return isAdmin(userId) || isOwner(userId);
    }
    
    private String generateInviteLink() {
        if (channelName == null || channelName.trim().isEmpty()) {
            return "t.me/channel_" + channelId.substring(0, 8);
        }
        return "t.me/" + channelName.toLowerCase().replaceAll("[^a-zA-Z0-9]", "_");
    }
    
    // Getters and Setters
    public String getChannelId() {
        return channelId;
    }
    
    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }
    
    public String getChannelName() {
        return channelName;
    }
    
    public void setChannelName(String channelName) {
        this.channelName = channelName;
        this.inviteLink = generateInviteLink();
    }
    
    public String getOwnerId() {
        return ownerId;
    }
    
    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getProfilePicturePath() {
        return profilePicturePath;
    }
    
    public void setProfilePicturePath(String profilePicturePath) {
        this.profilePicturePath = profilePicturePath;
    }
    
    public List<String> getSubscriberIds() {
        return subscriberIds;
    }
    
    public void setSubscriberIds(List<String> subscriberIds) {
        this.subscriberIds = subscriberIds;
    }
    
    public List<String> getAdminIds() {
        return adminIds;
    }
    
    public void setAdminIds(List<String> adminIds) {
        this.adminIds = adminIds;
    }
    
    public List<Message> getMessageHistory() {
        return messageHistory;
    }
    
    public void setMessageHistory(List<Message> messageHistory) {
        this.messageHistory = messageHistory;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getLastActivity() {
        return lastActivity;
    }
    
    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }
    
    public boolean isPrivate() {
        return isPrivate;
    }
    
    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }
    
    public String getInviteLink() {
        return inviteLink;
    }
    
    public void setInviteLink(String inviteLink) {
        this.inviteLink = inviteLink;
    }
    
    // Subscriber management methods
    public boolean addSubscriber(String userId) {
        if (!subscriberIds.contains(userId)) {
            subscriberIds.add(userId);
            return true;
        }
        return false;
    }
    
    public boolean removeSubscriber(String userId) {
        return subscriberIds.remove(userId);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Channel channel = (Channel) obj;
        return channelId.equals(channel.channelId);
    }
    
    @Override
    public int hashCode() {
        return channelId.hashCode();
    }
    
    @Override
    public String toString() {
        return "Channel{" +
                "channelId='" + channelId + '\'' +
                ", channelName='" + channelName + '\'' +
                ", ownerId='" + ownerId + '\'' +
                ", subscriberCount=" + subscriberIds.size() +
                ", adminCount=" + adminIds.size() +
                ", messageCount=" + messageHistory.size() +
                ", isPrivate=" + isPrivate +
                ", lastActivity=" + lastActivity +
                '}';
    }
}
