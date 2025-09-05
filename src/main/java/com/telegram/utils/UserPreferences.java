package com.telegram.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.prefs.Preferences;

/**
 * Utility class for managing user preferences using Java Preferences API
 */
public class UserPreferences {
    private static final Logger logger = LoggerFactory.getLogger(UserPreferences.class);
    private static UserPreferences instance;
    private final Preferences prefs;
    
    // Preference keys
    private static final String THEME = "theme";
    private static final String COMPACT_MODE = "compactMode";
    private static final String SHOW_AVATARS = "showAvatars";
    private static final String LANGUAGE = "language";
    private static final String SEND_ON_ENTER = "sendOnEnter";
    private static final String SHOW_TYPING = "showTyping";
    private static final String SHOW_ONLINE_STATUS = "showOnlineStatus";
    private static final String AUTO_DOWNLOAD_MEDIA = "autoDownloadMedia";
    private static final String MESSAGE_HISTORY_LIMIT = "messageHistoryLimit";
    
    private static final String NOTIFICATIONS_ENABLED = "notificationsEnabled";
    private static final String SOUND_NOTIFICATIONS = "soundNotifications";
    private static final String SHOW_PREVIEW = "showPreview";
    private static final String NOTIFY_PRIVATE_CHATS = "notifyPrivateChats";
    private static final String NOTIFY_GROUP_CHATS = "notifyGroupChats";
    private static final String NOTIFY_CHANNELS = "notifyChannels";
    private static final String DND_ENABLED = "dndEnabled";
    private static final String DND_START_TIME = "dndStartTime";
    private static final String DND_END_TIME = "dndEndTime";
    private static final String DND_IMPORTANT_ONLY = "dndImportantOnly";
    private static final String NOTIFICATION_SOUND = "notificationSound";
    private static final String VOLUME = "volume";
    
    private static final String ONLINE_STATUS_VISIBILITY = "onlineStatusVisibility";
    private static final String GROUP_ADD_PERMISSION = "groupAddPermission";
    private static final String SEND_READ_RECEIPTS = "sendReadReceipts";
    private static final String SEND_TYPING_INDICATORS = "sendTypingIndicators";
    private static final String SAVE_MEDIA = "saveMedia";
    private static final String DOWNLOAD_FOLDER = "downloadFolder";
    private static final String COMPRESS_IMAGES = "compressImages";
    
    private static final String AUTO_CONNECT = "autoConnect";
    
    private UserPreferences() {
        prefs = Preferences.userNodeForPackage(UserPreferences.class);
    }
    
    public static synchronized UserPreferences getInstance() {
        if (instance == null) {
            instance = new UserPreferences();
        }
        return instance;
    }
    
    // General Settings
    public String getTheme() {
        return prefs.get(THEME, "System");
    }
    
    public void setTheme(String theme) {
        prefs.put(THEME, theme);
    }
    
    public boolean isCompactMode() {
        return prefs.getBoolean(COMPACT_MODE, false);
    }
    
    public void setCompactMode(boolean compactMode) {
        prefs.putBoolean(COMPACT_MODE, compactMode);
    }
    
    public boolean isShowAvatars() {
        return prefs.getBoolean(SHOW_AVATARS, true);
    }
    
    public void setShowAvatars(boolean showAvatars) {
        prefs.putBoolean(SHOW_AVATARS, showAvatars);
    }
    
    public String getLanguage() {
        return prefs.get(LANGUAGE, "English");
    }
    
    public void setLanguage(String language) {
        prefs.put(LANGUAGE, language);
    }
    
    public boolean isSendOnEnter() {
        return prefs.getBoolean(SEND_ON_ENTER, true);
    }
    
    public void setSendOnEnter(boolean sendOnEnter) {
        prefs.putBoolean(SEND_ON_ENTER, sendOnEnter);
    }
    
    public boolean isShowTyping() {
        return prefs.getBoolean(SHOW_TYPING, true);
    }
    
