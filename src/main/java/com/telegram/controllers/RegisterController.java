package com.telegram.controllers;

import com.telegram.services.UserService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Controller for the registration screen
 */
public class RegisterController {
    private static final Logger logger = LoggerFactory.getLogger(RegisterController.class);
    
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField profileNameField;
    @FXML private TextArea bioField;
    @FXML private Label profilePictureLabel;
    @FXML private Button browseButton;
    @FXML private Button registerButton;
    @FXML private Button cancelButton;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label usernameRequirementsLabel;
    @FXML private Label passwordRequirementsLabel;
    
    private UserService userService;
    private String selectedProfilePicturePath;
    
    @FXML
    private void initialize() {
        userService = UserService.getInstance();
        loadingIndicator.setVisible(false);
        
        // Show requirements
        usernameRequirementsLabel.setText(userService.getUsernameRequirements());
        passwordRequirementsLabel.setText(userService.getPasswordRequirements());
        
        // Set up event handlers
        registerButton.setOnAction(e -> handleRegister());
        cancelButton.setOnAction(e -> handleCancel());
        browseButton.setOnAction(e -> handleBrowseProfilePicture());
        
        // Set up validation listeners for required fields
        usernameField.textProperty().addListener((obs, oldText, newText) -> updateRegisterButtonState());
        passwordField.textProperty().addListener((obs, oldText, newText) -> updateRegisterButtonState());
        confirmPasswordField.textProperty().addListener((obs, oldText, newText) -> updateRegisterButtonState());
        profileNameField.textProperty().addListener((obs, oldText, newText) -> updateRegisterButtonState());
        
        // Initial validation
        updateRegisterButtonState();
    }
    
    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String profileName = profileNameField.getText().trim();
        String bio = bioField.getText().trim();
        
        // Validation
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || profileName.isEmpty()) {
            showStatus("Please fill in all required fields", false);
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            showStatus("Passwords do not match", false);
            return;
        }
        
        setLoading(true);
        
        // Run registration in background thread
        new Thread(() -> {
            try {
                boolean success = userService.registerUser(username, password, profileName);
                
                Platform.runLater(() -> {
                    setLoading(false);
                    if (success) {
                        showStatus("Registration successful! You can now log in.", true);
                        // Close window after success
                        new Thread(() -> {
                            try {
                                Thread.sleep(2000);
                                Platform.runLater(() -> {
                                    Stage stage = (Stage) registerButton.getScene().getWindow();
                                    stage.close();
                                });
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }).start();
                    } else {
                        showStatus("Registration failed. Username might already exist or invalid format.", false);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setLoading(false);
                    showStatus("Registration failed: " + e.getMessage(), false);
                });
            }
        }).start();
    }
    
    @FXML
    private void handleCancel() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
    
    @FXML
    private void handleBrowseProfilePicture() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Picture");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        
        Stage stage = (Stage) browseButton.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        
        if (selectedFile != null) {
            selectedProfilePicturePath = selectedFile.getAbsolutePath();
            profilePictureLabel.setText("Selected: " + selectedFile.getName());
        }
    }
    
    private void setLoading(boolean loading) {
        loadingIndicator.setVisible(loading);
        registerButton.setDisable(loading);
        cancelButton.setDisable(loading);
        usernameField.setDisable(loading);
        passwordField.setDisable(loading);
        confirmPasswordField.setDisable(loading);
        profileNameField.setDisable(loading);
        bioField.setDisable(loading);
        browseButton.setDisable(loading);
    }
    
    private void updateRegisterButtonState() {
        boolean allFieldsFilled = !usernameField.getText().trim().isEmpty() &&
                                 !passwordField.getText().isEmpty() &&
                                 !confirmPasswordField.getText().isEmpty() &&
                                 !profileNameField.getText().trim().isEmpty();
        registerButton.setDisable(!allFieldsFilled);
    }
    
    private void showStatus(String message, boolean success) {
        statusLabel.setText(message);
        statusLabel.setStyle(success ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
        statusLabel.setVisible(true);
    }
}
