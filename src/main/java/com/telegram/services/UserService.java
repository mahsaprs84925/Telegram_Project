package com.telegram.services;

import com.telegram.dao.UserDAO;
import com.telegram.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Service class for user-related operations
 */
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static UserService instance;
    private final UserDAO userDAO;
    
    // Password validation regex - at least 8 characters, one uppercase, one lowercase, one digit
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,}$"
    );
    
    // Username validation regex - alphanumeric and underscore, 3-30 characters
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,30}$");
    
    private User currentUser;
    
    private UserService() {
        this.userDAO = new UserDAO();
    }
    
    public static synchronized UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }
    
    public boolean registerUser(String username, String password, String profileName) {
        try {
            // Validate input
            if (!validateUsername(username)) {
                logger.warn("Invalid username format: {}", username);
                return false;
            }
            
            if (!validatePassword(password)) {
                logger.warn("Invalid password format for user: {}", username);
                return false;
            }
            
            if (profileName == null || profileName.trim().isEmpty()) {
                logger.warn("Profile name cannot be empty");
                return false;
            }
            
            // Check if username already exists
            if (userDAO.usernameExists(username)) {
                logger.warn("Username already exists: {}", username);
                return false;
            }
            
            // Create new user
            User user = new User(username, password, profileName.trim());
            boolean success = userDAO.createUser(user);
            
            if (success) {
                logger.info("User registered successfully: {}", username);
            }
            
            return success;
            
        } catch (Exception e) {
            logger.error("Error registering user: {}", username, e);
            return false;
        }
    }
    
    public boolean loginUser(String username, String password) {
        try {
            if (userDAO.authenticateUser(username, password)) {
                currentUser = userDAO.findByUsername(username);
                if (currentUser != null) {
                    // Update user status to online
                    userDAO.updateUserStatus(currentUser.getUserId(), User.UserStatus.ONLINE);
                    currentUser.setStatus(User.UserStatus.ONLINE);
                    logger.info("User logged in successfully: {}", username);
                    return true;
                }
            }
            logger.warn("Login failed for user: {}", username);
            return false;
            
        } catch (Exception e) {
            logger.error("Error logging in user: {}", username, e);
            return false;
        }
    }
    
    public boolean loginUserFromSession(String username) {
        try {
            currentUser = userDAO.findByUsername(username);
            if (currentUser != null) {
                // Update user status to online
                userDAO.updateUserStatus(currentUser.getUserId(), User.UserStatus.ONLINE);
                currentUser.setStatus(User.UserStatus.ONLINE);
                logger.info("User logged in from session: {}", username);
                return true;
            }
            logger.warn("Session login failed - user not found: {}", username);
            return false;
            
        } catch (Exception e) {
            logger.error("Error logging in user from session: {}", username, e);
            return false;
        }
    }
    
    public void logoutUser() {
        if (currentUser != null) {
            userDAO.updateUserStatus(currentUser.getUserId(), User.UserStatus.OFFLINE);
            logger.info("User logged out: {}", currentUser.getUsername());
            currentUser = null;
        }
    }
    
    public boolean updateProfile(String profileName, String bio, String profilePicturePath) {
        if (currentUser == null) {
            return false;
        }
        
        try {
            currentUser.setProfileName(profileName);
            currentUser.setBio(bio);
            currentUser.setProfilePicturePath(profilePicturePath);
            
            boolean success = userDAO.updateUser(currentUser);
            if (success) {
                logger.info("Profile updated for user: {}", currentUser.getUsername());
            }
            return success;
            
        } catch (Exception e) {
            logger.error("Error updating profile for user: {}", currentUser.getUsername(), e);
            return false;
        }
    }
    
    public boolean updateUserStatus(User.UserStatus status) {
        if (currentUser == null) {
            return false;
        }
        
        try {
            boolean success = userDAO.updateUserStatus(currentUser.getUserId(), status);
            if (success) {
                currentUser.setStatus(status);
            }
            return success;
            
        } catch (Exception e) {
            logger.error("Error updating status for user: {}", currentUser.getUsername(), e);
            return false;
        }
    }
    
    public List<User> searchUsers(String searchTerm) {
        try {
            return userDAO.searchUsers(searchTerm);
        } catch (Exception e) {
            logger.error("Error searching users with term: {}", searchTerm, e);
            return List.of();
        }
    }
    
    public User findUserById(String userId) {
        try {
            return userDAO.findById(userId);
        } catch (Exception e) {
            logger.error("Error finding user by ID: {}", userId, e);
            return null;
        }
    }
    
    public User getUserById(String userId) {
        return findUserById(userId);
    }
    
    public User findUserByUsername(String username) {
        try {
            return userDAO.findByUsername(username);
        } catch (Exception e) {
            logger.error("Error finding user by username: {}", username, e);
            return null;
        }
    }
    
    public User getCurrentUser() {
        return currentUser;
    }
    
    public boolean isLoggedIn() {
        return currentUser != null;
    }
    
    private boolean validateUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }
    
    private boolean validatePassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }
    
    public String getPasswordRequirements() {
        return "Password must be at least 8 characters long and contain\nat least one uppercase letter, one lowercase letter, and one digit.";
    }
    
    public String getUsernameRequirements() {
        return "Username must be 3-30 characters long and contain\nonly letters, numbers, and underscores.";
    }
    
    public boolean updateUser(User user) {
        try {
            boolean success = userDAO.updateUser(user);
            if (success && currentUser != null && currentUser.getUserId().equals(user.getUserId())) {
                // Update current user if it's the same user
                this.currentUser = user;
            }
            return success;
        } catch (Exception e) {
            logger.error("Error updating user: {}", user.getUsername(), e);
            return false;
        }
    }
    
    public String getLastSeenTime(String userId) {
        try {
            User user = userDAO.findById(userId);
            if (user != null) {
                // Check if user allows showing last seen
                if (!user.isShowLastSeen()) {
                    return "Last seen recently";
                }
                
                // Check if user is currently online first
                if (isUserOnline(userId)) {
                    return "Online";
                }
                
                // Return last seen timestamp
                java.time.LocalDateTime lastSeen = user.getLastSeen();
                if (lastSeen != null) {
                    java.time.Duration duration = java.time.Duration.between(lastSeen, java.time.LocalDateTime.now());
                    
                    if (duration.toMinutes() < 1) {
                        return "Just now";
                    } else if (duration.toMinutes() < 60) {
                        return duration.toMinutes() + " minutes ago";
                    } else if (duration.toHours() < 24) {
                        return duration.toHours() + " hours ago";
                    } else {
                        return duration.toDays() + " days ago";
                    }
                }
            }
            return "Unknown";
        } catch (Exception e) {
            logger.error("Error getting last seen time for user: {}", userId, e);
            return "Unknown";
        }
    }
    
    private boolean isUserOnline(String userId) {
        try {
            // Check broker online status
            java.io.File onlineFile = new java.io.File("telegram_broker/online.json");
            if (onlineFile.exists()) {
                String content = java.nio.file.Files.readString(onlineFile.toPath());
                return content.contains("\"" + userId + "\"");
            }
        } catch (Exception e) {
            logger.debug("Could not check online status", e);
        }
        return false;
    }
}
