package com.telegram;

import com.telegram.database.DatabaseManager;
import com.telegram.utils.ThemeManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main application class for Telegram Clone
 * Handles application startup and initialization
 */
public class TelegramApp extends Application {
    
    private static final Logger logger = LoggerFactory.getLogger(TelegramApp.class);
    private static Stage primaryStage;
    
    @Override
    public void start(Stage stage) {
        try {
            primaryStage = stage;
            
            // Initialize database
            DatabaseManager.getInstance().initializeDatabase();
            
            // Create test users for development
            DatabaseManager.getInstance().createTestUsers();
            
            // Load login screen
            showLoginScreen();
            
        } catch (Exception e) {
            logger.error("Failed to start application", e);
            System.exit(1);
        }
    }
    
    public static void showLoginScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(TelegramApp.class.getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root, 500, 650);
            ThemeManager.applyTheme(scene);
            
            primaryStage.setTitle("Telegram Clone - Login");
            primaryStage.setScene(scene);
            primaryStage.setResizable(true);
            primaryStage.centerOnScreen();
            primaryStage.show();
            
        } catch (Exception e) {
            logger.error("Failed to load login screen", e);
        }
    }
    
    public static void showMainApplication() {
        try {
            FXMLLoader loader = new FXMLLoader(TelegramApp.class.getResource("/fxml/main.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root, 1400, 900);
            ThemeManager.applyTheme(scene);
            
            primaryStage.setTitle("Telegram Clone");
            primaryStage.setScene(scene);
            primaryStage.setResizable(true);
            primaryStage.setMinWidth(1000);
            primaryStage.setMinHeight(700);
            primaryStage.centerOnScreen();
            primaryStage.show();
            
        } catch (Exception e) {
            logger.error("Failed to load main application", e);
        }
    }
    
    public static Stage getPrimaryStage() {
        return primaryStage;
    }
    
    @Override
    public void stop() {
        try {
            // Set current user offline before closing
            com.telegram.services.UserService userService = com.telegram.services.UserService.getInstance();
            com.telegram.services.FileBasedMessageBroker messageBroker = com.telegram.services.FileBasedMessageBroker.getInstance();
            
            com.telegram.models.User currentUser = userService.getCurrentUser();
            if (currentUser != null) {
                messageBroker.setUserOnline(currentUser.getUserId(), false);
                logger.info("Set user {} offline during application shutdown", currentUser.getUserId());
            }
            
            DatabaseManager.getInstance().closeConnection();
        } catch (Exception e) {
            logger.error("Error during application shutdown", e);
        }
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
