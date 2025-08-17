package com.telegram.dao;

import com.telegram.database.DatabaseManager;
import com.telegram.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Data Access Object for Channel operations
 */
public class ChannelDAO {
    private static final Logger logger = LoggerFactory.getLogger(ChannelDAO.class);
    private final DatabaseManager dbManager;
    
    public ChannelDAO() {
        this.dbManager = DatabaseManager.getInstance();
    }
    
    public boolean createChannel(Channel channel) {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false);
            
            // Insert channel
            String channelSql = """
                INSERT INTO channels (channel_id, channel_name, owner_id, description, 
                                    profile_picture_path, is_private, invite_link, 
                                    created_at, last_activity)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
            
            try (PreparedStatement pstmt = conn.prepareStatement(channelSql)) {
                pstmt.setString(1, channel.getChannelId());
                pstmt.setString(2, channel.getChannelName());
                pstmt.setString(3, channel.getOwnerId());
                pstmt.setString(4, channel.getDescription());
                pstmt.setString(5, channel.getProfilePicturePath());
                pstmt.setBoolean(6, channel.isPrivate());
                pstmt.setString(7, channel.getInviteLink());
                pstmt.setTimestamp(8, Timestamp.valueOf(channel.getCreatedAt()));
                pstmt.setTimestamp(9, Timestamp.valueOf(channel.getLastActivity()));
                
                pstmt.executeUpdate();
            }
            
            // Add owner as subscriber and admin
            addSubscriberWithConnection(conn, channel.getChannelId(), channel.getOwnerId(), true);
            
            conn.commit();
            logger.info("Channel created: {}", channel.getChannelName());
            return true;
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.error("Error rolling back transaction", ex);
                }
            }
            logger.error("Error creating channel: {}", channel.getChannelName(), e);
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (SQLException e) {
                    logger.error("Error resetting auto-commit", e);
                }
            }
        }
    }
    
    public Channel findById(String channelId) {
        String sql = "SELECT * FROM channels WHERE channel_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, channelId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Channel channel = mapResultSetToChannel(rs);
                loadChannelSubscribers(channel);
                return channel;
            }
            
        } catch (SQLException e) {
            logger.error("Error finding channel by ID: {}", channelId, e);
        }
        
        return null;
    }
    
    public Channel findByName(String channelName) {
        String sql = "SELECT * FROM channels WHERE LOWER(channel_name) = LOWER(?)";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, channelName);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                Channel channel = mapResultSetToChannel(rs);
                loadChannelSubscribers(channel);
                return channel;
            }
            
        } catch (SQLException e) {
            logger.error("Error finding channel by name: {}", channelName, e);
        }
        
        return null;
    }
    
    public List<Channel> getUserChannels(String userId) {
        String sql = """
            SELECT c.* FROM channels c
            INNER JOIN channel_subscribers cs ON c.channel_id = cs.channel_id
            WHERE cs.user_id = ?
            ORDER BY c.last_activity DESC
        """;
        
        List<Channel> channels = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Channel channel = mapResultSetToChannel(rs);
                loadChannelSubscribers(channel);
                channels.add(channel);
            }
            
        } catch (SQLException e) {
            logger.error("Error getting user channels: {}", userId, e);
        }
        
        return channels;
    }
    
    public boolean addSubscriber(String channelId, String userId, boolean isAdmin) {
        try (Connection conn = dbManager.getConnection()) {
            return addSubscriberWithConnection(conn, channelId, userId, isAdmin);
        } catch (SQLException e) {
            logger.error("Error adding subscriber to channel: {} - {}", channelId, userId, e);
            return false;
        }
    }
    
    private boolean addSubscriberWithConnection(Connection conn, String channelId, String userId, boolean isAdmin) throws SQLException {
        String sql = """
            INSERT INTO channel_subscribers (channel_id, user_id, is_admin, subscribed_at)
            VALUES (?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT (channel_id, user_id) DO NOTHING
        """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, channelId);
            pstmt.setString(2, userId);
            pstmt.setBoolean(3, isAdmin);
            
            int result = pstmt.executeUpdate();
            return result > 0;
        }
    }
    
    public boolean removeSubscriber(String channelId, String userId) {
        String sql = "DELETE FROM channel_subscribers WHERE channel_id = ? AND user_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, channelId);
            pstmt.setString(2, userId);
            
            int result = pstmt.executeUpdate();
            return result > 0;
            
        } catch (SQLException e) {
            logger.error("Error removing subscriber from channel: {} - {}", channelId, userId, e);
            return false;
        }
    }
    
    public boolean updateSubscriberAdmin(String channelId, String userId, boolean isAdmin) {
        String sql = "UPDATE channel_subscribers SET is_admin = ? WHERE channel_id = ? AND user_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setBoolean(1, isAdmin);
            pstmt.setString(2, channelId);
            pstmt.setString(3, userId);
            
            int result = pstmt.executeUpdate();
            return result > 0;
            
        } catch (SQLException e) {
            logger.error("Error updating subscriber admin status: {} - {}", channelId, userId, e);
            return false;
        }
    }
    
    public List<User> getChannelSubscribers(String channelId) {
        String sql = """
            SELECT u.* FROM users u
            INNER JOIN channel_subscribers cs ON u.user_id = cs.user_id
            WHERE cs.channel_id = ?
            ORDER BY cs.is_admin DESC, cs.subscribed_at ASC
        """;
        
        List<User> subscribers = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, channelId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                subscribers.add(mapResultSetToUser(rs));
            }
            
        } catch (SQLException e) {
            logger.error("Error getting channel subscribers: {}", channelId, e);
        }
        
        return subscribers;
    }
    
    public boolean updateChannel(Channel channel) {
        String sql = """
            UPDATE channels SET channel_name = ?, description = ?, 
                              profile_picture_path = ?, is_private = ?, 
                              invite_link = ?, last_activity = ?
            WHERE channel_id = ?
        """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, channel.getChannelName());
            pstmt.setString(2, channel.getDescription());
            pstmt.setString(3, channel.getProfilePicturePath());
            pstmt.setBoolean(4, channel.isPrivate());
            pstmt.setString(5, channel.getInviteLink());
            pstmt.setTimestamp(6, Timestamp.valueOf(channel.getLastActivity()));
            pstmt.setString(7, channel.getChannelId());
            
            int result = pstmt.executeUpdate();
            return result > 0;
            
        } catch (SQLException e) {
            logger.error("Error updating channel: {}", channel.getChannelId(), e);
            return false;
        }
    }
    
    public List<Channel> searchPublicChannels(String searchTerm) {
        String sql = """
            SELECT * FROM channels 
            WHERE is_private = false 
              AND (LOWER(channel_name) LIKE LOWER(?) 
                   OR LOWER(description) LIKE LOWER(?))
            ORDER BY channel_name
            LIMIT 50
        """;
        
        List<Channel> channels = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String searchPattern = "%" + searchTerm + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Channel channel = mapResultSetToChannel(rs);
                loadChannelSubscribers(channel);
                channels.add(channel);
            }
            
        } catch (SQLException e) {
            logger.error("Error searching public channels with term: {}", searchTerm, e);
        }
        
        return channels;
    }
    
    private void loadChannelSubscribers(Channel channel) {
        String subscriberSql = "SELECT user_id, is_admin FROM channel_subscribers WHERE channel_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(subscriberSql)) {
            
            pstmt.setString(1, channel.getChannelId());
            ResultSet rs = pstmt.executeQuery();
            
            List<String> subscriberIds = new ArrayList<>();
            List<String> adminIds = new ArrayList<>();
            
            while (rs.next()) {
                String userId = rs.getString("user_id");
                boolean isAdmin = rs.getBoolean("is_admin");
                
                subscriberIds.add(userId);
                if (isAdmin) {
                    adminIds.add(userId);
                }
            }
            
            channel.setSubscriberIds(subscriberIds);
            channel.setAdminIds(adminIds);
            
        } catch (SQLException e) {
            logger.error("Error loading channel subscribers: {}", channel.getChannelId(), e);
        }
    }
    
    private Channel mapResultSetToChannel(ResultSet rs) throws SQLException {
        return new Channel(
            rs.getString("channel_id"),
            rs.getString("channel_name"),
            rs.getString("owner_id"),
            rs.getString("description"),
            rs.getString("profile_picture_path"),
            rs.getBoolean("is_private"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("last_activity").toLocalDateTime()
        );
    }
    
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        return new User(
            rs.getString("user_id"),
            rs.getString("username"),
            rs.getString("profile_name"),
            rs.getString("profile_picture_path"),
            User.UserStatus.valueOf(rs.getString("status")),
            rs.getString("bio"),
            rs.getTimestamp("last_seen").toLocalDateTime(),
            rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
    
    public boolean deleteChannel(String channelId) {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false);
            
            // Delete channel subscribers first
            String deleteSubscribersSql = "DELETE FROM channel_subscribers WHERE channel_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteSubscribersSql)) {
                pstmt.setString(1, channelId);
                pstmt.executeUpdate();
            }
            
            // Delete the channel
            String deleteChannelSql = "DELETE FROM channels WHERE channel_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteChannelSql)) {
                pstmt.setString(1, channelId);
                int rowsAffected = pstmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    conn.commit();
                    logger.info("Channel deleted successfully: {}", channelId);
                    return true;
                } else {
                    conn.rollback();
                    return false;
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error deleting channel: {}", channelId, e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    logger.error("Error rolling back transaction", rollbackEx);
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    logger.error("Error closing connection", e);
                }
            }
        }
    }
    
    // Enhanced Analytics Methods
    
    /**
     * Gets the total number of posts in a channel
     */
    public int getPostCount(String channelId) {
        String sql = "SELECT COUNT(*) FROM channel_posts WHERE channel_id = ? AND status = 'PUBLISHED'";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, channelId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting post count for channel: {}", channelId, e);
        }
        
        return 0;
    }
    
    /**
     * Creates a new channel post
     */
    public boolean createChannelPost(ChannelPost post) {
        String sql = """
            INSERT INTO channel_posts (post_id, channel_id, author_id, content, media_urls, 
                                     scheduled_at, published_at, status, view_count, reaction_count, 
                                     share_count, comment_count, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, post.getPostId());
            pstmt.setString(2, post.getChannelId());
            pstmt.setString(3, post.getAuthorId());
            pstmt.setString(4, post.getContent());
            pstmt.setString(5, String.join(",", post.getMediaUrls())); // Simple comma-separated storage
            
            if (post.getScheduledAt() != null) {
                pstmt.setTimestamp(6, Timestamp.valueOf(post.getScheduledAt()));
            } else {
                pstmt.setNull(6, Types.TIMESTAMP);
            }
            
            if (post.getPublishedAt() != null) {
                pstmt.setTimestamp(7, Timestamp.valueOf(post.getPublishedAt()));
            } else {
                pstmt.setNull(7, Types.TIMESTAMP);
            }
            
            pstmt.setString(8, post.getStatus().name());
            pstmt.setInt(9, (int) post.getViewCount());
            pstmt.setInt(10, (int) post.getReactionCount());
            pstmt.setInt(11, (int) post.getShareCount());
            pstmt.setInt(12, (int) post.getCommentCount());
            pstmt.setTimestamp(13, Timestamp.valueOf(post.getCreatedAt()));
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error creating channel post", e);
            return false;
        }
    }
    
    /**
     * Updates channel post statistics
     */
    public boolean updatePostStats(String postId, int viewCount, int reactionCount, int shareCount, int commentCount) {
        String sql = """
            UPDATE channel_posts 
            SET view_count = ?, reaction_count = ?, share_count = ?, comment_count = ?, updated_at = ?
            WHERE post_id = ?
        """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, viewCount);
            pstmt.setInt(2, reactionCount);
            pstmt.setInt(3, shareCount);
            pstmt.setInt(4, commentCount);
            pstmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setString(6, postId);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating post stats", e);
            return false;
        }
    }
    
    /**
     * Gets channel posts with pagination
     */
    public List<ChannelPost> getChannelPosts(String channelId, int limit, int offset) {
        String sql = """
            SELECT post_id, channel_id, author_id, content, media_urls, scheduled_at, 
                   published_at, status, view_count, reaction_count, share_count, 
                   comment_count, created_at, updated_at
            FROM channel_posts 
            WHERE channel_id = ? 
            ORDER BY created_at DESC
            LIMIT ? OFFSET ?
        """;
        
        List<ChannelPost> posts = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, channelId);
            pstmt.setInt(2, limit);
            pstmt.setInt(3, offset);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ChannelPost post = new ChannelPost(
                        rs.getString("post_id"),
                        rs.getString("channel_id"),
                        rs.getString("author_id"),
                        rs.getString("content")
                    );
                    
                    // Set media URLs
                    String mediaUrls = rs.getString("media_urls");
                    if (mediaUrls != null && !mediaUrls.isEmpty()) {
                        String[] urls = mediaUrls.split(",");
                        for (String url : urls) {
                            post.addMediaUrl(url.trim());
                        }
                    }
                    
                    // Set timestamps
                    Timestamp scheduledAt = rs.getTimestamp("scheduled_at");
                    if (scheduledAt != null) {
                        post.setScheduledAt(scheduledAt.toLocalDateTime());
                    }
                    
                    Timestamp publishedAt = rs.getTimestamp("published_at");
                    if (publishedAt != null) {
                        post.setPublishedAt(publishedAt.toLocalDateTime());
                    }
                    
                    post.setStatus(ChannelPost.PostStatus.valueOf(rs.getString("status")));
                    post.setViewCount(rs.getInt("view_count"));
                    post.setReactionCount(rs.getInt("reaction_count"));
                    post.setShareCount(rs.getInt("share_count"));
                    post.setCommentCount(rs.getInt("comment_count"));
                    post.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    
                    Timestamp updatedAt = rs.getTimestamp("updated_at");
                    if (updatedAt != null) {
                        post.setUpdatedAt(updatedAt.toLocalDateTime());
                    }
                    
                    posts.add(post);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting channel posts", e);
        }
        
        return posts;
    }
    
    /**
     * Creates or updates channel analytics
     */
    public boolean saveChannelAnalytics(ChannelAnalytics analytics) {
        String sql = """
            INSERT INTO channel_analytics (analytics_id, channel_id, period_start, period_end, 
                                         subscriber_count, new_subscribers, unsubscribes, 
                                         total_views, total_reactions, total_shares, 
                                         total_comments, engagement_rate, growth_rate, 
                                         top_performing_post_id, avg_views_per_post, 
                                         peak_hour, demographics_data, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (analytics_id) DO UPDATE SET
                subscriber_count = EXCLUDED.subscriber_count,
                new_subscribers = EXCLUDED.new_subscribers,
                unsubscribes = EXCLUDED.unsubscribes,
                total_views = EXCLUDED.total_views,
                total_reactions = EXCLUDED.total_reactions,
                total_shares = EXCLUDED.total_shares,
                total_comments = EXCLUDED.total_comments,
                engagement_rate = EXCLUDED.engagement_rate,
                growth_rate = EXCLUDED.growth_rate,
                top_performing_post_id = EXCLUDED.top_performing_post_id,
                avg_views_per_post = EXCLUDED.avg_views_per_post,
                peak_hour = EXCLUDED.peak_hour,
                demographics_data = EXCLUDED.demographics_data,
                updated_at = CURRENT_TIMESTAMP
        """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, analytics.getAnalyticsId());
            pstmt.setString(2, analytics.getChannelId());
            pstmt.setTimestamp(3, Timestamp.valueOf(analytics.getPeriodStart()));
            pstmt.setTimestamp(4, Timestamp.valueOf(analytics.getPeriodEnd()));
            pstmt.setInt(5, analytics.getSubscriberCount());
            pstmt.setInt(6, (int) analytics.getNewSubscribers());
            pstmt.setInt(7, analytics.getUnsubscribes());
            pstmt.setLong(8, analytics.getTotalViews());
            pstmt.setInt(9, (int) analytics.getTotalReactions());
            pstmt.setInt(10, (int) analytics.getTotalShares());
            pstmt.setInt(11, (int) analytics.getTotalComments());
            pstmt.setDouble(12, analytics.getEngagementRate());
            pstmt.setDouble(13, analytics.getGrowthRate());
            pstmt.setString(14, analytics.getTopPerformingPostId());
            pstmt.setDouble(15, analytics.getAvgViewsPerPost());
            pstmt.setString(16, analytics.getPeakHour());
            pstmt.setString(17, "{}"); // Demographics data as JSON string - simplified for now
            pstmt.setTimestamp(18, Timestamp.valueOf(analytics.getCreatedAt()));
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error saving channel analytics", e);
            return false;
        }
    }
    
    /**
     * Gets channel analytics for a specific period
     */
    public ChannelAnalytics getChannelAnalytics(String channelId, LocalDateTime periodStart, LocalDateTime periodEnd) {
        String sql = """
            SELECT analytics_id, channel_id, period_start, period_end, subscriber_count, 
                   new_subscribers, unsubscribes, total_views, total_reactions, total_shares, 
                   total_comments, engagement_rate, growth_rate, top_performing_post_id, 
                   avg_views_per_post, peak_hour, demographics_data, created_at, updated_at
            FROM channel_analytics 
            WHERE channel_id = ? AND period_start = ? AND period_end = ?
        """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, channelId);
            pstmt.setTimestamp(2, Timestamp.valueOf(periodStart));
            pstmt.setTimestamp(3, Timestamp.valueOf(periodEnd));
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    ChannelAnalytics analytics = new ChannelAnalytics(
                        rs.getString("analytics_id"),
                        rs.getString("channel_id"),
                        rs.getTimestamp("period_start").toLocalDateTime(),
                        rs.getTimestamp("period_end").toLocalDateTime()
                    );
                    
                    analytics.setSubscriberCount(rs.getInt("subscriber_count"));
                    analytics.setNewSubscribers(rs.getInt("new_subscribers"));
                    analytics.setUnsubscribes(rs.getInt("unsubscribes"));
                    analytics.setTotalViews(rs.getLong("total_views"));
                    analytics.setTotalReactions(rs.getInt("total_reactions"));
                    analytics.setTotalShares(rs.getInt("total_shares"));
                    analytics.setTotalComments(rs.getInt("total_comments"));
                    analytics.setEngagementRate(rs.getDouble("engagement_rate"));
                    analytics.setGrowthRate(rs.getDouble("growth_rate"));
                    analytics.setTopPerformingPostId(rs.getString("top_performing_post_id"));
                    analytics.setAvgViewsPerPost(rs.getDouble("avg_views_per_post"));
                    analytics.setPeakHour(rs.getInt("peak_hour"));
                    analytics.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    
                    Timestamp updatedAt = rs.getTimestamp("updated_at");
                    if (updatedAt != null) {
                        analytics.setUpdatedAt(updatedAt.toLocalDateTime());
                    }
                    
                    return analytics;
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting channel analytics", e);
        }
        
        return null;
    }
    
    /**
     * Gets channel analytics summary (basic metrics)
     */
    public Map<String, Object> getChannelAnalyticsSummary(String channelId) {
        Map<String, Object> summary = new HashMap<>();
        
        try (Connection conn = dbManager.getConnection()) {
            // Get subscriber count
            String subscriberSql = "SELECT COUNT(*) as subscriber_count FROM channel_subscribers WHERE channel_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(subscriberSql)) {
                pstmt.setString(1, channelId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        summary.put("subscriber_count", rs.getInt("subscriber_count"));
                    }
                }
            }
            
            // Get post count
            String postSql = "SELECT COUNT(*) as post_count FROM channel_posts WHERE channel_id = ? AND status = 'PUBLISHED'";
            try (PreparedStatement pstmt = conn.prepareStatement(postSql)) {
                pstmt.setString(1, channelId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        summary.put("post_count", rs.getInt("post_count"));
                    }
                }
            }
            
            // Get total views and engagement
            String engagementSql = """
                SELECT SUM(view_count) as total_views, 
                       SUM(reaction_count) as total_reactions,
                       SUM(share_count) as total_shares,
                       SUM(comment_count) as total_comments
                FROM channel_posts 
                WHERE channel_id = ? AND status = 'PUBLISHED'
            """;
            try (PreparedStatement pstmt = conn.prepareStatement(engagementSql)) {
                pstmt.setString(1, channelId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        summary.put("total_views", rs.getLong("total_views"));
                        summary.put("total_reactions", rs.getInt("total_reactions"));
                        summary.put("total_shares", rs.getInt("total_shares"));
                        summary.put("total_comments", rs.getInt("total_comments"));
                    }
                }
            }
            
            // Calculate engagement rate
            Long totalViews = (Long) summary.get("total_views");
            Integer totalReactions = (Integer) summary.get("total_reactions");
            Integer totalShares = (Integer) summary.get("total_shares");
            Integer totalComments = (Integer) summary.get("total_comments");
            
            if (totalViews != null && totalViews > 0) {
                int totalEngagement = (totalReactions != null ? totalReactions : 0) + 
                                    (totalShares != null ? totalShares : 0) + 
                                    (totalComments != null ? totalComments : 0);
                double engagementRate = (double) totalEngagement / totalViews * 100;
                summary.put("engagement_rate", Math.round(engagementRate * 100.0) / 100.0);
            } else {
                summary.put("engagement_rate", 0.0);
            }
            
        } catch (SQLException e) {
            logger.error("Error getting channel analytics summary", e);
        }
        
        return summary;
    }

    // Additional methods needed by ChannelService
    public ChannelPost getPostById(String postId) {
        String sql = "SELECT * FROM channel_posts WHERE post_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, postId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                ChannelPost post = new ChannelPost();
                post.setPostId(rs.getString("post_id"));
                post.setChannelId(rs.getString("channel_id"));
                post.setAuthorId(rs.getString("author_id"));
                post.setTitle(rs.getString("title"));
                post.setContent(rs.getString("content"));
                post.setPostType(ChannelPost.PostType.valueOf(rs.getString("post_type")));
                post.setStatus(ChannelPost.PostStatus.valueOf(rs.getString("status")));
                post.setViewCount(rs.getInt("view_count"));
                post.setReactionCount(rs.getInt("reaction_count"));
                post.setShareCount(rs.getInt("share_count"));
                post.setCommentCount(rs.getInt("comment_count"));
                
                Timestamp createdAt = rs.getTimestamp("created_at");
                if (createdAt != null) {
                    post.setCreatedAt(createdAt.toLocalDateTime());
                }
                
                return post;
            }
        } catch (SQLException e) {
            logger.error("Error getting post by ID: " + postId, e);
        }
        
        return null;
    }
    
    public boolean updatePost(ChannelPost post) {
        String sql = "UPDATE channel_posts SET title = ?, content = ?, post_type = ?, status = ?, " +
                    "view_count = ?, reaction_count = ?, share_count = ?, comment_count = ? " +
                    "WHERE post_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, post.getTitle());
            pstmt.setString(2, post.getContent());
            pstmt.setString(3, post.getPostType().name());
            pstmt.setString(4, post.getStatus().name());
            pstmt.setInt(5, (int) post.getViewCount());
            pstmt.setInt(6, (int) post.getReactionCount());
            pstmt.setInt(7, (int) post.getShareCount());
            pstmt.setInt(8, (int) post.getCommentCount());
            pstmt.setString(9, post.getPostId());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating post: " + post.getPostId(), e);
            return false;
        }
    }
    
    public boolean deletePost(String postId) {
        String sql = "DELETE FROM channel_posts WHERE post_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, postId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting post: " + postId, e);
            return false;
        }
    }
    
    // Additional utility methods for ChannelService
    public long getSubscriberCount(String channelId) {
        String sql = "SELECT COUNT(*) FROM channel_subscribers WHERE channel_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, channelId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            logger.error("Error getting subscriber count for channel: " + channelId, e);
        }
        
        return 0;
    }
    
    public boolean isUserSubscribed(String channelId, String userId) {
        String sql = "SELECT COUNT(*) FROM channel_subscribers WHERE channel_id = ? AND user_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, channelId);
            pstmt.setString(2, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            logger.error("Error checking user subscription: " + userId + " to channel: " + channelId, e);
        }
        
        return false;
    }
    
    public List<Channel> searchChannels(String query) {
        return searchPublicChannels(query); // Reuse existing method
    }
    
    public List<Channel> getPublicChannels() {
        String sql = "SELECT * FROM channels WHERE is_private = false ORDER BY created_at DESC LIMIT 50";
        List<Channel> channels = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Channel channel = new Channel();
                channel.setChannelId(rs.getString("channel_id"));
                channel.setChannelName(rs.getString("channel_name"));
                channel.setDescription(rs.getString("description"));
                channel.setOwnerId(rs.getString("owner_id"));
                channel.setPrivate(rs.getBoolean("is_private"));
                
                Timestamp createdAt = rs.getTimestamp("created_at");
                if (createdAt != null) {
                    channel.setCreatedAt(createdAt.toLocalDateTime());
                }
                
                channels.add(channel);
            }
        } catch (SQLException e) {
            logger.error("Error getting public channels", e);
        }
        
        return channels;
    }
    
    public boolean hasAdminPermission(String userId, String channelId, String permission) {
        String sql = "SELECT is_admin FROM channel_subscribers WHERE channel_id = ? AND user_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, channelId);
            pstmt.setString(2, userId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getBoolean("is_admin");
            }
        } catch (SQLException e) {
            logger.error("Error checking admin permission for user: " + userId + " in channel: " + channelId, e);
        }
        
        return false;
    }

}
