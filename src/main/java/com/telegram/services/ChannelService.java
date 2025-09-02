package com.telegram.services;

import com.telegram.dao.ChannelDAO;
import com.telegram.models.Channel;
import com.telegram.models.ChannelPost;
import com.telegram.models.ChannelSubscription;
import com.telegram.models.ChannelAnalytics;
import com.telegram.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service class for managing channel operations
 */
public class ChannelService {
    private static final Logger logger = LoggerFactory.getLogger(ChannelService.class);
    private final ChannelDAO channelDAO;

    public ChannelService() {
        this.channelDAO = new ChannelDAO();
    }

    // Channel management
    public boolean createChannel(Channel channel) {
        return channelDAO.createChannel(channel);
    }

    public Channel getChannelById(String channelId) {
        return channelDAO.findById(channelId);
    }

    public List<Channel> getUserChannels(String userId) {
        return channelDAO.getUserChannels(userId);
    }

    public boolean updateChannel(Channel channel) {
        return channelDAO.updateChannel(channel);
    }

    public boolean deleteChannel(String channelId) {
        return channelDAO.deleteChannel(channelId);
    }

    // Subscription management
    public boolean subscribeToChannel(String channelId, String userId) {
        return channelDAO.addSubscriber(channelId, userId, false);
    }

    public boolean unsubscribeFromChannel(String channelId, String userId) {
        return channelDAO.removeSubscriber(channelId, userId);
    }

    public List<ChannelSubscription> getChannelSubscriptions(String channelId) {
        // Convert User list to ChannelSubscription list
        List<User> subscribers = channelDAO.getChannelSubscribers(channelId);
        List<ChannelSubscription> subscriptions = new ArrayList<>();
        for (User user : subscribers) {
            subscriptions.add(new ChannelSubscription(channelId, user.getUserId()));
        }
        return subscriptions;
    }

    public List<Channel> getUserSubscribedChannels(String userId) {
        return channelDAO.getUserChannels(userId); // This method already returns user's channels
    }

    // Post management
    public boolean createPost(ChannelPost post) {
        return channelDAO.createChannelPost(post);
    }

    public ChannelPost getPostById(String postId) {
        return channelDAO.getPostById(postId);
    }

    public List<ChannelPost> getChannelPosts(String channelId) {
        return channelDAO.getChannelPosts(channelId, 100, 0); // Default limit and offset
    }

    public boolean updatePost(ChannelPost post) {
        return channelDAO.updatePost(post);
    }

    public boolean deletePost(String postId) {
        return channelDAO.deletePost(postId);
    }

    // Analytics
    public ChannelAnalytics getChannelAnalytics(String channelId, LocalDateTime periodStart, LocalDateTime periodEnd) {
        return channelDAO.getChannelAnalytics(channelId, periodStart, periodEnd);
    }

    public boolean saveChannelAnalytics(ChannelAnalytics analytics) {
        return channelDAO.saveChannelAnalytics(analytics);
    }

    // Utility methods
    public long getSubscriberCount(String channelId) {
        return channelDAO.getSubscriberCount(channelId);
    }

    public boolean isUserSubscribed(String channelId, String userId) {
        return channelDAO.isUserSubscribed(channelId, userId);
    }

    public List<Channel> searchChannels(String query) {
        return channelDAO.searchChannels(query);
    }

    public List<Channel> getPublicChannels() {
        return channelDAO.getPublicChannels();
    }

    public boolean hasPermission(String userId, String channelId, String permission) {
        Channel channel = getChannelById(channelId);
        if (channel == null) return false;
        
        // Owner has all permissions
        if (channel.getOwnerId().equals(userId)) {
            return true;
        }
        
        // Check admin permissions
        return channelDAO.hasAdminPermission(userId, channelId, permission);
    }
    
    // Additional utility methods for UI
    public int getMemberCount(String channelId) {
        return (int) getSubscriberCount(channelId);
    }
    
    public int getPostCount(String channelId) {
        return channelDAO.getPostCount(channelId);
    }
    
    // Additional methods for enhanced functionality
    public List<Channel> getPublicGroups() {
        try {
            // This method should return public groups - since we're dealing with channels,
            // we'll return public channels instead
            return channelDAO.getPublicChannels();
        } catch (Exception e) {
            logger.error("Error getting public groups", e);
            return List.of();
        }
    }
    
    public Channel joinChannelByInviteLink(String inviteLink, String userId) {
        try {
            // Parse invite link to get channel ID
            String channelId = parseInviteLink(inviteLink);
            if (channelId != null) {
                Channel channel = getChannelById(channelId);
                if (channel != null && !channel.isPrivate()) {
                    boolean success = channelDAO.addSubscriber(channelId, userId, false);
                    return success ? channel : null;
                }
            }
            return null;
        } catch (Exception e) {
            logger.error("Error joining channel by invite link", e);
            return null;
        }
    }
    
    private String parseInviteLink(String inviteLink) {
        // Simple implementation - extract channel ID from invite link
        // Format: https://t.me/channelname or https://t.me/joinchat/channelid
        if (inviteLink.contains("/joinchat/")) {
            return inviteLink.substring(inviteLink.lastIndexOf("/") + 1);
        } else if (inviteLink.contains("t.me/")) {
            String channelName = inviteLink.substring(inviteLink.lastIndexOf("/") + 1);
            // Would need to lookup channel by name - for now return as is
            return channelName;
        }
        return null;
    }
    
    // Additional missing methods
    public List<Channel> getAllUserChannels(String userId) {
        return getUserChannels(userId);
    }
    
    public List<Channel> searchPublicChannels(String searchTerm) {
        return searchChannels(searchTerm);
    }
    
    public List<Channel> getFeaturedPublicChannels(int limit) {
        try {
            List<Channel> allChannels = getPublicChannels();
            return allChannels.stream().limit(limit).collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting featured public channels", e);
            return List.of();
        }
    }
    
    public Channel getChannelByInviteToken(String token) {
        try {
            // Simple implementation - treat token as channel ID for now
            return getChannelById(token);
        } catch (Exception e) {
            logger.error("Error getting channel by invite token", e);
            return null;
        }
    }
}
