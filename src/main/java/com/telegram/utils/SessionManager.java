package com.telegram.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Manages user session persistence for automatic login
 */
public class SessionManager {
    private static final Logger logger = LoggerFactory.getLogger(SessionManager.class);
    private static final String SESSION_FILE_NAME = "session.properties";
    private static final String USER_DATA_DIR = System.getProperty("user.home") + "/.telegram_clone";
    private static final String SESSION_FILE_PATH = USER_DATA_DIR + "/" + SESSION_FILE_NAME;
    
    private static SessionManager instance;
    
    // Session tracking for real-time features
    private final Set<String> activeSessions = new HashSet<>();
    
    private SessionManager() {
        // Private constructor for singleton pattern
        createUserDataDirectory();
    }
    
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    
    /**
     * Creates the user data directory if it doesn't exist
     */
    private void createUserDataDirectory() {
        try {
            Path userDataPath = Paths.get(USER_DATA_DIR);
            if (!Files.exists(userDataPath)) {
                Files.createDirectories(userDataPath);
                logger.info("Created user data directory: {}", USER_DATA_DIR);
            }
        } catch (IOException e) {
            logger.error("Failed to create user data directory", e);
        }
    }
    
    /**
     * Saves the login session to persist user login
     * @param username The username to remember
     * @param rememberMe Whether to remember the session (kept for compatibility, but always saves)
     */
    public void saveSession(String username, boolean rememberMe) {
        Properties properties = new Properties();
        properties.setProperty("username", username);
        properties.setProperty("rememberMe", "true");
        properties.setProperty("timestamp", String.valueOf(System.currentTimeMillis()));
        
        try (FileOutputStream fos = new FileOutputStream(SESSION_FILE_PATH)) {
            properties.store(fos, "Telegram Clone Session Data");
            logger.info("Session saved for user: {}", username);
        } catch (IOException e) {
            logger.error("Failed to save session", e);
        }
    }
    
    /**
     * Saves the login session to persist user login
     * @param username The username to remember
     */
    public void saveSession(String username) {
        saveSession(username, true);
    }

    /**
     * Loads the saved session if it exists and is valid
     * @return The saved username if session is valid, null otherwise
     */
    public String loadSession() {
        File sessionFile = new File(SESSION_FILE_PATH);
        if (!sessionFile.exists()) {
            return null;
        }
        
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(SESSION_FILE_PATH)) {
            properties.load(fis);
            
            String username = properties.getProperty("username");
            String rememberMe = properties.getProperty("rememberMe");
            String timestampStr = properties.getProperty("timestamp");
            
            if (username == null || !"true".equals(rememberMe) || timestampStr == null) {
                return null;
            }
            
            // Check if session is not too old (30 days)
            long timestamp = Long.parseLong(timestampStr);
            long currentTime = System.currentTimeMillis();
            long thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000; // 30 days
            
            if (currentTime - timestamp > thirtyDaysInMillis) {
                logger.info("Session expired for user: {}", username);
                clearSession();
                return null;
            }
            
            logger.info("Loaded valid session for user: {}", username);
            return username;
            
        } catch (IOException | NumberFormatException e) {
            logger.error("Failed to load session", e);
            return null;
        }
    }
    
    /**
     * Clears the saved session
     */
    public void clearSession() {
        File sessionFile = new File(SESSION_FILE_PATH);
        if (sessionFile.exists()) {
            if (sessionFile.delete()) {
                logger.info("Session cleared successfully");
            } else {
                logger.warn("Failed to delete session file");
            }
        }
    }
    
    /**
     * Checks if there is a valid saved session
     * @return true if a valid session exists, false otherwise
     */
    public boolean hasValidSession() {
        return loadSession() != null;
    }
    
    /**
     * Add a user to active sessions
     */
    public void addActiveSession(String userId) {
        activeSessions.add(userId);
        logger.info("Added active session for user: {}", userId);
    }
    
    /**
     * Remove a user from active sessions
     */
    public void removeActiveSession(String userId) {
        activeSessions.remove(userId);
        logger.info("Removed active session for user: {}", userId);
    }
    
    /**
     * Get all currently active sessions
     */
    public Set<String> getActiveSessions() {
        return new HashSet<>(activeSessions);
    }
    
    /**
     * Check if a user has an active session
     */
    public boolean hasActiveSession(String userId) {
        return activeSessions.contains(userId);
    }
}
