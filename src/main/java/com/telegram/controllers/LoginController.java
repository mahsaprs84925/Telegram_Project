package com.telegram.controllers;

import com.telegram.TelegramApp;
import com.telegram.services.UserService;
import com.telegram.utils.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller for the login screen
 */
public class LoginController {
    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);
    
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator loadingIndicator;
    
    private UserService userService;
    
    @FXML
    private void initialize() {
        userService = UserService.getInstance();
        loadingIndicator.setVisible(false);
        
        // Check for saved session
        checkSavedSession();
        
        // Set up event handlers
        loginButton.setOnAction(e -> handleLogin());
        registerButton.setOnAction(e -> handleRegister());
        
        // Set up validation listeners
        usernameField.textProperty().addListener((obs, oldText, newText) -> updateLoginButtonState());
        passwordField.textProperty().addListener((obs, oldText, newText) -> updateLoginButtonState());
        
        // Initial validation
        updateLoginButtonState();
        
        // Handle Enter key press
        usernameField.setOnAction(e -> handleLogin());
        passwordField.setOnAction(e -> handleLogin());
    }
    
    private void checkSavedSession() {
        SessionManager sessionManager = SessionManager.getInstance();
        String savedUsername = sessionManager.loadSession();
        if (savedUsername != null && !savedUsername.trim().isEmpty()) {
            logger.info("Valid session found for user: {}, auto-logging in...", savedUsername);
            
            // Login the user through UserService to properly set currentUser
            new Thread(() -> {
                try {
                    boolean success = userService.loginUserFromSession(savedUsername);
                    
                    Platform.runLater(() -> {
                        if (success) {
                            logger.info("Auto-login successful for user: {}", savedUsername);
                            showStatus("Welcome back!", true);
                            // Small delay to show the welcome message
                            new Thread(() -> {
                                try {
                                    Thread.sleep(500);
                                    Platform.runLater(() -> TelegramApp.showMainApplication());
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            }).start();
                        } else {
                            logger.warn("Auto-login failed for user: {}, clearing session", savedUsername);
                            sessionManager.clearSession();
                            showStatus("Session expired, please login again", false);
                        }
                    });
                } catch (Exception e) {
                    logger.error("Error during auto-login for user: {}", savedUsername, e);
                    Platform.runLater(() -> {
                        sessionManager.clearSession();
                        showStatus("Session expired, please login again", false);
                    });
                }
            }).start();
        } else {
            logger.info("No valid session found, showing login form");
        }
    }
    
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        
        if (username.isEmpty() || password.isEmpty()) {
            showStatus("Please fill in all fields", false);
            return;
        }
        
        setLoading(true);
        
        // Run login in background thread
        new Thread(() -> {
            try {
                boolean success = userService.loginUser(username, password);
                
                Platform.runLater(() -> {
                    setLoading(false);
                    if (success) {
                        // Always save session on successful login
                        SessionManager.getInstance().saveSession(username);
                        
                        showStatus("Login successful!", true);
                        // Delay to show success message
                        new Thread(() -> {
                            try {
                                Thread.sleep(1000);
                                Platform.runLater(() -> TelegramApp.showMainApplication());
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }).start();
                    } else {
                        showStatus("Invalid username or password", false);
                        passwordField.clear();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setLoading(false);
                    showStatus("Login failed: " + e.getMessage(), false);
                });
            }
        }).start();
    }
    
    @FXML
    private void handleRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle("Register - Telegram Clone");
            stage.setScene(new Scene(root, 600, 700));
            stage.setResizable(false);
            stage.show();
            
        } catch (Exception e) {
            logger.error("Error opening register window", e);
            showStatus("Error opening registration window", false);
        }
    }
    
    private void setLoading(boolean loading) {
        loadingIndicator.setVisible(loading);
        loginButton.setDisable(loading);
        registerButton.setDisable(loading);
        usernameField.setDisable(loading);
        passwordField.setDisable(loading);
    }
    
    private void updateLoginButtonState() {
        boolean allFieldsFilled = !usernameField.getText().trim().isEmpty() &&
                                 !passwordField.getText().isEmpty();
        loginButton.setDisable(!allFieldsFilled);
    }
    
    private void showStatus(String message, boolean success) {
        statusLabel.setText(message);
        statusLabel.setStyle(success ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }
}
