package com.telegram.services;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.telegram.models.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * File-based message broker for real-time communication between different JVM instances
 * Uses file system for inter-process communication
 */
public class FileBasedMessageBroker {
    private static final Logger logger = LoggerFactory.getLogger(FileBasedMessageBroker.class);
    private static FileBasedMessageBroker instance;
    
    private static final String BROKER_DIR = "telegram_broker";
    private static final String MESSAGES_FILE = "messages.json";
    private static final String TYPING_FILE = "typing.json";
    private static final String ONLINE_FILE = "online.json";
    private static final String SESSION_FILE = "session_%s.json";
    
    private final Path brokerPath;
    private final Path messagesPath;
    private final Path typingPath;
    private final Path onlinePath;
    private final String sessionId;
    private final Path sessionPath;
    
    private final Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
        .create();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    // Active listeners for this JVM instance
    private final Map<String, List<MessageListener>> activeListeners = new ConcurrentHashMap<>();
    
    // Last processed message index to avoid duplicates
    private int lastProcessedMessageIndex = -1;
    private Map<String, Boolean> lastTypingStates = new ConcurrentHashMap<>();
    private Set<String> lastOnlineUsers = ConcurrentHashMap.newKeySet();
    
    public interface MessageListener {
        void onNewMessage(Message message);
        void onTypingStatusChanged(String chatId, String userId, boolean isTyping);
        void onUserOnlineStatusChanged(String userId, boolean isOnline);
        void onMessageRead(String messageId, String userId);
        void onUserProfileUpdated(String userId);
    }
    
    private FileBasedMessageBroker() {
        this.sessionId = UUID.randomUUID().toString();
        this.brokerPath = Paths.get(BROKER_DIR);
        this.messagesPath = brokerPath.resolve(MESSAGES_FILE);
        this.typingPath = brokerPath.resolve(TYPING_FILE);
        this.onlinePath = brokerPath.resolve(ONLINE_FILE);
        this.sessionPath = brokerPath.resolve(String.format(SESSION_FILE, sessionId));
        
        initializeBrokerDirectory();
        startPolling();
        setupShutdownHook();
        
        logger.info("File-based Message Broker initialized with session: {}", sessionId);
    }
    
    public static synchronized FileBasedMessageBroker getInstance() {
        if (instance == null) {
            instance = new FileBasedMessageBroker();
        }
        return instance;
    }
    
    private void initializeBrokerDirectory() {
        try {
            Files.createDirectories(brokerPath);
            
            // Initialize files if they don't exist
            if (!Files.exists(messagesPath)) {
                writeToFile(messagesPath, new ArrayList<BrokerMessage>());
            }
            if (!Files.exists(typingPath)) {
                writeToFile(typingPath, new HashMap<String, Set<String>>());
            }
            if (!Files.exists(onlinePath)) {
                writeToFile(onlinePath, new HashSet<String>());
            }
            
            // Create session file
            writeToFile(sessionPath, new SessionInfo(sessionId, System.currentTimeMillis()));
            
        } catch (Exception e) {
            logger.error("Error initializing broker directory", e);
        }
    }
    
    private void startPolling() {
        // Poll for new messages every 100ms
        scheduler.scheduleWithFixedDelay(this::pollMessages, 100, 100, TimeUnit.MILLISECONDS);
        
        // Poll for typing/online status every 200ms
        scheduler.scheduleWithFixedDelay(this::pollStatus, 200, 200, TimeUnit.MILLISECONDS);
        
        // Cleanup old sessions every 30 seconds
        scheduler.scheduleWithFixedDelay(this::cleanupOldSessions, 30, 30, TimeUnit.SECONDS);
    }
    
