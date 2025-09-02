package com.telegram.services;

import com.telegram.models.Message;
import com.telegram.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Local message broker for real-time communication between sessions
 * Simulates a message broker/websocket server for local multi-session chat
 */
public class LocalMessageBroker {
    private static final Logger logger = LoggerFactory.getLogger(LocalMessageBroker.class);
    private static LocalMessageBroker instance;
    
    // Active sessions (userId -> list of listeners)
    private final Map<String, List<MessageListener>> activeListeners = new ConcurrentHashMap<>();
    
    // Typing indicators (chatId -> set of userIds currently typing)
    private final Map<String, Set<String>> typingUsers = new ConcurrentHashMap<>();
    
    // Online users
    private final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();
    
    // Message listeners for different types of events
    public interface MessageListener {
        void onNewMessage(Message message);
        void onTypingStatusChanged(String chatId, String userId, boolean isTyping);
        void onUserOnlineStatusChanged(String userId, boolean isOnline);
        void onMessageRead(String messageId, String userId);
    }
    
    private LocalMessageBroker() {
        logger.info("Local Message Broker initialized");
    }
    
    public static synchronized LocalMessageBroker getInstance() {
        if (instance == null) {
            instance = new LocalMessageBroker();
        }
        return instance;
    }
    
    /**
     * Register a session listener for real-time updates
     */
    public void registerSession(String userId, MessageListener listener) {
        activeListeners.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(listener);
        setUserOnline(userId, true);
        logger.info("Session registered for user: {}", userId);
    }
    
    /**
     * Unregister a session when user logs out or closes app
     */
    public void unregisterSession(String userId, MessageListener listener) {
        List<MessageListener> listeners = activeListeners.get(userId);
        if (listeners != null) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                activeListeners.remove(userId);
                setUserOnline(userId, false);
            }
        }
        logger.info("Session unregistered for user: {}", userId);
    }
    
    /**
     * Broadcast a new message to all relevant sessions
     */
    public void broadcastMessage(Message message) {
        logger.info("Broadcasting message from {} to {}", message.getSenderId(), message.getReceiverId());
        
        // Notify sender's sessions
        notifyUser(message.getSenderId(), listener -> listener.onNewMessage(message));
        
        // Notify receiver's sessions (for private chats)
        if (!message.getSenderId().equals(message.getReceiverId())) {
            notifyUser(message.getReceiverId(), listener -> listener.onNewMessage(message));
        }
        
        // For group chats and channels, we would notify all members
        // This would require additional logic to get member lists
    }
    
    /**
     * Update typing status for a user in a chat
     */
    public void updateTypingStatus(String chatId, String userId, boolean isTyping) {
        Set<String> typingInChat = typingUsers.computeIfAbsent(chatId, k -> ConcurrentHashMap.newKeySet());
        
        if (isTyping) {
            typingInChat.add(userId);
        } else {
            typingInChat.remove(userId);
        }
        
        // Notify all active sessions about typing status change
        // This will broadcast to everyone, but the MainController will filter out
        // the typing user from seeing their own typing indicator
        activeListeners.values().forEach(listeners -> 
            listeners.forEach(listener -> 
                listener.onTypingStatusChanged(chatId, userId, isTyping)
            )
        );
        
        logger.debug("Typing status updated - Chat: {}, User: {}, Typing: {}", chatId, userId, isTyping);
    }
    
    /**
     * Set user online/offline status
     */
    public void setUserOnline(String userId, boolean isOnline) {
        if (isOnline) {
            onlineUsers.add(userId);
        } else {
            onlineUsers.remove(userId);
            // Clear typing status when user goes offline
            typingUsers.values().forEach(typingSet -> typingSet.remove(userId));
        }
        
        // Notify all sessions about online status change
        activeListeners.values().forEach(listeners -> 
            listeners.forEach(listener -> 
                listener.onUserOnlineStatusChanged(userId, isOnline)
            )
        );
        
        logger.info("User {} is now {}", userId, isOnline ? "online" : "offline");
    }
    
    /**
     * Mark message as read by a user
     */
    public void markMessageAsRead(String messageId, String userId) {
        // Notify all sessions about read status
        activeListeners.values().forEach(listeners -> 
            listeners.forEach(listener -> 
                listener.onMessageRead(messageId, userId)
            )
        );
        
        logger.debug("Message {} marked as read by {}", messageId, userId);
    }
    
    /**
     * Get currently online users
     */
    public Set<String> getOnlineUsers() {
        return new HashSet<>(onlineUsers);
    }
    
    /**
     * Get users currently typing in a chat
     */
    public Set<String> getTypingUsers(String chatId) {
        return new HashSet<>(typingUsers.getOrDefault(chatId, Collections.emptySet()));
    }
    
    /**
     * Check if a user is online
     */
    public boolean isUserOnline(String userId) {
        return onlineUsers.contains(userId);
    }
    
    /**
     * Helper method to notify all listeners for a specific user
     */
    private void notifyUser(String userId, java.util.function.Consumer<MessageListener> notification) {
        List<MessageListener> listeners = activeListeners.get(userId);
        if (listeners != null) {
            listeners.forEach(notification);
        }
    }
    
    /**
     * Get active session count
     */
    public int getActiveSessionCount() {
        return activeListeners.size();
    }
    
    /**
     * Clean up inactive sessions (can be called periodically)
     */
    public void cleanupInactiveSessions() {
        activeListeners.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
}
