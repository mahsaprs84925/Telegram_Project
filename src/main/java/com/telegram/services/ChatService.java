package com.telegram.services;

import com.telegram.dao.*;
import com.telegram.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Service class for chat-related operations
 */
public class ChatService {
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    private static ChatService instance;
    private final PrivateChatDAO privateChatDAO;
    private final GroupChatDAO groupChatDAO;
    private final ChannelDAO channelDAO;
    
    private ChatService() {
        this.privateChatDAO = new PrivateChatDAO();
        this.groupChatDAO = new GroupChatDAO();
        this.channelDAO = new ChannelDAO();
    }
    
    public static synchronized ChatService getInstance() {
        if (instance == null) {
            instance = new ChatService();
        }
        return instance;
    }
    
    // Private Chat methods
    public PrivateChat createOrGetPrivateChat(String user1Id, String user2Id) {
        try {
            PrivateChat existingChat = privateChatDAO.findByUsers(user1Id, user2Id);
            if (existingChat != null) {
                return existingChat;
            }
            
            PrivateChat newChat = new PrivateChat(user1Id, user2Id);
            boolean success = privateChatDAO.createPrivateChat(newChat);
            return success ? newChat : null;
        } catch (Exception e) {
            logger.error("Error creating/getting private chat", e);
            return null;
        }
    }
    
    public List<PrivateChat> getUserPrivateChats(String userId) {
        try {
            return privateChatDAO.getUserPrivateChats(userId);
        } catch (Exception e) {
            logger.error("Error getting user private chats", e);
            return List.of();
        }
    }
    
    // Group Chat methods
    public GroupChat createGroup(String groupName, String description, String creatorId) {
        try {
            GroupChat group = new GroupChat(groupName, creatorId);
            group.setDescription(description);
            
            boolean success = groupChatDAO.createGroup(group);
            return success ? group : null;
        } catch (Exception e) {
            logger.error("Error creating group", e);
            return null;
        }
    }
    
    public boolean addMemberToGroup(String groupId, String userId) {
        try {
            return groupChatDAO.addMember(groupId, userId, false);
        } catch (Exception e) {
            logger.error("Error adding member to group", e);
            return false;
        }
    }
    
    public boolean removeMemberFromGroup(String groupId, String userId, String removedBy) {
        try {
            GroupChat group = groupChatDAO.findById(groupId);
            if (group != null && (group.isCreator(removedBy) || group.isAdmin(removedBy))) {
                return groupChatDAO.removeMember(groupId, userId);
            }
            return false;
        } catch (Exception e) {
            logger.error("Error removing member from group", e);
            return false;
        }
    }
    
    public boolean makeGroupAdmin(String groupId, String userId, String promotedBy) {
        try {
            GroupChat group = groupChatDAO.findById(groupId);
            if (group != null && (group.isCreator(promotedBy) || group.isAdmin(promotedBy))) {
                return groupChatDAO.updateMemberAdmin(groupId, userId, true);
            }
            return false;
        } catch (Exception e) {
            logger.error("Error making group admin", e);
            return false;
        }
    }
    
    public List<GroupChat> getUserGroups(String userId) {
        try {
            return groupChatDAO.getUserGroups(userId);
        } catch (Exception e) {
            logger.error("Error getting user groups", e);
            return List.of();
        }
    }
    
    public List<User> getGroupMembers(String groupId) {
        try {
            return groupChatDAO.getGroupMembers(groupId);
        } catch (Exception e) {
            logger.error("Error getting group members", e);
            return List.of();
        }
    }
    
    public List<GroupChat> searchGroups(String searchTerm) {
        try {
            return groupChatDAO.searchGroups(searchTerm);
        } catch (Exception e) {
            logger.error("Error searching groups", e);
            return List.of();
        }
    }
    
    public GroupChat getGroupChatById(String groupId) {
        try {
            return groupChatDAO.findById(groupId);
        } catch (Exception e) {
            logger.error("Error getting group chat by ID", e);
            return null;
        }
    }
    
    // Channel methods
    public Channel createChannel(String channelName, String description, String ownerId, boolean isPrivate) {
        try {
            Channel channel = new Channel(channelName, ownerId);
            channel.setDescription(description);
            channel.setPrivate(isPrivate);
            
            boolean success = channelDAO.createChannel(channel);
            return success ? channel : null;
        } catch (Exception e) {
            logger.error("Error creating channel", e);
            return null;
        }
    }
    
    public boolean subscribeToChannel(String channelId, String userId) {
        try {
            return channelDAO.addSubscriber(channelId, userId, false);
        } catch (Exception e) {
            logger.error("Error subscribing to channel", e);
            return false;
        }
    }
    
    public boolean unsubscribeFromChannel(String channelId, String userId) {
        try {
            return channelDAO.removeSubscriber(channelId, userId);
        } catch (Exception e) {
            logger.error("Error unsubscribing from channel", e);
            return false;
        }
    }
    
    public boolean makeChannelAdmin(String channelId, String userId, String promotedBy) {
        try {
            Channel channel = channelDAO.findById(channelId);
            if (channel != null && (channel.isOwner(promotedBy) || channel.isAdmin(promotedBy))) {
                return channelDAO.updateSubscriberAdmin(channelId, userId, true);
            }
            return false;
        } catch (Exception e) {
            logger.error("Error making channel admin", e);
            return false;
        }
    }
    
