package com.telegram.dao;

import com.telegram.database.DatabaseManager;
import com.telegram.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

/**
 * Data Access Object for GroupChat operations
 */
public class GroupChatDAO {
    private static final Logger logger = LoggerFactory.getLogger(GroupChatDAO.class);
    private final DatabaseManager dbManager;
    private final UserDAO userDAO;
    
    public GroupChatDAO() {
        this.dbManager = DatabaseManager.getInstance();
        this.userDAO = new UserDAO();
    }
    
    public boolean createGroup(GroupChat group) {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false);
            
            // Insert group
            String groupSql = """
                INSERT INTO group_chats (group_id, group_name, creator_id, description, 
                                       profile_picture_path, created_at, last_activity, max_members)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
            
            try (PreparedStatement pstmt = conn.prepareStatement(groupSql)) {
                pstmt.setString(1, group.getGroupId());
                pstmt.setString(2, group.getGroupName());
                pstmt.setString(3, group.getCreatorId());
                pstmt.setString(4, group.getDescription());
                pstmt.setString(5, group.getProfilePicturePath());
                pstmt.setTimestamp(6, Timestamp.valueOf(group.getCreatedAt()));
                pstmt.setTimestamp(7, Timestamp.valueOf(group.getLastActivity()));
                pstmt.setInt(8, group.getMaxMembers());
                
                pstmt.executeUpdate();
            }
            
            // Add creator as member and admin
            addMemberWithConnection(conn, group.getGroupId(), group.getCreatorId(), true);
            
            conn.commit();
            logger.info("Group created: {}", group.getGroupName());
            return true;
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    logger.error("Error rolling back transaction", ex);
                }
            }
            logger.error("Error creating group: {}", group.getGroupName(), e);
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
    
    public GroupChat findById(String groupId) {
        String sql = "SELECT * FROM group_chats WHERE group_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, groupId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                GroupChat group = mapResultSetToGroupChat(rs);
                loadGroupMembers(group);
                return group;
            }
            
        } catch (SQLException e) {
            logger.error("Error finding group by ID: {}", groupId, e);
        }
        
        return null;
    }
    
    public List<GroupChat> getUserGroups(String userId) {
        String sql = """
            SELECT g.* FROM group_chats g
            INNER JOIN group_members gm ON g.group_id = gm.group_id
            WHERE gm.user_id = ?
            ORDER BY g.last_activity DESC
        """;
        
        List<GroupChat> groups = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                GroupChat group = mapResultSetToGroupChat(rs);
                loadGroupMembers(group);
                groups.add(group);
            }
            
        } catch (SQLException e) {
            logger.error("Error getting user groups: {}", userId, e);
        }
        
        return groups;
    }
    
    public boolean addMember(String groupId, String userId, boolean isAdmin) {
        try (Connection conn = dbManager.getConnection()) {
            return addMemberWithConnection(conn, groupId, userId, isAdmin);
        } catch (SQLException e) {
            logger.error("Error adding member to group: {} - {}", groupId, userId, e);
            return false;
        }
    }
    
    private boolean addMemberWithConnection(Connection conn, String groupId, String userId, boolean isAdmin) throws SQLException {
        String sql = """
            INSERT INTO group_members (group_id, user_id, is_admin, joined_at)
            VALUES (?, ?, ?, CURRENT_TIMESTAMP)
            ON CONFLICT (group_id, user_id) DO NOTHING
        """;
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, groupId);
            pstmt.setString(2, userId);
            pstmt.setBoolean(3, isAdmin);
            
            int result = pstmt.executeUpdate();
            return result > 0;
        }
    }
    
    public boolean removeMember(String groupId, String userId) {
        String sql = "DELETE FROM group_members WHERE group_id = ? AND user_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, groupId);
            pstmt.setString(2, userId);
            
            int result = pstmt.executeUpdate();
            return result > 0;
            
        } catch (SQLException e) {
            logger.error("Error removing member from group: {} - {}", groupId, userId, e);
            return false;
        }
    }
    
    public boolean updateMemberAdmin(String groupId, String userId, boolean isAdmin) {
        String sql = "UPDATE group_members SET is_admin = ? WHERE group_id = ? AND user_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setBoolean(1, isAdmin);
            pstmt.setString(2, groupId);
            pstmt.setString(3, userId);
            
            int result = pstmt.executeUpdate();
            return result > 0;
            
        } catch (SQLException e) {
            logger.error("Error updating member admin status: {} - {}", groupId, userId, e);
            return false;
        }
    }
    
    public List<User> getGroupMembers(String groupId) {
        String sql = """
            SELECT u.* FROM users u
            INNER JOIN group_members gm ON u.user_id = gm.user_id
            WHERE gm.group_id = ?
            ORDER BY gm.is_admin DESC, gm.joined_at ASC
        """;
        
        List<User> members = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, groupId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                members.add(mapResultSetToUser(rs));
            }
            
        } catch (SQLException e) {
            logger.error("Error getting group members: {}", groupId, e);
        }
        
        return members;
    }
    
    public boolean updateGroup(GroupChat group) {
        String sql = """
            UPDATE group_chats SET group_name = ?, description = ?, 
                                 profile_picture_path = ?, last_activity = ?
            WHERE group_id = ?
        """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, group.getGroupName());
            pstmt.setString(2, group.getDescription());
            pstmt.setString(3, group.getProfilePicturePath());
            pstmt.setTimestamp(4, Timestamp.valueOf(group.getLastActivity()));
            pstmt.setString(5, group.getGroupId());
            
            int result = pstmt.executeUpdate();
            return result > 0;
            
        } catch (SQLException e) {
            logger.error("Error updating group: {}", group.getGroupId(), e);
            return false;
        }
    }
    
    public List<GroupChat> searchGroups(String searchTerm) {
        String sql = """
            SELECT * FROM group_chats 
            WHERE LOWER(group_name) LIKE LOWER(?) 
               OR LOWER(description) LIKE LOWER(?)
            ORDER BY group_name
            LIMIT 50
        """;
        
        List<GroupChat> groups = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String searchPattern = "%" + searchTerm + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                GroupChat group = mapResultSetToGroupChat(rs);
                loadGroupMembers(group);
                groups.add(group);
            }
            
        } catch (SQLException e) {
            logger.error("Error searching groups with term: {}", searchTerm, e);
        }
        
        return groups;
    }
    
    private void loadGroupMembers(GroupChat group) {
        String memberSql = "SELECT user_id, is_admin FROM group_members WHERE group_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(memberSql)) {
            
            pstmt.setString(1, group.getGroupId());
            ResultSet rs = pstmt.executeQuery();
            
            List<String> memberIds = new ArrayList<>();
            List<String> adminIds = new ArrayList<>();
            
            while (rs.next()) {
                String userId = rs.getString("user_id");
                boolean isAdmin = rs.getBoolean("is_admin");
                
                memberIds.add(userId);
                if (isAdmin) {
                    adminIds.add(userId);
                }
            }
            
            group.setMemberIds(memberIds);
            group.setAdminIds(adminIds);
            
        } catch (SQLException e) {
            logger.error("Error loading group members: {}", group.getGroupId(), e);
        }
    }
    
    private GroupChat mapResultSetToGroupChat(ResultSet rs) throws SQLException {
        return new GroupChat(
            rs.getString("group_id"),
            rs.getString("group_name"),
            rs.getString("creator_id"),
            rs.getString("description"),
            rs.getString("profile_picture_path"),
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
    
    public boolean deleteGroup(String groupId) {
        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false);
            
            // Delete group members first
            String deleteMembersSql = "DELETE FROM group_members WHERE group_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteMembersSql)) {
                pstmt.setString(1, groupId);
                pstmt.executeUpdate();
            }
            
            // Delete the group
            String deleteGroupSql = "DELETE FROM group_chats WHERE group_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteGroupSql)) {
                pstmt.setString(1, groupId);
                int rowsAffected = pstmt.executeUpdate();
                
                if (rowsAffected > 0) {
                    conn.commit();
                    logger.info("Group deleted successfully: {}", groupId);
                    return true;
                } else {
                    conn.rollback();
                    return false;
                }
            }
            
        } catch (SQLException e) {
            logger.error("Error deleting group: {}", groupId, e);
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
    
    // Enhanced Role Management Methods
    
    /**
     * Creates a new group role
     */
    public boolean createGroupRole(String groupId, GroupRole role) {
        String sql = """
            INSERT INTO group_roles (role_id, group_id, role_name, description, 
                                   role_type, color, hierarchy_level, is_default, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, role.getRoleId());
            pstmt.setString(2, groupId);
            pstmt.setString(3, role.getRoleName());
            pstmt.setString(4, role.getDescription());
            pstmt.setString(5, role.getRoleType().name());
            pstmt.setString(6, role.getColor());
            pstmt.setInt(7, role.getHierarchyLevel());
            pstmt.setBoolean(8, role.isDefault());
            pstmt.setTimestamp(9, Timestamp.valueOf(role.getCreatedAt()));
            
