package com.telegram.utils;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.animation.PauseTransition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.TrayIcon.MessageType;

/**
 * Utility class for showing desktop notifications
 */
public class NotificationUtils {
    private static final Logger logger = LoggerFactory.getLogger(NotificationUtils.class);
    private static SystemTray systemTray;
    private static TrayIcon trayIcon;
    
    static {
        initializeSystemTray();
    }
    
    private static void initializeSystemTray() {
        try {
            if (SystemTray.isSupported()) {
                systemTray = SystemTray.getSystemTray();
                
                // Create a simple tray icon (using a default icon)
                Image trayImage = Toolkit.getDefaultToolkit().createImage("icon.png");
                trayIcon = new TrayIcon(trayImage, "Telegram Clone");
                trayIcon.setImageAutoSize(true);
                trayIcon.setToolTip("Telegram Clone");
                
                // Add to system tray
                systemTray.add(trayIcon);
                logger.info("System tray initialized successfully");
            } else {
                logger.warn("System tray is not supported on this platform");
            }
        } catch (Exception e) {
            logger.error("Failed to initialize system tray", e);
        }
    }
    
    /**
     * Show a desktop notification using system tray
     */
    public static void showSystemNotification(String title, String message, MessageType type) {
        if (!UserPreferences.areNotificationsEnabled()) {
            return;
        }
        
        try {
            if (trayIcon != null) {
                trayIcon.displayMessage(title, message, type);
                logger.debug("Showed system notification: {} - {}", title, message);
            } else {
                logger.warn("Cannot show system notification - tray icon not available");
            }
        } catch (Exception e) {
            logger.error("Error showing system notification", e);
        }
    }
    
    /**
     * Show a simple message notification
     */
    public static void showMessageNotification(String senderName, String messageContent) {
        if (!UserPreferences.areNotificationsEnabled()) {
            return;
        }
        
        // Truncate long messages
        String truncatedMessage = messageContent.length() > 50 ? 
            messageContent.substring(0, 47) + "..." : messageContent;
        
        showSystemNotification(
            senderName, 
            truncatedMessage, 
            MessageType.INFO
        );
    }
    
    /**
     * Show a JavaFX-based notification popup (fallback)
     */
    public static void showJavaFXNotification(String title, String message, Window owner) {
        if (!UserPreferences.areNotificationsEnabled()) {
            return;
        }
        
        Platform.runLater(() -> {
            try {
                // Create a popup notification
                Popup popup = new Popup();
                
                VBox content = new VBox(5);
                content.setStyle("-fx-background-color: #2b2b2b; -fx-background-radius: 8; " +
                               "-fx-padding: 12; -fx-border-color: #555; -fx-border-width: 1; " +
                               "-fx-border-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 3);");
                content.setAlignment(Pos.CENTER_LEFT);
                content.setMaxWidth(300);
                
                Label titleLabel = new Label(title);
                titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: white;");
                
                Label messageLabel = new Label(message);
                messageLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #cccccc; -fx-wrap-text: true;");
                messageLabel.setWrapText(true);
                
                content.getChildren().addAll(titleLabel, messageLabel);
                popup.getContent().add(content);
                
                // Position at top-right of screen
                if (owner != null) {
                    popup.show(owner, owner.getX() + owner.getWidth() - 320, owner.getY() + 50);
                } else {
                    // Show at top-right of primary screen - use stage from Platform.getPrimaryStage() if available
                    // For now, just show at a fixed position
                    popup.show(javafx.stage.Stage.getWindows().stream()
                        .filter(window -> window instanceof javafx.stage.Stage)
                        .map(window -> (javafx.stage.Stage) window)
                        .findFirst()
                        .orElse(null), 
                        javafx.stage.Screen.getPrimary().getVisualBounds().getMaxX() - 320,
                        javafx.stage.Screen.getPrimary().getVisualBounds().getMinY() + 50);
                }
                
                // Auto-hide after 4 seconds
                PauseTransition delay = new PauseTransition(Duration.seconds(4));
                delay.setOnFinished(e -> popup.hide());
                delay.play();
                
                // Click to dismiss
                content.setOnMouseClicked(e -> popup.hide());
                
                logger.debug("Showed JavaFX notification: {} - {}", title, message);
                
            } catch (Exception e) {
                logger.error("Error showing JavaFX notification", e);
            }
        });
    }
    
    /**
     * Show notification with automatic fallback
     */
    public static void showNotification(String title, String message, Window owner) {
        try {
            // Try system tray first
            if (SystemTray.isSupported() && trayIcon != null) {
                showSystemNotification(title, message, MessageType.INFO);
            } else {
                // Fallback to JavaFX popup
                showJavaFXNotification(title, message, owner);
            }
        } catch (Exception e) {
            logger.error("Error showing notification", e);
        }
    }
}
