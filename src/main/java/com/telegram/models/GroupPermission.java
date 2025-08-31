package com.telegram.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents different permission types for groups and channels
 */
public enum GroupPermission {
    // Basic messaging permissions
    SEND_MESSAGES("Send Messages", "Allow sending text messages"),
    SEND_MEDIA("Send Media", "Allow sending images, videos, and files"),
    SEND_STICKERS("Send Stickers", "Allow sending stickers and GIFs"),
    SEND_POLLS("Send Polls", "Allow creating and sending polls"),
    
    // Content management permissions
    DELETE_MESSAGES("Delete Messages", "Allow deleting any messages in the group"),
    EDIT_MESSAGES("Edit Messages", "Allow editing any messages in the group"),
    PIN_MESSAGES("Pin Messages", "Allow pinning and unpinning messages"),
    
    // Member management permissions
    ADD_MEMBERS("Add Members", "Allow adding new members to the group"),
    REMOVE_MEMBERS("Remove Members", "Allow removing members from the group"),
    BAN_MEMBERS("Ban Members", "Allow banning members permanently"),
    RESTRICT_MEMBERS("Restrict Members", "Allow restricting member permissions"),
    
    // Administrative permissions
    CHANGE_GROUP_INFO("Change Group Info", "Allow editing group name, description, and photo"),
    MANAGE_INVITE_LINKS("Manage Invite Links", "Allow creating and managing invite links"),
    MANAGE_ADMINS("Manage Admins", "Allow promoting and demoting other admins"),
    
    // Advanced permissions
    MANAGE_CALLS("Manage Calls", "Allow starting and managing voice/video calls"),
    VIEW_ADMIN_LOG("View Admin Log", "Allow viewing the admin action log"),
    ANONYMOUS_ADMIN("Anonymous Admin", "Allow posting messages anonymously as admin"),
    
    // Moderation permissions
    MODERATE_CONTENT("Moderate Content", "Allow moderating and filtering content"),
    MANAGE_REPORTS("Manage Reports", "Allow handling reported messages and users"),
    SET_GROUP_RULES("Set Group Rules", "Allow setting and editing group rules");
    
    private final String displayName;
    private final String description;
    
    GroupPermission(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Get default permissions for different role types
     */
    public static List<GroupPermission> getDefaultPermissions(GroupRole.RoleType roleType) {
        List<GroupPermission> permissions = new ArrayList<>();
        
        switch (roleType) {
            case OWNER:
                // Owner has all permissions
                permissions.addAll(List.of(values()));
                break;
                
            case ADMIN:
                permissions.addAll(List.of(
                    SEND_MESSAGES, SEND_MEDIA, SEND_STICKERS, SEND_POLLS,
                    DELETE_MESSAGES, EDIT_MESSAGES, PIN_MESSAGES,
                    ADD_MEMBERS, REMOVE_MEMBERS, RESTRICT_MEMBERS,
                    CHANGE_GROUP_INFO, MANAGE_INVITE_LINKS,
                    MANAGE_CALLS, VIEW_ADMIN_LOG,
                    MODERATE_CONTENT, MANAGE_REPORTS
                ));
                break;
                
            case MODERATOR:
                permissions.addAll(List.of(
                    SEND_MESSAGES, SEND_MEDIA, SEND_STICKERS, SEND_POLLS,
                    DELETE_MESSAGES, PIN_MESSAGES,
                    RESTRICT_MEMBERS,
                    MODERATE_CONTENT, MANAGE_REPORTS
                ));
                break;
                
            case MEMBER:
                permissions.addAll(List.of(
                    SEND_MESSAGES, SEND_MEDIA, SEND_STICKERS, SEND_POLLS
                ));
                break;
                
            case RESTRICTED:
                // Restricted members have minimal permissions
                permissions.add(SEND_MESSAGES);
                break;
                
            default:
                break;
        }
        
        return permissions;
    }
    
    /**
     * Check if a permission is considered administrative
     */
    public boolean isAdministrative() {
        return this == MANAGE_ADMINS || 
               this == CHANGE_GROUP_INFO || 
               this == MANAGE_INVITE_LINKS ||
               this == VIEW_ADMIN_LOG ||
               this == SET_GROUP_RULES;
    }
    
    /**
     * Check if a permission is related to content moderation
     */
    public boolean isModeration() {
        return this == DELETE_MESSAGES ||
               this == EDIT_MESSAGES ||
               this == MODERATE_CONTENT ||
               this == MANAGE_REPORTS ||
               this == BAN_MEMBERS ||
               this == RESTRICT_MEMBERS;
    }
}