            int result = pstmt.executeUpdate();
            
            if (result > 0) {
                // Insert role permissions - convert List to Set
                return insertRolePermissions(role.getRoleId(), new java.util.HashSet<>(role.getPermissions()));
            }
            
            return false;
        } catch (SQLException e) {
            logger.error("Error creating group role", e);
            return false;
        }
    }
    
    /**
     * Updates an existing group role
     */
    public boolean updateGroupRole(GroupRole role) {
        String sql = """
            UPDATE group_roles 
            SET role_name = ?, description = ?, color = ?, hierarchy_level = ?, 
                is_default = ?, updated_at = ?
            WHERE role_id = ?
        """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, role.getRoleName());
            pstmt.setString(2, role.getDescription());
            pstmt.setString(3, role.getColor());
            pstmt.setInt(4, role.getHierarchyLevel());
            pstmt.setBoolean(5, role.isDefault());
            pstmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setString(7, role.getRoleId());
            
            int result = pstmt.executeUpdate();
            
            if (result > 0) {
                // Update role permissions - convert List to Set
                deleteRolePermissions(role.getRoleId());
                return insertRolePermissions(role.getRoleId(), new java.util.HashSet<>(role.getPermissions()));
            }
            
            return false;
        } catch (SQLException e) {
            logger.error("Error updating group role", e);
            return false;
        }
    }
    
    /**
     * Deletes a group role
     */
    public boolean deleteGroupRole(String roleId) {
        try (Connection conn = dbManager.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Delete role permissions first
                deleteRolePermissions(roleId);
                
                // Reset members with this role to default role
                String resetMembersSql = """
                    UPDATE group_members 
                    SET role_id = (
                        SELECT role_id FROM group_roles 
                        WHERE group_id = (SELECT group_id FROM group_roles WHERE role_id = ?) 
                        AND is_default = true 
                        LIMIT 1
                    )
                    WHERE role_id = ?
                """;
                
                try (PreparedStatement pstmt = conn.prepareStatement(resetMembersSql)) {
                    pstmt.setString(1, roleId);
                    pstmt.setString(2, roleId);
                    pstmt.executeUpdate();
                }
                
                // Delete the role
                String deleteRoleSql = "DELETE FROM group_roles WHERE role_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(deleteRoleSql)) {
                    pstmt.setString(1, roleId);
                    int result = pstmt.executeUpdate();
                    
                    if (result > 0) {
                        conn.commit();
                        return true;
                    }
                }
                
                conn.rollback();
                return false;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Error deleting group role", e);
            return false;
        }
    }
    
    /**
     * Gets all roles for a group
     */
    public List<GroupRole> getGroupRoles(String groupId) {
        String sql = """
            SELECT role_id, group_id, role_name, description, role_type, color, 
                   hierarchy_level, is_default, created_at, updated_at
            FROM group_roles 
            WHERE group_id = ? 
            ORDER BY hierarchy_level DESC, role_name
        """;
        
        List<GroupRole> roles = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, groupId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    GroupRole role = new GroupRole(
                        rs.getString("role_id"),
                        rs.getString("group_id"),
                        rs.getString("role_name"),
                        rs.getString("description"),
                        GroupRole.RoleType.valueOf(rs.getString("role_type"))
                    );
                    
                    role.setColor(rs.getString("color"));
                    role.setHierarchyLevel(rs.getInt("hierarchy_level"));
                    role.setDefault(rs.getBoolean("is_default"));
                    role.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    
                    Timestamp updatedAt = rs.getTimestamp("updated_at");
                    if (updatedAt != null) {
                        role.setUpdatedAt(updatedAt.toLocalDateTime());
                    }
                    
                    // Load permissions for this role - convert Set to List
                    role.setPermissions(new java.util.ArrayList<>(getRolePermissions(role.getRoleId())));
                    
                    roles.add(role);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting group roles", e);
        }
        
        return roles;
    }
    
    /**
     * Gets a specific group role by ID
     */
    public GroupRole getGroupRole(String roleId) {
        String sql = """
            SELECT role_id, group_id, role_name, description, role_type, color, 
                   hierarchy_level, is_default, created_at, updated_at
            FROM group_roles 
            WHERE role_id = ?
        """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, roleId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    GroupRole role = new GroupRole(
                        rs.getString("role_id"),
                        rs.getString("group_id"),
                        rs.getString("role_name"),
                        rs.getString("description"),
                        GroupRole.RoleType.valueOf(rs.getString("role_type"))
                    );
                    
                    role.setColor(rs.getString("color"));
                    role.setHierarchyLevel(rs.getInt("hierarchy_level"));
                    role.setDefault(rs.getBoolean("is_default"));
                    role.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    
                    Timestamp updatedAt = rs.getTimestamp("updated_at");
                    if (updatedAt != null) {
                        role.setUpdatedAt(updatedAt.toLocalDateTime());
                    }
                    
                    // Load permissions for this role - convert Set to List
                    role.setPermissions(new java.util.ArrayList<>(getRolePermissions(role.getRoleId())));
                    
                    return role;
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting group role", e);
        }
        
        return null;
    }
    
    /**
     * Assigns a role to a group member
     */
    public boolean assignMemberRole(String groupId, String userId, String roleId) {
        String sql = """
            UPDATE group_members 
            SET role_id = ?, role_assigned_at = ?, role_assigned_by = ?
            WHERE group_id = ? AND user_id = ?
        """;
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, roleId);
            pstmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setString(3, getCurrentUserId()); // This would need to be passed in or retrieved from context
            pstmt.setString(4, groupId);
            pstmt.setString(5, userId);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error assigning member role", e);
            return false;
        }
    }
    
    /**
     * Gets enhanced group member information with roles
     */
    public List<GroupMember> getEnhancedGroupMembers(String groupId) {
        String sql = """
            SELECT gm.user_id, gm.group_id, gm.joined_at, gm.is_muted, gm.muted_until, 
                   gm.is_banned, gm.banned_until, gm.ban_reason, gm.message_count, 
                   gm.last_activity, gm.role_id, gm.role_assigned_at, gm.role_assigned_by,
                   u.username, u.display_name, u.profile_picture_path, u.last_seen, u.is_online,
                   gr.role_name, gr.role_type, gr.color, gr.hierarchy_level
            FROM group_members gm
            JOIN users u ON gm.user_id = u.user_id
            LEFT JOIN group_roles gr ON gm.role_id = gr.role_id
            WHERE gm.group_id = ?
            ORDER BY gr.hierarchy_level DESC, u.username
        """;
        
        List<GroupMember> members = new ArrayList<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, groupId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    // Create User object
                    User user = new User();
                    user.setUserId(rs.getString("user_id"));
                    user.setUsername(rs.getString("username"));
                    user.setProfileName(rs.getString("display_name"));
                    user.setProfilePicturePath(rs.getString("profile_picture_path"));
                    // Note: is_online should be handled via UserStatus enum
                    boolean isOnline = rs.getBoolean("is_online");
                    user.setStatus(isOnline ? User.UserStatus.ONLINE : User.UserStatus.OFFLINE);
                    
                    Timestamp lastSeen = rs.getTimestamp("last_seen");
                    if (lastSeen != null) {
                        user.setLastSeen(lastSeen.toLocalDateTime());
                    }
                    
                    // Create GroupMember object
                    GroupMember member = new GroupMember(user, groupId);
                    member.setJoinedAt(rs.getTimestamp("joined_at").toLocalDateTime());
                    member.setMuted(rs.getBoolean("is_muted"));
                    member.setBanned(rs.getBoolean("is_banned"));
                    member.setMessageCount(rs.getInt("message_count"));
                    
                    Timestamp lastActivity = rs.getTimestamp("last_activity");
                    if (lastActivity != null) {
                        member.setLastActivity(lastActivity.toLocalDateTime());
                    }
                    
                    Timestamp mutedUntil = rs.getTimestamp("muted_until");
                    if (mutedUntil != null) {
                        member.setMutedUntil(mutedUntil.toLocalDateTime());
                    }
                    
                    Timestamp bannedUntil = rs.getTimestamp("banned_until");
                    if (bannedUntil != null) {
                        member.setBannedUntil(bannedUntil.toLocalDateTime());
                    }
                    
                    member.setBanReason(rs.getString("ban_reason"));
                    
                    // Set role information if available
                    String roleId = rs.getString("role_id");
                    if (roleId != null) {
                        member.setRoleId(roleId);
                        
                        Timestamp roleAssignedAt = rs.getTimestamp("role_assigned_at");
                        if (roleAssignedAt != null) {
                            member.setRoleAssignedAt(roleAssignedAt.toLocalDateTime());
                        }
                        
                        member.setRoleAssignedBy(rs.getString("role_assigned_by"));
                    }
                    
                    members.add(member);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting enhanced group members", e);
        }
        
        return members;
    }
    
    // Helper methods for role permissions
    
    private boolean insertRolePermissions(String roleId, Set<GroupPermission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return true;
        }
        
        String sql = "INSERT INTO group_role_permissions (role_id, permission) VALUES (?, ?)";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            for (GroupPermission permission : permissions) {
                pstmt.setString(1, roleId);
                pstmt.setString(2, permission.name());
                pstmt.addBatch();
            }
            
            int[] results = pstmt.executeBatch();
            
            // Check if all insertions were successful
            for (int result : results) {
                if (result <= 0) {
                    return false;
                }
            }
            
            return true;
        } catch (SQLException e) {
            logger.error("Error inserting role permissions", e);
            return false;
        }
    }
    
    private boolean deleteRolePermissions(String roleId) {
        String sql = "DELETE FROM group_role_permissions WHERE role_id = ?";
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, roleId);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            logger.error("Error deleting role permissions", e);
            return false;
        }
    }
    
    private Set<GroupPermission> getRolePermissions(String roleId) {
        String sql = "SELECT permission FROM group_role_permissions WHERE role_id = ?";
        Set<GroupPermission> permissions = new HashSet<>();
        
        try (Connection conn = dbManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, roleId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    try {
                        GroupPermission permission = GroupPermission.valueOf(rs.getString("permission"));
                        permissions.add(permission);
                    } catch (IllegalArgumentException e) {
                        logger.warn("Unknown permission found in database: {}", rs.getString("permission"));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting role permissions", e);
        }
        
        return permissions;
    }
    
    // TODO: This should be retrieved from session/security context
    private String getCurrentUserId() {
        // Placeholder - in a real implementation, this would come from the security context
        return "current-user-id";
    }

}