    public List<Channel> getUserChannels(String userId) {
        try {
            return channelDAO.getUserChannels(userId);
        } catch (Exception e) {
            logger.error("Error getting user channels", e);
            return List.of();
        }
    }
    
    public List<Channel> searchPublicChannels(String searchTerm) {
        try {
            return channelDAO.searchPublicChannels(searchTerm);
        } catch (Exception e) {
            logger.error("Error searching public channels", e);
            return List.of();
        }
    }
    
    public List<User> getChannelSubscribers(String channelId) {
        try {
            return channelDAO.getChannelSubscribers(channelId);
        } catch (Exception e) {
            logger.error("Error getting channel subscribers", e);
            return List.of();
        }
    }
    
    public Channel getChannelById(String channelId) {
        try {
            return channelDAO.findById(channelId);
        } catch (Exception e) {
            logger.error("Error getting channel by ID", e);
            return null;
        }
    }
    
    // Chat management methods
    public boolean deletePrivateChat(String chatId) {
        try {
            return privateChatDAO.deletePrivateChat(chatId);
        } catch (Exception e) {
            logger.error("Error deleting private chat", e);
            return false;
        }
    }
    
    public boolean leaveGroup(String groupId, String userId) {
        try {
            GroupChat group = groupChatDAO.findById(groupId);
            if (group != null) {
                group.getMemberIds().remove(userId);
                group.getAdminIds().remove(userId); // Also remove from admins if applicable
                return groupChatDAO.updateGroup(group);
            }
            return false;
        } catch (Exception e) {
            logger.error("Error leaving group", e);
            return false;
        }
    }
    
    public boolean updateGroup(GroupChat group) {
        try {
            return groupChatDAO.updateGroup(group);
        } catch (Exception e) {
            logger.error("Error updating group", e);
            return false;
        }
    }
    
    public boolean deleteGroup(String groupId) {
        try {
            return groupChatDAO.deleteGroup(groupId);
        } catch (Exception e) {
            logger.error("Error deleting group", e);
            return false;
        }
    }
    
    public boolean updateChannel(Channel channel) {
        try {
            return channelDAO.updateChannel(channel);
        } catch (Exception e) {
            logger.error("Error updating channel", e);
            return false;
        }
    }
    
    public boolean deleteChannel(String channelId) {
        try {
            return channelDAO.deleteChannel(channelId);
        } catch (Exception e) {
            logger.error("Error deleting channel", e);
            return false;
        }
    }
    
    // Additional search methods for enhanced functionality
    public List<Object> searchChats(String searchTerm, String userId) {
        try {
            List<Object> results = new java.util.ArrayList<>();
            
            // Search private chats (by username/display name)
            List<PrivateChat> privateChats = privateChatDAO.getUserPrivateChats(userId);
            for (PrivateChat chat : privateChats) {
                // This would need to be enhanced to search by contact names
                results.add(chat);
            }
            
            // Search groups
            List<GroupChat> groups = groupChatDAO.searchGroups(searchTerm);
            results.addAll(groups);
            
            // Search public channels
            List<Channel> channels = channelDAO.searchPublicChannels(searchTerm);
            results.addAll(channels);
            
            return results;
        } catch (Exception e) {
            logger.error("Error searching chats", e);
            return List.of();
        }
    }
    
    public List<GroupChat> getPublicGroups() {
        try {
            // Return all groups since GroupChat doesn't have a private field
            return groupChatDAO.searchGroups("");
        } catch (Exception e) {
            logger.error("Error getting public groups", e);
            return List.of();
        }
    }
    
    public boolean markAsRead(String chatId, String userId) {
        try {
            // This would need to be implemented based on your message system
            // For now, return true as a placeholder
            return true;
        } catch (Exception e) {
            logger.error("Error marking chat as read", e);
            return false;
        }
    }
    
    // Additional missing methods
    public List<GroupChat> getAllUserGroups(String userId) {
        return getUserGroups(userId);
    }
    
    public List<GroupChat> searchPublicGroups(String searchTerm) {
        try {
            return groupChatDAO.searchGroups(searchTerm);
        } catch (Exception e) {
            logger.error("Error searching public groups", e);
            return List.of();
        }
    }
    
    public List<GroupChat> getFeaturedPublicGroups(int limit) {
        try {
            List<GroupChat> allGroups = groupChatDAO.searchGroups("");
            return allGroups.stream().limit(limit).collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting featured public groups", e);
            return List.of();
        }
    }
    
    public boolean joinGroup(String groupId, String userId) {
        return addMemberToGroup(groupId, userId);
    }
    
    public GroupChat getGroupById(String groupId) {
        return getGroupChatById(groupId);
    }
    
    public GroupChat getGroupByInviteToken(String token) {
        try {
            // Simple implementation - treat token as group ID for now
            return getGroupChatById(token);
        } catch (Exception e) {
            logger.error("Error getting group by invite token", e);
            return null;
        }
    }
    
    public PrivateChat getOrCreatePrivateChat(String user1Id, String user2Id) {
        return createOrGetPrivateChat(user1Id, user2Id);
    }
    
    public boolean addSubscriberToChannel(String channelId, String userId) {
        return subscribeToChannel(channelId, userId);
    }
    
    public boolean removeSubscriberFromChannel(String channelId, String subscriberId, String removedBy) {
        return unsubscribeFromChannel(channelId, subscriberId);
    }
}
