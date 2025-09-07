package com.telegram.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Represents a private chat between two users
 */
public class PrivateChat {
    private String chatId;
    private String user1Id;
    private String user2Id;
    private List<Message> messageHistory;
    private LocalDateTime createdAt;
    private LocalDateTime lastActivity;
    
    // Constructors
    public PrivateChat() {
        this.chatId = UUID.randomUUID().toString();
        this.messageHistory = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
    }
    
    public PrivateChat(String user1Id, String user2Id) {
        this();
        this.user1Id = user1Id;
        this.user2Id = user2Id;
        // Create deterministic chat ID based on user IDs
        String[] users = {user1Id, user2Id};
        java.util.Arrays.sort(users);
        this.chatId = "chat_" + users[0] + "_" + users[1];
    }
    
    public PrivateChat(String chatId, String user1Id, String user2Id, 
                      LocalDateTime createdAt, LocalDateTime lastActivity) {
        this.chatId = chatId;
        this.user1Id = user1Id;
        this.user2Id = user2Id;
        this.messageHistory = new ArrayList<>();
        this.createdAt = createdAt;
        this.lastActivity = lastActivity;
    }
    
    // Methods
    public void addMessage(Message message) {
        messageHistory.add(message);
        lastActivity = LocalDateTime.now();
    }
    
    public String getOtherUserId(String currentUserId) {
        return currentUserId.equals(user1Id) ? user2Id : user1Id;
    }
    
    public boolean containsUser(String userId) {
        return user1Id.equals(userId) || user2Id.equals(userId);
    }
    
    // Getters and Setters
    public String getChatId() {
        return chatId;
    }
    
    public void setChatId(String chatId) {
        this.chatId = chatId;
    }
    
    public String getUser1Id() {
        return user1Id;
    }
    
    public void setUser1Id(String user1Id) {
        this.user1Id = user1Id;
    }
    
    public String getUser2Id() {
        return user2Id;
    }
    
    public void setUser2Id(String user2Id) {
        this.user2Id = user2Id;
    }
    
    public List<String> getParticipantIds() {
        return Arrays.asList(user1Id, user2Id);
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
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PrivateChat that = (PrivateChat) obj;
        return chatId.equals(that.chatId);
    }
    
    @Override
    public int hashCode() {
        return chatId.hashCode();
    }
    
    @Override
    public String toString() {
        return "PrivateChat{" +
                "chatId='" + chatId + '\'' +
                ", user1Id='" + user1Id + '\'' +
                ", user2Id='" + user2Id + '\'' +
                ", messageCount=" + messageHistory.size() +
                ", lastActivity=" + lastActivity +
                '}';
    }
}
