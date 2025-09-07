package com.telegram.controllers;

import com.telegram.models.Channel;
import com.telegram.models.User;
import com.telegram.services.ChatService;
import com.telegram.services.UserService;
import com.telegram.utils.UserPreferences;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

/**
 * Controller for channel management dialog
 */
public class ChannelManagementController {
    private static final Logger logger = LoggerFactory.getLogger(ChannelManagementController.class);
    
    @FXML private Circle channelImageCircle;
    @FXML private ImageView channelImageView;
    @FXML private Button changeChannelPictureButton;
    @FXML private Label channelNameLabel;
    @FXML private Label subscriberCountLabel;
    @FXML private TabPane channelTabPane;
    
    // Info Tab
    @FXML private TextField channelNameField;
    @FXML private TextArea descriptionField;
    @FXML private CheckBox privateChannelCheckBox;
    @FXML private ComboBox<String> channelTypeComboBox;
    @FXML private TextField inviteLinkField;
    @FXML private Button copyInviteLinkButton;
    @FXML private Button regenerateInviteLinkButton;
    
    // Subscribers Tab
    @FXML private TextField searchSubscribersField;
    @FXML private Button inviteSubscriberButton;
    @FXML private ListView<String> subscribersListView;
    
    // Admins Tab
    @FXML private ListView<String> adminsListView;
    @FXML private Button promoteToAdminButton;
    @FXML private Button removeAdminButton;
    @FXML private CheckBox canPostMessagesCheckBox;
    @FXML private CheckBox canEditMessagesCheckBox;
    @FXML private CheckBox canDeleteMessagesCheckBox;
    @FXML private CheckBox canInviteUsersCheckBox;
    @FXML private CheckBox canRestrictMembersCheckBox;
    @FXML private CheckBox canPinMessagesCheckBox;
    @FXML private CheckBox canAddAdminsCheckBox;
    
    // Analytics Tab
    @FXML private Label totalSubscribersLabel;
    @FXML private Label totalMessagesLabel;
    @FXML private Label activeTodayLabel;
    @FXML private Label createdDateLabel;
    
    // Action Buttons
    @FXML private Button saveChannelButton;
    @FXML private Button cancelChannelButton;
    @FXML private Button deleteChannelButton;
    
    private ChatService chatService;
    private UserService userService;
    private Channel currentChannel;
    private Stage dialogStage;
    private ObservableList<String> subscribersList;
    private ObservableList<String> adminsList;
    
    @FXML
    private void initialize() {
        chatService = ChatService.getInstance();
        userService = UserService.getInstance();
        
        subscribersList = FXCollections.observableArrayList();
        adminsList = FXCollections.observableArrayList();
        
        subscribersListView.setItems(subscribersList);
        adminsListView.setItems(adminsList);
        
        setupChannelTypeComboBox();
        setupEventHandlers();
    }
    
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
        
