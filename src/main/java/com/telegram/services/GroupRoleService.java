package com.telegram.services;

import com.telegram.dao.GroupChatDAO;
import com.telegram.models.GroupRole;
import com.telegram.models.GroupMember;
import com.telegram.models.GroupPermission;
import com.telegram.models.GroupChat;
import com.telegram.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * Service for managing group roles and permissions
 */
public class GroupRoleService {
    private static final Logger logger = LoggerFactory.getLogger(GroupRoleService.class);
    private static GroupRoleService instance;
    
    private final GroupChatDAO groupChatDAO;
    private final Map<String, List<GroupRole>> groupRolesCache;
    private final Map<String, List<GroupMember>> groupMembersCache;
    
    private GroupRoleService() {
        this.groupChatDAO = new GroupChatDAO();
        this.groupRolesCache = new HashMap<>();
        this.groupMembersCache = new HashMap<>();
    }
    
    public static synchronized GroupRoleService getInstance() {
        if (instance == null) {
            instance = new GroupRoleService();
        }
        return instance;
    }
    
    /**
     * Initialize default roles for a new group
     */
    public boolean initializeDefaultRoles(String groupId, String creatorId) {
        try {
            List<GroupRole> defaultRoles = createDefaultRoles(groupId);
            
            // Create roles in database (implementation would be in DAO)
            for (GroupRole role : defaultRoles) {
                // TODO: Create role in database
                logger.info("Created default role: {} for group: {}", role.getRoleName(), groupId);
            }
            
            // Cache the roles
            groupRolesCache.put(groupId, defaultRoles);
            
            // Assign creator to owner role
            GroupRole ownerRole = defaultRoles.stream()
                .filter(role -> role.getRoleType() == GroupRole.RoleType.OWNER)
                .findFirst()
                .orElse(null);
            
            if (ownerRole != null) {
                GroupMember creatorMember = new GroupMember(groupId, creatorId, ownerRole.getRoleId());
                creatorMember.setInvitedBy("system");
                // TODO: Save member in database
                
                List<GroupMember> members = new ArrayList<>();
                members.add(creatorMember);
                groupMembersCache.put(groupId, members);
            }
            
            return true;
        } catch (Exception e) {
            logger.error("Error initializing default roles for group: {}", groupId, e);
            return false;
        }
    }
    
    /**
     * Create default role set for a group
     */
    private List<GroupRole> createDefaultRoles(String groupId) {
        List<GroupRole> roles = new ArrayList<>();
        
        // Owner role
        GroupRole ownerRole = new GroupRole(groupId, "Owner", GroupRole.RoleType.OWNER);
        ownerRole.setCreatedBy("system");
        roles.add(ownerRole);
        
        // Admin role
        GroupRole adminRole = new GroupRole(groupId, "Administrator", GroupRole.RoleType.ADMIN);
        adminRole.setCreatedBy("system");
        roles.add(adminRole);
        
        // Moderator role
        GroupRole moderatorRole = new GroupRole(groupId, "Moderator", GroupRole.RoleType.MODERATOR);
        moderatorRole.setCreatedBy("system");
        roles.add(moderatorRole);
        
        // Member role
        GroupRole memberRole = new GroupRole(groupId, "Member", GroupRole.RoleType.MEMBER);
        memberRole.setCreatedBy("system");
        roles.add(memberRole);
        
        // Restricted role
        GroupRole restrictedRole = new GroupRole(groupId, "Restricted", GroupRole.RoleType.RESTRICTED);
        restrictedRole.setCreatedBy("system");
        roles.add(restrictedRole);
        
        return roles;
    }
    
    /**
     * Check if a user has a specific permission in a group
     */
    public boolean hasPermission(String groupId, String userId, GroupPermission permission) {
        try {
            GroupMember member = getGroupMember(groupId, userId);
            if (member == null || !member.isActive()) {
                return false;
            }
            
            GroupRole role = getRole(groupId, member.getRoleId());
            if (role == null) {
                return false;
            }
            
            return role.hasPermission(permission);
        } catch (Exception e) {
            logger.error("Error checking permission {} for user {} in group {}", 
                permission, userId, groupId, e);
            return false;
        }
    }
    
