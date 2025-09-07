package com.telegram.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a role within a group with specific permissions
 */
public class GroupRole {
    
    public enum RoleType {
        OWNER("Owner", "Full control over the group"),
        ADMIN("Administrator", "Can manage members and settings"),
        MODERATOR("Moderator", "Can moderate content and restrict members"),
        MEMBER("Member", "Regular group member"),
        RESTRICTED("Restricted", "Limited permissions");
        
        private final String displayName;
        private final String description;
        
        RoleType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() {
            return displayName;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private String roleId;
    private String groupId;
    private String roleName;
    private RoleType roleType;
    private String description;
    private List<GroupPermission> permissions;
    private String color; // Hex color for role display
    private boolean isDefault; // Whether this is a default system role
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime modifiedAt;
    private String modifiedBy;
    
    // Constructors
    public GroupRole() {
        this.roleId = UUID.randomUUID().toString();
        this.permissions = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.modifiedAt = LocalDateTime.now();
        this.isDefault = false;
        this.color = "#999999"; // Default gray color
    }
    
    public GroupRole(String groupId, String roleName, RoleType roleType) {
        this();
        this.groupId = groupId;
        this.roleName = roleName;
        this.roleType = roleType;
        this.description = roleType.getDescription();
        this.permissions = GroupPermission.getDefaultPermissions(roleType);
        this.isDefault = true;
        
        // Set default colors for different role types
        switch (roleType) {
            case OWNER:
                this.color = "#FF6B6B"; // Red
                break;
            case ADMIN:
                this.color = "#4ECDC4"; // Teal
                break;
            case MODERATOR:
                this.color = "#45B7D1"; // Blue
                break;
            case MEMBER:
                this.color = "#96CEB4"; // Green
                break;
            case RESTRICTED:
                this.color = "#FECA57"; // Yellow
                break;
        }
    }
    
    // Custom role constructor
    public GroupRole(String groupId, String roleName, String description, List<GroupPermission> permissions, String color) {
        this();
        this.groupId = groupId;
        this.roleName = roleName;
        this.roleType = RoleType.MEMBER; // Custom roles are based on member type
        this.description = description;
        this.permissions = new ArrayList<>(permissions);
        this.color = color;
        this.isDefault = false;
    }
    
    // Constructor for DAO usage
    public GroupRole(String roleId, String groupId, String roleName, String description, RoleType roleType) {
        this();
        this.roleId = roleId;
        this.groupId = groupId;
        this.roleName = roleName;
        this.description = description;
        this.roleType = roleType;
        this.permissions = new ArrayList<>();
    }
    
    // Permission management methods
    public boolean hasPermission(GroupPermission permission) {
        return permissions.contains(permission);
    }
    
    public void addPermission(GroupPermission permission) {
        if (!permissions.contains(permission)) {
            permissions.add(permission);
            this.modifiedAt = LocalDateTime.now();
        }
    }
    
    public void removePermission(GroupPermission permission) {
        if (permissions.remove(permission)) {
            this.modifiedAt = LocalDateTime.now();
        }
    }
    
    public void setPermissions(List<GroupPermission> permissions) {
        this.permissions = new ArrayList<>(permissions);
        this.modifiedAt = LocalDateTime.now();
    }
    
    /**
     * Check if this role can manage another role
     */
    public boolean canManageRole(GroupRole otherRole) {
        // Owner can manage all roles
        if (this.roleType == RoleType.OWNER) {
            return true;
        }
        
        // Admin can manage moderators and members, but not owners or other admins
        if (this.roleType == RoleType.ADMIN) {
            return otherRole.roleType == RoleType.MODERATOR || 
                   otherRole.roleType == RoleType.MEMBER || 
                   otherRole.roleType == RoleType.RESTRICTED;
        }
        
        // Moderators can only manage regular members and restricted members
        if (this.roleType == RoleType.MODERATOR) {
            return otherRole.roleType == RoleType.MEMBER || 
                   otherRole.roleType == RoleType.RESTRICTED;
        }
        
        return false;
    }
    
    /**
     * Get role hierarchy level (higher number = more permissions)
     */
    public int getHierarchyLevel() {
        switch (roleType) {
            case OWNER: return 100;
            case ADMIN: return 80;
            case MODERATOR: return 60;
            case MEMBER: return 40;
            case RESTRICTED: return 20;
            default: return 0;
        }
    }
    
    /**
     * Create a copy of this role for another group
     */
    public GroupRole copyForGroup(String newGroupId) {
        GroupRole copy = new GroupRole();
        copy.groupId = newGroupId;
        copy.roleName = this.roleName;
        copy.roleType = this.roleType;
        copy.description = this.description;
        copy.permissions = new ArrayList<>(this.permissions);
        copy.color = this.color;
        copy.isDefault = this.isDefault;
        return copy;
    }
    
    // Getters and Setters
    public String getRoleId() {
        return roleId;
    }
    
    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }
    
    public String getGroupId() {
        return groupId;
    }
    
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
    
    public String getRoleName() {
        return roleName;
    }
    
    public void setRoleName(String roleName) {
        this.roleName = roleName;
        this.modifiedAt = LocalDateTime.now();
    }
    
    public RoleType getRoleType() {
        return roleType;
    }
    
    public void setRoleType(RoleType roleType) {
        this.roleType = roleType;
        this.modifiedAt = LocalDateTime.now();
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
        this.modifiedAt = LocalDateTime.now();
    }
    
    public List<GroupPermission> getPermissions() {
        return new ArrayList<>(permissions);
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
        this.modifiedAt = LocalDateTime.now();
    }
    
    public boolean isDefault() {
        return isDefault;
    }
    
    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public LocalDateTime getModifiedAt() {
        return modifiedAt;
    }
    
    public void setModifiedAt(LocalDateTime modifiedAt) {
        this.modifiedAt = modifiedAt;
    }
    
    public String getModifiedBy() {
        return modifiedBy;
    }
    
    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
        this.modifiedAt = LocalDateTime.now();
    }
    
    public void setHierarchyLevel(int level) {
        // Hierarchy level is derived from role type, so this is a no-op
        // but needed for DAO compatibility
    }
    
    public LocalDateTime getUpdatedAt() {
        return modifiedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.modifiedAt = updatedAt;
    }
    
    @Override
    public String toString() {
        return "GroupRole{" +
                "roleId='" + roleId + '\'' +
                ", groupId='" + groupId + '\'' +
                ", roleName='" + roleName + '\'' +
                ", roleType=" + roleType +
                ", permissionCount=" + permissions.size() +
                ", color='" + color + '\'' +
                ", isDefault=" + isDefault +
                '}';
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        GroupRole groupRole = (GroupRole) obj;
        return roleId.equals(groupRole.roleId);
    }
    
    @Override
    public int hashCode() {
        return roleId.hashCode();
    }
}
