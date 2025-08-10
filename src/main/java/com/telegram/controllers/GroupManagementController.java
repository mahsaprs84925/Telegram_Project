package com.telegram.controllers;

import com.telegram.models.GroupChat;
import com.telegram.models.User;
import com.telegram.services.ChatService;
import com.telegram.services.UserService;
import com.telegram.utils.UserPreferences;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldListCell;
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
import java.util.List;
import java.util.Optional;

/**
 * Controller for group management dialog
 */
public class GroupManagementController {
    private static final Logger logger = LoggerFactory.getLogger(GroupManagementController.class);
    
    @FXML private Circle groupImageCircle;
    @FXML private ImageView groupImageView;
    @FXML private Button changeGroupPictureButton;
    @FXML private Label groupNameLabel;
    @FXML private Label memberCountLabel;
    @FXML private TabPane groupTabPane;
    
    // Info Tab
    @FXML private TextField groupNameField;
    @FXML private TextArea descriptionField;
    @FXML private CheckBox privateGroupCheckBox;
    @FXML private ComboBox<String> groupTypeComboBox;
    
    // Members Tab
    @FXML private TextField searchMembersField;
    @FXML private Button addMemberButton;
    @FXML private ListView<String> membersListView;
    
    // Permissions Tab
    @FXML private CheckBox allCanSendMessagesCheckBox;
    @FXML private CheckBox allCanAddMembersCheckBox;
    @FXML private CheckBox allCanChangeInfoCheckBox;
    @FXML private CheckBox allCanPinMessagesCheckBox;
    @FXML private ListView<String> adminsListView;
    @FXML private Button promoteToAdminButton;
    
    // Action Buttons
    @FXML private Button saveGroupButton;
    @FXML private Button cancelGroupButton;
    @FXML private Button leaveGroupButton;
    @FXML private Button deleteGroupButton;
    
    private ChatService chatService;
    private UserService userService;
    private GroupChat currentGroup;
    private Stage dialogStage;
    private ObservableList<String> membersList;
    private ObservableList<String> adminsList;
    
    @FXML
    private void initialize() {
        chatService = ChatService.getInstance();
        userService = UserService.getInstance();
        
        membersList = FXCollections.observableArrayList();
        adminsList = FXCollections.observableArrayList();
        
        membersListView.setItems(membersList);
        adminsListView.setItems(adminsList);
        
        setupGroupTypeComboBox();
        setupEventHandlers();
    }
    
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
        
