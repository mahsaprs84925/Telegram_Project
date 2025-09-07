package com.telegram.controllers;

import com.telegram.utils.UserPreferences;
import com.telegram.services.UserService;
import com.telegram.models.User;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Optional;
import java.util.prefs.Preferences;

/**
 * Controller for the Settings dialog
 */
public class SettingsController {
    private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);
    
    // Dialog stage reference
    private Stage dialogStage;
    
    // Tabs
    @FXML private TabPane settingsTabPane;
    @FXML private Tab generalTab;
    @FXML private Tab privacyTab;
    @FXML private Tab advancedTab;
    
    // General Settings
    @FXML private ComboBox<String> themeComboBox;
    
    // Privacy Settings
    @FXML private CheckBox showLastSeenCheckBox;
    @FXML private ComboBox<String> onlineStatusVisibilityComboBox;
    @FXML private ComboBox<String> groupAddPermissionComboBox;
    @FXML private CheckBox readReceiptsCheckBox;
    @FXML private CheckBox typingIndicatorsCheckBox;
    @FXML private CheckBox saveMediaCheckBox;
    @FXML private TextField downloadFolderField;
    @FXML private Button browseDownloadButton;
    @FXML private CheckBox compressImagesCheckBox;
    @FXML private Button clearCacheButton;
    
    // Advanced Settings
    @FXML private CheckBox autoConnectCheckBox;
    @FXML private Button reportBugButton;
    
    // Dialog buttons
    @FXML private Button resetButton;
    @FXML private Button cancelButton;
    @FXML private Button applyButton;
    
    // User preferences instance
    private UserPreferences userPreferences;
    private UserService userService;
    
    @FXML
    private void initialize() {
        userPreferences = UserPreferences.getInstance();
        userService = UserService.getInstance();
        
        setupUI();
        setupEventHandlers();
        loadCurrentSettings();
        
        logger.info("Settings controller initialized");
    }
    
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
        
        // Apply current theme to the dialog
        applyThemeToDialog();
    }
    
    private void setupUI() {
        // Setup ComboBox items (only for controls that exist in simplified FXML)
        if (themeComboBox != null) {
            themeComboBox.getItems().addAll("Light", "Dark", "System");
            themeComboBox.setValue("System");
        }
        
        if (onlineStatusVisibilityComboBox != null) {
            onlineStatusVisibilityComboBox.setValue("Everyone");
        }
        if (groupAddPermissionComboBox != null) {
            groupAddPermissionComboBox.setValue("Contacts only");
        }
        
        // Setup download folder default
        if (downloadFolderField != null) {
            String userHome = System.getProperty("user.home");
            downloadFolderField.setText(userHome + "/Downloads/Telegram");
        }
    }
    
    private void setupEventHandlers() {
        // Dialog button handlers (only for controls that exist)
        if (applyButton != null) {
            applyButton.setOnAction(e -> handleApply());
        }
        if (cancelButton != null) {
            cancelButton.setOnAction(e -> handleCancel());
        }
        
        // Theme handler
        if (themeComboBox != null) {
            themeComboBox.setOnAction(e -> onThemeChanged());
        }
    }
    
    private void loadCurrentSettings() {
        try {
            // Load only settings for controls that exist in simplified FXML
            if (themeComboBox != null) {
                themeComboBox.setValue(userPreferences.getTheme());
            }
            
            // Load privacy settings from current user
            User currentUser = userService.getCurrentUser();
            if (currentUser != null) {
                if (showLastSeenCheckBox != null) {
                    showLastSeenCheckBox.setSelected(currentUser.isShowLastSeen());
                }
                if (readReceiptsCheckBox != null) {
                    readReceiptsCheckBox.setSelected(true); // Default value for now
                }
                if (typingIndicatorsCheckBox != null) {
                    typingIndicatorsCheckBox.setSelected(currentUser.isShowTypingIndicators());
                }
            }
            
        } catch (Exception e) {
            logger.error("Error loading current settings", e);
            showAlert("Error", "Failed to load current settings: " + e.getMessage());
        }
    }
    
    private void saveSettings() {
        try {
            // Save only settings for controls that exist in simplified FXML
            if (themeComboBox != null && themeComboBox.getValue() != null) {
                userPreferences.setTheme(themeComboBox.getValue());
            }
            
            // Save privacy settings to current user
            User currentUser = userService.getCurrentUser();
            if (currentUser != null) {
                boolean privacyChanged = false;
                
                if (showLastSeenCheckBox != null) {
                    currentUser.setShowLastSeen(showLastSeenCheckBox.isSelected());
                    privacyChanged = true;
                }
                
                if (typingIndicatorsCheckBox != null) {
                    currentUser.setShowTypingIndicators(typingIndicatorsCheckBox.isSelected());
                    privacyChanged = true;
                }
                
                if (privacyChanged) {
                    userService.updateUser(currentUser);
                }
            }
            
            // Save to persistent storage
            userPreferences.save();
            
            logger.info("Settings saved successfully");
            
        } catch (Exception e) {
            logger.error("Error saving settings", e);
            showAlert("Error", "Failed to save settings: " + e.getMessage());
        }
    }
    
    // Event handlers
    @FXML
    private void handleApply() {
        saveSettings();
        showAlert("Settings", "Settings have been applied successfully.");
        
        // Apply theme change immediately if needed
        if (themeComboBox.getValue() != null) {
            applyThemeChange();
        }
        
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
    
    @FXML
    private void handleCancel() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
    
    @FXML
    private void handleReset() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Reset Settings");
        alert.setHeaderText("Reset all settings to defaults?");
        alert.setContentText("This will reset all your preferences to their default values. This action cannot be undone.");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                userPreferences.resetToDefaults();
                loadCurrentSettings();
                showAlert("Settings", "All settings have been reset to defaults.");
            }
        });
    }
    
    private void onThemeChanged() {
        // Preview theme change
        String selectedTheme = themeComboBox.getValue();
        if (selectedTheme != null) {
            logger.info("Theme changed to: {}", selectedTheme);
        }
    }
    
    private void handleChangePassword() {
        // Open change password dialog
        showAlert("Change Password", "Change password functionality would be implemented here.");
    }
    
    private void handleBrowseDownloadFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Download Folder");
        directoryChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        
        File selectedDirectory = directoryChooser.showDialog(dialogStage);
        if (selectedDirectory != null) {
            downloadFolderField.setText(selectedDirectory.getAbsolutePath());
        }
    }
    
    private void handleClearCache() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Clear Cache");
        alert.setHeaderText("Clear application cache?");
        alert.setContentText("This will clear all cached data including downloaded media. This action cannot be undone.");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Clear cache directories
                    String userHome = System.getProperty("user.home");
                    java.io.File cacheDir = new java.io.File(userHome, ".telegram-clone/cache");
                    
                    if (cacheDir.exists()) {
                        deleteDirectory(cacheDir);
                        logger.info("Cache directory cleared: {}", cacheDir.getAbsolutePath());
                    }
                    
                    // Also clear temp files
                    java.io.File tempDir = new java.io.File(System.getProperty("java.io.tmpdir"), "telegram-clone");
                    if (tempDir.exists()) {
                        deleteDirectory(tempDir);
                        logger.info("Temp directory cleared: {}", tempDir.getAbsolutePath());
                    }
                    
                    showAlert("Cache Cleared", "Application cache has been cleared successfully.");
                    
                } catch (Exception e) {
                    logger.error("Error clearing cache", e);
                    showAlert("Error", "Failed to clear cache: " + e.getMessage());
                }
            }
        });
    }
    
    private void deleteDirectory(java.io.File directory) {
        if (directory.isDirectory()) {
            java.io.File[] files = directory.listFiles();
            if (files != null) {
                for (java.io.File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        directory.delete();
    }
    
    private void handleReportBug() {
        try {
            // Create a bug report dialog
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Report Bug");
            dialog.setHeaderText("Report a Bug");
            dialog.setContentText("Please describe the bug:");
            
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent() && !result.get().trim().isEmpty()) {
                // Create bug report file
                String bugReport = result.get().trim();
                java.io.File reportFile = new java.io.File(System.getProperty("user.home"), 
                    "telegram-bug-report-" + System.currentTimeMillis() + ".txt");
                
                try (java.io.PrintWriter writer = new java.io.PrintWriter(reportFile)) {
                    writer.println("Telegram Clone - Bug Report");
                    writer.println("Report Date: " + java.time.LocalDateTime.now());
                    writer.println("Java Version: " + System.getProperty("java.version"));
                    writer.println("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
                    writer.println("User Description:");
                    writer.println(bugReport);
                    writer.println();
                    writer.println("Current Settings:");
                    writer.println("Theme: " + userPreferences.getTheme());
                    writer.println("Notifications: " + userPreferences.isNotificationsEnabled());
                    writer.println("Compact Mode: " + userPreferences.isCompactMode());
                }
                
                showAlert("Bug Report", "Bug report saved to: " + reportFile.getAbsolutePath() + 
                    "\n\nPlease send this file to the development team.");
            }
        } catch (Exception e) {
            logger.error("Error creating bug report", e);
            showAlert("Error", "Failed to create bug report: " + e.getMessage());
        }
    }
    
    private void applyThemeChange() {
        // Apply theme change to the application
        String theme = themeComboBox.getValue();
        logger.info("Applying theme change: {}", theme);
        
        // Get the main stage and apply theme
        try {
            if (dialogStage != null) {
                javafx.stage.Stage mainStage = null;
                
                // Find the main stage (owner of this dialog)
                javafx.stage.Stage owner = (javafx.stage.Stage) dialogStage.getOwner();
                if (owner != null) {
                    mainStage = owner;
                }
                
                if (mainStage != null && mainStage.getScene() != null) {
                    // Clear existing stylesheets
                    mainStage.getScene().getStylesheets().clear();
                    
                    // Apply theme-specific stylesheet
                    switch (theme) {
                        case "Light":
                            mainStage.getScene().getStylesheets().add(getClass().getResource("/css/telegram-theme.css").toExternalForm());
                            break;
                        case "Dark":
                            mainStage.getScene().getStylesheets().add(getClass().getResource("/css/telegram-theme.css").toExternalForm());
                            mainStage.getScene().getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());
                            break;
                        case "Auto":
                        case "System":
                            // For now, default to light theme
                            mainStage.getScene().getStylesheets().add(getClass().getResource("/css/telegram-theme.css").toExternalForm());
                            break;
                        default:
                            mainStage.getScene().getStylesheets().add(getClass().getResource("/css/telegram-theme.css").toExternalForm());
                            break;
                    }
                    
                    logger.info("Applied theme to main application: {}", theme);
                }
            }
        } catch (Exception e) {
            logger.error("Error applying theme change", e);
        }
    }
    
    private void applyThemeToDialog() {
        if (dialogStage != null && dialogStage.getScene() != null) {
            String theme = userPreferences.getTheme();
            
            // Clear existing stylesheets
            dialogStage.getScene().getStylesheets().clear();
            
            // Apply theme-specific stylesheet
            switch (theme) {
                case "Light":
                    dialogStage.getScene().getStylesheets().add(getClass().getResource("/css/telegram-theme.css").toExternalForm());
                    break;
                case "Dark":
                    dialogStage.getScene().getStylesheets().add(getClass().getResource("/css/telegram-theme.css").toExternalForm());
                    dialogStage.getScene().getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());
                    break;
                case "Auto":
                case "System":
                    // For now, default to light theme
                    dialogStage.getScene().getStylesheets().add(getClass().getResource("/css/telegram-theme.css").toExternalForm());
                    break;
                default:
                    dialogStage.getScene().getStylesheets().add(getClass().getResource("/css/telegram-theme.css").toExternalForm());
                    break;
            }
            
            // Force the root node to refresh styling
            if (dialogStage.getScene().getRoot() != null) {
                dialogStage.getScene().getRoot().applyCss();
            }
            
            logger.info("Applied theme to settings dialog: {}", theme);
        }
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
