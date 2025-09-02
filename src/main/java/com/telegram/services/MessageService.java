package com.telegram.services;

import com.telegram.dao.MessageDAO;
import com.telegram.models.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Service class for message-related operations
 */
public class MessageService {
    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);
    private static MessageService instance;
    private final MessageDAO messageDAO;
    
    private MessageService() {
        this.messageDAO = new MessageDAO();
    }
    
    public static synchronized MessageService getInstance() {
        if (instance == null) {
            instance = new MessageService();
        }
        return instance;
    }
    
    public boolean sendMessage(Message message) {
        try {
            boolean success = messageDAO.createMessage(message);
            if (success) {
                logger.info("Message sent successfully: {}", message.getMessageId());
            }
            return success;
        } catch (Exception e) {
            logger.error("Error sending message", e);
            return false;
        }
    }
    
    public List<Message> getPrivateChatMessages(String user1Id, String user2Id, int limit) {
        try {
            return messageDAO.getMessagesForPrivateChat(user1Id, user2Id, limit);
        } catch (Exception e) {
            logger.error("Error getting private chat messages", e);
            return List.of();
        }
    }
    
    public List<Message> getGroupMessages(String groupId, int limit) {
        try {
            return messageDAO.getMessagesForGroup(groupId, limit);
        } catch (Exception e) {
            logger.error("Error getting group messages", e);
            return List.of();
        }
    }
    
    public List<Message> getChannelMessages(String channelId, int limit) {
        try {
            return messageDAO.getMessagesForChannel(channelId, limit);
        } catch (Exception e) {
            logger.error("Error getting channel messages", e);
            return List.of();
        }
    }
    
    public boolean updateMessageStatus(String messageId, Message.MessageStatus status) {
        try {
            return messageDAO.updateMessageStatus(messageId, status);
        } catch (Exception e) {
            logger.error("Error updating message status", e);
            return false;
        }
    }
    
    public boolean deleteMessage(String messageId) {
        try {
            boolean success = messageDAO.deleteMessage(messageId);
            if (success) {
                logger.info("Message deleted successfully: {}", messageId);
            }
            return success;
        } catch (Exception e) {
            logger.error("Error deleting message", e);
            return false;
        }
    }
    
    public boolean updateMessage(Message message) {
        try {
            boolean success = messageDAO.updateMessage(message);
            if (success) {
                logger.info("Message updated successfully: {}", message.getMessageId());
            }
            return success;
        } catch (Exception e) {
            logger.error("Error updating message", e);
            return false;
        }
    }
    
    public Message getMessageById(String messageId) {
        try {
            return messageDAO.getMessageById(messageId);
        } catch (Exception e) {
            logger.error("Error getting message by ID", e);
            return null;
        }
    }
    
    public List<Message> getMessagesByIds(java.util.Set<String> messageIds) {
        try {
            java.util.List<Message> messages = new java.util.ArrayList<>();
            for (String messageId : messageIds) {
                Message message = messageDAO.getMessageById(messageId);
                if (message != null) {
                    messages.add(message);
                }
            }
            return messages;
        } catch (Exception e) {
            logger.error("Error getting messages by IDs", e);
            return List.of();
        }
    }
    
    public List<Message> searchMessages(String searchTerm, String userId, int limit) {
        try {
            return messageDAO.searchMessages(searchTerm, userId, limit);
        } catch (Exception e) {
            logger.error("Error searching messages", e);
            return List.of();
        }
    }
    
    public List<Message> searchMessagesInChat(String searchTerm, String chatId, int limit) {
        try {
            return messageDAO.searchMessagesInChat(searchTerm, chatId, limit);
        } catch (Exception e) {
            logger.error("Error searching messages in chat", e);
            return List.of();
        }
    }
    
    /**
     * Clear all messages in a private chat between two users
     */
    public boolean clearPrivateChatHistory(String user1Id, String user2Id) {
        try {
            boolean success = messageDAO.deleteAllPrivateChatMessages(user1Id, user2Id);
            if (success) {
                logger.info("Private chat history cleared between {} and {}", user1Id, user2Id);
            }
            return success;
        } catch (Exception e) {
            logger.error("Error clearing private chat history", e);
            return false;
        }
    }
    
    /**
     * Clear all messages in a group chat
     */
    public boolean clearGroupChatHistory(String groupId) {
        try {
            boolean success = messageDAO.deleteAllGroupMessages(groupId);
            if (success) {
                logger.info("Group chat history cleared for group {}", groupId);
            }
            return success;
        } catch (Exception e) {
            logger.error("Error clearing group chat history", e);
            return false;
        }
    }
    
    /**
     * Clear all messages in a channel
     */
    public boolean clearChannelHistory(String channelId) {
        try {
            boolean success = messageDAO.deleteAllChannelMessages(channelId);
            if (success) {
                logger.info("Channel history cleared for channel {}", channelId);
            }
            return success;
        } catch (Exception e) {
            logger.error("Error clearing channel history", e);
            return false;
        }
    }
    
    /**
     * Get unread messages in a chat for a specific user
     */
    public List<Message> getUnreadMessagesInChat(String chatId, String userId) {
        try {
            return messageDAO.getUnreadMessagesInChat(chatId, userId);
        } catch (Exception e) {
            logger.error("Error getting unread messages in chat", e);
            return List.of();
        }
    }
    
    /**
     * Search messages globally across all chats for a user
     */
    public List<Message> searchMessagesGlobally(String searchTerm, String userId, int limit) {
        try {
            return messageDAO.searchMessagesGlobally(searchTerm, userId, limit);
        } catch (Exception e) {
            logger.error("Error searching messages globally", e);
            return List.of();
        }
    }
}
