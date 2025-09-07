package com.telegram.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Utility class for formatting last seen timestamps
 */
public class LastSeenFormatter {
    
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("MMM dd 'at' HH:mm");
    private static final DateTimeFormatter FULL_DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm");
    
    /**
     * Format a last seen timestamp for display
     * @param lastSeen The last seen timestamp
     * @return Formatted string like "Online", "last seen 5 minutes ago", etc.
     */
    public static String formatLastSeen(LocalDateTime lastSeen) {
        if (lastSeen == null) {
            return "Unknown";
        }
        
        LocalDateTime now = LocalDateTime.now();
        long minutesAgo = ChronoUnit.MINUTES.between(lastSeen, now);
        long hoursAgo = ChronoUnit.HOURS.between(lastSeen, now);
        long daysAgo = ChronoUnit.DAYS.between(lastSeen, now);
        
        if (minutesAgo < 1) {
            return "Online";
        } else if (minutesAgo < 5) {
            return "last seen recently";
        } else if (minutesAgo < 60) {
            return "last seen " + minutesAgo + " minute" + (minutesAgo == 1 ? "" : "s") + " ago";
        } else if (hoursAgo < 24) {
            return "last seen " + hoursAgo + " hour" + (hoursAgo == 1 ? "" : "s") + " ago";
        } else if (daysAgo == 1) {
            return "last seen yesterday at " + lastSeen.format(TIME_FORMAT);
        } else if (daysAgo < 7) {
            return "last seen " + getWeekdayName(lastSeen) + " at " + lastSeen.format(TIME_FORMAT);
        } else if (daysAgo < 365) {
            return "last seen " + lastSeen.format(DATE_TIME_FORMAT);
        } else {
            return "last seen " + lastSeen.format(FULL_DATE_FORMAT);
        }
    }
    
    /**
     * Get a user-friendly online status for chat headers
     */
    public static String formatOnlineStatus(boolean isOnline, LocalDateTime lastSeen) {
        if (isOnline) {
            return "Online";
        } else {
            return formatLastSeen(lastSeen);
        }
    }
    
    /**
     * Get style class for last seen status
     */
    public static String getStatusStyle(boolean isOnline, LocalDateTime lastSeen) {
        if (isOnline) {
            return "-fx-font-size: 13px; -fx-text-fill: #4CAF50; -fx-font-weight: bold;"; // Green for online
        } else {
            LocalDateTime now = LocalDateTime.now();
            long minutesAgo = lastSeen != null ? ChronoUnit.MINUTES.between(lastSeen, now) : Long.MAX_VALUE;
            
            if (minutesAgo < 5) {
                return "-fx-font-size: 13px; -fx-text-fill: #FF9800;"; // Orange for recently online
            } else {
                return "-fx-font-size: 13px; -fx-text-fill: #9E9E9E;"; // Gray for offline
            }
        }
    }
    
    private static String getWeekdayName(LocalDateTime dateTime) {
        switch (dateTime.getDayOfWeek()) {
            case MONDAY: return "Monday";
            case TUESDAY: return "Tuesday";
            case WEDNESDAY: return "Wednesday";
            case THURSDAY: return "Thursday";
            case FRIDAY: return "Friday";
            case SATURDAY: return "Saturday";
            case SUNDAY: return "Sunday";
            default: return "";
        }
    }
}
