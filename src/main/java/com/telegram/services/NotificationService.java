package com.telegram.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;

/**
 * Service for handling system notifications with native desktop integration.
 * Falls back to JavaFX alerts if system notifications are not available.
 */
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    private static NotificationService instance;
    private boolean systemTraySupported;
    private SystemTray systemTray;
    private TrayIcon trayIcon;
    
    private NotificationService() {
        initializeSystemTray();
    }
    
    public static NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }
    
    private void initializeSystemTray() {
        try {
            if (SystemTray.isSupported()) {
                systemTray = SystemTray.getSystemTray();
                
                // Create a simple tray icon for notifications (use default if custom icon not found)
                Image image;
                try {
                    // Try to load custom icon
                    File iconFile = new File("src/main/resources/icons/telegram_icon.png");
                    if (iconFile.exists()) {
                        image = Toolkit.getDefaultToolkit().getImage(iconFile.getAbsolutePath());
                    } else {
                        // Fallback to a simple colored rectangle
                        image = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_RGB);
                        Graphics2D g2d = ((java.awt.image.BufferedImage) image).createGraphics();
                        g2d.setColor(new Color(0, 136, 204)); // Telegram blue
                        g2d.fillRect(0, 0, 16, 16);
                        g2d.dispose();
                    }
                } catch (Exception e) {
                    // Create a simple fallback icon
                    image = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2d = ((java.awt.image.BufferedImage) image).createGraphics();
                    g2d.setColor(new Color(0, 136, 204));
                    g2d.fillRect(0, 0, 16, 16);
                    g2d.dispose();
                }
                
                trayIcon = new TrayIcon(image, "Telegram Clone");
                trayIcon.setImageAutoSize(true);
                
                systemTraySupported = true;
                logger.info("System tray notification support initialized successfully");
            } else {
                systemTraySupported = false;
                logger.warn("System tray is not supported on this platform");
            }
        } catch (Exception e) {
            systemTraySupported = false;
            logger.error("Failed to initialize system tray: {}", e.getMessage());
        }
    }
    
    /**
     * Show a native system notification
     * @param title The notification title
     * @param message The notification message
     * @param type The notification type (INFO, WARNING, ERROR)
     */
    public void showNotification(String title, String message, NotificationType type) {
        if (systemTraySupported && trayIcon != null) {
            try {
                // Ensure tray icon is added (remove first if already added)
                try {
                    systemTray.remove(trayIcon);
                } catch (Exception ignored) {}
                
                systemTray.add(trayIcon);
                
                TrayIcon.MessageType messageType;
                switch (type) {
                    case ERROR:
                        messageType = TrayIcon.MessageType.ERROR;
                        break;
                    case WARNING:
                        messageType = TrayIcon.MessageType.WARNING;
                        break;
                    case SUCCESS:
                    case INFO:
                    default:
                        messageType = TrayIcon.MessageType.INFO;
                        break;
                }
                
                trayIcon.displayMessage(title, message, messageType);
                
                // Remove tray icon after a delay to avoid clutter
                new Thread(() -> {
                    try {
                        Thread.sleep(5000); // Show for 5 seconds
                        systemTray.remove(trayIcon);
                    } catch (Exception ignored) {}
                }).start();
                
                logger.debug("Native notification shown: {} - {}", title, message);
            } catch (Exception e) {
                logger.error("Failed to show native notification: {}", e.getMessage());
                // Don't fallback to alert - this reduces spam
            }
        } else {
            logger.debug("System tray not available, notification suppressed: {} - {}", title, message);
        }
    }
    
    /**
     * Show a simple info notification
     */
    public void showInfo(String title, String message) {
        showNotification(title, message, NotificationType.INFO);
    }
    
    /**
     * Show a success notification
     */
    public void showSuccess(String title, String message) {
        showNotification(title, message, NotificationType.SUCCESS);
    }
    
    /**
     * Show an error notification
     */
    public void showError(String title, String message) {
        showNotification(title, message, NotificationType.ERROR);
    }
    
    /**
     * Show a warning notification
     */
    public void showWarning(String title, String message) {
        showNotification(title, message, NotificationType.WARNING);
    }
    
    public enum NotificationType {
        INFO, SUCCESS, WARNING, ERROR
    }
}