    /**
     * Promote a member to a higher role
     */
    public boolean promoteMember(String groupId, String userId, String newRoleId, String promotedBy) {
        try {
            // Check if promoter has permission
            if (!hasPermission(groupId, promotedBy, GroupPermission.MANAGE_ADMINS)) {
                logger.warn("User {} does not have permission to promote members in group {}", 
                    promotedBy, groupId);
                return false;
            }
            
            GroupMember member = getGroupMember(groupId, userId);
            if (member == null) {
                logger.warn("Member {} not found in group {}", userId, groupId);
                return false;
            }
            
            GroupRole newRole = getRole(groupId, newRoleId);
            if (newRole == null) {
                logger.warn("Role {} not found in group {}", newRoleId, groupId);
                return false;
            }
            
            // Check role hierarchy
            GroupRole promoterRole = getUserRole(groupId, promotedBy);
            if (promoterRole == null || !promoterRole.canManageRole(newRole)) {
                logger.warn("User {} cannot promote to role {} in group {}", 
                    promotedBy, newRole.getRoleName(), groupId);
                return false;
            }
            
            // Update member role
            member.promoteRole(newRoleId, promotedBy);
            
            // TODO: Update in database
            logger.info("Member {} promoted to role {} in group {} by {}", 
                userId, newRole.getRoleName(), groupId, promotedBy);
            
            return true;
        } catch (Exception e) {
            logger.error("Error promoting member {} to role {} in group {}", 
                userId, newRoleId, groupId, e);
            return false;
        }
    }
    
    /**
     * Create a custom role in a group
     */
    public GroupRole createCustomRole(String groupId, String roleName, String description, 
                                    List<GroupPermission> permissions, String color, String createdBy) {
        try {
            // Check if creator has permission
            if (!hasPermission(groupId, createdBy, GroupPermission.MANAGE_ADMINS)) {
                logger.warn("User {} does not have permission to create roles in group {}", 
                    createdBy, groupId);
                return null;
            }
            
            GroupRole customRole = new GroupRole(groupId, roleName, description, permissions, color);
            customRole.setCreatedBy(createdBy);
            
            // TODO: Save to database
            
            // Add to cache
            List<GroupRole> roles = groupRolesCache.getOrDefault(groupId, new ArrayList<>());
            roles.add(customRole);
            groupRolesCache.put(groupId, roles);
            
            logger.info("Custom role {} created in group {} by {}", roleName, groupId, createdBy);
            return customRole;
        } catch (Exception e) {
            logger.error("Error creating custom role {} in group {}", roleName, groupId, e);
            return null;
        }
    }
    
    /**
     * Restrict a member (mute, ban, etc.)
     */
    public boolean restrictMember(String groupId, String userId, String restrictionType, 
                                LocalDateTime until, String reason, String restrictedBy) {
        try {
            // Check if restrictor has permission
            if (!hasPermission(groupId, restrictedBy, GroupPermission.RESTRICT_MEMBERS)) {
                logger.warn("User {} does not have permission to restrict members in group {}", 
                    restrictedBy, groupId);
                return false;
            }
            
            GroupMember member = getGroupMember(groupId, userId);
            if (member == null) {
                logger.warn("Member {} not found in group {}", userId, groupId);
                return false;
            }
            
            // Check role hierarchy
            GroupRole restrictorRole = getUserRole(groupId, restrictedBy);
            GroupRole memberRole = getRole(groupId, member.getRoleId());
            
            if (restrictorRole == null || memberRole == null || 
                !restrictorRole.canManageRole(memberRole)) {
                logger.warn("User {} cannot restrict member {} in group {}", 
                    restrictedBy, userId, groupId);
                return false;
            }
            
            // Apply restriction
            switch (restrictionType.toLowerCase()) {
                case "mute":
                    member.mute(until);
                    break;
                case "ban":
                    member.ban(until, reason);
                    break;
                default:
                    logger.warn("Unknown restriction type: {}", restrictionType);
                    return false;
            }
            
            // TODO: Update in database
            logger.info("Member {} {} in group {} until {} by {}", 
                userId, restrictionType, groupId, until, restrictedBy);
            
            return true;
        } catch (Exception e) {
            logger.error("Error restricting member {} in group {}", userId, groupId, e);
            return false;
        }
    }
    
