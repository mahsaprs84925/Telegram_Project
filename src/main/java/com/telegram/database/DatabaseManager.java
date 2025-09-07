package com.telegram.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Singleton class for managing database connections and initialization
 */
public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static DatabaseManager instance;

    // Database configuration
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/telegram_clone";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "Telegram123!";
    private Connection connection;

    private DatabaseManager() {
        // Private constructor for singleton pattern
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("org.postgresql.Driver");
                connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
                logger.info("Database connection established successfully");
            } catch (ClassNotFoundException e) {
                logger.error("PostgreSQL JDBC driver not found", e);
                throw new SQLException("PostgreSQL JDBC driver not found", e);
            } catch (SQLException e) {
                logger.error("Failed to establish database connection", e);
                throw e;
            }
        }
        return connection;
    }

    public void initializeDatabase() {
        try {
            createTables();
            loadEnhancedSchema(); // Load the enhanced schema for groups and channels
            logger.info("Database initialized successfully");
        } catch (SQLException e) {
            logger.error("Failed to initialize database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = getConnection().createStatement()) {

            // Create Users table
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS users (
                            user_id VARCHAR(255) PRIMARY KEY,
                            username VARCHAR(50) UNIQUE NOT NULL,
                            password VARCHAR(255) NOT NULL,
                            profile_name VARCHAR(100) NOT NULL,
                            profile_picture_path VARCHAR(500),
                            status VARCHAR(20) DEFAULT 'OFFLINE',
                            bio TEXT DEFAULT '',
                            last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            show_last_seen BOOLEAN DEFAULT TRUE,
                            show_typing_indicators BOOLEAN DEFAULT TRUE
                        )
                    """);

            // Create Messages table
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS messages (
                            message_id VARCHAR(255) PRIMARY KEY,
                            sender_id VARCHAR(255) NOT NULL,
                            receiver_id VARCHAR(255) NOT NULL,
                            content TEXT,
                            message_type VARCHAR(20) DEFAULT 'TEXT',
                            timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            status VARCHAR(20) DEFAULT 'SENT',
                            reply_to_message_id VARCHAR(255),
                            media_path VARCHAR(500),
                            FOREIGN KEY (sender_id) REFERENCES users(user_id) ON DELETE CASCADE
                        )
                    """);

            // Create Private Chats table
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS private_chats (
                            chat_id VARCHAR(255) PRIMARY KEY,
                            user1_id VARCHAR(255) NOT NULL,
                            user2_id VARCHAR(255) NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (user1_id) REFERENCES users(user_id) ON DELETE CASCADE,
                            FOREIGN KEY (user2_id) REFERENCES users(user_id) ON DELETE CASCADE,
                            UNIQUE(user1_id, user2_id)
                        )
                    """);

            // Create Group Chats table
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS group_chats (
                            group_id VARCHAR(255) PRIMARY KEY,
                            group_name VARCHAR(100) NOT NULL,
                            creator_id VARCHAR(255) NOT NULL,
                            description TEXT DEFAULT '',
                            profile_picture_path VARCHAR(500),
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            max_members INTEGER DEFAULT 200,
                            FOREIGN KEY (creator_id) REFERENCES users(user_id) ON DELETE CASCADE
                        )
                    """);

            // Create Group Members table
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS group_members (
                            group_id VARCHAR(255),
                            user_id VARCHAR(255),
                            is_admin BOOLEAN DEFAULT FALSE,
                            joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (group_id, user_id),
                            FOREIGN KEY (group_id) REFERENCES group_chats(group_id) ON DELETE CASCADE,
                            FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
                        )
                    """);

            // Create Channels table
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS channels (
                            channel_id VARCHAR(255) PRIMARY KEY,
                            channel_name VARCHAR(100) NOT NULL,
                            owner_id VARCHAR(255) NOT NULL,
                            description TEXT DEFAULT '',
                            profile_picture_path VARCHAR(500),
                            is_private BOOLEAN DEFAULT FALSE,
                            invite_link VARCHAR(255),
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (owner_id) REFERENCES users(user_id) ON DELETE CASCADE
                        )
                    """);

            // Create Channel Subscribers table
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS channel_subscribers (
                            channel_id VARCHAR(255),
                            user_id VARCHAR(255),
                            is_admin BOOLEAN DEFAULT FALSE,
                            subscribed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (channel_id, user_id),
                            FOREIGN KEY (channel_id) REFERENCES channels(channel_id) ON DELETE CASCADE,
                            FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
                        )
                    """);

            // Create indexes for better performance
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_messages_receiver ON messages(receiver_id)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_messages_timestamp ON messages(timestamp)");
            stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_users_username ON users(username)");
            stmt.executeUpdate(
                    "CREATE INDEX IF NOT EXISTS idx_private_chats_users ON private_chats(user1_id, user2_id)");

            // Add privacy columns to existing users table if they don't exist
            try {
                stmt.executeUpdate("ALTER TABLE users ADD COLUMN IF NOT EXISTS show_last_seen BOOLEAN DEFAULT TRUE");
                stmt.executeUpdate(
                        "ALTER TABLE users ADD COLUMN IF NOT EXISTS show_typing_indicators BOOLEAN DEFAULT TRUE");
                logger.info("Privacy settings columns added to users table");
            } catch (SQLException e) {
                logger.debug("Privacy columns may already exist: {}", e.getMessage());
            }

            logger.info("All database tables created successfully");
        }
    }

    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                logger.info("Database connection closed");
            } catch (SQLException e) {
                logger.error("Error closing database connection", e);
            }
        }
    }

    /**
     * Creates test users for development and testing purposes
     */
    public void createTestUsers() {
        try {
            com.telegram.dao.UserDAO userDAO = new com.telegram.dao.UserDAO();

            // Check if test users already exist
            if (userDAO.usernameExists("testuser")) {
                logger.info("Test users already exist");
                return;
            }

            // Create test user 1
            com.telegram.models.User testUser1 = new com.telegram.models.User("testuser", "password123", "Test User");
            testUser1.setBio("This is a test user account for development");
            userDAO.createUser(testUser1);

            // Create test user 2
            com.telegram.models.User testUser2 = new com.telegram.models.User("alice", "password123", "Alice Johnson");
            testUser2.setBio("Hello! I'm Alice, another test user");
            userDAO.createUser(testUser2);

            // Create test user 3
            com.telegram.models.User testUser3 = new com.telegram.models.User("bob", "password123", "Bob Smith");
            testUser3.setBio("Hey there! I'm Bob");
            userDAO.createUser(testUser3);

            logger.info("Test users created successfully");
            logger.info("You can now login with:");
            logger.info("  Username: testuser, Password: password123");
            logger.info("  Username: alice, Password: password123");
            logger.info("  Username: bob, Password: password123");

        } catch (Exception e) {
            logger.error("Error creating test users", e);
        }
    }

    /**
     * Load enhanced schema for groups and channels
     */
    private void loadEnhancedSchema() throws SQLException {
        try {
            // Read and execute the enhanced schema SQL file
            String schemaPath = "database/enhanced_schema.sql";
            logger.info("Attempting to load enhanced schema from: {}", schemaPath);

            try (java.io.InputStream inputStream = getClass().getClassLoader().getResourceAsStream(schemaPath)) {
                if (inputStream == null) {
                    logger.warn("Enhanced schema not found in classpath, trying direct file path");
                    // Try direct file path
                    java.nio.file.Path path = java.nio.file.Paths.get(schemaPath);
                    if (java.nio.file.Files.exists(path)) {
                        logger.info("Found enhanced schema at direct path: {}", path.toAbsolutePath());
                        String sql = java.nio.file.Files.readString(path);
                        executeSchemaSQL(sql);
                    } else {
                        logger.warn("Enhanced schema file not found at: {}", schemaPath);
                        logger.info("Creating essential tables manually as fallback");
                        createChannelPostsTable(); // Create the table manually if file not found
                    }
                } else {
                    logger.info("Found enhanced schema in classpath, reading content");
                    String sql = new String(inputStream.readAllBytes());
                    logger.info("Enhanced schema content length: {} characters", sql.length());
                    executeSchemaSQL(sql);
                }
            }
            logger.info("Enhanced schema loading process completed");
        } catch (Exception e) {
            logger.error("Error loading enhanced schema", e);
            // Fallback: create essential tables manually
            logger.info("Using fallback table creation");
            createChannelPostsTable();
        }
    }

    private void executeSchemaSQL(String sql) throws SQLException {
        try (Statement stmt = getConnection().createStatement()) {
            // Split SQL into individual statements and execute them
            String[] statements = sql.split(";");
            logger.info("Executing {} SQL statements from enhanced schema", statements.length);

            int successCount = 0;
            int errorCount = 0;

            // First pass: Create all tables
            for (String statement : statements) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("--") &&
                        (trimmed.toUpperCase().startsWith("CREATE TABLE") ||
                                trimmed.toUpperCase().startsWith("ALTER TABLE"))) {
                    try {
                        stmt.executeUpdate(trimmed);
                        successCount++;
                        logger.debug("Successfully executed table statement");
                    } catch (SQLException e) {
                        errorCount++;
                        logger.warn("Error executing table statement: {} - SQL: {}", e.getMessage(),
                                trimmed.substring(0, Math.min(trimmed.length(), 100)) + "...");
                    }
                }
            }

            // Second pass: Create indexes and other statements
            for (String statement : statements) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("--") &&
                        !(trimmed.toUpperCase().startsWith("CREATE TABLE") ||
                                trimmed.toUpperCase().startsWith("ALTER TABLE"))) {
                    try {
                        stmt.executeUpdate(trimmed);
                        successCount++;
                        logger.debug("Successfully executed index/other statement");
                    } catch (SQLException e) {
                        errorCount++;
                        // Don't log as warning for index creation failures - they're often expected
                        if (trimmed.toUpperCase().contains("CREATE INDEX")) {
                            logger.debug("Index creation skipped (table may not exist): {}", e.getMessage());
                        } else {
                            logger.warn("Error executing statement: {} - SQL: {}", e.getMessage(),
                                    trimmed.substring(0, Math.min(trimmed.length(), 100)) + "...");
                        }
                    }
                }
            }

            logger.info("Schema execution completed: {} successful, {} errors", successCount, errorCount);
        }
    }

    private void createChannelPostsTable() throws SQLException {
        try (Statement stmt = getConnection().createStatement()) {
            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS channel_posts (
                            post_id VARCHAR(255) PRIMARY KEY,
                            channel_id VARCHAR(255) NOT NULL,
                            author_id VARCHAR(255) NOT NULL,
                            title VARCHAR(255),
                            content TEXT NOT NULL,
                            post_type VARCHAR(50) DEFAULT 'TEXT',
                            status VARCHAR(50) DEFAULT 'DRAFT',
                            view_count INTEGER DEFAULT 0,
                            reaction_count INTEGER DEFAULT 0,
                            share_count INTEGER DEFAULT 0,
                            comment_count INTEGER DEFAULT 0,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            scheduled_at TIMESTAMP,
                            published_at TIMESTAMP,
                            last_edited_at TIMESTAMP,
                            last_edited_by VARCHAR(255),
                            is_pinned BOOLEAN DEFAULT FALSE,
                            allow_comments BOOLEAN DEFAULT TRUE,
                            send_notification BOOLEAN DEFAULT TRUE,
                            media_urls TEXT[],
                            tags TEXT[],
                            mentions TEXT[],
                            thumbnail_url VARCHAR(500),
                            template_id VARCHAR(255),
                            original_post_id VARCHAR(255),
                            original_channel_id VARCHAR(255),
                            FOREIGN KEY (channel_id) REFERENCES channels(channel_id) ON DELETE CASCADE,
                            FOREIGN KEY (author_id) REFERENCES users(user_id) ON DELETE CASCADE,
                            FOREIGN KEY (last_edited_by) REFERENCES users(user_id) ON DELETE SET NULL,
                            FOREIGN KEY (original_post_id) REFERENCES channel_posts(post_id) ON DELETE SET NULL,
                            FOREIGN KEY (original_channel_id) REFERENCES channels(channel_id) ON DELETE SET NULL
                        )
                    """);
            logger.info("Channel posts table created successfully");
        }
    }
}