        // Apply current theme to the dialog
        applyThemeToDialog();
    }
    
    public void setGroup(GroupChat group) {
        this.currentGroup = group;
        loadGroupData();
    }
    
    private void setupGroupTypeComboBox() {
        groupTypeComboBox.getItems().addAll(
            "Public Group", "Private Group", "Study Group", 
            "Work Group", "Friends", "Family"
        );
        groupTypeComboBox.setValue("Public Group");
    }
    
    private void setupEventHandlers() {
        // Search functionality for members
        searchMembersField.textProperty().addListener((obs, oldText, newText) -> {
            filterMembers(newText);
        });
        
        // Enable/disable buttons based on selection
        membersListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            promoteToAdminButton.setDisable(newSelection == null);
        });
        
        // Add context menu for member management
        setupMemberContextMenu();
    }
    
    private void setupMemberContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        
        MenuItem promoteMenuItem = new MenuItem("Promote to Admin");
        promoteMenuItem.setOnAction(e -> handlePromoteToAdmin());
        
        MenuItem removeMenuItem = new MenuItem("Remove Member");
        removeMenuItem.setOnAction(e -> handleRemoveMember());
        
        contextMenu.getItems().addAll(promoteMenuItem, removeMenuItem);
        
        membersListView.setContextMenu(contextMenu);
        
        // Update menu items based on current user permissions and selection
        contextMenu.setOnShowing(e -> {
            String selectedMember = membersListView.getSelectionModel().getSelectedItem();
            if (selectedMember != null) {
                // Extract username from the display string
                String username = selectedMember.substring(selectedMember.indexOf("(@") + 2, selectedMember.lastIndexOf(")"));
                User selectedUser = userService.findUserByUsername(username);
                User currentUser = userService.getCurrentUser();
                
                boolean isCurrentUserAdmin = currentGroup.isAdmin(currentUser.getUserId()) || 
                                           currentGroup.getCreatorId().equals(currentUser.getUserId());
                boolean isSelectedUserAdmin = selectedUser != null && currentGroup.isAdmin(selectedUser.getUserId());
                boolean isSelectedUserCreator = selectedUser != null && currentGroup.getCreatorId().equals(selectedUser.getUserId());
                boolean isSelf = selectedUser != null && selectedUser.getUserId().equals(currentUser.getUserId());
                
                // Enable/disable promote option
                promoteMenuItem.setDisable(!isCurrentUserAdmin || isSelectedUserAdmin || isSelectedUserCreator || isSelf);
                
                // Enable/disable remove option
                removeMenuItem.setDisable(!isCurrentUserAdmin || isSelectedUserCreator || isSelf);
            }
        });
    }
    
    private void loadGroupData() {
        if (currentGroup == null) return;
        
        // Basic info
        groupNameLabel.setText(currentGroup.getGroupName());
        groupNameField.setText(currentGroup.getGroupName());
        descriptionField.setText(currentGroup.getDescription() != null ? currentGroup.getDescription() : "");
        
        // Load profile picture if available
        if (currentGroup.getProfilePicturePath() != null && !currentGroup.getProfilePicturePath().isEmpty()) {
            loadGroupPicture(currentGroup.getProfilePicturePath());
        }
        
        // Load members
        loadMembers();
        
        // Load admins
        loadAdmins();
        
        // Update member count
        memberCountLabel.setText(currentGroup.getMemberIds().size() + " members");
        
        // Set permissions
        allCanSendMessagesCheckBox.setSelected(true); // Default values
        allCanAddMembersCheckBox.setSelected(false);
        allCanChangeInfoCheckBox.setSelected(false);
        allCanPinMessagesCheckBox.setSelected(false);
        
        // Check if current user is admin/creator
        User currentUser = userService.getCurrentUser();
        boolean isAdmin = currentGroup.isAdmin(currentUser.getUserId()) || 
                         currentGroup.getCreatorId().equals(currentUser.getUserId());
        
        // Enable/disable controls based on permissions
        setAdminControlsEnabled(isAdmin);
    }
    
    private void loadGroupPicture(String imagePath) {
        try {
            File imageFile = new File(imagePath);
            if (imageFile.exists()) {
                Image image = new Image(imageFile.toURI().toString());
                groupImageView.setImage(image);
                groupImageView.setVisible(true);
                groupImageCircle.setVisible(false);
            }
        } catch (Exception e) {
            logger.error("Error loading group picture", e);
        }
    }
    
    private void loadMembers() {
        membersList.clear();
        for (String memberId : currentGroup.getMemberIds()) {
            User member = userService.findUserById(memberId);
            if (member != null) {
                membersList.add(member.getProfileName() + " (@" + member.getUsername() + ")");
            }
        }
        
        // Force ListView to update by setting the items again
        if (searchMembersField != null && !searchMembersField.getText().trim().isEmpty()) {
            // If there's a search filter, reapply it
            filterMembers(searchMembersField.getText());
        } else {
            // Reset the ListView to show all members
            membersListView.setItems(membersList);
        }
    }
    
    private void loadAdmins() {
        adminsList.clear();
        for (String adminId : currentGroup.getAdminIds()) {
            User admin = userService.findUserById(adminId);
            if (admin != null) {
                adminsList.add(admin.getProfileName() + " (@" + admin.getUsername() + ")");
            }
        }
        
        // Force ListView to update
        adminsListView.setItems(adminsList);
    }
    
    private void filterMembers(String searchTerm) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            loadMembers();
            return;
        }
        
        ObservableList<String> filteredList = FXCollections.observableArrayList();
        for (String member : membersList) {
            if (member.toLowerCase().contains(searchTerm.toLowerCase())) {
                filteredList.add(member);
            }
        }
        membersListView.setItems(filteredList);
    }
    
    private void setAdminControlsEnabled(boolean enabled) {
        groupNameField.setEditable(enabled);
        descriptionField.setEditable(enabled);
        privateGroupCheckBox.setDisable(!enabled);
        groupTypeComboBox.setDisable(!enabled);
        addMemberButton.setDisable(!enabled);
        promoteToAdminButton.setDisable(!enabled);
        saveGroupButton.setDisable(!enabled);
        deleteGroupButton.setDisable(!enabled);
        changeGroupPictureButton.setDisable(!enabled);
        
        // Permissions checkboxes
        allCanSendMessagesCheckBox.setDisable(!enabled);
        allCanAddMembersCheckBox.setDisable(!enabled);
        allCanChangeInfoCheckBox.setDisable(!enabled);
        allCanPinMessagesCheckBox.setDisable(!enabled);
    }
    
    @FXML
    private void handleChangeGroupPicture() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Group Picture");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        
        File selectedFile = fileChooser.showOpenDialog(dialogStage);
        if (selectedFile != null) {
            try {
                // Create group pictures directory if it doesn't exist
                Path groupPicturesDir = Paths.get("group_pictures");
                if (!Files.exists(groupPicturesDir)) {
                    Files.createDirectories(groupPicturesDir);
                }
                
                // Copy the selected file to the group pictures directory
                String fileName = currentGroup.getGroupId() + "_" + selectedFile.getName();
                Path targetPath = groupPicturesDir.resolve(fileName);
                Files.copy(selectedFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                
                // Load the new group picture
                loadGroupPicture(targetPath.toString());
                currentGroup.setProfilePicturePath(targetPath.toString());
                
                logger.info("Group picture updated for group: {}", currentGroup.getGroupName());
                
            } catch (Exception e) {
                logger.error("Error updating group picture", e);
                showAlert("Error", "Failed to update group picture: " + e.getMessage());
            }
        }
    }
    
    @FXML
    private void handleAddMember() {
        // Create a simple dialog to add a member by username
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Member");
        dialog.setHeaderText("Add a new member to the group");
        dialog.setContentText("Enter username:");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(username -> {
            User user = userService.findUserByUsername(username.trim());
            if (user != null) {
                if (!currentGroup.isMember(user.getUserId())) {
                    currentGroup.addMember(user.getUserId());
                    loadMembers();
                    memberCountLabel.setText(currentGroup.getMemberIds().size() + " members");
                    showAlert("Success", "Member added successfully!");
                } else {
                    showAlert("Info", "User is already a member of this group.");
                }
            } else {
                showAlert("Error", "User not found with username: " + username);
            }
        });
    }
    
    @FXML
    private void handlePromoteToAdmin() {
        String selectedMember = membersListView.getSelectionModel().getSelectedItem();
        if (selectedMember != null) {
            // Extract username from the display string
            String username = selectedMember.substring(selectedMember.indexOf("(@") + 2, selectedMember.lastIndexOf(")"));
            User user = userService.findUserByUsername(username);
            
            if (user != null && !currentGroup.isAdmin(user.getUserId())) {
                currentGroup.addAdmin(user.getUserId());
                loadAdmins();
                showAlert("Success", "User promoted to admin successfully!");
            }
        }
    }
    
    private void handleRemoveMember() {
        String selectedMember = membersListView.getSelectionModel().getSelectedItem();
        if (selectedMember != null) {
            // Extract username from the display string
            String username = selectedMember.substring(selectedMember.indexOf("(@") + 2, selectedMember.lastIndexOf(")"));
            User user = userService.findUserByUsername(username);
            
            if (user != null) {
                // Confirm removal
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Remove Member");
                confirmAlert.setHeaderText("Remove " + user.getProfileName() + " from the group?");
                confirmAlert.setContentText("This member will no longer be able to see group messages.");
                
                confirmAlert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        // Remove from group
                        currentGroup.removeMember(user.getUserId());
                        
                        // If the user was an admin, remove admin status too
                        if (currentGroup.isAdmin(user.getUserId())) {
                            currentGroup.removeAdmin(user.getUserId());
                        }
                        
                        // Update the database
                        boolean success = chatService.updateGroup(currentGroup);
                        
                        if (success) {
                            logger.info("Member {} removed successfully. New member count: {}", user.getUsername(), currentGroup.getMemberIds().size());
                            
                            // Force complete UI refresh
                            Platform.runLater(() -> {
                                // Recreate observable lists to force refresh
                                membersList = FXCollections.observableArrayList();
                                adminsList = FXCollections.observableArrayList();
                                
                                // Reassign to ListView
                                membersListView.setItems(membersList);
                                adminsListView.setItems(adminsList);
                                
                                // Reload data
                                loadMembers();
                                loadAdmins();
                                memberCountLabel.setText(currentGroup.getMemberIds().size() + " members");
                                
                                // Clear selection
                                membersListView.getSelectionModel().clearSelection();
                                
                                logger.info("UI refreshed. Members list size: {}, Admins list size: {}", 
                                    membersList.size(), adminsList.size());
                            });
                            
                            showAlert("Success", "Member removed successfully!");
                        } else {
                            showAlert("Error", "Failed to remove member. Please try again.");
                        }
                    }
                });
            }
        }
    }
    
    @FXML
    private void handleSaveGroup() {
        try {
            // Update group information
            String newGroupName = groupNameField.getText().trim();
            String newDescription = descriptionField.getText().trim();
            
            if (newGroupName.isEmpty()) {
                showAlert("Validation Error", "Group name cannot be empty.");
                return;
            }
            
            currentGroup.setGroupName(newGroupName);
            currentGroup.setDescription(newDescription);
            
            // Save to database
            boolean success = chatService.updateGroup(currentGroup);
            
            if (success) {
                showAlert("Success", "Group updated successfully!");
                groupNameLabel.setText(newGroupName);
            } else {
                showAlert("Error", "Failed to update group. Please try again.");
            }
            
        } catch (Exception e) {
            logger.error("Error saving group", e);
            showAlert("Error", "An error occurred while saving group: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleCancel() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
    
    @FXML
    private void handleLeaveGroup() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Leave Group");
        confirmAlert.setHeaderText("Are you sure you want to leave this group?");
        confirmAlert.setContentText("You will no longer receive messages from this group.");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                User currentUser = userService.getCurrentUser();
                currentGroup.removeMember(currentUser.getUserId());
                
                boolean success = chatService.updateGroup(currentGroup);
                if (success) {
                    showAlert("Success", "You have left the group.");
                    if (dialogStage != null) {
                        dialogStage.close();
                    }
                } else {
                    showAlert("Error", "Failed to leave group. Please try again.");
                }
            }
        });
    }
    
    @FXML
    private void handleDeleteGroup() {
        User currentUser = userService.getCurrentUser();
        if (!currentGroup.getCreatorId().equals(currentUser.getUserId())) {
            showAlert("Permission Error", "Only the group creator can delete the group.");
            return;
        }
        
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete Group");
        confirmAlert.setHeaderText("Are you sure you want to delete this group?");
        confirmAlert.setContentText("This action cannot be undone. All messages and data will be lost.");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean success = chatService.deleteGroup(currentGroup.getGroupId());
                if (success) {
                    showAlert("Success", "Group deleted successfully.");
                    if (dialogStage != null) {
                        dialogStage.close();
                    }
                } else {
                    showAlert("Error", "Failed to delete group. Please try again.");
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
            
            logger.info("Applied theme to group management dialog: {}", theme);
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