    /**
     * Get all roles for a group
     */
    public List<GroupRole> getGroupRoles(String groupId) {
        try {
            // Try cache first
            List<GroupRole> cachedRoles = groupRolesCache.get(groupId);
            if (cachedRoles != null) {
                return new ArrayList<>(cachedRoles);
            }
            
            // TODO: Load from database
            List<GroupRole> roles = new ArrayList<>(); // Load from DAO
            groupRolesCache.put(groupId, roles);
            
            return roles;
        } catch (Exception e) {
            logger.error("Error getting roles for group {}", groupId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get a specific role
     */
    public GroupRole getRole(String groupId, String roleId) {
        List<GroupRole> roles = getGroupRoles(groupId);
        return roles.stream()
            .filter(role -> role.getRoleId().equals(roleId))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Get a member's current role
     */
    public GroupRole getUserRole(String groupId, String userId) {
        GroupMember member = getGroupMember(groupId, userId);
        if (member == null) {
            return null;
        }
        return getRole(groupId, member.getRoleId());
    }
    
    /**
     * Get group member information
     */
    public GroupMember getGroupMember(String groupId, String userId) {
        try {
            // Try cache first
            List<GroupMember> cachedMembers = groupMembersCache.get(groupId);
            if (cachedMembers != null) {
                return cachedMembers.stream()
                    .filter(member -> member.getUserId().equals(userId))
                    .findFirst()
                    .orElse(null);
            }
            
            // TODO: Load from database
            return null;
        } catch (Exception e) {
            logger.error("Error getting member {} for group {}", userId, groupId, e);
            return null;
        }
    }
    
    /**
     * Get all members of a group
     */
    public List<GroupMember> getGroupMembers(String groupId) {
        try {
            // Try cache first
            List<GroupMember> cachedMembers = groupMembersCache.get(groupId);
            if (cachedMembers != null) {
                return new ArrayList<>(cachedMembers);
            }
            
            // TODO: Load from database
            List<GroupMember> members = new ArrayList<>(); // Load from DAO
            groupMembersCache.put(groupId, members);
            
            return members;
        } catch (Exception e) {
            logger.error("Error getting members for group {}", groupId, e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get group statistics
     */
    public Map<String, Object> getGroupStatistics(String groupId) {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            List<GroupMember> members = getGroupMembers(groupId);
            List<GroupRole> roles = getGroupRoles(groupId);
            
            // Basic stats
            stats.put("totalMembers", members.size());
            stats.put("totalRoles", roles.size());
            
            // Member stats by role
            Map<String, Long> membersByRole = members.stream()
                .collect(Collectors.groupingBy(
                    GroupMember::getRoleId, 
                    Collectors.counting()
                ));
            stats.put("membersByRole", membersByRole);
            
            // Activity stats
            long activeMembers = members.stream()
                .mapToLong(member -> member.isActive() ? 1 : 0)
                .sum();
            stats.put("activeMembers", activeMembers);
            
            long mutedMembers = members.stream()
                .mapToLong(member -> member.isMutedCurrently() ? 1 : 0)
                .sum();
            stats.put("mutedMembers", mutedMembers);
            
            long bannedMembers = members.stream()
                .mapToLong(member -> member.isBannedCurrently() ? 1 : 0)
                .sum();
            stats.put("bannedMembers", bannedMembers);
            
            // Message stats
            int totalMessages = members.stream()
                .mapToInt(GroupMember::getTotalMessages)
                .sum();
            stats.put("totalMessages", totalMessages);
            
            double averageActivityScore = members.stream()
                .mapToDouble(GroupMember::getActivityScore)
                .average()
                .orElse(0.0);
            stats.put("averageActivityScore", averageActivityScore);
            
        } catch (Exception e) {
            logger.error("Error calculating group statistics for group {}", groupId, e);
        }
        
        return stats;
    }
    
    /**
     * Clear cache for a group (useful when group is updated)
     */
    public void clearGroupCache(String groupId) {
        groupRolesCache.remove(groupId);
        groupMembersCache.remove(groupId);
    }
    
    /**
     * Clear all caches
     */
    public void clearAllCaches() {
        groupRolesCache.clear();
        groupMembersCache.clear();
    }
}
