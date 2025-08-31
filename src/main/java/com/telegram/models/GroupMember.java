package com.telegram.models;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Enhanced model representing a group member with role-based permissions
 */
public class GroupMember {
    private String memberId;
    private String groupId;
    private String userId;
    private String roleId; // Reference to GroupRole
    private LocalDateTime joinedAt;
    private LocalDateTime lastActivity;
    private String invitedBy;
    private String nickname; // Group-specific nickname
    private boolean isMuted;
    private LocalDateTime mutedUntil;
    private boolean isBanned;
    private LocalDateTime bannedUntil;
    private String banReason;
    private LocalDateTime promotedAt;
    private String promotedBy;
    private int messageCount;
    private LocalDateTime lastMessageAt;
    private String customTitle; // Custom title for special members
    
    // Statistics
    private int totalMessages;
    private int totalReactions;
    private int warningsCount;
    
    // Constructors
    public GroupMember() {
        this.memberId = UUID.randomUUID().toString();
        this.joinedAt = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
        this.isMuted = false;
        this.isBanned = false;
        this.messageCount = 0;
        this.totalMessages = 0;
        this.totalReactions = 0;
        this.warningsCount = 0;
    }
    
    public GroupMember(String groupId, String userId, String roleId) {
        this();
        this.groupId = groupId;
        this.userId = userId;
        this.roleId = roleId;
    }
    
    // Additional constructor for DAO usage  
    public GroupMember(String memberId, String groupId, String userId, String roleId) {
        this();
        this.memberId = memberId;
        this.groupId = groupId;
        this.userId = userId;
        this.roleId = roleId;
    }
    
    // Constructor for DAO with User object
    public GroupMember(User user, String groupId) {
        this();
        this.userId = user.getUserId();
        this.groupId = groupId;
    }
    
    // Utility methods
    public boolean isActive() {
        return !isBanned && (mutedUntil == null || mutedUntil.isBefore(LocalDateTime.now()));
    }
    
    public boolean isMutedCurrently() {
        return isMuted || (mutedUntil != null && mutedUntil.isAfter(LocalDateTime.now()));
    }
    
    public boolean isBannedCurrently() {
        return isBanned || (bannedUntil != null && bannedUntil.isAfter(LocalDateTime.now()));
    }
    
    public void incrementMessageCount() {
        this.messageCount++;
        this.totalMessages++;
        this.lastMessageAt = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
    }
    
    public void addReaction() {
        this.totalReactions++;
        this.lastActivity = LocalDateTime.now();
    }
    
    public void addWarning() {
        this.warningsCount++;
    }
    
    public void mute(LocalDateTime until) {
        this.isMuted = true;
        this.mutedUntil = until;
    }
    
    public void unmute() {
        this.isMuted = false;
        this.mutedUntil = null;
    }
    
    public void ban(LocalDateTime until, String reason) {
        this.isBanned = true;
        this.bannedUntil = until;
        this.banReason = reason;
    }
    
    public void unban() {
        this.isBanned = false;
        this.bannedUntil = null;
        this.banReason = null;
    }
    
    public void promoteRole(String newRoleId, String promotedBy) {
        this.roleId = newRoleId;
        this.promotedAt = LocalDateTime.now();
        this.promotedBy = promotedBy;
    }
    
    /**
     * Calculate member activity score based on various factors
     */
    public double getActivityScore() {
        long daysSinceJoined = java.time.temporal.ChronoUnit.DAYS.between(joinedAt, LocalDateTime.now());
        if (daysSinceJoined == 0) daysSinceJoined = 1; // Avoid division by zero
        
        double messagesPerDay = (double) totalMessages / daysSinceJoined;
        double reactionsPerDay = (double) totalReactions / daysSinceJoined;
        
        // Weight messages more than reactions
        return (messagesPerDay * 2.0) + reactionsPerDay;
    }
    