        // Apply current theme to the dialog
        applyThemeToDialog();
    }
    
    public void setChannel(Channel channel) {
        this.currentChannel = channel;
        loadChannelData();
    }
    
    private void setupChannelTypeComboBox() {
        channelTypeComboBox.getItems().addAll(
            "News Channel", "Entertainment", "Educational", 
            "Business", "Technology", "Sports", "General"
        );
        channelTypeComboBox.setValue("General");
    }
    
    private void setupEventHandlers() {
        // Search functionality for subscribers
        searchSubscribersField.textProperty().addListener((obs, oldText, newText) -> {
            filterSubscribers(newText);
        });
        
        // Enable/disable buttons based on selection
        subscribersListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            promoteToAdminButton.setDisable(newSelection == null);
        });
        
        adminsListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            removeAdminButton.setDisable(newSelection == null);
        });
        
        // Update invite link visibility based on private checkbox
        privateChannelCheckBox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            inviteLinkField.setVisible(!isSelected);
            copyInviteLinkButton.setVisible(!isSelected);
            regenerateInviteLinkButton.setVisible(!isSelected);
        });
        
        // Add context menu for subscriber management
        setupSubscriberContextMenu();
    }
    
    private void setupSubscriberContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        
        MenuItem promoteMenuItem = new MenuItem("Promote to Admin");
        promoteMenuItem.setOnAction(e -> handlePromoteToAdmin());
        
        MenuItem removeMenuItem = new MenuItem("Remove Subscriber");
        removeMenuItem.setOnAction(e -> handleRemoveSubscriber());
        
        contextMenu.getItems().addAll(promoteMenuItem, removeMenuItem);
        
        subscribersListView.setContextMenu(contextMenu);
        
        // Update menu items based on current user permissions and selection
        contextMenu.setOnShowing(e -> {
            String selectedSubscriber = subscribersListView.getSelectionModel().getSelectedItem();
            if (selectedSubscriber != null) {
                // Extract username from the display string
                String username = selectedSubscriber.substring(selectedSubscriber.indexOf("(@") + 2, selectedSubscriber.lastIndexOf(")"));
                User selectedUser = userService.findUserByUsername(username);
                User currentUser = userService.getCurrentUser();
                
                boolean isCurrentUserAdmin = currentChannel.isAdmin(currentUser.getUserId()) || 
                                           currentChannel.getOwnerId().equals(currentUser.getUserId());
                boolean isSelectedUserAdmin = selectedUser != null && currentChannel.isAdmin(selectedUser.getUserId());
                boolean isSelectedUserCreator = selectedUser != null && currentChannel.getOwnerId().equals(selectedUser.getUserId());
                boolean isSelf = selectedUser != null && selectedUser.getUserId().equals(currentUser.getUserId());
                
                // Enable/disable promote option
                promoteMenuItem.setDisable(!isCurrentUserAdmin || isSelectedUserAdmin || isSelectedUserCreator || isSelf);
                
                // Enable/disable remove option
                removeMenuItem.setDisable(!isCurrentUserAdmin || isSelectedUserCreator || isSelf);
            }
        });
    }
    
    private void loadChannelData() {
        if (currentChannel == null) return;
        
        // Basic info
        channelNameLabel.setText(currentChannel.getChannelName());
        channelNameField.setText(currentChannel.getChannelName());
        descriptionField.setText(currentChannel.getDescription() != null ? currentChannel.getDescription() : "");
        privateChannelCheckBox.setSelected(currentChannel.isPrivate());
        
        // Load profile picture if available
        if (currentChannel.getProfilePicturePath() != null && !currentChannel.getProfilePicturePath().isEmpty()) {
            loadChannelPicture(currentChannel.getProfilePicturePath());
        }
        
        // Load invite link
        inviteLinkField.setText(currentChannel.getInviteLink());
        
        // Load subscribers
        loadSubscribers();
        
        // Load admins
        loadAdmins();
        
        // Update subscriber count
        subscriberCountLabel.setText(currentChannel.getSubscriberIds().size() + " subscribers");
        
        // Load analytics
        loadAnalytics();
        
        // Set admin permissions (default values)
        canPostMessagesCheckBox.setSelected(true);
        canEditMessagesCheckBox.setSelected(true);
        canDeleteMessagesCheckBox.setSelected(true);
        canInviteUsersCheckBox.setSelected(false);
        canRestrictMembersCheckBox.setSelected(false);
        canPinMessagesCheckBox.setSelected(true);
        canAddAdminsCheckBox.setSelected(false);
        
        // Check if current user is owner/admin
        User currentUser = userService.getCurrentUser();
        boolean isOwner = currentChannel.getOwnerId().equals(currentUser.getUserId());
        boolean isAdmin = currentChannel.isAdmin(currentUser.getUserId()) || isOwner;
        
        // Enable/disable controls based on permissions
        setAdminControlsEnabled(isAdmin);
    }
    
    private void loadChannelPicture(String imagePath) {
        try {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                Image image = new Image(imageFile.toURI().toString());
                channelImageView.setImage(image);
                channelImageView.setVisible(true);
                channelImageCircle.setVisible(false);
            }
        } catch (Exception e) {
            logger.error("Error loading channel picture", e);
        }
    }
    
    private void loadSubscribers() {
        subscribersList.clear();
        for (String subscriberId : currentChannel.getSubscriberIds()) {
            User subscriber = userService.findUserById(subscriberId);
            if (subscriber != null) {
                subscribersList.add(subscriber.getProfileName() + " (@" + subscriber.getUsername() + ")");
            }
        }
        
        // Force ListView to update by setting the items again
        if (searchSubscribersField != null && !searchSubscribersField.getText().trim().isEmpty()) {
            // If there's a search filter, reapply it
            filterSubscribers(searchSubscribersField.getText());
        } else {
            // Reset the ListView to show all subscribers
            subscribersListView.setItems(subscribersList);
        }
    }
    
    private void loadAdmins() {
        adminsList.clear();
        for (String adminId : currentChannel.getAdminIds()) {
            User admin = userService.findUserById(adminId);
            if (admin != null) {
                adminsList.add(admin.getProfileName() + " (@" + admin.getUsername() + ")");
            }
        }
        
        // Force ListView to update
        adminsListView.setItems(adminsList);
    }
    
    private void loadAnalytics() {
        totalSubscribersLabel.setText(String.valueOf(currentChannel.getSubscriberIds().size()));
        totalMessagesLabel.setText(String.valueOf(currentChannel.getMessageHistory().size()));
        activeTodayLabel.setText("0"); // Would need additional tracking for active users
        createdDateLabel.setText(currentChannel.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
    }
    
    private void filterSubscribers(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            loadSubscribers();
            return;
        }
        
        ObservableList<String> filteredList = FXCollections.observableArrayList();
        for (String subscriber : subscribersList) {
            if (subscriber.toLowerCase().contains(searchTerm.toLowerCase())) {
                filteredList.add(subscriber);
            }
        }
        subscribersListView.setItems(filteredList);
    }
    
    private void setAdminControlsEnabled(boolean enabled) {
        channelNameField.setEditable(enabled);
        descriptionField.setEditable(enabled);
        privateChannelCheckBox.setDisable(!enabled);
        channelTypeComboBox.setDisable(!enabled);
        inviteSubscriberButton.setDisable(!enabled);
        promoteToAdminButton.setDisable(!enabled);
        removeAdminButton.setDisable(!enabled);
        saveChannelButton.setDisable(!enabled);
        deleteChannelButton.setDisable(!enabled);
        changeChannelPictureButton.setDisable(!enabled);
        regenerateInviteLinkButton.setDisable(!enabled);
        
        // Admin permission checkboxes
        canPostMessagesCheckBox.setDisable(!enabled);
        canEditMessagesCheckBox.setDisable(!enabled);
        canDeleteMessagesCheckBox.setDisable(!enabled);
        canInviteUsersCheckBox.setDisable(!enabled);
        canRestrictMembersCheckBox.setDisable(!enabled);
        canPinMessagesCheckBox.setDisable(!enabled);
        canAddAdminsCheckBox.setDisable(!enabled);
    }
    
    @FXML
    private void handleChangeChannelPicture() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Channel Picture");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        
        File selectedFile = fileChooser.showOpenDialog(dialogStage);
        if (selectedFile != null) {
            try {
                // Create channel pictures directory if it doesn't exist
                Path channelPicturesDir = Paths.get("channel_pictures");
                if (!Files.exists(channelPicturesDir)) {
                    Files.createDirectories(channelPicturesDir);
                }
                
                // Copy the selected file to the channel pictures directory
                String fileName = currentChannel.getChannelId() + "_" + selectedFile.getName();
                Path targetPath = channelPicturesDir.resolve(fileName);
                Files.copy(selectedFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                
                // Load the new channel picture
                loadChannelPicture(targetPath.toString());
                currentChannel.setProfilePicturePath(targetPath.toString());
                
                logger.info("Channel picture updated for channel: {}", currentChannel.getChannelName());
                
            } catch (Exception e) {
                logger.error("Error updating channel picture", e);
                showAlert("Error", "Failed to update channel picture: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void handleInviteSubscriber() {
        // Create a simple dialog to invite a subscriber by username
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Invite Subscriber");
        dialog.setHeaderText("Invite a user to subscribe to the channel");
        dialog.setContentText("Enter username:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(username -> {
            User user = userService.findUserByUsername(username.trim());
            if (user != null) {
                if (!currentChannel.isSubscriber(user.getUserId())) {
                    currentChannel.addSubscriber(user.getUserId());
                    loadSubscribers();
                    subscriberCountLabel.setText(currentChannel.getSubscriberIds().size() + " subscribers");
                    showAlert("Success", "User invited successfully!");
                } else {
                    showAlert("Info", "User is already subscribed to this channel.");
                }
            } else {
                showAlert("Error", "User not found with username: " + username);
            }
        });
    }
    
    @FXML
    private void handlePromoteToAdmin() {
        String selectedSubscriber = subscribersListView.getSelectionModel().getSelectedItem();
        if (selectedSubscriber != null) {
            // Extract username from the display string
            String username = selectedSubscriber.substring(selectedSubscriber.indexOf("(@") + 2, selectedSubscriber.lastIndexOf(")"));
            User user = userService.findUserByUsername(username);
            
            if (user != null && !currentChannel.isAdmin(user.getUserId())) {
                currentChannel.addAdmin(user.getUserId());
                loadAdmins();
                showAlert("Success", "User promoted to admin successfully!");
            }
        }
    }
    
    private void handleRemoveSubscriber() {
        String selectedSubscriber = subscribersListView.getSelectionModel().getSelectedItem();
        if (selectedSubscriber != null) {
            // Extract username from the display string
            String username = selectedSubscriber.substring(selectedSubscriber.indexOf("(@") + 2, selectedSubscriber.lastIndexOf(")"));
            User user = userService.findUserByUsername(username);
            
            if (user != null) {
                // Confirm removal
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Remove Subscriber");
                confirmAlert.setHeaderText("Remove " + user.getProfileName() + " from the channel?");
                confirmAlert.setContentText("This subscriber will no longer receive channel updates.");
                
                confirmAlert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        // Remove from channel
                        currentChannel.removeSubscriber(user.getUserId());
                        
                        // If the user was an admin, remove admin status too
                        if (currentChannel.isAdmin(user.getUserId())) {
                            currentChannel.removeAdmin(user.getUserId());
                        }
                        
                        // Update the database
                        boolean success = chatService.updateChannel(currentChannel);
                        
                        if (success) {
                            // Refresh the UI immediately
                            loadSubscribers();
                            loadAdmins();
                            subscriberCountLabel.setText(currentChannel.getSubscriberIds().size() + " subscribers");
                            
                            // Force ListView refresh
                            subscribersListView.refresh();
                            adminsListView.refresh();
                            
                            // Clear selection
                            subscribersListView.getSelectionModel().clearSelection();
                            
                            showAlert("Success", "Subscriber removed successfully!");
                        } else {
                            showAlert("Error", "Failed to remove subscriber. Please try again.");
                        }
                    }
                });
            }
        }
    }
    
    @FXML
    private void handleRemoveAdmin() {
        String selectedAdmin = adminsListView.getSelectionModel().getSelectedItem();
        if (selectedAdmin != null) {
            // Extract username from the display string
            String username = selectedAdmin.substring(selectedAdmin.indexOf("(@") + 2, selectedAdmin.lastIndexOf(")"));
            User user = userService.findUserByUsername(username);
            
            if (user != null && currentChannel.isAdmin(user.getUserId())) {
                // Don't allow removing the owner
                if (user.getUserId().equals(currentChannel.getOwnerId())) {
                    showAlert("Error", "Cannot remove the channel owner from admin role.");
                    return;
                }
                
                currentChannel.removeAdmin(user.getUserId());
                loadAdmins();
                showAlert("Success", "Admin removed successfully!");
            }
        }
    }
    
    @FXML
    private void handleCopyInviteLink() {
        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
        content.putString(currentChannel.getInviteLink());
        clipboard.setContent(content);
        showAlert("Success", "Invite link copied to clipboard!");
    }
    
    @FXML
    private void handleRegenerateInviteLink() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Regenerate Invite Link");
        confirmAlert.setHeaderText("Are you sure you want to regenerate the invite link?");
        confirmAlert.setContentText("The old invite link will no longer work.");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String newInviteLink = "https://t.me/+" + UUID.randomUUID().toString().substring(0, 8);
                currentChannel.setInviteLink(newInviteLink);
                inviteLinkField.setText(newInviteLink);
                showAlert("Success", "Invite link regenerated successfully!");
            }
        });
    }
    
    @FXML
    private void handleSaveChannel() {
        try {
            // Update channel information
            String newChannelName = channelNameField.getText().trim();
            String newDescription = descriptionField.getText().trim();
            
            if (newChannelName.isEmpty()) {
                showAlert("Validation Error", "Channel name cannot be empty.");
                return;
            }
            
            currentChannel.setChannelName(newChannelName);
            currentChannel.setDescription(newDescription);
            currentChannel.setPrivate(privateChannelCheckBox.isSelected());
            
            // Save to database
            boolean success = chatService.updateChannel(currentChannel);
            
            if (success) {
                showAlert("Success", "Channel updated successfully!");
                channelNameLabel.setText(newChannelName);
            } else {
                showAlert("Error", "Failed to update channel. Please try again.");
            }
            
        } catch (Exception e) {
            logger.error("Error saving channel", e);
            showAlert("Error", "An error occurred while saving channel: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleCancel() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
    
    @FXML
    private void handleDeleteChannel() {
        User currentUser = userService.getCurrentUser();
        if (!currentChannel.getOwnerId().equals(currentUser.getUserId())) {
            showAlert("Permission Error", "Only the channel owner can delete the channel.");
            return;
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete Channel");
        confirmAlert.setHeaderText("Are you sure you want to delete this channel?");
        confirmAlert.setContentText("This action cannot be undone. All messages and data will be lost.");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean success = chatService.deleteChannel(currentChannel.getChannelId());
                if (success) {
                    showAlert("Success", "Channel deleted successfully.");
                    if (dialogStage != null) {
                        dialogStage.close();
                    }
                } else {
                    showAlert("Error", "Failed to delete channel. Please try again.");
                }
            }
        });
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
            
            logger.info("Applied theme to channel management dialog: {}", theme);
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
