package com.telegram.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a group chat with multiple members
 */
public class GroupChat {
    private String groupId;
    private String groupName;
    private String creatorId;
    private String description;
    private String profilePicturePath;
    private List<String> memberIds;
    private List<String> adminIds;
    private List<Message> messageHistory;
    private LocalDateTime createdAt;
    private LocalDateTime lastActivity;
    private int maxMembers;
    
    // Constructors
    public GroupChat() {
        this.groupId = UUID.randomUUID().toString();
        this.memberIds = new ArrayList<>();
        this.adminIds = new ArrayList<>();
        this.messageHistory = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
        this.maxMembers = 200; // Default max members
        this.description = "";
    }
    
    public GroupChat(String groupName, String creatorId) {
        this();
        this.groupName = groupName;
        this.creatorId = creatorId;
        this.memberIds.add(creatorId);
        this.adminIds.add(creatorId);
    }
    
    public GroupChat(String groupId, String groupName, String creatorId, String description,
                    String profilePicturePath, LocalDateTime createdAt, LocalDateTime lastActivity) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.creatorId = creatorId;
        this.description = description;
        this.profilePicturePath = profilePicturePath;
        this.memberIds = new ArrayList<>();
        this.adminIds = new ArrayList<>();
        this.messageHistory = new ArrayList<>();
        this.createdAt = createdAt;
        this.lastActivity = lastActivity;
        this.maxMembers = 200;
    }
    
    // Methods
    public void addMessage(Message message) {
        messageHistory.add(message);
        lastActivity = LocalDateTime.now();
    }
    
    public boolean addMember(String userId) {
        if (memberIds.size() >= maxMembers || memberIds.contains(userId)) {
            return false;
        }
        memberIds.add(userId);
        return true;
    }
    
    public boolean removeMember(String userId) {
        if (userId.equals(creatorId)) {
            return false; // Cannot remove creator
        }
        adminIds.remove(userId);
        return memberIds.remove(userId);
    }
    
    public boolean addAdmin(String userId) {
        if (!memberIds.contains(userId) || adminIds.contains(userId)) {
            return false;
        }
        adminIds.add(userId);
        return true;
    }
    
    public boolean removeAdmin(String userId) {
        if (userId.equals(creatorId)) {
            return false; // Cannot remove creator from admins
        }
        return adminIds.remove(userId);
    }
    
    public boolean isAdmin(String userId) {
        return adminIds.contains(userId);
    }
    
    public boolean isMember(String userId) {
        return memberIds.contains(userId);
    }
    
    public boolean isCreator(String userId) {
        return creatorId.equals(userId);
    }
    
    // Getters and Setters
    public String getGroupId() {
        return groupId;
    }
    
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
    
    public String getGroupName() {
        return groupName;
    }
    
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
    
    public String getCreatorId() {
        return creatorId;
    }
    
    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
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
    
    public List<String> getMemberIds() {
        return memberIds;
    }
    
    public void setMemberIds(List<String> memberIds) {
        this.memberIds = memberIds;
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
    
    public int getMaxMembers() {
        return maxMembers;
    }
    
    public void setMaxMembers(int maxMembers) {
        this.maxMembers = maxMembers;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        GroupChat groupChat = (GroupChat) obj;
        return groupId.equals(groupChat.groupId);
    }
    
    @Override
    public int hashCode() {
        return groupId.hashCode();
    }
    
    @Override
    public String toString() {
        return "GroupChat{" +
                "groupId='" + groupId + '\'' +
                ", groupName='" + groupName + '\'' +
                ", creatorId='" + creatorId + '\'' +
                ", memberCount=" + memberIds.size() +
                ", adminCount=" + adminIds.size() +
                ", messageCount=" + messageHistory.size() +
                ", lastActivity=" + lastActivity +
                '}';
    }
}
