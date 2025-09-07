package com.telegram.dao;

import com.telegram.database.DatabaseManager;
import com.telegram.models.PrivateChat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for PrivateChat operations
 */
public class PrivateChatDAO {
    private static final Logger logger = LoggerFactory.getLogger(PrivateChatDAO.class);
    private final DatabaseManager dbManager;
    
    public PrivateChatDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }
    
    public boolean createPrivateChat(PrivateChat chat) {
        String sql = """
            INSERT INTO private_chats (chat_id, user1_id, user2_id, created_at, last_activity)
            VALUES (?, ?, ?, ?, ?)
        """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, chat.getChatId());
            pstmt.setString(2, chat.getUser1Id());
            pstmt.setString(3, chat.getUser2Id());
            pstmt.setTimestamp(4, Timestamp.valueOf(chat.getCreatedAt()));
            pstmt.setTimestamp(5, Timestamp.valueOf(chat.getLastActivity()));
            
            int result = pstmt.executeUpdate();
            logger.debug("Private chat created: {}", chat.getChatId());
            return result > 0;
            
        } catch (SQLException e) {
            logger.error("Error creating private chat: {}", chat.getChatId(), e);
            return false;
        }
    }
    
    public PrivateChat findByUsers(String user1Id, String user2Id) {
        String sql = """
            SELECT * FROM private_chats 
            WHERE (user1_id = ? AND user2_id = ?) 
               OR (user1_id = ? AND user2_id = ?)
        """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, user1Id);
            pstmt.setString(2, user2Id);
            pstmt.setString(3, user2Id);
            pstmt.setString(4, user1Id);
            
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToPrivateChat(rs);
            }
            
        } catch (SQLException e) {
            logger.error("Error finding private chat by users: {} - {}", user1Id, user2Id, e);
        }
        
        return null;
    }
    
    public PrivateChat findById(String chatId) {
        String sql = "SELECT * FROM private_chats WHERE chat_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, chatId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToPrivateChat(rs);
            }
            
        } catch (SQLException e) {
            logger.error("Error finding private chat by ID: {}", chatId, e);
        }
        
        return null;
    }
    
    public List<PrivateChat> getUserPrivateChats(String userId) {
        String sql = """
            SELECT * FROM private_chats 
            WHERE user1_id = ? OR user2_id = ?
            ORDER BY last_activity DESC
        """;
        
        List<PrivateChat> chats = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userId);
            pstmt.setString(2, userId);
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                chats.add(mapResultSetToPrivateChat(rs));
            }
            
        } catch (SQLException e) {
            logger.error("Error getting user private chats: {}", userId, e);
        }
        
        return chats;
    }
    
    public boolean updateLastActivity(String chatId) {
        String sql = "UPDATE private_chats SET last_activity = CURRENT_TIMESTAMP WHERE chat_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, chatId);
            
            int result = pstmt.executeUpdate();
            return result > 0;
            
        } catch (SQLException e) {
            logger.error("Error updating last activity: {}", chatId, e);
            return false;
        }
    }
    
    public boolean deletePrivateChat(String chatId) {
        String sql = "DELETE FROM private_chats WHERE chat_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, chatId);
            
            int result = pstmt.executeUpdate();
            return result > 0;
            
        } catch (SQLException e) {
            logger.error("Error deleting private chat: {}", chatId, e);
            return false;
        }
    }
    
    private PrivateChat mapResultSetToPrivateChat(ResultSet rs) throws SQLException {
        return new PrivateChat(
            rs.getString("chat_id"),
            rs.getString("user1_id"),
            rs.getString("user2_id"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("last_activity").toLocalDateTime()
        );
    }
}
