package com.telegram.dao;

import com.telegram.database.DatabaseManager;
import com.telegram.models.User;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for User operations
 */
public class UserDAO {
    private static final Logger logger = LoggerFactory.getLogger(UserDAO.class);
    private final DatabaseManager dbManager;
    
    public UserDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }
    
    public boolean createUser(User user) {
        String sql = """
            INSERT INTO users (user_id, username, password, profile_name, profile_picture_path, 
                             status, bio, last_seen, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, user.getUserId());
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, hashPassword(user.getPassword()));
            pstmt.setString(4, user.getProfileName());
            pstmt.setString(5, user.getProfilePicturePath());
            pstmt.setString(6, user.getStatus().name());
            pstmt.setString(7, user.getBio());
            pstmt.setTimestamp(8, Timestamp.valueOf(user.getLastSeen()));
            pstmt.setTimestamp(9, Timestamp.valueOf(user.getCreatedAt()));
            
            int result = pstmt.executeUpdate();
            logger.info("User created: {}", user.getUsername());
            return result > 0;
            
        } catch (SQLException e) {
            logger.error("Error creating user: {}", user.getUsername(), e);
            return false;
        }
    }
    
    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
            
        } catch (SQLException e) {
            logger.error("Error finding user by username: {}", username, e);
        }
        
        return null;
    }
    
    public User findById(String userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
            
        } catch (SQLException e) {
            logger.error("Error finding user by ID: {}", userId, e);
        }
        
        return null;
    }
    
    public boolean updateUser(User user) {
        String sql = """
            UPDATE users SET profile_name = ?, profile_picture_path = ?, 
                           status = ?, bio = ?, last_seen = ?,
                           show_last_seen = ?, show_typing_indicators = ?
            WHERE user_id = ?
        """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, user.getProfileName());
            pstmt.setString(2, user.getProfilePicturePath());
            pstmt.setString(3, user.getStatus().name());
            pstmt.setString(4, user.getBio());
            pstmt.setTimestamp(5, Timestamp.valueOf(user.getLastSeen()));
            pstmt.setBoolean(6, user.isShowLastSeen());
            pstmt.setBoolean(7, user.isShowTypingIndicators());
            pstmt.setString(8, user.getUserId());
            
            int result = pstmt.executeUpdate();
            return result > 0;
            
        } catch (SQLException e) {
            logger.error("Error updating user: {}", user.getUserId(), e);
            return false;
        }
    }
    
    public boolean updateUserStatus(String userId, User.UserStatus status) {
        String sql = "UPDATE users SET status = ?, last_seen = CURRENT_TIMESTAMP WHERE user_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, status.name());
            pstmt.setString(2, userId);
            
            int result = pstmt.executeUpdate();
            return result > 0;
            
        } catch (SQLException e) {
            logger.error("Error updating user status: {}", userId, e);
            return false;
        }
    }
    
    public List<User> searchUsers(String searchTerm) {
        String sql = """
            SELECT * FROM users 
            WHERE LOWER(username) LIKE LOWER(?) 
               OR LOWER(profile_name) LIKE LOWER(?)
            ORDER BY username
            LIMIT 50
        """;
        
        List<User> users = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String searchPattern = "%" + searchTerm + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
            
        } catch (SQLException e) {
            logger.error("Error searching users with term: {}", searchTerm, e);
        }
        
        return users;
    }
    
    public boolean authenticateUser(String username, String password) {
        User user = findByUsername(username);
        if (user != null) {
            return checkPassword(password, user.getPassword());
        }
        return false;
    }
    
    public boolean usernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            logger.error("Error checking username existence: {}", username, e);
        }
        
        return false;
    }
    
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User(
            rs.getString("user_id"),
            rs.getString("username"),
            rs.getString("profile_name"),
            rs.getString("profile_picture_path"),
            User.UserStatus.valueOf(rs.getString("status")),
            rs.getString("bio"),
            rs.getTimestamp("last_seen").toLocalDateTime(),
            rs.getTimestamp("created_at").toLocalDateTime()
        );
        user.setPassword(rs.getString("password"));
        
        // Set privacy settings with defaults if columns don't exist yet
        try {
            user.setShowLastSeen(rs.getBoolean("show_last_seen"));
        } catch (SQLException e) {
            user.setShowLastSeen(true); // Default value
        }
        
        try {
            user.setShowTypingIndicators(rs.getBoolean("show_typing_indicators"));
        } catch (SQLException e) {
            user.setShowTypingIndicators(true); // Default value
        }
        
        return user;
    }
    
    private String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }
    
    private boolean checkPassword(String password, String hashedPassword) {
        return BCrypt.checkpw(password, hashedPassword);
    }
}
