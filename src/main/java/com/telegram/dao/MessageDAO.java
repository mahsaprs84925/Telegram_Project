package com.telegram.dao;

import com.telegram.database.DatabaseManager;
import com.telegram.models.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Message operations
 */
public class MessageDAO {
    private static final Logger logger = LoggerFactory.getLogger(MessageDAO.class);
    private final DatabaseManager dbManager;
    
    public MessageDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }
    
    public boolean createMessage(Message message) {
        String sql = """
            INSERT INTO messages (message_id, sender_id, receiver_id, content, message_type,
                                timestamp, status, reply_to_message_id, media_path)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, message.getMessageId());
            pstmt.setString(2, message.getSenderId());
            pstmt.setString(3, message.getReceiverId());
            pstmt.setString(4, message.getContent());
            pstmt.setString(5, message.getType().name());
            pstmt.setTimestamp(6, Timestamp.valueOf(message.getTimestamp()));
            pstmt.setString(7, message.getStatus().name());
            pstmt.setString(8, message.getReplyToMessageId());
            pstmt.setString(9, message.getMediaPath());
            
            int result = pstmt.executeUpdate();
            logger.debug("Message created: {}", message.getMessageId());
            return result > 0;
            
        } catch (SQLException e) {
            logger.error("Error creating message: {}", message.getMessageId(), e);
            return false;
        }
    }
    
    public List<Message> getMessagesForPrivateChat(String user1Id, String user2Id, int limit) {
        // Create the expected chat ID format
        String[] users = {user1Id, user2Id};
        java.util.Arrays.sort(users);
        String chatId = "chat_" + users[0] + "_" + users[1];
        
        String sql = """
            SELECT * FROM messages 
            WHERE (receiver_id = ?)
               OR ((sender_id = ? AND receiver_id = ?) 
               OR (sender_id = ? AND receiver_id = ?))
            ORDER BY timestamp DESC
            LIMIT ?
        """;
        
        List<Message> messages = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // First try to find messages with the chat ID format
            pstmt.setString(1, chatId);
            // Also check for legacy format (direct user-to-user)
            pstmt.setString(2, user1Id);
            pstmt.setString(3, user2Id);
            pstmt.setString(4, user2Id);
            pstmt.setString(5, user1Id);
            pstmt.setInt(6, limit);
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs));
            }
            
            // Reverse to get chronological order
            java.util.Collections.reverse(messages);
            
        } catch (SQLException e) {
            logger.error("Error getting messages for private chat: {} - {}", user1Id, user2Id, e);
        }
        
        return messages;
    }
    
    public List<Message> getMessagesForGroup(String groupId, int limit) {
        String sql = """
            SELECT * FROM messages 
            WHERE receiver_id = ?
            ORDER BY timestamp DESC
            LIMIT ?
        """;
        
        List<Message> messages = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, groupId);
            pstmt.setInt(2, limit);
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs));
            }
            
            // Reverse to get chronological order
            java.util.Collections.reverse(messages);
            
        } catch (SQLException e) {
            logger.error("Error getting messages for group: {}", groupId, e);
        }
        
        return messages;
    }
    
    public List<Message> getMessagesForChannel(String channelId, int limit) {
        String sql = """
            SELECT * FROM messages 
            WHERE receiver_id = ?
            ORDER BY timestamp DESC
            LIMIT ?
        """;
        
        List<Message> messages = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, channelId);
            pstmt.setInt(2, limit);
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs));
            }
            
            // Reverse to get chronological order
            java.util.Collections.reverse(messages);
            
        } catch (SQLException e) {
            logger.error("Error getting messages for channel: {}", channelId, e);
        }
        
        return messages;
    }
    
    public boolean updateMessageStatus(String messageId, Message.MessageStatus status) {
        String sql = "UPDATE messages SET status = ? WHERE message_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, status.name());
            pstmt.setString(2, messageId);
            
            int result = pstmt.executeUpdate();
            return result > 0;
            
        } catch (SQLException e) {
            logger.error("Error updating message status: {}", messageId, e);
            return false;
        }
    }
    
    public boolean deleteMessage(String messageId) {
        String sql = "DELETE FROM messages WHERE message_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, messageId);
            int result = pstmt.executeUpdate();
            
            if (result > 0) {
                logger.debug("Message deleted: {}", messageId);
            }
            return result > 0;
            
        } catch (SQLException e) {
            logger.error("Error deleting message: {}", messageId, e);
            return false;
        }
    }
    
    public boolean updateMessage(Message message) {
        String sql = """
            UPDATE messages SET 
                content = ?, 
                status = ?, 
                reply_to_message_id = ?, 
                media_path = ?
            WHERE message_id = ?
        """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, message.getContent());
            pstmt.setString(2, message.getStatus().name());
            pstmt.setString(3, message.getReplyToMessageId());
            pstmt.setString(4, message.getMediaPath());
            pstmt.setString(5, message.getMessageId());
            
            int result = pstmt.executeUpdate();
            
            if (result > 0) {
                logger.debug("Message updated: {}", message.getMessageId());
            }
            return result > 0;
            
        } catch (SQLException e) {
            logger.error("Error updating message: {}", message.getMessageId(), e);
            return false;
        }
    }
    
    public Message getMessageById(String messageId) {
        String sql = """
            SELECT message_id, sender_id, receiver_id, content, message_type,
                   timestamp, status, reply_to_message_id, media_path
            FROM messages 
            WHERE message_id = ?
        """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, messageId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToMessage(rs);
            }
            
        } catch (SQLException e) {
            logger.error("Error getting message by ID: {}", messageId, e);
        }
        
        return null;
    }
    
    public List<Message> searchMessages(String searchTerm, String userId, int limit) {
        String sql = """
            SELECT * FROM messages 
            WHERE (sender_id = ? OR receiver_id = ?)
              AND LOWER(content) LIKE LOWER(?)
            ORDER BY timestamp DESC
            LIMIT ?
        """;
        
        List<Message> messages = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userId);
            pstmt.setString(2, userId);
            pstmt.setString(3, "%" + searchTerm + "%");
            pstmt.setInt(4, limit);
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs));
            }
            
        } catch (SQLException e) {
            logger.error("Error searching messages with term: {}", searchTerm, e);
        }
        
        return messages;
    }
    
    public List<Message> searchMessagesInChat(String searchTerm, String chatId, int limit) {
        String sql = """
            SELECT * FROM messages 
            WHERE receiver_id = ?
              AND LOWER(content) LIKE LOWER(?)
            ORDER BY timestamp DESC
            LIMIT ?
        """;
        
        List<Message> messages = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, chatId);
            pstmt.setString(2, "%" + searchTerm + "%");
            pstmt.setInt(3, limit);
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs));
            }
            
        } catch (SQLException e) {
            logger.error("Error searching messages in chat {} with term: {}", chatId, searchTerm, e);
        }
        
        return messages;
    }
    
    /**
     * Delete all messages between two users in a private chat
     */
    public boolean deleteAllPrivateChatMessages(String user1Id, String user2Id) {
        String sql = """
            DELETE FROM messages 
            WHERE (sender_id = ? AND receiver_id = ?) 
               OR (sender_id = ? AND receiver_id = ?)
        """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, user1Id);
            pstmt.setString(2, user2Id);
            pstmt.setString(3, user2Id);
            pstmt.setString(4, user1Id);
            
            int result = pstmt.executeUpdate();
            logger.info("Deleted {} messages from private chat between {} and {}", result, user1Id, user2Id);
            return true;
            
        } catch (SQLException e) {
            logger.error("Error deleting private chat messages between {} and {}", user1Id, user2Id, e);
            return false;
        }
    }
    
    /**
     * Delete all messages in a group chat
     */
    public boolean deleteAllGroupMessages(String groupId) {
        String sql = "DELETE FROM messages WHERE receiver_id = ? AND message_type = 'GROUP'";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, groupId);
            int result = pstmt.executeUpdate();
            logger.info("Deleted {} messages from group {}", result, groupId);
            return true;
            
        } catch (SQLException e) {
            logger.error("Error deleting group messages for group {}", groupId, e);
            return false;
        }
    }
    
    /**
     * Delete all messages in a channel
     */
    public boolean deleteAllChannelMessages(String channelId) {
        String sql = "DELETE FROM messages WHERE receiver_id = ? AND message_type = 'CHANNEL'";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, channelId);
            int result = pstmt.executeUpdate();
            logger.info("Deleted {} messages from channel {}", result, channelId);
            return true;
            
        } catch (SQLException e) {
            logger.error("Error deleting channel messages for channel {}", channelId, e);
            return false;
        }
    }

    private Message mapResultSetToMessage(ResultSet rs) throws SQLException {
        return new Message(
            rs.getString("message_id"),
            rs.getString("sender_id"),
            rs.getString("receiver_id"),
            rs.getString("content"),
            Message.MessageType.valueOf(rs.getString("message_type")),
            rs.getTimestamp("timestamp").toLocalDateTime(),
            Message.MessageStatus.valueOf(rs.getString("status")),
            rs.getString("reply_to_message_id"),
            rs.getString("media_path")
        );
    }
    
    public List<Message> getUnreadMessagesInChat(String chatId, String userId) {
        String sql = """
            SELECT * FROM messages 
            WHERE receiver_id = ? AND sender_id != ? AND status != 'READ'
            ORDER BY timestamp DESC
        """;
        
        List<Message> messages = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, chatId);
            pstmt.setString(2, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapResultSetToMessage(rs));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error getting unread messages in chat", e);
        }
        
        return messages;
    }
    
    public List<Message> searchMessagesGlobally(String searchTerm, String userId, int limit) {
        String sql = """
            SELECT * FROM messages 
            WHERE (sender_id = ? OR receiver_id = ?) 
            AND content LIKE ?
            ORDER BY timestamp DESC 
            LIMIT ?
        """;
        
        List<Message> messages = new ArrayList<>();
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userId);
            pstmt.setString(2, userId);
            pstmt.setString(3, "%" + searchTerm + "%");
            pstmt.setInt(4, limit);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapResultSetToMessage(rs));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error searching messages globally", e);
        }
        
        return messages;
    }
}