    private void setupShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Files.deleteIfExists(sessionPath);
                scheduler.shutdown();
            } catch (Exception e) {
                logger.error("Error during shutdown", e);
            }
        }));
    }
    
    public void registerSession(String userId, MessageListener listener) {
        activeListeners.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(listener);
        setUserOnline(userId, true);
        logger.info("Session registered for user: {} in session: {}", userId, sessionId);
    }
    
    public void unregisterSession(String userId, MessageListener listener) {
        List<MessageListener> listeners = activeListeners.get(userId);
        if (listeners != null) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                activeListeners.remove(userId);
                setUserOnline(userId, false);
            }
        }
        logger.info("Session unregistered for user: {} in session: {}", userId, sessionId);
    }
    
    public void broadcastMessage(Message message) {
        try {
            List<BrokerMessage> messages = readFromFile(messagesPath, new TypeToken<List<BrokerMessage>>(){}.getType());
            messages.add(new BrokerMessage(message, System.currentTimeMillis(), sessionId));
            writeToFile(messagesPath, messages);
            
            logger.info("Message broadcasted from {} to {} via file broker", message.getSenderId(), message.getReceiverId());
        } catch (Exception e) {
            logger.error("Error broadcasting message", e);
        }
    }
    
    public void updateTypingStatus(String chatId, String userId, boolean isTyping) {
        try {
            Map<String, Set<String>> typingUsers = readFromFile(typingPath, new TypeToken<Map<String, Set<String>>>(){}.getType());
            
            Set<String> chatTypingUsers = typingUsers.computeIfAbsent(chatId, k -> new HashSet<>());
            if (isTyping) {
                chatTypingUsers.add(userId);
            } else {
                chatTypingUsers.remove(userId);
            }
            
            writeToFile(typingPath, typingUsers);
            
            logger.debug("Typing status updated: {} in chat {} is {}", userId, chatId, isTyping ? "typing" : "not typing");
        } catch (Exception e) {
            logger.error("Error updating typing status", e);
        }
    }
    
    public void setUserOnline(String userId, boolean isOnline) {
        try {
            Set<String> onlineUsers = readFromFile(onlinePath, new TypeToken<Set<String>>(){}.getType());
            
            if (isOnline) {
                onlineUsers.add(userId);
            } else {
                onlineUsers.remove(userId);
            }
            
            writeToFile(onlinePath, onlineUsers);
            
            logger.info("User {} is now {}", userId, isOnline ? "online" : "offline");
        } catch (Exception e) {
            logger.error("Error updating online status", e);
        }
    }
    
    public boolean isUserOnline(String userId) {
        try {
            Set<String> onlineUsers = readFromFile(onlinePath, new TypeToken<Set<String>>(){}.getType());
            return onlineUsers.contains(userId);
        } catch (Exception e) {
            logger.error("Error checking online status for user: {}", userId, e);
            return false;
        }
    }
    
    private void pollMessages() {
        try {
            List<BrokerMessage> messages = readFromFile(messagesPath, new TypeToken<List<BrokerMessage>>(){}.getType());
            
            for (int i = lastProcessedMessageIndex + 1; i < messages.size(); i++) {
                BrokerMessage brokerMessage = messages.get(i);
                
                // Skip messages from this session to avoid echo
                if (!sessionId.equals(brokerMessage.sessionId)) {
                    logger.debug("Processing new message from session {}: {} -> {}", 
                        brokerMessage.sessionId, brokerMessage.message.getSenderId(), brokerMessage.message.getReceiverId());
                    notifyListeners(brokerMessage.message);
                } else {
                    logger.debug("Skipping own message from session {}", sessionId);
                }
            }
            
            lastProcessedMessageIndex = messages.size() - 1;
            
        } catch (Exception e) {
            logger.error("Error polling messages", e);
        }
    }
    
    private void pollStatus() {
        try {
            // Poll typing status
            Map<String, Set<String>> typingUsers = readFromFile(typingPath, new TypeToken<Map<String, Set<String>>>(){}.getType());
            for (Map.Entry<String, Set<String>> entry : typingUsers.entrySet()) {
                String chatId = entry.getKey();
                for (String userId : entry.getValue()) {
                    String key = chatId + ":" + userId;
                    Boolean lastState = lastTypingStates.get(key);
                    if (lastState == null || !lastState) {
                        lastTypingStates.put(key, true);
                        notifyTypingStatus(chatId, userId, true);
                    }
                }
            }
            
            // Check for users who stopped typing
            Iterator<Map.Entry<String, Boolean>> iter = lastTypingStates.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<String, Boolean> entry = iter.next();
                String[] parts = entry.getKey().split(":");
                if (parts.length == 2) {
                    String chatId = parts[0];
                    String userId = parts[1];
                    Set<String> currentlyTyping = typingUsers.getOrDefault(chatId, Collections.emptySet());
                    if (!currentlyTyping.contains(userId)) {
                        iter.remove();
                        notifyTypingStatus(chatId, userId, false);
                    }
                }
            }
            
            // Poll online status
            Set<String> onlineUsers = readFromFile(onlinePath, new TypeToken<Set<String>>(){}.getType());
            Set<String> newOnline = new HashSet<>(onlineUsers);
            newOnline.removeAll(lastOnlineUsers);
            Set<String> wentOffline = new HashSet<>(lastOnlineUsers);
            wentOffline.removeAll(onlineUsers);
            
            for (String userId : newOnline) {
                notifyOnlineStatus(userId, true);
            }
            for (String userId : wentOffline) {
                notifyOnlineStatus(userId, false);
            }
            
            lastOnlineUsers = new HashSet<>(onlineUsers);
            
        } catch (Exception e) {
            logger.error("Error polling status", e);
        }
    }
    
    private void cleanupOldSessions() {
        try {
            long cutoff = System.currentTimeMillis() - 60000; // 1 minute
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(brokerPath, "session_*.json")) {
                for (Path sessionFile : stream) {
                    try {
                        SessionInfo session = readFromFile(sessionFile, SessionInfo.class);
                        if (session.timestamp < cutoff) {
                            Files.deleteIfExists(sessionFile);
                            logger.debug("Cleaned up old session: {}", sessionFile.getFileName());
                        }
                    } catch (Exception e) {
                        // If we can't read the session file, delete it
                        Files.deleteIfExists(sessionFile);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error cleaning up sessions", e);
        }
    }
    
    private void notifyListeners(Message message) {
        // Notify sender's sessions
        notifyUser(message.getSenderId(), listener -> listener.onNewMessage(message));
        
        // Notify receiver's sessions
        String receiverId = message.getReceiverId();
        
        // Handle different chat types
        if (receiverId.startsWith("chat_")) {
            // This is a private chat with format: chat_userId1_userId2
            String[] parts = receiverId.split("_");
            if (parts.length == 3) {
                String user1Id = parts[1];
                String user2Id = parts[2];
                
                // Notify both users in the private chat (except sender)
                if (!message.getSenderId().equals(user1Id)) {
                    notifyUser(user1Id, listener -> listener.onNewMessage(message));
                }
                if (!message.getSenderId().equals(user2Id)) {
                    notifyUser(user2Id, listener -> listener.onNewMessage(message));
                }
            }
        } else if (receiverId.startsWith("group_")) {
            // This is a group chat - notify all group members (implementation would need group member lookup)
            // For now, just log it
            logger.debug("Group message notification not yet implemented for: {}", receiverId);
        } else if (receiverId.startsWith("channel_")) {
            // This is a channel - notify all subscribers (implementation would need subscriber lookup)
            // For now, just log it
            logger.debug("Channel message notification not yet implemented for: {}", receiverId);
        } else {
            // Direct user-to-user message (old format)
            if (!message.getSenderId().equals(receiverId)) {
                notifyUser(receiverId, listener -> listener.onNewMessage(message));
            }
        }
    }
    
    private void notifyTypingStatus(String chatId, String userId, boolean isTyping) {
        // Notify all active users except the one typing
        for (Map.Entry<String, List<MessageListener>> entry : activeListeners.entrySet()) {
            String activeUserId = entry.getKey();
            if (!activeUserId.equals(userId)) {
                for (MessageListener listener : entry.getValue()) {
                    listener.onTypingStatusChanged(chatId, userId, isTyping);
                }
            }
        }
    }
    
    private void notifyOnlineStatus(String userId, boolean isOnline) {
        for (List<MessageListener> listeners : activeListeners.values()) {
            for (MessageListener listener : listeners) {
                listener.onUserOnlineStatusChanged(userId, isOnline);
            }
        }
    }
    
    private void notifyUser(String userId, java.util.function.Consumer<MessageListener> action) {
        List<MessageListener> listeners = activeListeners.get(userId);
        if (listeners != null && !listeners.isEmpty()) {
            logger.debug("Notifying {} listeners for user {}", listeners.size(), userId);
            for (MessageListener listener : listeners) {
                try {
                    action.accept(listener);
                } catch (Exception e) {
                    logger.error("Error notifying listener for user {}", userId, e);
                }
            }
        } else {
            logger.debug("No active listeners found for user {}", userId);
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T> T readFromFile(Path path, Type type) throws IOException {
        if (!Files.exists(path)) {
            if (type.equals(new TypeToken<List<BrokerMessage>>(){}.getType())) {
                return (T) new ArrayList<BrokerMessage>();
            } else if (type.equals(new TypeToken<Map<String, Set<String>>>(){}.getType())) {
                return (T) new HashMap<String, Set<String>>();
            } else if (type.equals(new TypeToken<Set<String>>(){}.getType())) {
                return (T) new HashSet<String>();
            }
        }
        
        try {
            String content = Files.readString(path);
            if (content.trim().isEmpty()) {
                if (type.equals(new TypeToken<List<BrokerMessage>>(){}.getType())) {
                    return (T) new ArrayList<BrokerMessage>();
                } else if (type.equals(new TypeToken<Map<String, Set<String>>>(){}.getType())) {
                    return (T) new HashMap<String, Set<String>>();
                } else if (type.equals(new TypeToken<Set<String>>(){}.getType())) {
                    return (T) new HashSet<String>();
                }
            }
            return gson.fromJson(content, type);
        } catch (Exception e) {
            logger.warn("Error reading file {}, returning empty collection", path, e);
            if (type.equals(new TypeToken<List<BrokerMessage>>(){}.getType())) {
                return (T) new ArrayList<BrokerMessage>();
            } else if (type.equals(new TypeToken<Map<String, Set<String>>>(){}.getType())) {
                return (T) new HashMap<String, Set<String>>();
            } else if (type.equals(new TypeToken<Set<String>>(){}.getType())) {
                return (T) new HashSet<String>();
            }
            throw e;
        }
    }
    
    private <T> T readFromFile(Path path, Class<T> clazz) throws IOException {
        String content = Files.readString(path);
        return gson.fromJson(content, clazz);
    }
    
    private void writeToFile(Path path, Object data) throws IOException {
        String json = gson.toJson(data);
        Files.writeString(path, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    // Data classes for broker communication
    private static class BrokerMessage {
        public Message message;
        public long timestamp;
        public String sessionId;
        
        public BrokerMessage(Message message, long timestamp, String sessionId) {
            this.message = message;
            this.timestamp = timestamp;
            this.sessionId = sessionId;
        }
    }
    
    private static class SessionInfo {
        public String sessionId;
        public long timestamp;
        
        public SessionInfo(String sessionId, long timestamp) {
            this.sessionId = sessionId;
            this.timestamp = timestamp;
        }
    }
    
    /**
     * Custom adapter for LocalDateTime to handle JSON serialization/deserialization
     */
    private static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        
        @Override
        public JsonElement serialize(LocalDateTime localDateTime, Type type, JsonSerializationContext context) {
            return new JsonPrimitive(localDateTime.format(formatter));
        }
        
        @Override
        public LocalDateTime deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
            return LocalDateTime.parse(jsonElement.getAsString(), formatter);
        }
    }

    public void logActiveSessionInfo() {
        logger.info("Active listeners count: {}", activeListeners.size());
        for (Map.Entry<String, List<MessageListener>> entry : activeListeners.entrySet()) {
            logger.info("User {} has {} listeners", entry.getKey(), entry.getValue().size());
        }
        logger.info("Session ID: {}", sessionId);
    }
    
    /**
     * Mark message as read and send read receipt
     */
    public void markMessageAsRead(String messageId, String userId) {
        try {
            // Store read receipt information (you could persist this in database)
            logger.debug("Message {} marked as read by user {}", messageId, userId);
            
            // Notify other sessions about read receipt
            for (List<MessageListener> listeners : activeListeners.values()) {
                for (MessageListener listener : listeners) {
                    listener.onMessageRead(messageId, userId);
                }
            }
        } catch (Exception e) {
            logger.error("Error marking message as read", e);
        }
    }
    
    /**
     * Broadcast profile update to all sessions
     */
    public void broadcastProfileUpdate(String userId) {
        try {
            logger.info("Broadcasting profile update for user: {}", userId);
            
            // Notify all active listeners about the profile update
            for (Map.Entry<String, List<MessageListener>> entry : activeListeners.entrySet()) {
                for (MessageListener listener : entry.getValue()) {
                    try {
                        listener.onUserProfileUpdated(userId);
                    } catch (Exception e) {
                        logger.error("Error notifying listener about profile update", e);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Error broadcasting profile update", e);
        }
    }
    
    /**
     * Notify that a message has been read by a user
     */
    public void notifyMessageRead(String messageId, String readerId) {
        try {
            logger.info("Notifying message read: {} by {}", messageId, readerId);
            
            // Notify all active listeners about the message being read
            for (Map.Entry<String, List<MessageListener>> entry : activeListeners.entrySet()) {
                for (MessageListener listener : entry.getValue()) {
                    try {
                        listener.onMessageRead(messageId, readerId);
                    } catch (Exception e) {
                        logger.error("Error notifying listener about message read", e);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Error notifying message read", e);
        }
    }
}