    public void setShowTyping(boolean showTyping) {
        prefs.putBoolean(SHOW_TYPING, showTyping);
    }
    
    public boolean isShowOnlineStatus() {
        return prefs.getBoolean(SHOW_ONLINE_STATUS, true);
    }
    
    public void setShowOnlineStatus(boolean showOnlineStatus) {
        prefs.putBoolean(SHOW_ONLINE_STATUS, showOnlineStatus);
    }
    
    public boolean isAutoDownloadMedia() {
        return prefs.getBoolean(AUTO_DOWNLOAD_MEDIA, true);
    }
    
    public void setAutoDownloadMedia(boolean autoDownloadMedia) {
        prefs.putBoolean(AUTO_DOWNLOAD_MEDIA, autoDownloadMedia);
    }
    
    public int getMessageHistoryLimit() {
        return prefs.getInt(MESSAGE_HISTORY_LIMIT, 100);
    }
    
    public void setMessageHistoryLimit(int limit) {
        prefs.putInt(MESSAGE_HISTORY_LIMIT, limit);
    }
    
    // Notification Settings
    public boolean isNotificationsEnabled() {
        return prefs.getBoolean(NOTIFICATIONS_ENABLED, true);
    }
    
    public void setNotificationsEnabled(boolean enabled) {
        prefs.putBoolean(NOTIFICATIONS_ENABLED, enabled);
    }
    
    public boolean isSoundNotifications() {
        return prefs.getBoolean(SOUND_NOTIFICATIONS, true);
    }
    
    public void setSoundNotifications(boolean soundNotifications) {
        prefs.putBoolean(SOUND_NOTIFICATIONS, soundNotifications);
    }
    
    public boolean isShowPreview() {
        return prefs.getBoolean(SHOW_PREVIEW, true);
    }
    
    public void setShowPreview(boolean showPreview) {
        prefs.putBoolean(SHOW_PREVIEW, showPreview);
    }
    
    public boolean isNotifyPrivateChats() {
        return prefs.getBoolean(NOTIFY_PRIVATE_CHATS, true);
    }
    
    public void setNotifyPrivateChats(boolean notify) {
        prefs.putBoolean(NOTIFY_PRIVATE_CHATS, notify);
    }
    
    public boolean isNotifyGroupChats() {
        return prefs.getBoolean(NOTIFY_GROUP_CHATS, true);
    }
    
    public void setNotifyGroupChats(boolean notify) {
        prefs.putBoolean(NOTIFY_GROUP_CHATS, notify);
    }
    
    public boolean isNotifyChannels() {
        return prefs.getBoolean(NOTIFY_CHANNELS, false);
    }
    
    public void setNotifyChannels(boolean notify) {
        prefs.putBoolean(NOTIFY_CHANNELS, notify);
    }
    
    public boolean isDndEnabled() {
        return prefs.getBoolean(DND_ENABLED, false);
    }
    
    public void setDndEnabled(boolean enabled) {
        prefs.putBoolean(DND_ENABLED, enabled);
    }
    
    public String getDndStartTime() {
        return prefs.get(DND_START_TIME, "22:00");
    }
    
    public void setDndStartTime(String time) {
        prefs.put(DND_START_TIME, time);
    }
    
    public String getDndEndTime() {
        return prefs.get(DND_END_TIME, "08:00");
    }
    
    public void setDndEndTime(String time) {
        prefs.put(DND_END_TIME, time);
    }
    
    public boolean isDndImportantOnly() {
        return prefs.getBoolean(DND_IMPORTANT_ONLY, false);
    }
    
    public void setDndImportantOnly(boolean importantOnly) {
        prefs.putBoolean(DND_IMPORTANT_ONLY, importantOnly);
    }
    
    public String getNotificationSound() {
        return prefs.get(NOTIFICATION_SOUND, "Default");
    }
    
    public void setNotificationSound(String sound) {
        prefs.put(NOTIFICATION_SOUND, sound);
    }
    