    /**
     * Get member status description
     */
    public String getStatusDescription() {
        if (isBannedCurrently()) {
            return "Banned" + (bannedUntil != null ? " until " + bannedUntil.toLocalDate() : " permanently");
        }
        if (isMutedCurrently()) {
            return "Muted" + (mutedUntil != null ? " until " + mutedUntil.toLocalDate() : " indefinitely");
        }
        if (warningsCount > 0) {
            return "Active (" + warningsCount + " warning" + (warningsCount > 1 ? "s" : "") + ")";
        }
        return "Active";
    }
    
    // Getters and Setters
    public String getMemberId() {
        return memberId;
    }
    
    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }
    
    public String getGroupId() {
        return groupId;
    }
    
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getRoleId() {
        return roleId;
    }
    
    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }
    
    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }
    
    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
    
    public LocalDateTime getLastActivity() {
        return lastActivity;
    }
    
    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }
    
    public String getInvitedBy() {
        return invitedBy;
    }
    
    public void setInvitedBy(String invitedBy) {
        this.invitedBy = invitedBy;
    }
    
    public String getNickname() {
        return nickname;
    }
    
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
    
    public boolean isMuted() {
        return isMuted;
    }
    
    public void setMuted(boolean muted) {
        isMuted = muted;
    }
    
    public LocalDateTime getMutedUntil() {
        return mutedUntil;
    }
    
    public void setMutedUntil(LocalDateTime mutedUntil) {
        this.mutedUntil = mutedUntil;
    }
    
    public boolean isBanned() {
        return isBanned;
    }
    
    public void setBanned(boolean banned) {
        isBanned = banned;
    }
    
    public LocalDateTime getBannedUntil() {
        return bannedUntil;
    }
    
    public void setBannedUntil(LocalDateTime bannedUntil) {
        this.bannedUntil = bannedUntil;
    }
    
    public String getBanReason() {
        return banReason;
    }
    
    public void setBanReason(String banReason) {
        this.banReason = banReason;
    }
    
    public LocalDateTime getPromotedAt() {
        return promotedAt;
    }
    
    public void setPromotedAt(LocalDateTime promotedAt) {
        this.promotedAt = promotedAt;
    }
    
    public String getPromotedBy() {
        return promotedBy;
    }
    
    public void setPromotedBy(String promotedBy) {
        this.promotedBy = promotedBy;
    }
    
    public int getMessageCount() {
        return messageCount;
    }
    
    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }
    
    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }
    
    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }
    
    public String getCustomTitle() {
        return customTitle;
    }
    
    public void setCustomTitle(String customTitle) {
        this.customTitle = customTitle;
    }
    
    public int getTotalMessages() {
        return totalMessages;
    }
    
    public void setTotalMessages(int totalMessages) {
        this.totalMessages = totalMessages;
    }
    
    public int getTotalReactions() {
        return totalReactions;
    }
    
    public void setTotalReactions(int totalReactions) {
        this.totalReactions = totalReactions;
    }
    
    public int getWarningsCount() {
        return warningsCount;
    }
    
    public void setWarningsCount(int warningsCount) {
        this.warningsCount = warningsCount;
    }
    
    // Additional methods for DAO compatibility
    public LocalDateTime getRoleAssignedAt() {
        return promotedAt;
    }
    
    public void setRoleAssignedAt(LocalDateTime roleAssignedAt) {
        this.promotedAt = roleAssignedAt;
    }
    
    public String getRoleAssignedBy() {
        return promotedBy;
    }
    
    public void setRoleAssignedBy(String roleAssignedBy) {
        this.promotedBy = roleAssignedBy;
    }
    
    @Override
    public String toString() {
        return "GroupMember{" +
                "memberId='" + memberId + '\'' +
                ", groupId='" + groupId + '\'' +
                ", userId='" + userId + '\'' +
                ", roleId='" + roleId + '\'' +
                ", joinedAt=" + joinedAt +
                ", totalMessages=" + totalMessages +
                ", isActive=" + isActive() +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        GroupMember that = (GroupMember) obj;
        return memberId.equals(that.memberId);
    }
    
    @Override
    public int hashCode() {
        return memberId.hashCode();
    }
}
