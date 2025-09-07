package com.telegram.controllers;

import com.telegram.TelegramApp;
import com.telegram.models.User;
import com.telegram.services.UserService;
import com.telegram.services.FileBasedMessageBroker;
import com.telegram.utils.ProfilePictureCropper;
import com.telegram.utils.SessionManager;
import com.telegram.utils.UserPreferences;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Controller for the profile management dialog
 */
public class ProfileController {
    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);
    
    @FXML private Circle profileImageCircle;
    @FXML private ImageView profileImageView;
    @FXML private Button changeProfilePictureButton;
    @FXML private Label usernameLabel;
    @FXML private TextField profileNameField;
    @FXML private TextArea bioField;
    @FXML private TextField usernameField;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Button logoutButton;
    @FXML private Button cancelButtonAlt;
    @FXML private HBox multiButtonContainer;
    @FXML private HBox singleButtonContainer;
    
    private UserService userService;
    private User currentUser;
    private Stage dialogStage;
    private FileBasedMessageBroker messageBroker;
    
    @FXML
    private void initialize() {
        userService = UserService.getInstance();
        messageBroker = FileBasedMessageBroker.getInstance();
        loadCurrentUser();
    }
    
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
        
        // Apply current theme to the dialog
        applyThemeToDialog();
    }
    
    /**
     * Set a specific user to display (for viewing other users' profiles)
     */
    public void setUser(User user) {
        this.currentUser = user;
        if (user != null) {
            // Populate form fields with the specified user's data
            usernameLabel.setText("@" + user.getUsername());
            profileNameField.setText(user.getProfileName());
            bioField.setText(user.getBio() != null ? user.getBio() : "");
            usernameField.setText(user.getUsername());
            
            // Make fields read-only since this is viewing another user's profile
            profileNameField.setEditable(false);
            bioField.setEditable(false);
            usernameField.setEditable(false);
            changeProfilePictureButton.setVisible(false);
            saveButton.setVisible(false);
            logoutButton.setVisible(false);
            // Keep cancelButton visible for the layout detection
            cancelButton.setVisible(true);
            
            // Adjust button layout when only cancel button is visible
            adjustButtonLayout();
            
            // Load profile picture if available
            if (user.getProfilePicturePath() != null && !user.getProfilePicturePath().isEmpty()) {
                loadProfilePicture(user.getProfilePicturePath());
            }
        }
    }
    
    private void loadCurrentUser() {
        currentUser = userService.getCurrentUser();
        if (currentUser != null) {
            // Populate form fields
            usernameLabel.setText("@" + currentUser.getUsername());
            profileNameField.setText(currentUser.getProfileName());
            bioField.setText(currentUser.getBio() != null ? currentUser.getBio() : "");
            usernameField.setText(currentUser.getUsername());
            
            // Load profile picture if available
            if (currentUser.getProfilePicturePath() != null && !currentUser.getProfilePicturePath().isEmpty()) {
                loadProfilePicture(currentUser.getProfilePicturePath());
            }
            
            // Adjust button layout for current user (all buttons visible, should be centered)
            adjustButtonLayout();
        }
    }
    
    private void loadProfilePicture(String imagePath) {
        try {
            if (imagePath != null && !imagePath.isEmpty()) {
                // Decode URL-encoded path (fix %20 to spaces, etc.)
                String decodedPath = java.net.URLDecoder.decode(imagePath, "UTF-8");
                logger.info("Loading profile picture: {} -> {}", imagePath, decodedPath);
                
                File imageFile = new File(decodedPath);
                logger.info("Profile picture file exists: {}, path: {}", imageFile.exists(), imageFile.getAbsolutePath());
                
                if (imageFile.exists()) {
                    Image image = new Image(imageFile.toURI().toString());
                    
                    // Ensure the profile picture fits properly in the circular view
                    profileImageView.setImage(image);
                    profileImageView.setFitWidth(120); // Standard size for profile dialog
                    profileImageView.setFitHeight(120);
                    profileImageView.setPreserveRatio(true); // Preserve ratio to avoid distortion
                    
                    // Create circular clip for profile dialog
                    Circle profileClip = new Circle(60, 60, 60); // Center at (60, 60) with radius 60
                    profileImageView.setClip(profileClip);
                    
                    profileImageView.setVisible(true);
                    profileImageCircle.setVisible(false);
                    
                    logger.info("Successfully loaded profile picture: {} ({}x{})", decodedPath, 
                        (int) image.getWidth(), (int) image.getHeight());
                } else {
                    logger.warn("Profile picture file not found: {}", decodedPath);
                    profileImageView.setVisible(false);
                    profileImageCircle.setVisible(true);
                }
            } else {
                logger.info("No profile picture path provided");
                profileImageView.setVisible(false);
                profileImageCircle.setVisible(true);
            }
        } catch (Exception e) {
            logger.error("Error loading profile picture: {}", e.getMessage());
            profileImageView.setVisible(false);
            profileImageCircle.setVisible(true);
        }
    }
    
    @FXML
    private void handleChangeProfilePicture() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Picture");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        
        File selectedFile = fileChooser.showOpenDialog(dialogStage);
        if (selectedFile != null) {
            // Show crop dialog
            ProfilePictureCropper.showCropDialog(dialogStage, selectedFile)
                .thenAccept(croppedFile -> {
                    if (croppedFile != null) {
                        try {
                            // Create profile pictures directory if it doesn't exist
                            Path profilePicturesDir = Paths.get("profile_pictures");
                            if (!Files.exists(profilePicturesDir)) {
                                Files.createDirectories(profilePicturesDir);
                            }
                            
                            // Save the cropped file with standard naming
                            String fileName = currentUser.getUserId() + "_" + selectedFile.getName().replaceAll("\\.[^.]+$", "") + "_cropped.png";
                            Path targetPath = profilePicturesDir.resolve(fileName);
                            Files.copy(croppedFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                            
                            // Load the new profile picture
                            javafx.application.Platform.runLater(() -> {
                                loadProfilePicture(targetPath.toString());
                                logger.info("Profile picture updated and cropped for user: {}", currentUser.getUsername());
                            });
                            
                        } catch (Exception e) {
                            logger.error("Error saving cropped profile picture", e);
                            javafx.application.Platform.runLater(() -> 
                                showAlert("Error", "Failed to save cropped profile picture: " + e.getMessage())
                            );
                        }
                    }
                })
                .exceptionally(throwable -> {
                    logger.error("Error in profile picture cropping", throwable);
                    javafx.application.Platform.runLater(() -> 
                        showAlert("Error", "Failed to crop profile picture: " + throwable.getMessage())
                    );
                    return null;
                });
        }
    }
    
    @FXML
    private void handleSaveProfile() {
        try {
            // Validate input
            String newProfileName = profileNameField.getText().trim();
            String newUsername = usernameField.getText().trim();
            String newBio = bioField.getText().trim();
            
            if (newProfileName.isEmpty()) {
                showAlert("Validation Error", "Display name cannot be empty.");
                return;
            }
            
            if (newUsername.isEmpty()) {
                showAlert("Validation Error", "Username cannot be empty.");
                return;
            }
            
            // Check if username is already taken (if changed)
            if (!newUsername.equals(currentUser.getUsername())) {
                if (userService.findUserByUsername(newUsername) != null) {
                    showAlert("Validation Error", "Username is already taken.");
                    return;
                }
            }
            
            // Update user information
            currentUser.setProfileName(newProfileName);
            currentUser.setUsername(newUsername);
            currentUser.setBio(newBio);
            
            // Update profile picture path if changed
            if (profileImageView.isVisible() && profileImageView.getImage() != null) {
                // The path is already set when the image is loaded
                String imagePath = profileImageView.getImage().getUrl();
                if (imagePath.startsWith("file:")) {
                    currentUser.setProfilePicturePath(imagePath.substring(5)); // Remove "file:" prefix
                }
            }
            
            // Save to database
            boolean success = userService.updateUser(currentUser);
            
            if (success) {
                // Broadcast profile update to all active sessions
                messageBroker.broadcastProfileUpdate(currentUser.getUserId());
                
                showAlert("Success", "Profile updated successfully!");
                if (dialogStage != null) {
                    dialogStage.close();
                }
            } else {
                showAlert("Error", "Failed to update profile. Please try again.");
            }
            
        } catch (Exception e) {
            logger.error("Error saving profile", e);
            showAlert("Error", "An error occurred while saving profile: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleCancel() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
    
    @FXML
    private void handleLogout() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Logout");
        confirmAlert.setHeaderText("Are you sure you want to logout?");
        confirmAlert.setContentText("You will need to login again to access your account.");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                // Clear saved session
                SessionManager.getInstance().clearSession();
                
                // Logout user
                userService.logoutUser();
                
                // Close profile dialog
                if (dialogStage != null) {
                    dialogStage.close();
                }
                
                // Show login screen
                TelegramApp.showLoginScreen();
            }
        });
    }
    
    /**
     * Adjusts the button container visibility based on which buttons should be shown
     */
    private void adjustButtonLayout() {
        // Count visible buttons in the main container
        int visibleButtons = 0;
        if (saveButton.isVisible()) visibleButtons++;
        if (cancelButton.isVisible()) visibleButtons++;
        if (logoutButton.isVisible()) visibleButtons++;
        
        logger.info("Adjusting button layout. Visible buttons: {}, Save: {}, Cancel: {}, Logout: {}", 
                   visibleButtons, saveButton.isVisible(), cancelButton.isVisible(), logoutButton.isVisible());
        
        // If only cancel button should be visible (viewing other user's profile)
        if (visibleButtons == 1 && cancelButton.isVisible()) {
            // Hide the multi-button container and show the single-button container
            multiButtonContainer.setVisible(false);
            multiButtonContainer.setManaged(false);
            
            singleButtonContainer.setVisible(true);
            singleButtonContainer.setManaged(true);
            
            logger.info("Switched to single button container (left-aligned cancel button)");
        } else {
            // Show the multi-button container and hide the single-button container
            multiButtonContainer.setVisible(true);
            multiButtonContainer.setManaged(true);
            
            singleButtonContainer.setVisible(false);
            singleButtonContainer.setManaged(false);
            
            logger.info("Switched to multi button container (centered buttons)");
        }
    }
    
    private void applyThemeToDialog() {
        if (dialogStage != null && dialogStage.getScene() != null) {
            UserPreferences userPreferences = UserPreferences.getInstance();
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
            
            logger.info("Applied theme to profile dialog: {}", theme);
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
