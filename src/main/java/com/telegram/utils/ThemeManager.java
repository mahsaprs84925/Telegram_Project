package com.telegram.utils;

import javafx.scene.Scene;

/**
 * Utility class for managing application themes
 */
public class ThemeManager {
    
    public static void applyTheme(Scene scene) {
        // Get the user's theme preference
        UserPreferences userPreferences = UserPreferences.getInstance();
        String theme = userPreferences.getTheme();
        
        // Clear existing stylesheets
        scene.getStylesheets().clear();
        
        // Apply theme-specific stylesheet
        switch (theme) {
            case "Light":
                scene.getStylesheets().add(ThemeManager.class.getResource("/css/telegram-theme.css").toExternalForm());
                break;
            case "Dark":
                scene.getStylesheets().add(ThemeManager.class.getResource("/css/telegram-theme.css").toExternalForm());
                scene.getStylesheets().add(ThemeManager.class.getResource("/css/dark-theme.css").toExternalForm());
                break;
            case "Auto":
            case "System":
                // For now, default to light theme
                scene.getStylesheets().add(ThemeManager.class.getResource("/css/telegram-theme.css").toExternalForm());
                break;
            default:
                scene.getStylesheets().add(ThemeManager.class.getResource("/css/telegram-theme.css").toExternalForm());
                break;
        }
    }
    
    public static String getDefaultTheme() {
        return "/css/telegram-theme.css";
    }
}