    public double getVolume() {
        return prefs.getDouble(VOLUME, 70.0);
    }
    
    public void setVolume(double volume) {
        prefs.putDouble(VOLUME, volume);
    }
    
    // Privacy Settings
    public String getOnlineStatusVisibility() {
        return prefs.get(ONLINE_STATUS_VISIBILITY, "Everyone");
    }
    
    public void setOnlineStatusVisibility(String visibility) {
        prefs.put(ONLINE_STATUS_VISIBILITY, visibility);
    }
    
    public String getGroupAddPermission() {
        return prefs.get(GROUP_ADD_PERMISSION, "Contacts only");
    }
    
    public void setGroupAddPermission(String permission) {
        prefs.put(GROUP_ADD_PERMISSION, permission);
    }
    
    public boolean isSendReadReceipts() {
        return prefs.getBoolean(SEND_READ_RECEIPTS, true);
    }
    
    public void setSendReadReceipts(boolean send) {
        prefs.putBoolean(SEND_READ_RECEIPTS, send);
    }
    
    public boolean isSendTypingIndicators() {
        return prefs.getBoolean(SEND_TYPING_INDICATORS, true);
    }
    
    public void setSendTypingIndicators(boolean send) {
        prefs.putBoolean(SEND_TYPING_INDICATORS, send);
    }
    
    public boolean isSaveMedia() {
        return prefs.getBoolean(SAVE_MEDIA, true);
    }
    
    public void setSaveMedia(boolean save) {
        prefs.putBoolean(SAVE_MEDIA, save);
    }
    
    public String getDownloadFolder() {
        String userHome = System.getProperty("user.home");
        return prefs.get(DOWNLOAD_FOLDER, userHome + "/Downloads/Telegram");
    }
    
    public void setDownloadFolder(String folder) {
        prefs.put(DOWNLOAD_FOLDER, folder);
    }
    
    public boolean isCompressImages() {
        return prefs.getBoolean(COMPRESS_IMAGES, false);
    }
    
    public void setCompressImages(boolean compress) {
        prefs.putBoolean(COMPRESS_IMAGES, compress);
    }
    
    // Advanced Settings
    public boolean isAutoConnect() {
        return prefs.getBoolean(AUTO_CONNECT, true);
    }
    
    public void setAutoConnect(boolean autoConnect) {
        prefs.putBoolean(AUTO_CONNECT, autoConnect);
    }
    
    // Utility methods
    public void save() {
        try {
            prefs.flush();
            logger.info("User preferences saved successfully");
        } catch (Exception e) {
            logger.error("Error saving user preferences", e);
        }
    }
    
    public void resetToDefaults() {
        try {
            prefs.clear();
            logger.info("User preferences reset to defaults");
        } catch (Exception e) {
            logger.error("Error resetting user preferences", e);
        }
    }
    
    // Helper method to check if DND is currently active
    public boolean isDndActive() {
        if (!isDndEnabled()) {
            return false;
        }
        
        try {
            java.time.LocalTime now = java.time.LocalTime.now();
            java.time.LocalTime start = java.time.LocalTime.parse(getDndStartTime());
            java.time.LocalTime end = java.time.LocalTime.parse(getDndEndTime());
            
            if (start.isBefore(end)) {
                // Same day range (e.g., 08:00 to 22:00)
                return now.isAfter(start) && now.isBefore(end);
            } else {
                // Overnight range (e.g., 22:00 to 08:00)
                return now.isAfter(start) || now.isBefore(end);
            }
        } catch (Exception e) {
            logger.warn("Error checking DND status", e);
            return false;
        }
    }
    
    // Static convenience methods for common operations
    public static String getCurrentTheme() {
        return getInstance().getTheme();
    }
    
    public static void setCurrentTheme(String theme) {
        getInstance().setTheme(theme);
    }
    
    public static boolean areNotificationsEnabled() {
        return getInstance().isNotificationsEnabled();
    }
}
