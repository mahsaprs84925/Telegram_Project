package com.telegram.models;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a user's subscription to a channel
 */
public class ChannelSubscription {
    private String subscriptionId;
    private String channelId;
    private String userId;
    private LocalDateTime subscribedAt;
    private boolean receiveNotifications;
    private SubscriptionStatus status;
    
    public enum SubscriptionStatus {
        ACTIVE("Active", "Subscription is active"),
        MUTED("Muted", "Subscription is muted - no notifications"),
        PAUSED("Paused", "Subscription is temporarily paused"),
        CANCELLED("Cancelled", "Subscription has been cancelled");
        
        private final String displayName;
        private final String description;
        
        SubscriptionStatus(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    // Constructors
    public ChannelSubscription() {
        this.subscriptionId = UUID.randomUUID().toString();
        this.subscribedAt = LocalDateTime.now();
        this.receiveNotifications = true;
        this.status = SubscriptionStatus.ACTIVE;
    }
    
    public ChannelSubscription(String channelId, String userId) {
        this();
        this.channelId = channelId;
        this.userId = userId;
    }
    
    public ChannelSubscription(String subscriptionId, String channelId, String userId, 
                             LocalDateTime subscribedAt, boolean receiveNotifications, 
                             SubscriptionStatus status) {
        this.subscriptionId = subscriptionId;
        this.channelId = channelId;
        this.userId = userId;
        this.subscribedAt = subscribedAt;
        this.receiveNotifications = receiveNotifications;
        this.status = status;
    }
    
    // Getters and Setters
    public String getSubscriptionId() {
        return subscriptionId;
    }
    
    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }
    
    public String getChannelId() {
        return channelId;
    }
    
    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public LocalDateTime getSubscribedAt() {
        return subscribedAt;
    }
    
    public void setSubscribedAt(LocalDateTime subscribedAt) {
        this.subscribedAt = subscribedAt;
    }
    
    public boolean isReceiveNotifications() {
        return receiveNotifications;
    }
    
    public void setReceiveNotifications(boolean receiveNotifications) {
        this.receiveNotifications = receiveNotifications;
    }
    
    public SubscriptionStatus getStatus() {
        return status;
    }
    
    public void setStatus(SubscriptionStatus status) {
        this.status = status;
    }
    
    // Utility methods
    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE;
    }
    
    public boolean isMuted() {
        return status == SubscriptionStatus.MUTED || !receiveNotifications;
    }
    
    public void mute() {
        this.status = SubscriptionStatus.MUTED;
        this.receiveNotifications = false;
    }
    
    public void unmute() {
        this.status = SubscriptionStatus.ACTIVE;
        this.receiveNotifications = true;
    }
    
    public void cancel() {
        this.status = SubscriptionStatus.CANCELLED;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ChannelSubscription that = (ChannelSubscription) obj;
        return subscriptionId.equals(that.subscriptionId);
    }
    
    @Override
    public int hashCode() {
        return subscriptionId.hashCode();
    }
    
    @Override
    public String toString() {
        return "ChannelSubscription{" +
                "subscriptionId='" + subscriptionId + '\'' +
                ", channelId='" + channelId + '\'' +
                ", userId='" + userId + '\'' +
                ", subscribedAt=" + subscribedAt +
                ", status=" + status +
                ", receiveNotifications=" + receiveNotifications +
                '}';
    }
}
