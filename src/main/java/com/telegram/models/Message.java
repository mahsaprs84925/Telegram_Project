package com.telegram.models;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a message in the Telegram clone application
 */
public class Message {
    private String messageId;
    private String senderId;
    private String receiverId; // Can be user ID, group ID, or channel ID
    private String content;
    private MessageType type;
    private LocalDateTime timestamp;
    private MessageStatus status;
    private String replyToMessageId; // For message replies
    private String mediaPath; // For images, videos, files
    
    public enum MessageType {
        TEXT, IMAGE, VIDEO, AUDIO, VOICE, FILE, DOCUMENT
    }
    
    public enum MessageStatus {
        SENT, DELIVERED, READ
    }
    
    // Constructors
    public Message() {
        this.messageId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.status = MessageStatus.SENT;
        this.type = MessageType.TEXT;
    }
    
    public Message(String senderId, String receiverId, String content, MessageType type) {
        this();
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.type = type;
    }
    
    public Message(String messageId, String senderId, String receiverId, String content, 
                  MessageType type, LocalDateTime timestamp, MessageStatus status, 
                  String replyToMessageId, String mediaPath) {
        this.messageId = messageId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.type = type;
        this.timestamp = timestamp;
        this.status = status;
        this.replyToMessageId = replyToMessageId;
        this.mediaPath = mediaPath;
    }
    
    // Getters and Setters
    public String getMessageId() {
        return messageId;
    }
    
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }
    
    public String getSenderId() {
        return senderId;
    }
    
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }
    
    public String getReceiverId() {
        return receiverId;
    }
    
    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public MessageType getType() {
        return type;
    }
    
    public void setType(MessageType type) {
        this.type = type;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public MessageStatus getStatus() {
        return status;
    }
    
    public void setStatus(MessageStatus status) {
        this.status = status;
    }
    
    public String getReplyToMessageId() {
        return replyToMessageId;
    }
    
    public void setReplyToMessageId(String replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
    }
    
    public String getMediaPath() {
        return mediaPath;
    }
    
    public void setMediaPath(String mediaPath) {
        this.mediaPath = mediaPath;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Message message = (Message) obj;
        return messageId.equals(message.messageId);
    }
    
    @Override
    public int hashCode() {
        return messageId.hashCode();
    }
    
    @Override
    public String toString() {
        return "Message{" +
                "messageId='" + messageId + '\'' +
                ", senderId='" + senderId + '\'' +
                ", receiverId='" + receiverId + '\'' +
                ", content='" + content + '\'' +
                ", type=" + type +
                ", timestamp=" + timestamp +
                ", status=" + status +
                '}';
    }
}
