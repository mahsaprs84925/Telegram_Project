package com.telegram.controllers;

import com.telegram.TelegramApp;
import com.telegram.models.*;
import com.telegram.services.*;
import com.telegram.utils.MediaHandler;
import com.telegram.utils.UserPreferences;
import com.telegram.utils.ProfilePictureCropper;
import com.telegram.utils.LastSeenFormatter;
import com.telegram.utils.SessionManager;
import com.telegram.ui.MediaMessageComponent;
import com.telegram.ui.VoiceRecorder;
import com.telegram.ui.VoicePlayer;
import com.telegram.services.FileBasedMessageBroker;
import com.telegram.services.FileBasedMessageBroker.MessageListener;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint                                                                                                                                                               .Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.animation.FadeTransition;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

/**
 * Main controller for the Telegram application with full real-time features
 */
public class MainController implements MessageListener {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    
    // FXML elements
    @FXML private BorderPane mainPane;
    @FXML private VBox leftSidebar;
    @FXML private ListView<Object> chatListView;
    @FXML private VBox chatArea;
    @FXML private ScrollPane messageScrollPane;
    @FXML private VBox messageContainer;
    @FXML private HBox messageInputArea;
    @FXML private TextField messageInputField;
    @FXML private Button sendButton;
    @FXML private Button attachButton;
    @FXML private Button voiceButton;
    @FXML private HBox channelMuteArea;
    @FXML private Button channelMuteButton;
    private VoiceRecorder voiceRecorder;
    private VoicePlayer voicePlayer;
    private boolean isRecordingIndicatorVisible = false;
    private volatile boolean isVoiceRecordingActive = false;
    private boolean isVoiceRecordingLocked = false; // For tap-to-start, tap-to-stop mode
    @FXML private Label chatTitleLabel;
    @FXML private Label chatSubtitleLabel;
    @FXML private Label typingIndicatorLabel;
    @FXML private Circle onlineStatusIndicator;
    @FXML private ImageView chatProfileImageView;
    @FXML private StackPane chatProfileContainer;
    @FXML private Circle chatProfileCircle;
    @FXML private HBox chatHeaderContainer;
    @FXML private Button hamburgerMenuButton;
    @FXML private MenuButton profileMenuButton;
    @FXML private MenuItem profileMenuItem;
    @FXML private Button newChatButton;
    @FXML private HBox searchContainer;
    @FXML private ImageView searchIcon;
    @FXML private TextField searchField;
    
    // Side Panel Elements
    @FXML private StackPane overlayPane;
    @FXML private VBox sidePanel;
    @FXML private ImageView sidePanelProfileImageView;
    @FXML private Circle sidePanelProfileCircle;
    @FXML private Label sidePanelUsernameLabel;
    @FXML private Button closeSidePanelButton;
    @FXML private Button sidePanelProfileButton;
    @FXML private Button sidePanelNewGroupButton;
    @FXML private Button sidePanelNewChannelButton;
    @FXML private Button sidePanelSettingsButton;
    
    // Chat Options Menu
    @FXML private MenuButton chatOptionsMenuButton;
    @FXML private MenuItem searchInChatMenuItem;
    @FXML private MenuItem mediaGalleryMenuItem;
    @FXML private MenuItem viewProfileMenuItem;
    @FXML private MenuItem muteNotificationsMenuItem;
    @FXML private MenuItem clearHistoryMenuItem;
    @FXML private MenuItem deleteChatMenuItem;
    
    // Selection toolbar elements
    @FXML private ToolBar selectionToolbar;
    @FXML private Label selectionCountLabel;
    @FXML private Button deleteSelectedButton;
    @FXML private Button copySelectedButton;
    @FXML private Button cancelSelectionButton;
    
    // Services
    private UserService userService;
    private MessageService messageService;
    private ChatService chatService;
    private ChannelService channelService;
    private FileBasedMessageBroker messageBroker;
    
    // Current state
    private Object currentChat;
    private ObservableList<Object> chatItems;
    
    // Real-time features
    private Timeline typingTimer;
    private volatile boolean isTyping = false;
    
    // Message selection and quality of life features
private Set<String> selectedMessageIds = new HashSet<>();
private boolean isSelectionMode = false;
// For shift+click range selection
private int lastSelectedMessageIndex = -1;
    
    // Drag selection state
    private boolean isDragSelecting = false;
    private double dragStartY = 0;
    private Timeline dragSelectionTimeline;
    
    // Map to track message boxes by message ID for selection
    private Map<String, VBox> messageBoxMap = new HashMap<>();
    
    @FXML
    private void initialize() {
        userService = UserService.getInstance();
        messageService = MessageService.getInstance();
        chatService = ChatService.getInstance();
        channelService = new ChannelService();
        messageBroker = FileBasedMessageBroker.getInstance();
        
        setupUI();
        setupEventHandlers();
        setupRealTimeFeatures();
        loadUserChats();
        
        // Welcome message
        showWelcomeMessage();
        
        logger.info("MainController initialized with full real-time features");
    }
    
    private void setupUI() {
        chatItems = FXCollections.observableArrayList();
        chatListView.setItems(chatItems);
        setupChatListCellFactory();
        
        // Setup message input
        messageInputField.setPromptText("Type a message...");
        sendButton.setDisable(true);
        
        // Setup chat area
        if (messageContainer == null) {
            messageContainer = new VBox(10);
            messageContainer.setPadding(new Insets(10));
            messageScrollPane.setContent(messageContainer);
        }
        messageScrollPane.setFitToWidth(true);
        
        // Initialize chat profile image view
        if (chatProfileImageView != null) {
            chatProfileImageView.setFitWidth(40);
            chatProfileImageView.setFitHeight(40);
            chatProfileImageView.setPreserveRatio(true);
            chatProfileImageView.setVisible(false); // Hidden by default until a chat is selected
            logger.info("Chat profile image view initialized");
        } else {
            logger.warn("Chat profile image view is null during setup!");
        }
        
        // Setup user profile info and side panel
        User currentUser = userService.getCurrentUser();
        if (currentUser != null) {
            updateSidePanelProfile(currentUser);
        }
        
        // Setup side panel
        setupSidePanel();
        
        // Setup enhanced search bar
        setupSearchBar();
        
        // Apply saved theme
        applyCurrentTheme();
    }
    
    private void setupSearchBar() {
        if (searchContainer != null && searchIcon != null && searchField != null) {
            // Add focus styling to the search container
            searchField.focusedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    // Focused state
                    searchContainer.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 20px; " +
                                          "-fx-border-radius: 20px; -fx-padding: 8px 16px; " +
                                          "-fx-border-color: #007AFF; -fx-border-width: 2px; " +
                                          "-fx-effect: dropshadow(gaussian, rgba(0,122,255,0.2), 8, 0, 0, 2);");
                    searchIcon.setOpacity(1.0);
                } else {
                    // Unfocused state
                    searchContainer.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 20px; " +
                                          "-fx-border-radius: 20px; -fx-padding: 8px 16px;");
                    searchIcon.setOpacity(0.6);
                }
            });
            
            // Add hover effect
            searchContainer.setOnMouseEntered(e -> {
                if (!searchField.isFocused()) {
                    searchContainer.setStyle("-fx-background-color: #f0f1f2; -fx-background-radius: 20px; " +
                                          "-fx-border-radius: 20px; -fx-padding: 8px 16px;");
                    searchIcon.setOpacity(0.8);
                }
            });
            
            searchContainer.setOnMouseExited(e -> {
                if (!searchField.isFocused()) {
                    searchContainer.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 20px; " +
                                          "-fx-border-radius: 20px; -fx-padding: 8px 16px;");
                    searchIcon.setOpacity(0.6);
                }
            });
            
            // Add smooth animation for the search icon
            searchField.textProperty().addListener((obs, oldText, newText) -> {
                if (newText.trim().isEmpty()) {
                    searchIcon.setOpacity(searchField.isFocused() ? 1.0 : 0.6);
                } else {
                    searchIcon.setOpacity(1.0);
                }
            });
            
            logger.info("Enhanced search bar setup complete");
        } else {
            logger.warn("Search components not found during setup");
        }
    }
    
    private void setupEventHandlers() {
        // Message input handlers
        messageInputField.textProperty().addListener((obs, oldText, newText) -> {
            sendButton.setDisable(newText.trim().isEmpty());
            
            // Trigger typing indicator when user types
            if (!newText.trim().isEmpty() && !newText.equals(oldText)) {
                startTyping();
            } else if (newText.trim().isEmpty()) {
                stopTyping();
            }
            
            // Update button state and provide visual feedback for long messages
            if (newText.length() > 3500) {
                messageInputField.setStyle("-fx-background-radius: 20px; -fx-border-radius: 20px; -fx-padding: 12px 16px; -fx-border-color: #ff9800; -fx-border-width: 2px;");
            } else if (newText.length() > 4000) {
                messageInputField.setStyle("-fx-background-radius: 20px; -fx-border-radius: 20px; -fx-padding: 12px 16px; -fx-border-color: #f44336; -fx-border-width: 2px;");
                sendButton.setDisable(true);
            } else {
                messageInputField.setStyle("-fx-background-radius: 20px; -fx-border-radius: 20px; -fx-padding: 12px 16px;");
            }
        });
        
        messageInputField.setOnAction(e -> handleSendMessage());
        sendButton.setOnAction(e -> {
            if (!sendButton.isDisabled()) {
                handleSendMessage();
            }
        });
        attachButton.setOnAction(e -> {
            if (!attachButton.isDisabled()) {
                handleAttachFile();
            }
        });
        
        // Voice button with dual-mode functionality: hold-to-record OR tap-to-lock
        voiceButton.setOnAction(null); // Remove FXML action
        voiceButton.setOnMousePressed(e -> {
            if (currentChat != null && !voiceButton.isDisabled()) {
                handleVoiceButtonPressed();
            }
        });
        
        voiceButton.setOnMouseReleased(e -> {
            if (!voiceButton.isDisabled()) {
                handleVoiceButtonReleased();
            }
        });
        
        // Enhanced slide-to-cancel with visual feedback (only for hold mode)
        voiceButton.setOnMouseDragged(e -> {
            if (voiceRecorder != null && voiceRecorder.isRecording() && !isVoiceRecordingLocked) {
                double distance = Math.sqrt(e.getX() * e.getX() + e.getY() * e.getY());
                updateSlideToCancel(distance);
            }
        });
        
        // Cancel recording if mouse exits button while pressed (slide away to cancel - hold mode only)
        voiceButton.setOnMouseExited(e -> {
            if (voiceRecorder != null && voiceRecorder.isRecording() && !isVoiceRecordingLocked) {
                cancelVoiceRecording();
            }
        });
        
        // Button handlers
        newChatButton.setOnAction(e -> handleNewPrivateChat());
        hamburgerMenuButton.setOnAction(e -> toggleSidePanel());
        
        // Side panel handlers
        closeSidePanelButton.setOnAction(e -> closeSidePanel());
        
        // Chat selection handler
        chatListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectChat(newSelection);
            }
        });
        
        // Enhanced search handler with global search support
        searchField.textProperty().addListener((obs, oldText, newText) -> {
            if (newText != null && !newText.trim().isEmpty()) {
                searchChats(newText.trim());
            } else {
                loadUserChats();
            }
        });
        
        // Enhanced search on Enter key with global search
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                String searchText = searchField.getText().trim();
                if (!searchText.isEmpty()) {
                    // Check if Ctrl is held for global search
                    if (e.isControlDown()) {
                        performGlobalSearch(searchText);
                    } else if (e.isShiftDown()) {
                        // Shift+Enter for discovery mode
                        openDiscoveryDialog(searchText);
                    } else {
                        searchChats(searchText);
                    }
                }
            }
        });
        
        // Add tooltip for search functionality
        Tooltip searchTooltip = new Tooltip("Search chats, groups, and channels\nPress Ctrl+Enter for global search (messages, users)\nPress Shift+Enter for discovery");
        searchField.setTooltip(searchTooltip);
        
        // Selection toolbar handlers
        deleteSelectedButton.setOnAction(e -> handleDeleteSelected());
        copySelectedButton.setOnAction(e -> handleCopySelected());
        cancelSelectionButton.setOnAction(e -> handleCancelSelection());
        
        // Keyboard shortcuts
        mainPane.setOnKeyPressed(event -> {
            if (event.isControlDown()) {
                switch (event.getCode()) {
                    case F:
                        // Ctrl+F: Focus search field
                        searchField.requestFocus();
                        event.consume();
                        break;
                    case DIGIT1:
                        // Ctrl+1: Search in current chat
                        handleSearchInCurrentChat();
                        event.consume();
                        break;
                    case DIGIT2:
                        // Ctrl+2: Global message search
                        handleSearchMessages();
                        event.consume();
                        break;
                }
            }
        });
        
        // Make main pane focusable for keyboard shortcuts
        mainPane.setFocusTraversable(true);
        mainPane.requestFocus();
    }
    
    private void setupRealTimeFeatures() {
        // Register as message listener for real-time updates
        User currentUser = userService.getCurrentUser();
        if (currentUser != null) {
            messageBroker.registerSession(currentUser.getUserId(), this);
            messageBroker.setUserOnline(currentUser.getUserId(), true);
            
            // Session tracking handled internally by broker
            
            logger.info("Real-time features initialized for user: {}", currentUser.getUsername());
            
            // Debug: Log session info
            messageBroker.logActiveSessionInfo();
        }
    }
    
    private void showWelcomeMessage() {
        User currentUser = userService.getCurrentUser();
        chatTitleLabel.setText("Welcome, " + currentUser.getProfileName() + "!");
        chatSubtitleLabel.setText("Select a chat to start messaging");
        
        messageContainer.getChildren().clear();
        Label welcomeLabel = new Label("Welcome to Telegram Clone!\n\nSelect a chat from the sidebar or create a new one to start messaging.");
        welcomeLabel.setStyle("-fx-font-size: 16px; -fx-text-alignment: center;");
        welcomeLabel.setAlignment(Pos.CENTER);
        welcomeLabel.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(welcomeLabel, Priority.ALWAYS);
        messageContainer.getChildren().add(welcomeLabel);
    }
    
    @FXML
    public void toggleSidePanel() {
        if (overlayPane.isVisible()) {
            closeSidePanel();
        } else {
            openSidePanel();
        }
    }
    
    private void openSidePanel() {
        overlayPane.setVisible(true);
        // Animate side panel sliding in
        sidePanel.setTranslateX(-320);
        javafx.animation.TranslateTransition slideIn = new javafx.animation.TranslateTransition(
            javafx.util.Duration.millis(250), sidePanel);
        slideIn.setToX(0);
        slideIn.play();
        
        // Add click handler to overlay to close panel
        overlayPane.setOnMouseClicked(e -> {
            if (e.getTarget() == overlayPane) {
                closeSidePanel();
            }
        });
    }
    
    private void closeSidePanel() {
        // Animate side panel sliding out
        javafx.animation.TranslateTransition slideOut = new javafx.animation.TranslateTransition(
            javafx.util.Duration.millis(200), sidePanel);
        slideOut.setToX(-320);
        slideOut.setOnFinished(e -> overlayPane.setVisible(false));
        slideOut.play();
    }
    
    private void setupSidePanel() {
        // Initially hide the side panel
        overlayPane.setVisible(false);
        sidePanel.setTranslateX(-320);
        
        // Setup theme based on saved preference
        String currentTheme = UserPreferences.getCurrentTheme();
    }
    
    @FXML
    private void updateSidePanelProfile(User user) {
        if (user != null) {
            sidePanelUsernameLabel.setText(user.getProfileName());
            
            String profilePicturePath = user.getProfilePicturePath();
            if (profilePicturePath != null && !profilePicturePath.isEmpty()) {
                try {
                    // Decode URL-encoded path
                    String decodedPath = java.net.URLDecoder.decode(profilePicturePath, "UTF-8");
                    java.io.File profileFile = new java.io.File(decodedPath);
                    
                    if (profileFile.exists()) {
                        Image profileImage = new Image(profileFile.toURI().toString());
                        Image circularImage = createCircularProfileImage(profileImage, 60);
                        if (circularImage != null) {
                            sidePanelProfileImageView.setImage(circularImage);
                            sidePanelProfileImageView.setVisible(true);
                            sidePanelProfileCircle.setVisible(false);
                            
                            logger.info("Updated side panel profile picture for user: {}", user.getUsername());
                        } else {
                            sidePanelProfileImageView.setVisible(false);
                            sidePanelProfileCircle.setVisible(true);
                        }
                    } else {
                        sidePanelProfileImageView.setVisible(false);
                        sidePanelProfileCircle.setVisible(true);
                    }
                } catch (Exception e) {
                    logger.error("Error loading side panel profile picture: {}", e.getMessage());
                    sidePanelProfileImageView.setVisible(false);
                    sidePanelProfileCircle.setVisible(true);
                }
            } else {
                sidePanelProfileImageView.setVisible(false);
                sidePanelProfileCircle.setVisible(true);
            }
        }
    }
    
    @FXML
    public void handleProfile() {
        try {
            // Load the profile info dialog
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/profile.fxml"));
            Parent root = loader.load();
            
            ProfileController profileController = loader.getController();
            
            Stage profileStage = new Stage();
            profileStage.setTitle("Profile Info");
            profileStage.setScene(new Scene(root, 600, 700));
            profileStage.setResizable(false);
            profileStage.initModality(Modality.APPLICATION_MODAL);
            
            profileController.setDialogStage(profileStage);
            
            profileStage.showAndWait();
            
        } catch (Exception e) {
            logger.error("Error opening profile dialog", e);
            showAlert("Profile", "Profile info functionality");
        }
    }
    
    public void handleChatHeaderClick() {
        try {
            if (currentChat == null) {
                logger.info("No chat selected, not opening profile");
                return;
            }
            
            if (currentChat instanceof PrivateChat) {
                PrivateChat privateChat = (PrivateChat) currentChat;
                String otherUserId = privateChat.getParticipantIds().stream()
                    .filter(id -> !id.equals(userService.getCurrentUser().getUserId()))
                    .findFirst()
                    .orElse(null);
                
                if (otherUserId != null) {
                    User otherUser = userService.findUserById(otherUserId);
                    if (otherUser != null) {
                        logger.info("Opening profile for user: {}", otherUser.getUsername());
                        
                        // Load the profile info dialog
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/profile.fxml"));
                        Parent root = loader.load();
                        
                        ProfileController profileController = loader.getController();
                        
                        // Set the other user's profile instead of current user
                        profileController.setUser(otherUser);
                        
                        Stage profileStage = new Stage();
                        profileStage.setTitle(otherUser.getProfileName() + "'s Profile");
                        profileStage.setScene(new Scene(root, 600, 700));
                        profileStage.setResizable(false);
                        profileStage.initModality(Modality.APPLICATION_MODAL);
                        
                        profileController.setDialogStage(profileStage);
                        
                        profileStage.showAndWait();
                    } else {
                        logger.warn("Could not find other user with ID: {}", otherUserId);
                    }
                } else {
                    logger.warn("Could not determine other user in private chat");
                }
            } else if (currentChat instanceof GroupChat) {
                // For groups, show group info (we can implement this later if needed)
                logger.info("Group info not implemented yet");
                showAlert("Group Info", "Group information dialog not implemented yet");
            } else if (currentChat instanceof Channel) {
                // For channels, show channel info (we can implement this later if needed)
                logger.info("Channel info not implemented yet");
                showAlert("Channel Info", "Channel information dialog not implemented yet");
            }
            
        } catch (Exception e) {
            logger.error("Error opening chat header profile", e);
            showAlert("Error", "Could not open profile information");
        }
    }
    
    @FXML
    public void handleSettings() {
        try {
            // Load the settings dialog
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings.fxml"));
            Parent root = loader.load();
            
            SettingsController settingsController = loader.getController();
            
            Stage settingsStage = new Stage();
            settingsStage.setTitle("Settings");
            settingsStage.setScene(new Scene(root, 800, 600));
            settingsStage.setResizable(false);
            settingsStage.initModality(Modality.APPLICATION_MODAL);
            
            settingsController.setDialogStage(settingsStage);
            
            settingsStage.showAndWait();
            
            // Apply any theme changes that might have occurred
            applyCurrentTheme();
            
        } catch (Exception e) {
            logger.error("Error opening settings dialog", e);
            showAlert("Settings", "Could not open settings dialog: " + e.getMessage());
        }
    }
    
    @FXML
    public void handleSearchMessages() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Search Messages");
        dialog.setHeaderText("Search all messages");
        dialog.setContentText("Enter search term:");
        
        dialog.showAndWait().ifPresent(searchTerm -> {
            if (!searchTerm.trim().isEmpty()) {
                // Implement global message search
                User currentUser = userService.getCurrentUser();
                List<Message> results = messageService.searchMessages(searchTerm.trim(), 
                    currentUser != null ? currentUser.getUserId() : null, 100);
                showSearchResults(results, "Global Search Results");
            }
        });
    }
    
    @FXML
    public void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("Are you sure you want to logout?");
        alert.setContentText("You will need to login again to access your account.");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    // Cleanup real-time features
                    User currentUser = userService.getCurrentUser();
                    if (currentUser != null && messageBroker != null) {
                        messageBroker.setUserOnline(currentUser.getUserId(), false);
                        messageBroker.unregisterSession(currentUser.getUserId(), this);
                    }
                    
                    SessionManager.getInstance().clearSession();
                    Stage stage = (Stage) mainPane.getScene().getWindow();
                    TelegramApp.showLoginScreen();
                    logger.info("User logged out successfully");
                } catch (Exception e) {
                    logger.error("Error during logout", e);
                    showAlert("Error", "Error occurred during logout.");
                }
            }
        });
    }
    
    @FXML
    public void handleDeleteSelected() {
        if (selectedMessageIds.isEmpty()) {
            showAlert("No messages selected", "Please select messages to delete.");
            return;
        }
        
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Messages");
        alert.setHeaderText("Delete " + selectedMessageIds.size() + " message(s)?");
        alert.setContentText("This action cannot be undone.");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    for (String messageId : selectedMessageIds) {
                        messageService.deleteMessage(messageId);
                    }
                    exitSelectionMode();
                    refreshCurrentChat();
                    showSuccessAlert("Messages Deleted", "Messages deleted successfully.");
                } catch (Exception e) {
                    logger.error("Error deleting messages", e);
                    showAlert("Error", "Failed to delete some messages.");
                }
            }
        });
    }
    
    @FXML
    public void handleCopySelected() {
        if (selectedMessageIds.isEmpty()) {
            showAlert("No messages selected", "Please select messages to copy.");
            return;
        }
        
        try {
            StringBuilder messageText = new StringBuilder();
            for (String messageId : selectedMessageIds) {
                Message message = messageService.getMessageById(messageId);
                if (message != null) {
                    messageText.append(message.getContent()).append("\n");
                }
            }
            
            // Copy to clipboard
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(messageText.toString());
            clipboard.setContent(content);
            
            exitSelectionMode();
            showAlert("Success", "Messages copied to clipboard.");
        } catch (Exception e) {
            logger.error("Error copying messages", e);
            showAlert("Error", "Failed to copy messages.");
        }
    }
    
    @FXML
    public void handleCancelSelection() {
        exitSelectionMode();
    }
    
    // Helper methods for UI management
    private void selectChat(Object chat) {
        logger.info("Selecting chat: {} (type: {})", 
            chat != null ? getChatDisplayName(chat) : "null", 
            chat != null ? chat.getClass().getSimpleName() : "null");
        
        currentChat = chat;
        loadMessagesForChat(chat);
        updateChatHeader(chat);
        updateChatOptionsMenu(chat);
        updateMessageInputPermissions(chat);
        exitSelectionMode();
        
        logger.info("Chat selection completed. Current chat: {}", 
            currentChat != null ? getChatDisplayName(currentChat) : "null");
    }
    
    private void updateMessageInputPermissions(Object chat) {
        try {
            // Safety check for UI elements
            if (messageInputArea == null || channelMuteArea == null || channelMuteButton == null) {
                logger.warn("UI elements not yet initialized, skipping permission update");
                return;
            }
            
            if (chat instanceof Channel) {
                Channel channel = (Channel) chat;
                User currentUser = userService.getCurrentUser();
                
                if (currentUser != null) {
                    boolean isOwner = channel.getOwnerId().equals(currentUser.getUserId());
                    boolean isAdmin = channel.getAdminIds().contains(currentUser.getUserId());
                    boolean canPost = isOwner || isAdmin;
                    
                    if (!canPost) {
                        // Hide normal input area and show mute button (like real Telegram)
                        messageInputArea.setVisible(false);
                        messageInputArea.setManaged(false);
                        channelMuteArea.setVisible(true);
                        channelMuteArea.setManaged(true);
                        
                        // Set up mute button text and behavior
                        channelMuteButton.setText("You are muted in this channel");
                        channelMuteButton.setOnAction(e -> {
                            // Could implement mute/unmute functionality here
                            showAlert("Channel Permissions", "Only admins can post messages to this channel.");
                        });
                    } else {
                        // Show normal input area for admins
                        messageInputArea.setVisible(true);
                        messageInputArea.setManaged(true);
                        channelMuteArea.setVisible(false);
                        channelMuteArea.setManaged(false);
                        
                        // Enable all input methods for admins
                        if (messageInputField != null) messageInputField.setDisable(false);
                        if (sendButton != null) sendButton.setDisable(false);
                        if (attachButton != null) attachButton.setDisable(false);
                        if (voiceButton != null) voiceButton.setDisable(false);
                        if (messageInputField != null) messageInputField.setPromptText("Type a message...");
                        
                        // Reset button styles
                        if (attachButton != null) attachButton.setStyle("-fx-opacity: 1.0;");
                        resetVoiceButton();
                    }
                }
            } else {
                // For private chats and groups, always show normal input area
                messageInputArea.setVisible(true);
                messageInputArea.setManaged(true);
                channelMuteArea.setVisible(false);
                channelMuteArea.setManaged(false);
                
                // Enable all input methods
                if (messageInputField != null) messageInputField.setDisable(false);
                if (sendButton != null) sendButton.setDisable(false);
                if (attachButton != null) attachButton.setDisable(false);
                if (voiceButton != null) voiceButton.setDisable(false);
                if (messageInputField != null) messageInputField.setPromptText("Type a message...");
                
                // Reset button styles
                if (attachButton != null) attachButton.setStyle("-fx-opacity: 1.0;");
                resetVoiceButton();
            }
        } catch (Exception e) {
            logger.error("Error updating message input permissions", e);
        }
    }
    
    private void loadUserChats() {
        try {
            User currentUser = userService.getCurrentUser();
            chatItems.clear();
            
            // Load private chats
            List<PrivateChat> privateChats = chatService.getUserPrivateChats(currentUser.getUserId());
            chatItems.addAll(privateChats);
            
            // Load group chats
            List<GroupChat> groupChats = chatService.getUserGroups(currentUser.getUserId());
            chatItems.addAll(groupChats);
            
            // Load channels
            List<Channel> channels = chatService.getUserChannels(currentUser.getUserId());
            chatItems.addAll(channels);
            
        } catch (Exception e) {
            logger.error("Error loading user chats", e);
        }
    }
    
    private void loadMessagesForChat(Object chat) {
        if (chat == null) return;
        
        try {
            messageContainer.getChildren().clear();
            messageBoxMap.clear(); // Clear message box mapping when loading new chat
            List<Message> messages = List.of();
            
            if (chat instanceof PrivateChat) {
                PrivateChat privateChat = (PrivateChat) chat;
                User currentUser = userService.getCurrentUser();
                if (currentUser != null) {
                    String otherUserId = privateChat.getOtherUserId(currentUser.getUserId());
                    messages = messageService.getPrivateChatMessages(currentUser.getUserId(), otherUserId, 50);
                }
            } else if (chat instanceof GroupChat) {
                GroupChat groupChat = (GroupChat) chat;
                messages = messageService.getGroupMessages(groupChat.getGroupId(), 50);
            } else if (chat instanceof Channel) {
                Channel channel = (Channel) chat;
                messages = messageService.getChannelMessages(channel.getChannelId(), 50);
            }
            
            if (messages.isEmpty()) {
                // Show a helpful message when no messages exist
                Label noMessagesLabel = new Label("No messages yet. Start the conversation!");
                noMessagesLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #999; -fx-text-alignment: center;");
                noMessagesLabel.setAlignment(Pos.CENTER);
                noMessagesLabel.setMaxWidth(Double.MAX_VALUE);
                VBox.setVgrow(noMessagesLabel, Priority.ALWAYS);
                messageContainer.getChildren().add(noMessagesLabel);
            } else {
                for (Message message : messages) {
                    addMessageToUI(message);
                }
            }
            
            scrollToBottom();
            
            // Mark messages as read when user views them
            markMessagesAsRead();
            
        } catch (Exception e) {
            logger.error("Error loading messages for chat", e);
            // Show error message to user
            Label errorLabel = new Label("Failed to load messages. Please try again.");
            errorLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #f44336; -fx-text-alignment: center;");
            errorLabel.setAlignment(Pos.CENTER);
            errorLabel.setMaxWidth(Double.MAX_VALUE);
            messageContainer.getChildren().add(errorLabel);
        }
    }
    
    private void addMessageToUI(Message message) {
        try {
            VBox messageBox = createMessageBubble(message);
            boolean isFromCurrentUser = isMessageFromCurrentUser(message);
            
            // Store message box in map for selection functionality
            messageBoxMap.put(message.getMessageId(), messageBox);
            
            // Debug logging
            User currentUser = userService.getCurrentUser();
            logger.info("Adding message: '{}' from {} (current user: {}), isFromCurrentUser: {}", 
                message.getContent(), message.getSenderId(), 
                currentUser != null ? currentUser.getUserId() : "null", isFromCurrentUser);
            
            // Create an HBox to properly align the message bubble
            HBox messageContainer = new HBox();
            messageContainer.setPadding(new Insets(2, 10, 2, 10));
            messageContainer.getStyleClass().add("message-container");
            
            if (isFromCurrentUser) {
                // Use CSS classes for current user's messages
                messageContainer.setAlignment(Pos.CENTER_RIGHT);
                messageBox.getStyleClass().addAll("message-bubble", "own");
                messageContainer.getChildren().add(messageBox);
            } else {
                // Use CSS classes for other users' messages  
                messageContainer.setAlignment(Pos.CENTER_LEFT);
                messageBox.getStyleClass().addAll("message-bubble", "other");
                messageContainer.getChildren().add(messageBox);
            }
            
            this.messageContainer.getChildren().add(messageContainer);
            
            // Add fade-in animation for new messages
            messageContainer.setOpacity(0);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), messageContainer);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        } catch (Exception e) {
            logger.error("Error adding message to UI", e);
        }
    }
    
    private VBox createMessageBubble(Message message) {
        VBox messageBox = new VBox(5);
        messageBox.setPadding(new Insets(8, 12, 8, 12));
        messageBox.setMaxWidth(300);
        boolean isFromCurrentUser = isMessageFromCurrentUser(message);
        if (isFromCurrentUser) {
            messageBox.setAlignment(Pos.CENTER_RIGHT);
        } else {
            messageBox.setAlignment(Pos.CENTER_LEFT);
        }
        if (!isFromCurrentUser && (currentChat instanceof GroupChat || currentChat instanceof Channel)) {
            Label senderLabel = new Label(getSenderName(message.getSenderId()));
            senderLabel.getStyleClass().addAll("message-sender", "field-label");
            messageBox.getChildren().add(senderLabel);
        }
        Label contentLabel = new Label(message.getContent());
        contentLabel.setWrapText(true);
        // Use CSS classes for message text styling
        if (isFromCurrentUser) {
            contentLabel.getStyleClass().addAll("message-text", "own");
        } else {
            contentLabel.getStyleClass().addAll("message-text", "other");
        }
        messageBox.getChildren().add(contentLabel);
        if (message.getMediaPath() != null && !message.getMediaPath().isEmpty()) {
            MediaMessageComponent mediaComponent = new MediaMessageComponent(
                message, isFromCurrentUser, (Stage) mainPane.getScene().getWindow()
            );
            VBox mediaBox = mediaComponent.createMediaComponent();
            if (mediaBox != null) {
                messageBox.getChildren().add(mediaBox);
            } else {
                String fileName = extractFileName(message.getMediaPath());
                String fileIcon = MediaHandler.getFileIcon(fileName);
                Label fileLabel = new Label(fileIcon + " " + fileName);
                fileLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #007AFF; -fx-cursor: hand;");
                fileLabel.setOnMouseClicked(e -> openFile(message.getMediaPath()));
                messageBox.getChildren().add(fileLabel);
            }
        }
        HBox messageFooter = new HBox(5);
        messageFooter.setAlignment(isFromCurrentUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        Label timeLabel = new Label(formatTimestamp(message.getTimestamp()));
        if (isFromCurrentUser) {
            timeLabel.getStyleClass().addAll("message-time", "own");
        } else {
            timeLabel.getStyleClass().addAll("message-time", "other");
        }
        messageFooter.getChildren().add(timeLabel);
        if (isFromCurrentUser) {
            HBox statusBox = new HBox(3);
            statusBox.setAlignment(Pos.CENTER_RIGHT);
            Label statusLabel = new Label(getMessageStatusIcon(message));
            statusLabel.setId("status-indicator-" + message.getMessageId());
            
            if (message.getStatus() == Message.MessageStatus.READ) {
                statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #4CAF50;");
            } else if (message.getStatus() == Message.MessageStatus.DELIVERED) {
                statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #cce7ff;");
            } else {
                statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #cce7ff;");
            }
            statusBox.getChildren().add(statusLabel);
            messageFooter.getChildren().add(statusBox);
        }
        messageBox.getChildren().add(messageFooter);
        // Set userData for shift+click selection
        messageBox.setUserData(message.getMessageId());
        messageBox.setOnMouseClicked(event -> {
            if (event.isConsumed()) return;
            if (event.getButton() == MouseButton.PRIMARY) {
                if (isSelectionMode) {
                    int clickedIndex = getMessageBoxIndex(messageBox);
                    logger.debug("Message clicked in selection mode. Index: {}, Shift: {}", clickedIndex, event.isShiftDown());
                    if (event.isShiftDown() && lastSelectedMessageIndex != -1 && clickedIndex != -1) {
                        logger.info("Selecting range from {} to {}", lastSelectedMessageIndex, clickedIndex);
                        selectMessageRange(lastSelectedMessageIndex, clickedIndex);
                    } else {
                        toggleMessageSelection(message.getMessageId(), messageBox);
                        lastSelectedMessageIndex = clickedIndex;
                    }
                    logger.info("Message clicked in selection mode: {}", message.getMessageId());
                }
            } else if (event.getButton() == MouseButton.SECONDARY) {
                // Just show context menu without auto-selecting the message
                showMessageContextMenu(message, event.getScreenX(), event.getScreenY());
            }
        });
        messageBox.setOnMouseEntered(event -> {
            if (!selectedMessageIds.contains(message.getMessageId())) {
                String currentStyle = messageBox.getStyle();
                if (currentStyle == null) currentStyle = "";
                if (!currentStyle.contains("-fx-background-color:")) {
                    messageBox.setStyle(currentStyle + " -fx-background-color: rgba(0, 136, 204, 0.1);");
                }
            }
        });
        messageBox.setOnMouseExited(event -> {
            if (!selectedMessageIds.contains(message.getMessageId())) {
                String currentStyle = messageBox.getStyle();
                if (currentStyle != null) {
                    messageBox.setStyle(currentStyle.replace(" -fx-background-color: rgba(0, 136, 204, 0.1);", ""));
                }
            }
        });
        return messageBox;
    }

    // --- Shift+Click Selection Helpers ---
    private int getMessageBoxIndex(VBox messageBox) {
        for (int i = 0; i < messageContainer.getChildren().size(); i++) {
            javafx.scene.Node child = messageContainer.getChildren().get(i);
            
            // Skip labels and other non-message elements
            if (!(child instanceof HBox)) continue;
            
            HBox hbox = (HBox) child;
            for (javafx.scene.Node node : hbox.getChildren()) {
                if (node == messageBox) return i;
            }
        }
        return -1;
    }

    private void selectMessageRange(int from, int to) {
        int start = Math.min(from, to);
        int end = Math.max(from, to);
        
        for (int i = start; i <= end; i++) {
            if (i < messageContainer.getChildren().size()) {
                javafx.scene.Node child = messageContainer.getChildren().get(i);
                
                // Skip non-HBox elements (labels, etc.)
                if (!(child instanceof HBox)) continue;
                
                HBox hbox = (HBox) child;
                for (javafx.scene.Node node : hbox.getChildren()) {
                    if (node instanceof VBox messageBox) {
                        String msgId = getMessageIdFromBox(messageBox);
                        if (msgId != null && !selectedMessageIds.contains(msgId)) {
                            toggleMessageSelection(msgId, messageBox);
                        }
                        break; // Only process the first VBox in the HBox
                    }
                }
            }
        }
    }

    private String getMessageIdFromBox(VBox messageBox) {
        Object userData = messageBox.getUserData();
        if (userData instanceof String s) return s;
        return null;
    }
    
    private void updateChatProfilePicture(User user) {
        if (chatProfileImageView != null && user != null) {
            logger.info("Attempting to update chat profile picture for user: {} (id: {})", user.getUsername(), user.getUserId());
            String profilePicturePath = user.getProfilePicturePath();
            logger.info("Profile picture path: {}", profilePicturePath);
            
            if (profilePicturePath != null && !profilePicturePath.isEmpty()) {
                try {
                    // Decode URL-encoded path (fix %20 to spaces, etc.)
                    String decodedPath = java.net.URLDecoder.decode(profilePicturePath, "UTF-8");
                    logger.info("Decoded profile picture path: {}", decodedPath);
                    
                    // Load profile picture from file
                    java.io.File profileFile = new java.io.File(decodedPath);
                    logger.info("Profile file exists: {}, file path: {}", profileFile.exists(), profileFile.getAbsolutePath());
                    
                    if (profileFile.exists()) {
                        Image profileImage = new Image(profileFile.toURI().toString());
                        
                        // Ensure profile picture is properly sized and circular
                        chatProfileImageView.setImage(profileImage);
                        chatProfileImageView.setFitWidth(40);
                        chatProfileImageView.setFitHeight(40);
                        chatProfileImageView.setPreserveRatio(true); // Preserve ratio to avoid distortion
                        
                        // Create circular clip for chat profile picture
                        Circle chatClip = new Circle(20, 20, 20); // Center at (20, 20) with radius 20
                        chatProfileImageView.setClip(chatClip);
                        
                        chatProfileImageView.setVisible(true);
                        
                        logger.info("Successfully updated chat profile picture for user: {}", user.getUsername());
                    } else {
                        // Hide image view if file doesn't exist
                        chatProfileImageView.setVisible(false);
                        logger.warn("Profile picture file not found: {}", decodedPath);
                    }
                } catch (Exception e) {
                    logger.error("Error loading profile picture: {}", e.getMessage());
                    chatProfileImageView.setVisible(false);
                }
            } else {
                // No profile picture set, hide the image view
                chatProfileImageView.setVisible(false);
                logger.info("No profile picture set for user: {}", user.getUsername());
            }
        } else {
            logger.warn("Cannot update profile picture: chatProfileImageView={}, user={}", 
                chatProfileImageView != null ? "not null" : "null", 
                user != null ? user.getUsername() : "null");
        }
    }
    
    private void refreshCurrentChat() {
        if (currentChat != null) {
            loadMessagesForChat(currentChat);
        }
    }
    
    private void searchChats(String searchTerm) {
        // Enhanced comprehensive search functionality
        try {
            User currentUser = userService.getCurrentUser();
            chatItems.clear();
            
            if (searchTerm.isEmpty()) {
                loadUserChats();
                return;
            }
            
            // Enhanced search results
            List<Object> searchResults = new ArrayList<>();
            
            // Search in private chats by username, display name, or phone
            List<PrivateChat> privateChats = chatService.getUserPrivateChats(currentUser.getUserId());
            for (PrivateChat chat : privateChats) {
                try {
                    String otherUserId = chat.getOtherUserId(currentUser.getUserId());
                    User otherUser = userService.getUserById(otherUserId);
                    if (otherUser != null) {
                        String searchLower = searchTerm.toLowerCase();
                        boolean matches = otherUser.getDisplayName().toLowerCase().contains(searchLower) ||
                                        otherUser.getUsername().toLowerCase().contains(searchLower) ||
                                        (otherUser.getPhoneNumber() != null && otherUser.getPhoneNumber().contains(searchTerm));
                        
                        if (matches) {
                            searchResults.add(chat);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Error searching private chat: " + e.getMessage());
                }
            }
            
            // Search in group chats by name, description, or member usernames
            List<GroupChat> allGroups = chatService.getAllUserGroups(currentUser.getUserId());
            for (GroupChat group : allGroups) {
                String searchLower = searchTerm.toLowerCase();
                boolean matches = group.getGroupName().toLowerCase().contains(searchLower) ||
                                (group.getDescription() != null && group.getDescription().toLowerCase().contains(searchLower));
                
                // Also search in member names if not already matched
                if (!matches) {
                    try {
                        List<String> memberIds = group.getMemberIds();
                        for (String memberId : memberIds) {
                            User member = userService.getUserById(memberId);
                            if (member != null && 
                                (member.getDisplayName().toLowerCase().contains(searchLower) ||
                                 member.getUsername().toLowerCase().contains(searchLower))) {
                                matches = true;
                                break;
                            }
                        }
                    } catch (Exception e) {
                        logger.warn("Error searching group members: " + e.getMessage());
                    }
                }
                
                if (matches) {
                    searchResults.add(group);
                }
            }
            
            // Search in channels by name only
            List<Channel> allChannels = channelService.getAllUserChannels(currentUser.getUserId());
            for (Channel channel : allChannels) {
                String searchLower = searchTerm.toLowerCase();
                boolean matches = channel.getChannelName().toLowerCase().contains(searchLower);
                
                if (matches) {
                    searchResults.add(channel);
                }
            }
            
            // Search for public channels/groups by name if not already in user's list
            try {
                List<Channel> publicChannels = channelService.searchPublicChannels(searchTerm);
                for (Channel channel : publicChannels) {
                    boolean alreadyAdded = searchResults.stream()
                            .anyMatch(item -> item instanceof Channel && 
                                    ((Channel) item).getChannelId().equals(channel.getChannelId()));
                    if (!alreadyAdded) {
                        searchResults.add(channel);
                    }
                }
            } catch (Exception e) {
                logger.warn("Error searching public channels: " + e.getMessage());
            }
            
            // Search for public groups
            try {
                List<GroupChat> publicGroups = chatService.searchGroups(searchTerm);
                for (GroupChat group : publicGroups) {
                    boolean alreadyAdded = searchResults.stream()
                            .anyMatch(item -> item instanceof GroupChat && 
                                    ((GroupChat) item).getGroupId().equals(group.getGroupId()));
                    if (!alreadyAdded) {
                        searchResults.add(group);
                    }
                }
            } catch (Exception e) {
                logger.warn("Error searching public groups: " + e.getMessage());
            }
            
            // Sort results by relevance (exact matches first, then partial matches)
            searchResults.sort((a, b) -> {
                String aName = getChatDisplayName(a).toLowerCase();
                String bName = getChatDisplayName(b).toLowerCase();
                String searchLower = searchTerm.toLowerCase();
                
                boolean aExact = aName.equals(searchLower);
                boolean bExact = bName.equals(searchLower);
                if (aExact && !bExact) return -1;
                if (!aExact && bExact) return 1;
                
                boolean aStarts = aName.startsWith(searchLower);
                boolean bStarts = bName.startsWith(searchLower);
                if (aStarts && !bStarts) return -1;
                if (!aStarts && bStarts) return 1;
                
                return aName.compareTo(bName);
            });
            
            chatItems.addAll(searchResults);
            
        } catch (Exception e) {
            logger.error("Error searching chats", e);
        }
    }
    
    private void searchMessagesInCurrentChat(String searchTerm) {
        if (currentChat == null) return;
        
        try {
            List<Message> foundMessages = new java.util.ArrayList<>();
            String chatId = getChatId(currentChat);
            
            if (currentChat instanceof PrivateChat) {
                PrivateChat privateChat = (PrivateChat) currentChat;
                User currentUser = userService.getCurrentUser();
                String otherUserId = privateChat.getOtherUserId(currentUser.getUserId());
                foundMessages = messageService.searchMessagesInChat(searchTerm, chatId, 20);
            } else if (currentChat instanceof GroupChat) {
                foundMessages = messageService.searchMessagesInChat(searchTerm, chatId, 20);
            } else if (currentChat instanceof Channel) {
                foundMessages = messageService.searchMessagesInChat(searchTerm, chatId, 20);
            }
            
            if (foundMessages.isEmpty()) {
                showAlert("No Results", "No messages found containing '" + searchTerm + "'");
            } else {
                showSearchResults(foundMessages, "Search Results in " + getChatDisplayName(currentChat));
            }
            
        } catch (Exception e) {
            logger.error("Error searching messages in chat", e);
            showAlert("Search Error", "Failed to search messages: " + e.getMessage());
        }
    }
    
    /**
     * Enhanced global search across all messages and entities
     */
    private void performGlobalSearch(String searchTerm) {
        if (searchTerm.trim().isEmpty()) {
            searchChats(searchTerm);
            return;
        }
        
        try {
            User currentUser = userService.getCurrentUser();
            Map<String, List<Object>> searchResults = new HashMap<>();
            
            // Search users
            List<User> foundUsers = userService.searchUsers(searchTerm);
            if (!foundUsers.isEmpty()) {
                searchResults.put("Users", new ArrayList<>(foundUsers));
            }
            
            // Search chats (already enhanced above)
            List<Object> chatResults = new ArrayList<>();
            searchChats(searchTerm); // This populates chatItems
            chatResults.addAll(chatItems);
            if (!chatResults.isEmpty()) {
                searchResults.put("Chats & Channels", chatResults);
            }
            
            // Search messages across all chats
            List<Message> foundMessages = messageService.searchMessagesGlobally(searchTerm, currentUser.getUserId(), 50);
            if (!foundMessages.isEmpty()) {
                searchResults.put("Messages", new ArrayList<>(foundMessages));
            }
            
            // Show comprehensive search results dialog
            showComprehensiveSearchResults(searchResults, searchTerm);
            
        } catch (Exception e) {
            logger.error("Error performing global search", e);
            showAlert("Search Error", "Failed to search: " + e.getMessage());
        }
    }
    
    /**
     * Open discovery dialog for finding public channels and groups
     */
    private void openDiscoveryDialog(String searchTerm) {
        try {
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Discover Channels & Groups");
            
            VBox root = new VBox(15);
            root.setPadding(new Insets(20));
            root.setStyle("-fx-background-color: white;");
            
            // Header
            Label titleLabel = new Label("Discover Public Channels & Groups");
            titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0088cc;");
            
            // Search section
            HBox searchBox = new HBox(10);
            TextField discoverySearchField = new TextField(searchTerm);
            discoverySearchField.setPromptText("Search for channels and groups...");
            discoverySearchField.setPrefWidth(300);
            
            Button searchButton = new Button("Search");
            Button createPublicButton = new Button("+ Create Public");
            
            searchBox.getChildren().addAll(discoverySearchField, searchButton, createPublicButton);
            
            // Results area
            ScrollPane resultsPane = new ScrollPane();
            VBox resultsContainer = new VBox(10);
            resultsPane.setContent(resultsContainer);
            resultsPane.setPrefHeight(400);
            resultsPane.setFitToWidth(true);
            
            // Category tabs
            TabPane categoryTabs = new TabPane();
            
            Tab channelsTab = new Tab("Public Channels");
            channelsTab.setClosable(false);
            VBox channelsContent = new VBox(10);
            channelsTab.setContent(channelsContent);
            
            Tab groupsTab = new Tab("Public Groups");
            groupsTab.setClosable(false);
            VBox groupsContent = new VBox(10);
            groupsTab.setContent(groupsContent);
            
            Tab nearbyTab = new Tab("Nearby");
            nearbyTab.setClosable(false);
            VBox nearbyContent = new VBox(10);
            nearbyContent.getChildren().add(new Label("Location-based discovery feature coming soon..."));
            nearbyTab.setContent(nearbyContent);
            
            categoryTabs.getTabs().addAll(channelsTab, groupsTab, nearbyTab);
            
            // Search functionality
            Runnable performDiscoverySearch = () -> {
                String query = discoverySearchField.getText().trim();
                if (query.isEmpty()) {
                    loadFeaturedChannelsAndGroups(channelsContent, groupsContent);
                } else {
                    searchPublicChannelsAndGroups(query, channelsContent, groupsContent);
                }
            };
            
            searchButton.setOnAction(e -> performDiscoverySearch.run());
            discoverySearchField.setOnAction(e -> performDiscoverySearch.run());
            
            // Create public functionality
            createPublicButton.setOnAction(e -> {
                stage.close();
                showCreatePublicDialog();
            });
            
            // Initial load
            performDiscoverySearch.run();
            
            // Close button
            Button closeButton = new Button("Close");
            closeButton.setOnAction(e -> stage.close());
            
            root.getChildren().addAll(titleLabel, searchBox, categoryTabs, closeButton);
            
            Scene scene = new Scene(root, 700, 600);
            stage.setScene(scene);
            stage.show();
            
        } catch (Exception e) {
            logger.error("Error opening discovery dialog", e);
            showAlert("Error", "Failed to open discovery dialog: " + e.getMessage());
        }
    }
    
    /**
     * Load featured channels and groups
     */
    private void loadFeaturedChannelsAndGroups(VBox channelsContent, VBox groupsContent) {
        try {
            // Clear existing content
            channelsContent.getChildren().clear();
            groupsContent.getChildren().clear();
            
            // Load featured public channels
            List<Channel> featuredChannels = channelService.getFeaturedPublicChannels(20);
            if (featuredChannels.isEmpty()) {
                channelsContent.getChildren().add(new Label("No featured channels available"));
            } else {
                for (Channel channel : featuredChannels) {
                    channelsContent.getChildren().add(createDiscoveryChannelItem(channel));
                }
            }
            
            // Load featured public groups
            List<GroupChat> featuredGroups = chatService.getFeaturedPublicGroups(20);
            if (featuredGroups.isEmpty()) {
                groupsContent.getChildren().add(new Label("No featured groups available"));
            } else {
                for (GroupChat group : featuredGroups) {
                    groupsContent.getChildren().add(createDiscoveryGroupItem(group));
                }
            }
            
        } catch (Exception e) {
            logger.error("Error loading featured content", e);
            channelsContent.getChildren().add(new Label("Error loading featured channels"));
            groupsContent.getChildren().add(new Label("Error loading featured groups"));
        }
    }
    
    /**
     * Search public channels and groups
     */
    private void searchPublicChannelsAndGroups(String query, VBox channelsContent, VBox groupsContent) {
        try {
            // Clear existing content
            channelsContent.getChildren().clear();
            groupsContent.getChildren().clear();
            
            // Search public channels
            List<Channel> foundChannels = channelService.searchPublicChannels(query);
            if (foundChannels.isEmpty()) {
                channelsContent.getChildren().add(new Label("No channels found for: " + query));
            } else {
                for (Channel channel : foundChannels) {
                    channelsContent.getChildren().add(createDiscoveryChannelItem(channel));
                }
            }
            
            // Search public groups
            List<GroupChat> foundGroups = chatService.searchPublicGroups(query);
            if (foundGroups.isEmpty()) {
                groupsContent.getChildren().add(new Label("No groups found for: " + query));
            } else {
                for (GroupChat group : foundGroups) {
                    groupsContent.getChildren().add(createDiscoveryGroupItem(group));
                }
            }
            
        } catch (Exception e) {
            logger.error("Error searching public content", e);
            channelsContent.getChildren().add(new Label("Error searching channels"));
            groupsContent.getChildren().add(new Label("Error searching groups"));
        }
    }
    
    /**
     * Create discovery item for channel
     */
    private HBox createDiscoveryChannelItem(Channel channel) {
        HBox item = new HBox(15);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(10));
        item.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-cursor: hand;");
        
        // Channel icon
        ImageView icon = new ImageView(loadDefaultChannelPicture());
        icon.setFitHeight(40);
        icon.setFitWidth(40);
        
        // Channel details
        VBox details = new VBox(3);
        Label nameLabel = new Label(channel.getChannelName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        Label infoLabel = new Label(channel.getSubscriberIds().size() + " subscribers");
        infoLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 12px;");
        
        Label descLabel = new Label(channel.getDescription() != null ? 
            (channel.getDescription().length() > 60 ? 
                channel.getDescription().substring(0, 60) + "..." : 
                channel.getDescription()) : "");
        descLabel.setStyle("-fx-text-fill: #555; -fx-font-size: 11px;");
        
        details.getChildren().addAll(nameLabel, infoLabel, descLabel);
        
        // Action buttons
        VBox actions = new VBox(5);
        actions.setAlignment(Pos.CENTER_RIGHT);
        
        Button joinButton = new Button("Join");
        joinButton.setStyle("-fx-background-color: #0088cc; -fx-text-fill: white; -fx-background-radius: 15;");
        
        Button shareButton = new Button("Share");
        shareButton.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-background-radius: 15;");
        
        // Check if already subscribed
        try {
            User currentUser = userService.getCurrentUser();
            if (channel.getSubscriberIds().contains(currentUser.getUserId())) {
                joinButton.setText("Joined");
                joinButton.setDisable(true);
                joinButton.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-background-radius: 15;");
            }
        } catch (Exception e) {
            logger.warn("Error checking channel subscription", e);
        }
        
        joinButton.setOnAction(e -> {
            try {
                User currentUser = userService.getCurrentUser();
                boolean success = channelService.subscribeToChannel(channel.getChannelId(), currentUser.getUserId());
                if (success) {
                    joinButton.setText("Joined");
                    joinButton.setDisable(true);
                    joinButton.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-background-radius: 15;");
                    loadUserChats(); // Refresh chat list
                    showAlert("Success", "Successfully joined " + channel.getChannelName());
                } else {
                    showAlert("Error", "Failed to join channel");
                }
            } catch (Exception ex) {
                logger.error("Error joining channel", ex);
                showAlert("Error", "Failed to join channel: " + ex.getMessage());
            }
        });
        
        shareButton.setOnAction(e -> shareChannel(channel));
        
        actions.getChildren().addAll(joinButton, shareButton);
        
        item.getChildren().addAll(icon, details, actions);
        HBox.setHgrow(details, Priority.ALWAYS);
        
        return item;
    }
    
    /**
     * Create discovery item for group
     */
    private HBox createDiscoveryGroupItem(GroupChat group) {
        HBox item = new HBox(15);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(10));
        item.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 8; -fx-cursor: hand;");
        
        // Group icon
        ImageView icon = new ImageView(loadDefaultGroupPicture());
        icon.setFitHeight(40);
        icon.setFitWidth(40);
        
        // Group details
        VBox details = new VBox(3);
        Label nameLabel = new Label(group.getGroupName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        Label infoLabel = new Label(group.getMemberIds().size() + " members");
        infoLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 12px;");
        
        Label descLabel = new Label(group.getDescription() != null ? 
            (group.getDescription().length() > 60 ? 
                group.getDescription().substring(0, 60) + "..." : 
                group.getDescription()) : "");
        descLabel.setStyle("-fx-text-fill: #555; -fx-font-size: 11px;");
        
        details.getChildren().addAll(nameLabel, infoLabel, descLabel);
        
        // Action buttons
        VBox actions = new VBox(5);
        actions.setAlignment(Pos.CENTER_RIGHT);
        
        Button joinButton = new Button("Join");
        joinButton.setStyle("-fx-background-color: #0088cc; -fx-text-fill: white; -fx-background-radius: 15;");
        
        Button shareButton = new Button("Share");
        shareButton.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-background-radius: 15;");
        
        // Check if already a member
        try {
            User currentUser = userService.getCurrentUser();
            if (group.getMemberIds().contains(currentUser.getUserId())) {
                joinButton.setText("Joined");
                joinButton.setDisable(true);
                joinButton.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-background-radius: 15;");
            }
        } catch (Exception e) {
            logger.warn("Error checking group membership", e);
        }
        
        joinButton.setOnAction(e -> {
            try {
                User currentUser = userService.getCurrentUser();
                boolean success = chatService.joinGroup(group.getGroupId(), currentUser.getUserId());
                if (success) {
                    joinButton.setText("Joined");
                    joinButton.setDisable(true);
                    joinButton.setStyle("-fx-background-color: #6c757d; -fx-text-fill: white; -fx-background-radius: 15;");
                    loadUserChats(); // Refresh chat list
                    showAlert("Success", "Successfully joined " + group.getGroupName());
                } else {
                    showAlert("Error", "Failed to join group");
                }
            } catch (Exception ex) {
                logger.error("Error joining group", ex);
                showAlert("Error", "Failed to join group: " + ex.getMessage());
            }
        });
        
        shareButton.setOnAction(e -> shareGroup(group));
        
        actions.getChildren().addAll(joinButton, shareButton);
        
        item.getChildren().addAll(icon, details, actions);
        HBox.setHgrow(details, Priority.ALWAYS);
        
        return item;
    }
    
    /**
     * Share channel functionality
     */
    private void shareChannel(Channel channel) {
        try {
            String shareText = "Channel: " + channel.getChannelName() + "\n" +
                             "" + channel.getSubscriberIds().size() + " subscribers\n";
            if (channel.getDescription() != null && !channel.getDescription().isEmpty()) {
                shareText += "" + channel.getDescription();
            }
            
            // Copy to clipboard
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(shareText);
            clipboard.setContent(content);
            
            showAlert("Shared", "Channel link copied to clipboard!");
            
        } catch (Exception e) {
            logger.error("Error sharing channel", e);
            showAlert("Error", "Failed to share channel");
        }
    }
    
    /**
     * Share group functionality
     */
    private void shareGroup(GroupChat group) {
        try {
            String shareText = "Join Group: " + group.getGroupName() + "\n" +
                             "" + group.getMemberIds().size() + " members\n";
            if (group.getDescription() != null && !group.getDescription().isEmpty()) {
                shareText += "" + group.getDescription() + "\n";
            }
            
            // Generate a shareable link for the group (if it has one)
            String groupLink = "telegram://join?group=" + group.getGroupId();
            shareText += "\n" + groupLink;
            
            // Copy to clipboard
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(shareText);
            clipboard.setContent(content);
            
            showAlert("Shared", "Group link copied to clipboard!");
            
        } catch (Exception e) {
            logger.error("Error sharing group", e);
            showAlert("Error", "Failed to share group");
        }
    }
    
    /**
     * Show dialog to create public channel or group
     */
    private void showCreatePublicDialog() {
        try {
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Create Public Channel/Group");
            
            VBox root = new VBox(20);
            root.setPadding(new Insets(20));
            root.setStyle("-fx-background-color: white;");
            
            Label titleLabel = new Label("Create Public Channel or Group");
            titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
            
            // Type selection
            ToggleGroup typeGroup = new ToggleGroup();
            RadioButton channelRadio = new RadioButton("Public Channel");
            RadioButton groupRadio = new RadioButton("Public Group");
            channelRadio.setToggleGroup(typeGroup);
            groupRadio.setToggleGroup(typeGroup);
            channelRadio.setSelected(true);
            
            VBox typeBox = new VBox(10);
            typeBox.getChildren().addAll(new Label("Type:"), channelRadio, groupRadio);
            
            // Name field
            TextField nameField = new TextField();
            nameField.setPromptText("Enter name...");
            VBox nameBox = new VBox(5);
            nameBox.getChildren().addAll(new Label("Name:"), nameField);
            
            // Description field
            TextArea descArea = new TextArea();
            descArea.setPromptText("Enter description...");
            descArea.setPrefRowCount(3);
            VBox descBox = new VBox(5);
            descBox.getChildren().addAll(new Label("Description:"), descArea);
            
            // Buttons
            HBox buttonBox = new HBox(10);
            Button createButton = new Button("Create");
            Button cancelButton = new Button("Cancel");
            
            createButton.setStyle("-fx-background-color: #0088cc; -fx-text-fill: white;");
            cancelButton.setOnAction(e -> stage.close());
            
            createButton.setOnAction(e -> {
                String name = nameField.getText().trim();
                String description = descArea.getText().trim();
                
                if (name.isEmpty()) {
                    showAlert("Error", "Please enter a name");
                    return;
                }
                
                try {
                    User currentUser = userService.getCurrentUser();
                    boolean success = false;
                    
                    if (channelRadio.isSelected()) {
                        // Create public channel
                        Channel channel = new Channel(name, currentUser.getUserId());
                        channel.setDescription(description);
                        channel.setPrivate(false); // Make it public
                        success = channelService.createChannel(channel);
                    } else {
                        // Create public group
                        GroupChat group = new GroupChat(name, currentUser.getUserId());
                        group.setDescription(description);
                        success = chatService.createGroup(name, description, currentUser.getUserId()) != null;
                    }
                    
                    if (success) {
                        stage.close();
                        loadUserChats(); // Refresh chat list
                        showAlert("Success", "Successfully created " + 
                                (channelRadio.isSelected() ? "channel" : "group"));
                    } else {
                        showAlert("Error", "Failed to create " + 
                                (channelRadio.isSelected() ? "channel" : "group"));
                    }
                    
                } catch (Exception ex) {
                    logger.error("Error creating public chat", ex);
                    showAlert("Error", "Failed to create: " + ex.getMessage());
                }
            });
            
            buttonBox.getChildren().addAll(createButton, cancelButton);
            buttonBox.setAlignment(Pos.CENTER);
            
            root.getChildren().addAll(titleLabel, typeBox, nameBox, descBox, buttonBox);
            
            Scene scene = new Scene(root, 450, 400);
            stage.setScene(scene);
            stage.show();
            
        } catch (Exception e) {
            logger.error("Error showing create public dialog", e);
            showAlert("Error", "Failed to open create dialog");
        }
    }
    
    /**
     * Show comprehensive search results in a new dialog
     */
    private void showComprehensiveSearchResults(Map<String, List<Object>> results, String searchTerm) {
        try {
            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Search Results for: " + searchTerm);
            
            VBox root = new VBox(10);
            root.setPadding(new Insets(20));
            root.setStyle("-fx-background-color: white;");
            
            Label titleLabel = new Label("Search Results for: \"" + searchTerm + "\"");
            titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
            root.getChildren().add(titleLabel);
            
            ScrollPane scrollPane = new ScrollPane();
            VBox resultsContainer = new VBox(15);
            
            if (results.isEmpty()) {
                Label noResults = new Label("No results found");
                noResults.setStyle("-fx-font-size: 14px; -fx-text-fill: gray;");
                resultsContainer.getChildren().add(noResults);
            } else {
                for (Map.Entry<String, List<Object>> category : results.entrySet()) {
                    if (!category.getValue().isEmpty()) {
                        // Category header
                        Label categoryLabel = new Label(category.getKey() + " (" + category.getValue().size() + ")");
                        categoryLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0088cc;");
                        resultsContainer.getChildren().add(categoryLabel);
                        
                        // Category items
                        VBox categoryItems = new VBox(5);
                        categoryItems.setPadding(new Insets(0, 0, 0, 20));
                        
                        for (Object item : category.getValue()) {
                            HBox itemBox = createSearchResultItem(item, searchTerm);
                            categoryItems.getChildren().add(itemBox);
                        }
                        
                        resultsContainer.getChildren().add(categoryItems);
                    }
                }
            }
            
            scrollPane.setContent(resultsContainer);
            scrollPane.setFitToWidth(true);
            scrollPane.setPrefHeight(400);
            
            Button closeButton = new Button("Close");
            closeButton.setOnAction(e -> stage.close());
            
            root.getChildren().addAll(scrollPane, closeButton);
            
            Scene scene = new Scene(root, 600, 500);
            stage.setScene(scene);
            stage.show();
            
        } catch (Exception e) {
            logger.error("Error showing comprehensive search results", e);
        }
    }
    
    /**
     * Create a search result item display
     */
    private HBox createSearchResultItem(Object item, String searchTerm) {
        HBox itemBox = new HBox(10);
        itemBox.setAlignment(Pos.CENTER_LEFT);
        itemBox.setPadding(new Insets(8));
        itemBox.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 8; -fx-cursor: hand;");
        
        // Item icon
        ImageView icon = new ImageView();
        icon.setFitHeight(24);
        icon.setFitWidth(24);
        
        // Item details
        VBox details = new VBox(2);
        Label nameLabel = new Label();
        Label subtitleLabel = new Label();
        subtitleLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 12px;");
        
        if (item instanceof User) {
            User user = (User) item;
            icon.setImage(loadDefaultProfilePicture());
            nameLabel.setText(user.getDisplayName());
            subtitleLabel.setText("@" + user.getUsername());
            
            itemBox.setOnMouseClicked(e -> {
                try {
                    // Start a private chat with this user
                    PrivateChat chat = chatService.getOrCreatePrivateChat(
                        userService.getCurrentUser().getUserId(), user.getUserId());
                    selectChat(chat);
                    ((Stage) itemBox.getScene().getWindow()).close();
                } catch (Exception ex) {
                    logger.error("Error selecting user from search", ex);
                }
            });
            
        } else if (item instanceof GroupChat) {
            GroupChat group = (GroupChat) item;
            icon.setImage(loadDefaultGroupPicture());
            nameLabel.setText(group.getGroupName());
            subtitleLabel.setText(group.getMemberIds().size() + " members");
            
            itemBox.setOnMouseClicked(e -> {
                selectChat(group);
                ((Stage) itemBox.getScene().getWindow()).close();
            });
            
        } else if (item instanceof Channel) {
            Channel channel = (Channel) item;
            icon.setImage(loadDefaultChannelPicture());
            nameLabel.setText(channel.getChannelName());
            subtitleLabel.setText(channel.getSubscriberIds().size() + " subscribers");
            
            itemBox.setOnMouseClicked(e -> {
                selectChat(channel);
                ((Stage) itemBox.getScene().getWindow()).close();
            });
            
        } else if (item instanceof Message) {
            Message message = (Message) item;
            icon.setImage(loadDefaultMessagePicture());
            nameLabel.setText("Message from " + getChatDisplayNameById(message.getReceiverId()));
            String content = message.getContent();
            if (content.length() > 50) {
                content = content.substring(0, 50) + "...";
            }
            subtitleLabel.setText(content);
            
            itemBox.setOnMouseClicked(e -> {
                try {
                    // Navigate to the chat containing this message
                    String chatId = message.getReceiverId();
                    Object chat = findChatById(chatId);
                    if (chat != null) {
                        selectChat(chat);
                        // TODO: Scroll to specific message
                        ((Stage) itemBox.getScene().getWindow()).close();
                    }
                } catch (Exception ex) {
                    logger.error("Error navigating to message from search", ex);
                }
            });
        }
        
        details.getChildren().addAll(nameLabel, subtitleLabel);
        itemBox.getChildren().addAll(icon, details);
        
        return itemBox;
    }
    
    private void selectChatById(String chatId) {
        for (Object chat : chatItems) {
            if (getChatId(chat).equals(chatId)) {
                chatListView.getSelectionModel().select(chat);
                selectChat(chat);
                break;
            }
        }
    }
    
    // Real-time features
    /**
     * Start typing indicator
     */
    private void startTyping() {
        if (currentChat != null && !isTyping) {
            // Check if user allows showing typing indicators
            User currentUser = userService.getCurrentUser();
            if (currentUser != null && !currentUser.isShowTypingIndicators()) {
                return; // Don't send typing indicator if privacy setting is disabled
            }
            
            isTyping = true;
            String chatId = getChatId(currentChat);
            String currentUserId = currentUser.getUserId();
            messageBroker.updateTypingStatus(chatId, currentUserId, true);
            
            // Auto-stop typing after 3 seconds of inactivity
            if (typingTimer != null) {
                typingTimer.stop();
            }
            typingTimer = new Timeline(new KeyFrame(Duration.seconds(3), e -> stopTyping()));
            typingTimer.play();
        }
    }
    
    /**
     * Stop typing indicator
     */
    private void stopTyping() {
        if (currentChat != null && isTyping) {
            isTyping = false;
            String chatId = getChatId(currentChat);
            User currentUser = userService.getCurrentUser();
            if (currentUser != null) {
                String currentUserId = currentUser.getUserId();
                messageBroker.updateTypingStatus(chatId, currentUserId, false);
            }
            
            if (typingTimer != null) {
                typingTimer.stop();
                typingTimer = null;
            }
        }
    }
    
    // Message selection features
    private void toggleMessageSelection(String messageId, VBox messageBox) {
        try {
            if (selectedMessageIds.contains(messageId)) {
                selectedMessageIds.remove(messageId);
                // Remove selection styling and restore original style
                String currentStyle = messageBox.getStyle();
                if (currentStyle != null) {
                    // Remove selection-specific styles
                    currentStyle = currentStyle.replace("-fx-border-color: #0088cc; -fx-border-width: 2;", "");
                    currentStyle = currentStyle.replace("-fx-background-color: rgba(0, 136, 204, 0.15);", "");
                    currentStyle = currentStyle.trim();
                    
                    // If style is empty, reset to original message box style based on sender
                    if (currentStyle.isEmpty() || currentStyle.equals("")) {
                        // Check if this is from current user by looking at existing style or message data
                        String msgId = (String) messageBox.getUserData();
                        if (msgId != null && msgId.equals(messageId)) {
                            // Find the actual message to determine sender
                            boolean isFromCurrentUser = false;
                            for (javafx.scene.Node child : messageContainer.getChildren()) {
                                if (child instanceof HBox) {
                                    HBox hbox = (HBox) child;
                                    for (javafx.scene.Node node : hbox.getChildren()) {
                                        if (node == messageBox) {
                                            // Check alignment to determine sender
                                            isFromCurrentUser = hbox.getAlignment() == javafx.geometry.Pos.CENTER_RIGHT;
                                            break;
                                        }
                                    }
                                }
                            }
                            
                            // Restore original styling based on sender
                            if (isFromCurrentUser) {
                                messageBox.setStyle("-fx-background-color: #0088cc; -fx-background-radius: 15; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 1);");
                            } else {
                                messageBox.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 1);");
                            }
                        }
                    } else {
                        messageBox.setStyle(currentStyle);
                    }
                }
                logger.info("Message deselected: {} (Total selected: {})", messageId, selectedMessageIds.size());
            } else {
                selectedMessageIds.add(messageId);
                // Add selection styling with background and border
                String currentStyle = messageBox.getStyle();
                if (currentStyle == null) currentStyle = "";
                // Remove hover effect and add selection effect
                currentStyle = currentStyle.replace(" -fx-background-color: rgba(0, 136, 204, 0.1);", "");
                messageBox.setStyle(currentStyle + " -fx-border-color: #0088cc; -fx-border-width: 2; -fx-background-color: rgba(0, 136, 204, 0.15);");
                logger.info("Message selected: {} (Total selected: {})", messageId, selectedMessageIds.size());
            }
            
            updateSelectionMode();
        } catch (Exception e) {
            logger.error("Error toggling message selection for: " + messageId, e);
        }
    }
    
    private void updateSelectionMode() {
        boolean hasSelection = !selectedMessageIds.isEmpty();
        logger.debug("Updating selection mode: hasSelection={}, isSelectionMode={}, count={}", 
            hasSelection, isSelectionMode, selectedMessageIds.size());
        
        if (hasSelection && !isSelectionMode) {
            enterSelectionMode();
        } else if (!hasSelection && isSelectionMode) {
            exitSelectionMode();
        }
        
        if (hasSelection && selectionCountLabel != null) {
            selectionCountLabel.setText(selectedMessageIds.size() + " selected");
        }
    }
    
    private void enterSelectionMode() {
        isSelectionMode = true;
        if (selectionToolbar != null) {
            selectionToolbar.setVisible(true);
            logger.info("Entered selection mode with {} messages", selectedMessageIds.size());
        } else {
            logger.warn("Selection toolbar is null!");
        }
        if (selectionCountLabel != null) {
            selectionCountLabel.setText(selectedMessageIds.size() + " selected");
        }
    }
    
    private void exitSelectionMode() {
        isSelectionMode = false;
        
        // Clear visual selection from all messages
        for (String messageId : selectedMessageIds) {
            VBox messageBox = messageBoxMap.get(messageId);
            if (messageBox != null) {
                String currentStyle = messageBox.getStyle();
                if (currentStyle != null) {
                    // Remove selection styles and restore original background
                    currentStyle = currentStyle.replace("-fx-border-color: #0088cc; -fx-border-width: 2;", "");
                    currentStyle = currentStyle.replace("-fx-background-color: rgba(0, 136, 204, 0.15);", "");
                    currentStyle = currentStyle.trim();
                    
                    // If style is empty after removing selection styles, restore original style
                    if (currentStyle.isEmpty() || currentStyle.equals("")) {
                        // Check if this is from current user by finding the message container
                        boolean isFromCurrentUser = false;
                        for (javafx.scene.Node child : messageContainer.getChildren()) {
                            if (child instanceof HBox) {
                                HBox hbox = (HBox) child;
                                for (javafx.scene.Node node : hbox.getChildren()) {
                                    if (node == messageBox) {
                                        isFromCurrentUser = hbox.getAlignment() == javafx.geometry.Pos.CENTER_RIGHT;
                                        break;
                                    }
                                }
                            }
                        }
                        
                        // Restore original styling based on sender
                        if (isFromCurrentUser) {
                            messageBox.setStyle("-fx-background-color: #0088cc; -fx-background-radius: 15; -fx-text-fill: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 1);");
                        } else {
                            messageBox.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 1);");
                        }
                    } else {
                        messageBox.setStyle(currentStyle);
                    }
                }
            }
        }
        
        selectedMessageIds.clear();
        if (selectionToolbar != null) {
            selectionToolbar.setVisible(false);
            logger.info("Exited selection mode");
        }
        
        // Remove selection styling from all messages - improved error handling
        try {
            for (javafx.scene.Node node : messageContainer.getChildren()) {
                if (node instanceof HBox) {
                    HBox messageContainer = (HBox) node;
                    for (javafx.scene.Node child : messageContainer.getChildren()) {
                        if (child instanceof VBox) {
                            VBox messageBox = (VBox) child;
                            String style = messageBox.getStyle();
                            if (style != null && style.contains("-fx-border-color: #0088cc")) {
                                messageBox.setStyle(style.replace("-fx-border-color: #0088cc; -fx-border-width: 2;", ""));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error clearing message selection styling", e);
        }
    }
    
    /**
     * Setup custom cell factory for chat list with online indicators
     */
    private void setupChatListCellFactory() {
        chatListView.setCellFactory(listView -> new ListCell<Object>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle(""); // Reset style
                    return;
                }
                
                HBox cellContent = new HBox(12);
                cellContent.setAlignment(Pos.CENTER_LEFT);
                cellContent.setPadding(new Insets(8, 12, 8, 12));
                
                // Make the cell content fill the entire cell and be clickable
                cellContent.setMaxWidth(Double.MAX_VALUE);
                cellContent.setStyle("-fx-cursor: hand;");
                
                // Profile picture container
                StackPane profileContainer = new StackPane();
                Circle profileCircle = new Circle(20, Color.LIGHTGRAY);
                profileContainer.getChildren().add(profileCircle);
                
                // Add actual profile picture if available
                if (item instanceof PrivateChat) {
                    PrivateChat privateChat = (PrivateChat) item;
                    User currentUser = userService.getCurrentUser();
                    if (currentUser != null) {
                        String otherUserId = privateChat.getOtherUserId(currentUser.getUserId());
                        User otherUser = userService.findUserById(otherUserId);
                        
                        // Load profile picture if available
                        if (otherUser != null && otherUser.getProfilePicturePath() != null && !otherUser.getProfilePicturePath().isEmpty()) {
                            try {
                                // Decode URL-encoded path (fix %20 to spaces, etc.)
                                String decodedPath = java.net.URLDecoder.decode(otherUser.getProfilePicturePath(), "UTF-8");
                                File profileFile = new File(decodedPath);
                                if (profileFile.exists()) {
                                    Image profileImage = new Image(profileFile.toURI().toString());
                                    ImageView profileImageView = createCircularProfileImageView(profileImage, 40);
                                    if (profileImageView != null) {
                                        profileContainer.getChildren().add(profileImageView);
                                        // Hide the default circle background
                                        profileCircle.setVisible(false);
                                    }
                                }
                            } catch (Exception e) {
                                logger.debug("Could not load profile picture for user {}: {}", otherUserId, e.getMessage());
                            }
                        }
                    }
                }
                
                // Online indicator for private chats
                if (item instanceof PrivateChat) {
                    PrivateChat privateChat = (PrivateChat) item;
                    User currentUser = userService.getCurrentUser();
                    if (currentUser != null) {
                        String otherUserId = privateChat.getOtherUserId(currentUser.getUserId());
                        boolean isOnline = messageBroker.isUserOnline(otherUserId);
                        
                        if (isOnline) {
                            Circle onlineIndicator = new Circle(6, Color.GREEN);
                            onlineIndicator.setStroke(Color.WHITE);
                            onlineIndicator.setStrokeWidth(2);
                            StackPane.setAlignment(onlineIndicator, Pos.BOTTOM_RIGHT);
                            onlineIndicator.setTranslateX(5);
                            onlineIndicator.setTranslateY(5);
                            profileContainer.getChildren().add(onlineIndicator);
                        }
                    }
                }
                
                cellContent.getChildren().add(profileContainer);
                
                // Chat info
                VBox chatInfo = new VBox(2);
                chatInfo.setAlignment(Pos.CENTER_LEFT);
                
                if (item instanceof PrivateChat) {
                    PrivateChat privateChat = (PrivateChat) item;
                    User currentUser = userService.getCurrentUser();
                    if (currentUser != null) {
                        String otherUserId = privateChat.getOtherUserId(currentUser.getUserId());
                        User otherUser = userService.findUserById(otherUserId);
                        if (otherUser != null) {
                            Label nameLabel = new Label(otherUser.getProfileName());
                            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                            
                            boolean isOnline = messageBroker.isUserOnline(otherUserId);
                            Label statusLabel = new Label(isOnline ? "Online" : "Last seen recently");
                            statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + 
                                (isOnline ? "#4CAF50" : "#757575") + ";");
                            
                            chatInfo.getChildren().addAll(nameLabel, statusLabel);
                        }
                    }
                } else if (item instanceof GroupChat) {
                    GroupChat groupChat = (GroupChat) item;
                    Label nameLabel = new Label(groupChat.getGroupName());
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                    
                    List<User> members = chatService.getGroupMembers(groupChat.getGroupId());
                    Label membersLabel = new Label(members.size() + " members");
                    membersLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #757575;");
                    
                    chatInfo.getChildren().addAll(nameLabel, membersLabel);
                } else if (item instanceof Channel) {
                    Channel channel = (Channel) item;
                    Label nameLabel = new Label(channel.getChannelName());
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
                    
                    Label typeLabel = new Label("Channel");
                    typeLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #757575;");
                    
                    chatInfo.getChildren().addAll(nameLabel, typeLabel);
                }
                
                HBox.setHgrow(chatInfo, Priority.ALWAYS);
                cellContent.getChildren().add(chatInfo);
                
                // Set up click handling on the cell content
                cellContent.setOnMouseClicked(event -> {
                    logger.info("Cell content clicked: {} clicks, button: {}", event.getClickCount(), event.getButton());
                    if (event.getButton() == MouseButton.PRIMARY) {
                        if (event.getClickCount() == 1) {
                            // Single click - select the item
                            getListView().getSelectionModel().select(getIndex());
                            selectChat(item);
                            logger.info("Chat selected (single-click): {}", getChatDisplayName(item));
                        } else if (event.getClickCount() == 2) {
                            // Double click - also select the item (same as single click for chats)
                            getListView().getSelectionModel().select(getIndex());
                            selectChat(item);
                            logger.info("Chat double-clicked: {}", getChatDisplayName(item));
                        }
                        event.consume(); // Prevent event propagation
                    } else if (event.getButton() == MouseButton.SECONDARY) {
                        // Right click - show context menu without opening the chat
                        getListView().getSelectionModel().select(getIndex());
                        showChatContextMenu(item, event.getScreenX(), event.getScreenY());
                        logger.info("Chat right-clicked, context menu shown: {}", getChatDisplayName(item));
                        event.consume();
                    }
                });
                
                setGraphic(cellContent);
                setText(null);
                
                // Also add click handler to the cell itself as backup
                setOnMouseClicked(event -> {
                    logger.info("Cell clicked: {} clicks, button: {}", event.getClickCount(), event.getButton());
                    if (item != null && !event.isConsumed()) {
                        if (event.getButton() == MouseButton.PRIMARY) {
                            if (event.getClickCount() == 1) {
                                getListView().getSelectionModel().select(getIndex());
                                selectChat(item);
                                logger.info("Cell selected (fallback single-click): {}", getChatDisplayName(item));
                            } else if (event.getClickCount() == 2) {
                                getListView().getSelectionModel().select(getIndex());
                                selectChat(item);
                                logger.info("Cell double-clicked (fallback): {}", getChatDisplayName(item));
                            }
                        } else if (event.getButton() == MouseButton.SECONDARY) {
                            // Right click fallback - show context menu without opening the chat
                            getListView().getSelectionModel().select(getIndex());
                            showChatContextMenu(item, event.getScreenX(), event.getScreenY());
                            logger.info("Cell right-clicked (fallback), context menu shown: {}", getChatDisplayName(item));
                        }
                    }
                });
                
                setGraphic(cellContent);
                setText(null);
                
                // Style selected cells
                if (isSelected()) {
                    setStyle("-fx-background-color: #E3F2FD;");
                } else {
                    setStyle("-fx-background-color: transparent;");
                }
            }
        });
    }
    
    // Real-time message listener implementation
    @Override
    public void onNewMessage(Message message) {
        Platform.runLater(() -> {
            try {
                logger.debug("Received new message: {} from {} to {}", 
                    message.getContent(), message.getSenderId(), message.getReceiverId());
                
                // Only add messages from other users to avoid duplicates (we add our own messages immediately when sending)
                if (currentChat != null && isMessageForCurrentChat(message) && !isMessageFromCurrentUser(message)) {
                    logger.debug("Message is for current chat from another user, adding to UI");
                    addMessageToUI(message);
                    scrollToBottom();
                } else {
                    logger.debug("Message filtered out: currentChat={}, messageForCurrentChat={}, fromCurrentUser={}", 
                        currentChat != null ? getChatId(currentChat) : "null", 
                        isMessageForCurrentChat(message),
                        isMessageFromCurrentUser(message));
                }
                
            } catch (Exception e) {
                logger.error("Error handling new message", e);
            }
        });
    }
    
    @Override
    public void onTypingStatusChanged(String chatId, String userId, boolean isTyping) {
        Platform.runLater(() -> {
            if (currentChat != null && getChatId(currentChat).equals(chatId)) {
                if (isTyping && !isCurrentUser(userId)) {
                    // Show typing indicator
                    String userName = getSenderName(userId);
                    if (typingIndicatorLabel != null) {
                        typingIndicatorLabel.setText(userName + " is typing...");
                        typingIndicatorLabel.setVisible(true);
                    }
                    // Temporarily change subtitle
                    chatSubtitleLabel.setText(userName + " is typing...");
                    chatSubtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #007AFF; -fx-font-style: italic;");
                } else {
                    // Restore normal subtitle
                    updateChatSubtitle();
                    if (typingIndicatorLabel != null) {
                        typingIndicatorLabel.setVisible(false);
                    }
                }
            }
        });
    }
    
    @Override
    public void onMessageRead(String messageId, String userId) {
        Platform.runLater(() -> {
            try {
                // Always update read status indicators instantly, regardless of current chat
                updateMessageReadStatusGlobally(messageId, userId);
                
                // Also update if it's the current chat for immediate feedback
                if (currentChat != null) {
                    updateMessageReadStatusInstant(messageId, userId);
                }
                
                // Log the read receipt
                User reader = userService.getUserById(userId);
                if (reader != null) {
                    logger.info("Message {} instantly read by {}", messageId, reader.getDisplayName());
                }
                
            } catch (Exception e) {
                logger.error("Error handling instant message read", e);
            }
        });
    }
    
    /**
     * Update message read status globally across all chat contexts
     */
    private void updateMessageReadStatusGlobally(String messageId, String userId) {
        try {
            // Update in database first for persistence
            messageService.updateMessageStatus(messageId, Message.MessageStatus.READ);
            
            // Update in current UI if the message is visible
            updateMessageReadStatusInstant(messageId, userId);
            
            // Update chat list to reflect read status changes
            Platform.runLater(() -> {
                // Refresh chat list to show updated read indicators
                refreshChatList();
                
                // Update current chat title/subtitle if needed
                if (currentChat != null) {
                    updateChatSubtitle();
                }
            });
            
        } catch (Exception e) {
            logger.error("Error updating message read status globally", e);
        }
    }
    
    /**
     * Refresh the entire chat list to reflect any status changes
     */
    private void refreshChatList() {
        try {
            User currentUser = userService.getCurrentUser();
            if (currentUser == null) return;
            
            // Get current selection
            Object selectedChat = chatListView.getSelectionModel().getSelectedItem();
            
            // Reload all chats
            chatItems.clear();
            loadUserChats();
            
            // Restore selection if possible
            if (selectedChat != null) {
                for (Object chat : chatItems) {
                    if (getChatId(chat).equals(getChatId(selectedChat))) {
                        chatListView.getSelectionModel().select(chat);
                        break;
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Error refreshing chat list", e);
        }
    }
    
    /**
     * Instantly update message read status in the UI without full refresh
     */
    private void updateMessageReadStatusInstant(String messageId, String userId) {
        try {
            // Find and update the specific message immediately
            for (javafx.scene.Node node : messageContainer.getChildren()) {
                if (node instanceof HBox) {
                    HBox messageRow = (HBox) node;
                    for (javafx.scene.Node child : messageRow.getChildren()) {
                        if (child instanceof VBox) {
                            VBox messageBox = (VBox) child;
                            String msgId = (String) messageBox.getUserData();
                            if (messageId.equals(msgId)) {
                                // Add instant read receipt indicator for received messages
                                addInstantReadReceiptIndicator(messageBox, userId);
                                
                                // Also update status indicator for sent messages
                                updateMessageStatusIndicator(messageBox, messageId, Message.MessageStatus.READ);
                                return; // Found and updated, exit immediately
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error updating message read status instantly", e);
        }
    }
    
    /**
     * Update the status indicator for a specific message
     */
    private void updateMessageStatusIndicator(VBox messageBox, String messageId, Message.MessageStatus status) {
        try {
            // Find the status indicator by ID
            String statusIndicatorId = "status-indicator-" + messageId;
            for (javafx.scene.Node node : messageBox.getChildren()) {
                if (node instanceof HBox) {
                    HBox hbox = (HBox) node;
                    for (javafx.scene.Node child : hbox.getChildren()) {
                        if (child instanceof HBox) {
                            HBox statusBox = (HBox) child;
                            for (javafx.scene.Node statusChild : statusBox.getChildren()) {
                                if (statusChild instanceof Label && statusIndicatorId.equals(statusChild.getId())) {
                                    // Remove the old label and replace with ImageView
                                    statusBox.getChildren().remove(statusChild);
                                    
                                    ImageView statusIcon = createStatusIcon(status);
                                    if (statusIcon != null) {
                                        statusIcon.setId(statusIndicatorId);
                                        statusBox.getChildren().add(statusIcon);
                                        
                                        // Add a quick flash animation to show the change
                                        Timeline flashTimeline = new Timeline();
                                        KeyFrame flash1 = new KeyFrame(Duration.millis(0), 
                                            new KeyValue(statusIcon.scaleXProperty(), 1.0),
                                            new KeyValue(statusIcon.scaleYProperty(), 1.0));
                                        KeyFrame flash2 = new KeyFrame(Duration.millis(150), 
                                            new KeyValue(statusIcon.scaleXProperty(), 1.3),
                                            new KeyValue(statusIcon.scaleYProperty(), 1.3));
                                        KeyFrame flash3 = new KeyFrame(Duration.millis(300), 
                                            new KeyValue(statusIcon.scaleXProperty(), 1.0),
                                            new KeyValue(statusIcon.scaleYProperty(), 1.0));
                                        flashTimeline.getKeyFrames().addAll(flash1, flash2, flash3);
                                        flashTimeline.play();
                                    }
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error updating message status indicator", e);
        }
    }
    
    /**
     * Add instant read receipt indicator to message
     */
    private void addInstantReadReceiptIndicator(VBox messageBox, String readerId) {
        try {
            // Check if read indicator already exists
            boolean hasReadIndicator = messageBox.getChildren().stream()
                    .anyMatch(node -> "read-indicator".equals(node.getId()));
            
            if (!hasReadIndicator) {
                ImageView readIndicator = createStatusIcon(Message.MessageStatus.READ);
                if (readIndicator != null) {
                    readIndicator.setId("read-indicator");
                    
                    // Add tooltip with reader info
                    User reader = userService.getUserById(readerId);
                    if (reader != null) {
                        Tooltip readTooltip = new Tooltip("Read by " + reader.getDisplayName());
                        Tooltip.install(readIndicator, readTooltip);
                    }
                
                    // Add to the message box with instant visual feedback
                    messageBox.getChildren().add(readIndicator);
                    
                    // Optional: Add a brief highlight animation for instant feedback
                    Timeline timeline = new Timeline();
                    KeyFrame frame1 = new KeyFrame(Duration.millis(0), 
                        new KeyValue(readIndicator.opacityProperty(), 0.0));
                    KeyFrame frame2 = new KeyFrame(Duration.millis(200), 
                        new KeyValue(readIndicator.opacityProperty(), 1.0));
                    timeline.getKeyFrames().addAll(frame1, frame2);
                    timeline.play();
                }
            }
        } catch (Exception e) {
            logger.error("Error adding instant read receipt indicator", e);
        }
    }
    @Override
    public void onUserProfileUpdated(String userId) {
        Platform.runLater(() -> {
            logger.info("Received profile update for user: {}", userId);
            
            // Update side panel profile if it's the current user
            User currentUser = userService.getCurrentUser();
            if (currentUser != null && currentUser.getUserId().equals(userId)) {
                updateSidePanelProfile(currentUser);
            }
            
            // Refresh the current chat if it's with the updated user
            if (currentChat instanceof PrivateChat) {
                PrivateChat privateChat = (PrivateChat) currentChat;
                if (privateChat.getParticipantIds().contains(userId)) {
                    updateChatHeader(currentChat);
                    refreshCurrentChat();
                }
            } else if (currentChat instanceof GroupChat) {
                GroupChat groupChat = (GroupChat) currentChat;
                if (groupChat.getMemberIds().contains(userId)) {
                    refreshCurrentChat();
                }
            }
            
            // Refresh the chat list to update display names and profile pictures
            loadUserChats();
            
            logger.info("Profile update processed for user: {}", userId);
        });
    }
    
    // Utility methods
    private boolean isMessageForCurrentChat(Message message) {
        if (currentChat == null) return false;
        
        String currentChatId = getChatId(currentChat);
        String messageReceiverId = message.getReceiverId();
        
        // Direct match for chat ID
        if (currentChatId.equals(messageReceiverId)) {
            return true;
        }
        
        // For private chats, check if the message is between the two users in this chat
        if (currentChat instanceof PrivateChat) {
            User currentUser = userService.getCurrentUser();
            if (currentUser != null) {
                String currentUserId = currentUser.getUserId();
                String messageSenderId = message.getSenderId();
                
                // Check if this message is part of the private chat between current user and the other user
                if (currentChatId.contains(currentUserId) && 
                    (currentChatId.contains(messageSenderId) || messageReceiverId.contains(currentUserId))) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private boolean isMessageFromCurrentUser(Message message) {
        User currentUser = userService.getCurrentUser();
        return currentUser != null && currentUser.getUserId().equals(message.getSenderId());
    }
    
    private boolean isCurrentUser(String userId) {
        User currentUser = userService.getCurrentUser();
        return currentUser != null && currentUser.getUserId().equals(userId);
    }
    
    private String getSenderName(String senderId) {
        User sender = userService.findUserById(senderId);
        return sender != null ? sender.getProfileName() : "Unknown User";
    }
    
    private String getChatName(Object chat) {
        if (chat instanceof PrivateChat) {
            PrivateChat privateChat = (PrivateChat) chat;
            User currentUser = userService.getCurrentUser();
            String otherUserId = privateChat.getOtherUserId(currentUser.getUserId());
            User otherUser = userService.findUserById(otherUserId);
            return otherUser != null ? otherUser.getProfileName() : "Unknown User";
        } else if (chat instanceof GroupChat) {
            return ((GroupChat) chat).getGroupName();
        } else if (chat instanceof Channel) {
            return ((Channel) chat).getChannelName();
        }
        return "Unknown Chat";
    }
    
    private String getChatId(Object chat) {
        if (chat instanceof PrivateChat) {
            return ((PrivateChat) chat).getChatId();
        } else if (chat instanceof GroupChat) {
            return ((GroupChat) chat).getGroupId();
        } else if (chat instanceof Channel) {
            return ((Channel) chat).getChannelId();
        }
        return null;
    }
    
    private ImageView createStatusIcon(Message.MessageStatus status) {
        ImageView statusIcon = new ImageView();
        statusIcon.setFitHeight(12);
        statusIcon.setFitWidth(12);
        statusIcon.setPreserveRatio(true);
        
        switch (status) {
            case SENT:
                statusIcon.setImage(new javafx.scene.image.Image(
                    getClass().getResourceAsStream("/icons/check_24dp_E3E3E3_FILL0_wght400_GRAD0_opsz24.png")));
                break;
            case DELIVERED:
                statusIcon.setImage(new javafx.scene.image.Image(
                    getClass().getResourceAsStream("/icons/done_all_24dp_E3E3E3_FILL0_wght400_GRAD0_opsz24.png")));
                break;
            case READ:
                statusIcon.setImage(new javafx.scene.image.Image(
                    getClass().getResourceAsStream("/icons/done_all_24dp_E3E3E3_FILL0_wght400_GRAD0_opsz24.png")));
                // Add color adjustment for read status (blue tint)
                javafx.scene.effect.ColorAdjust colorAdjust = new javafx.scene.effect.ColorAdjust();
                colorAdjust.setHue(0.6); // Blue tint
                statusIcon.setEffect(colorAdjust);
                break;
            default:
                // For pending, we can use a simple circle or just return null
                return null;
        }
        
        return statusIcon;
    }
    
    private String getMessageStatusIcon(Message message) {
        switch (message.getStatus()) {
            case SENT:
                return "SENT";  // Single check - sent
            case DELIVERED:
                return "DELIVERED"; // Double check - delivered
            case READ:
                return "READ"; // Double check in blue/color (could be styled differently)
            default:
                return "PENDING";  // Pending/unsent
        }
    }
    
    private void updateChatSubtitle() {
        if (currentChat instanceof PrivateChat) {
            PrivateChat privateChat = (PrivateChat) currentChat;
            User currentUser = userService.getCurrentUser();
            String otherUserId = privateChat.getOtherUserId(currentUser.getUserId());
            boolean isOnline = messageBroker.isUserOnline(otherUserId);
            chatSubtitleLabel.setText(isOnline ? "Online" : "Last seen recently");
        } else if (currentChat instanceof GroupChat) {
            GroupChat groupChat = (GroupChat) currentChat;
            List<User> members = chatService.getGroupMembers(groupChat.getGroupId());
            int memberCount = members.size();
            chatSubtitleLabel.setText(memberCount + " members");
        } else if (currentChat instanceof Channel) {
            Channel channel = (Channel) currentChat;
            chatSubtitleLabel.setText("Channel • Public");
        }
    }
    
    private void scrollToBottom() {
        Platform.runLater(() -> {
            // Use a small delay to ensure UI elements are fully rendered before scrolling
            Timeline scrollTimeline = new Timeline(new KeyFrame(Duration.millis(50), e -> {
                messageScrollPane.setVvalue(1.0);
            }));
            scrollTimeline.play();
        });
    }
    
    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            
            // Improve styling (skip icon if not found)
            try {
                Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
                // Only add icon if available
            } catch (Exception e) {
                // Ignore icon errors
            }
            
            alert.showAndWait();
        });
    }
    
    private void showSuccessAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText("Success");
            alert.setContentText(message);
            
            // Add success styling
            alert.getDialogPane().setStyle("-fx-background-color: #e8f5e8; -fx-border-color: #4caf50;");
            
            alert.showAndWait();
        });
    }
    
    private void showErrorAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText("Error");
            alert.setContentText(message);
            
            // Add error styling
            alert.getDialogPane().setStyle("-fx-background-color: #ffeaea; -fx-border-color: #f44336;");
            
            alert.showAndWait();
        });
    }
    
    private void showConfirmationDialog(String title, String header, String content, Runnable onConfirm) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(title);
            alert.setHeaderText(header);
            alert.setContentText(content);
            
            // Add confirmation styling
            alert.getDialogPane().setStyle("-fx-background-color: #e3f2fd; -fx-border-color: #2196f3;");
            
            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                onConfirm.run();
            }
        });
    }
    
    private void showSearchResults(List<Message> messages, String title) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText("Found " + messages.size() + " message(s)");
        
        StringBuilder content = new StringBuilder();
        for (Message message : messages.stream().limit(10).toList()) {
            content.append(getSenderName(message.getSenderId()))
                   .append(": ")
                   .append(message.getContent())
                   .append("\n");
        }
        
        if (messages.size() > 10) {
            content.append("... and ").append(messages.size() - 10).append(" more");
        }
        
        alert.setContentText(content.toString());
        alert.showAndWait();
    }
    
    private void showMessageContextMenu(Message message, double screenX, double screenY) {
        ContextMenu contextMenu = new ContextMenu();
        
        MenuItem selectItem = new MenuItem("Select");
        selectItem.setOnAction(e -> {
            VBox messageBox = findMessageBox(message.getMessageId());
            if (messageBox != null) {
                toggleMessageSelection(message.getMessageId(), messageBox);
            }
        });
        
        MenuItem copyItem = new MenuItem("Copy");
        copyItem.setOnAction(e -> {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(message.getContent());
            clipboard.setContent(content);
        });
        
        if (isMessageFromCurrentUser(message)) {
            MenuItem deleteItem = new MenuItem("Delete");
            deleteItem.setOnAction(e -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Delete Message");
                alert.setContentText("Delete this message?");
                alert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        messageService.deleteMessage(message.getMessageId());
                        refreshCurrentChat();
                    }
                });
            });
            contextMenu.getItems().addAll(selectItem, copyItem, deleteItem);
        } else {
            contextMenu.getItems().addAll(selectItem, copyItem);
        }
        
        // Show message context menu with proper auto-hide behavior
        contextMenu.setAutoHide(true);
        contextMenu.show(mainPane, screenX, screenY);
    }
    
    private VBox findMessageBox(String messageId) {
        VBox messageBox = messageBoxMap.get(messageId);
        if (messageBox == null) {
            logger.warn("Message box not found for message ID: {}", messageId);
        }
        return messageBox;
    }
    
    private void showChatContextMenu(Object chat, double screenX, double screenY) {
        ContextMenu contextMenu = new ContextMenu();
        
        // Chat-specific menu items based on type
        if (chat instanceof PrivateChat) {
            // Private Chat Menu Items
            MenuItem viewProfileItem = new MenuItem("View Profile");
            viewProfileItem.setOnAction(e -> showUserProfile((PrivateChat) chat));
            
            MenuItem clearHistoryItem = new MenuItem("Clear Chat History");
            clearHistoryItem.setOnAction(e -> clearChatHistory(chat));
            
            MenuItem deleteItem = new MenuItem("Delete Chat");
            deleteItem.setOnAction(e -> deletePrivateChat(chat));
            
            contextMenu.getItems().addAll(viewProfileItem, new SeparatorMenuItem(), 
                clearHistoryItem, deleteItem);
                
        } else if (chat instanceof GroupChat) {
            // Group Chat Menu Items
            GroupChat groupChat = (GroupChat) chat;
            
            MenuItem groupInfoItem = new MenuItem("Group Info");
            groupInfoItem.setOnAction(e -> showGroupManagement(groupChat));
            
            MenuItem leaveItem = new MenuItem("Leave Group");
            leaveItem.setOnAction(e -> leaveGroup(groupChat));
            
            contextMenu.getItems().addAll(groupInfoItem, new SeparatorMenuItem(), leaveItem);
                
        } else if (chat instanceof Channel) {
            // Channel Menu Items
            Channel channel = (Channel) chat;
            
            MenuItem channelInfoItem = new MenuItem("Channel Info");
            channelInfoItem.setOnAction(e -> showChannelManagement(channel));
            
            MenuItem leaveItem = new MenuItem("Leave Channel");
            leaveItem.setOnAction(e -> leaveChannel(channel));
            
            contextMenu.getItems().addAll(channelInfoItem, new SeparatorMenuItem(), leaveItem);
        }
        
        // Show context menu with proper auto-hide behavior
        contextMenu.setAutoHide(true);
        contextMenu.show(mainPane, screenX, screenY);
        logger.info("Showing enhanced context menu for {}: {}", 
            chat.getClass().getSimpleName(), getChatDisplayName(chat));
    }
    
    // Helper methods for context menu actions
    private void showUserProfile(PrivateChat chat) {
        try {
            User currentUser = userService.getCurrentUser();
            String otherUserId = chat.getOtherUserId(currentUser.getUserId());
            User otherUser = userService.findUserById(otherUserId);
            
            if (otherUser != null) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("User Profile");
                alert.setHeaderText(otherUser.getProfileName());
                
                StringBuilder content = new StringBuilder();
                content.append("Username: ").append(otherUser.getUsername()).append("\n");
                content.append("Bio: ").append(otherUser.getBio() != null && !otherUser.getBio().isEmpty() ? 
                    otherUser.getBio() : "No bio").append("\n");
                content.append("Status: ").append(messageBroker.isUserOnline(otherUserId) ? "Online" : "Offline").append("\n");
                content.append("Member since: ").append(otherUser.getCreatedAt() != null ? 
                    otherUser.getCreatedAt().toLocalDate() : "Unknown");
                
                alert.setContentText(content.toString());
                alert.showAndWait();
            }
        } catch (Exception e) {
            logger.error("Error showing user profile", e);
            showErrorAlert("Error", "Failed to load user profile: " + e.getMessage());
        }
    }
    
    private void showGroupInfo(GroupChat group) {
        try {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Group Information");
            alert.setHeaderText(group.getGroupName());
            
            StringBuilder content = new StringBuilder();
            content.append("Group ID: ").append(group.getGroupId()).append("\n");
            content.append("Description: ").append(group.getDescription() != null ? group.getDescription() : "No description").append("\n");
            
            List<User> members = chatService.getGroupMembers(group.getGroupId());
            content.append("Members: ").append(members.size()).append("\n\n");
            content.append("Member list:\n");
            
            for (User member : members.stream().limit(10).toList()) {
                boolean isOnline = messageBroker.isUserOnline(member.getUserId());
                content.append("• ").append(member.getProfileName())
                       .append(isOnline ? " (Online)" : " (Offline)").append("\n");
            }
            
            if (members.size() > 10) {
                content.append("... and ").append(members.size() - 10).append(" more members");
            }
            
            alert.setContentText(content.toString());
            alert.showAndWait();
        } catch (Exception e) {
            logger.error("Error showing group info", e);
            showErrorAlert("Error", "Failed to load group information: " + e.getMessage());
        }
    }
    
    private void showChannelInfo(Channel channel) {
        try {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Channel Information");
            alert.setHeaderText(channel.getChannelName());
            
            StringBuilder content = new StringBuilder();
            content.append("Channel ID: ").append(channel.getChannelId()).append("\n");
            content.append("Description: ").append(channel.getDescription() != null ? channel.getDescription() : "No description").append("\n");
            content.append("Type: ").append(channel.isPrivate() ? "Private" : "Public").append("\n");
            content.append("Created: ").append(channel.getCreatedAt() != null ? 
                channel.getCreatedAt().toLocalDate() : "Unknown").append("\n");
            
            // TODO: Add subscriber count when available
            content.append("Subscribers: ").append("Feature coming soon");
            
            alert.setContentText(content.toString());
            alert.showAndWait();
        } catch (Exception e) {
            logger.error("Error showing channel info", e);
            showErrorAlert("Error", "Failed to load channel information: " + e.getMessage());
        }
    }
    
    private void manageGroupMembers(GroupChat group) {
        try {
            List<User> members = chatService.getGroupMembers(group.getGroupId());
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Manage Members");
            alert.setHeaderText("Group: " + group.getGroupName());
            
            StringBuilder content = new StringBuilder();
            content.append("Current Members (").append(members.size()).append("):\n\n");
            
            for (User member : members) {
                boolean isOnline = messageBroker.isUserOnline(member.getUserId());
                content.append("* ").append(member.getProfileName())
                       .append(" (@").append(member.getUsername()).append(")")
                       .append(isOnline ? " [Online]" : " [Offline]")
                       .append("\n");
            }
            
            content.append("\nNote: Advanced member management features (add/remove members, change permissions, promote to admin) will be implemented in a future update.");
            
            alert.setContentText(content.toString());
            alert.showAndWait();
            
            logger.info("Viewed members for group: {}", group.getGroupName());
        } catch (Exception e) {
            logger.error("Error loading group members", e);
            showErrorAlert("Error", "Failed to load group members: " + e.getMessage());
        }
    }
    
    private void viewChannelSubscribers(Channel channel) {
        try {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Channel Subscribers");
            alert.setHeaderText("Channel: " + channel.getChannelName());
            
            StringBuilder content = new StringBuilder();
            content.append("Subscriber Information:\n\n");
            
            // TODO: Get actual subscriber list from database
            // For now, show placeholder data
            content.append("📊 Subscription Stats:\n");
            content.append("• Total subscribers: Loading...\n");
            content.append("• Active subscribers: Loading...\n");
            content.append("* New subscribers today: Loading...\n\n");
            
            content.append("Recent Subscribers:\n");
            content.append("* Loading subscriber list...\n\n");
            
            content.append("Subscriber Management:\n");
            content.append("* View all subscribers\n");
            content.append("* Export subscriber list\n");
            content.append("* Subscriber analytics\n");
            content.append("* Manage banned users\n\n");
            
            content.append("Note: Full subscriber management will be implemented in a future update.");
            
            alert.setContentText(content.toString());
            alert.showAndWait();
            
            logger.info("Viewed subscribers for channel: {}", channel.getChannelName());
        } catch (Exception e) {
            logger.error("Error loading channel subscribers", e);
            showErrorAlert("Error", "Failed to load channel subscribers: " + e.getMessage());
        }
    }
    
    /**
     * Show advanced settings dialog for group management
     */
    private void showGroupSettings(GroupChat group) {
        try {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Group Settings");
            dialog.setHeaderText("Settings for " + group.getGroupName());
            
            // Create single consolidated settings view
            ScrollPane scrollPane = new ScrollPane();
            VBox mainContent = new VBox(20);
            mainContent.setPadding(new Insets(20));
            
            // === GENERAL SETTINGS ===
            Label generalLabel = new Label("General Settings");
            generalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
            
            // Group Name
            Label nameLabel = new Label("Group Name:");
            nameLabel.setStyle("-fx-font-weight: bold;");
            TextField nameField = new TextField(group.getGroupName());
            nameField.setPromptText("Enter group name");
            
            // Group Description
            Label descLabel = new Label("Description:");
            descLabel.setStyle("-fx-font-weight: bold;");
            TextArea descArea = new TextArea(group.getDescription() != null ? group.getDescription() : "");
            descArea.setPromptText("Enter group description (optional)");
            descArea.setPrefRowCount(3);
            descArea.setWrapText(true);
            
            Separator separator1 = new Separator();
            
            // === PRIVACY SETTINGS ===
            Label privacyLabel = new Label("🔒 Privacy Settings");
            privacyLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
            
            // Group Type
            Label typeLabel = new Label("Group Type:");
            typeLabel.setStyle("-fx-font-weight: bold;");
            RadioButton publicGroup = new RadioButton("Public Group - Can be found in search");
            RadioButton privateGroup = new RadioButton("Private Group - Invitation only");
            ToggleGroup typeGroup = new ToggleGroup();
            publicGroup.setToggleGroup(typeGroup);
            privateGroup.setToggleGroup(typeGroup);
            privateGroup.setSelected(true); // Default
            
            // Join Settings
            Label joinLabel = new Label("Who can add members:");
            joinLabel.setStyle("-fx-font-weight: bold;");
            ComboBox<String> joinCombo = new ComboBox<>();
            joinCombo.getItems().addAll("Only Admins", "All Members");
            joinCombo.setValue("Only Admins");
            
            Separator separator2 = new Separator();
            
            // === PERMISSIONS ===
            Label permissionsLabel = new Label("Member Permissions");
            permissionsLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
            
            CheckBox sendMessages = new CheckBox("Send Messages");
            sendMessages.setSelected(true);
            CheckBox sendMedia = new CheckBox("Send Media Files");
            sendMedia.setSelected(true);
            CheckBox addMembers = new CheckBox("Add New Members");
            addMembers.setSelected(false);
            
            mainContent.getChildren().addAll(
                generalLabel,
                nameLabel, nameField,
                descLabel, descArea,
                separator1,
                privacyLabel,
                typeLabel, publicGroup, privateGroup,
                joinLabel, joinCombo,
                separator2,
                permissionsLabel,
                sendMessages, sendMedia, addMembers
            );
            
            scrollPane.setContent(mainContent);
            scrollPane.setFitToWidth(true);
            scrollPane.setPrefHeight(500);
            
            dialog.getDialogPane().setContent(scrollPane);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            dialog.getDialogPane().setPrefSize(500, 600);
            
            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Apply settings changes
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Settings Updated");
                alert.setHeaderText(null);
                alert.setContentText("Group settings have been updated successfully!");
                alert.showAndWait();
                
                logger.info("Group settings updated for: {} by user {}", 
                    group.getGroupName(), userService.getCurrentUser().getUsername());
            }
            
        } catch (Exception e) {
            logger.error("Error showing group settings", e);
            showAlert("Error", "Failed to load group settings: " + e.getMessage());
        }
    }
    
    /**
     * Show consolidated settings dialog for channel management
     */
    private void showChannelSettings(Channel channel) {
        try {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Channel Settings");
            dialog.setHeaderText("Settings for " + channel.getChannelName());
            
            // Create single consolidated settings view
            ScrollPane scrollPane = new ScrollPane();
            VBox mainContent = new VBox(20);
            mainContent.setPadding(new Insets(20));
            
            // === GENERAL SETTINGS ===
            Label generalLabel = new Label("📢 General Settings");
            generalLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
            
            // Channel Name
            Label nameLabel = new Label("Channel Name:");
            nameLabel.setStyle("-fx-font-weight: bold;");
            TextField nameField = new TextField(channel.getChannelName());
            nameField.setPromptText("Enter channel name");
            
            // Channel Description
            Label descLabel = new Label("Description:");
            descLabel.setStyle("-fx-font-weight: bold;");
            TextArea descArea = new TextArea(channel.getDescription() != null ? channel.getDescription() : "");
            descArea.setPromptText("Enter channel description (optional)");
            descArea.setPrefRowCount(3);
            descArea.setWrapText(true);
            
            Separator separator1 = new Separator();
            
            // === PRIVACY & TYPE SETTINGS ===
            Label privacyLabel = new Label("🔒 Privacy & Type");
            privacyLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
            
            // Channel Type
            Label typeLabel = new Label("Channel Type:");
            typeLabel.setStyle("-fx-font-weight: bold;");
            RadioButton publicChannel = new RadioButton("Public Channel - Can be found in search");
            RadioButton privateChannel = new RadioButton("Private Channel - Invitation only");
            ToggleGroup typeGroup = new ToggleGroup();
            publicChannel.setToggleGroup(typeGroup);
            privateChannel.setToggleGroup(typeGroup);
            privateChannel.setSelected(true); // Default
            
            // Username (for public channels)
            Label usernameLabel = new Label("Channel Username (for public channels):");
            usernameLabel.setStyle("-fx-font-weight: bold;");
            TextField usernameField = new TextField();
            usernameField.setPromptText("@channelname");
            usernameField.setDisable(true); // Disabled by default for private
            
            // Update username field based on channel type
            publicChannel.setOnAction(e -> usernameField.setDisable(false));
            privateChannel.setOnAction(e -> usernameField.setDisable(true));
            
            Separator separator2 = new Separator();
            
            // === CONTENT SETTINGS ===
            Label contentLabel = new Label("Content Settings");
            contentLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
            
            CheckBox signMessages = new CheckBox("Sign messages with author name");
            signMessages.setSelected(false);
            CheckBox restrictSaving = new CheckBox("Restrict saving content");
            restrictSaving.setSelected(false);
            CheckBox commentsEnabled = new CheckBox("Enable comments");
            commentsEnabled.setSelected(false);
            
            mainContent.getChildren().addAll(
                generalLabel,
                nameLabel, nameField,
                descLabel, descArea,
                separator1,
                privacyLabel,
                typeLabel, publicChannel, privateChannel,
                usernameLabel, usernameField,
                separator2,
                contentLabel,
                signMessages, restrictSaving, commentsEnabled
            );
            
            scrollPane.setContent(mainContent);
            scrollPane.setFitToWidth(true);
            scrollPane.setPrefHeight(500);
            
            dialog.getDialogPane().setContent(scrollPane);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            dialog.getDialogPane().setPrefSize(500, 600);
            
            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                // Apply settings changes
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Settings Updated");
                alert.setHeaderText(null);
                alert.setContentText("Channel settings have been updated successfully!");
                alert.showAndWait();
                
                logger.info("Channel settings updated for: {} by user {}", 
                    channel.getChannelName(), userService.getCurrentUser().getUsername());
            }
            
        } catch (Exception e) {
            logger.error("Error showing channel settings", e);
            showAlert("Error", "Failed to load channel settings: " + e.getMessage());
        }
    }
    
    private void clearChatHistory(Object chat) {
        // Create a detailed confirmation dialog
        Alert confirmAlert = new Alert(Alert.AlertType.WARNING);
        confirmAlert.setTitle("Clear Chat History");
        confirmAlert.setHeaderText("WARNING: This action cannot be undone");
        confirmAlert.setContentText("Are you sure you want to clear all message history with " + 
            getChatDisplayName(chat) + "?\n\n" +
            "This will:\n" +
            "* Delete all messages from your device\n" +
            "* Remove shared media and files\n" +
            "* Clear search history for this chat\n\n" +
            "Other participants will still see their copy of the conversation.");
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    String chatId = getChatId(chat);
                    User currentUser = userService.getCurrentUser();
                    boolean deleteSuccess = false;
                    
                    // Delete messages from database based on chat type
                    if (chat instanceof PrivateChat) {
                        PrivateChat privateChat = (PrivateChat) chat;
                        String otherUserId = privateChat.getOtherUserId(currentUser.getUserId());
                        deleteSuccess = messageService.clearPrivateChatHistory(currentUser.getUserId(), otherUserId);
                    } else if (chat instanceof GroupChat) {
                        GroupChat groupChat = (GroupChat) chat;
                        deleteSuccess = messageService.clearGroupChatHistory(groupChat.getGroupId());
                    } else if (chat instanceof Channel) {
                        Channel channel = (Channel) chat;
                        deleteSuccess = messageService.clearChannelHistory(channel.getChannelId());
                    }
                    
                    // Clear current UI if this is the active chat
                    if (currentChat == chat) {
                        messageContainer.getChildren().clear();
                        messageBoxMap.clear(); // Clear message box mapping
                    }
                    
                    // Show success/error message
                    Alert alert = new Alert(deleteSuccess ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING);
                    alert.setTitle(deleteSuccess ? "History Cleared" : "Partial Success");
                    alert.setHeaderText(null);
                    
                    if (deleteSuccess) {
                        alert.setContentText("Chat history with " + getChatDisplayName(chat) + " has been successfully cleared from the database.\n\n" +
                            "All messages have been permanently deleted.");
                    } else {
                        alert.setContentText("WARNING: Chat history with " + getChatDisplayName(chat) + " has been cleared from the interface, but there may have been an issue with database deletion.\n\n" +
                            "Please check the logs for more details.");
                    }
                    alert.showAndWait();
                    
                    logger.info("Cleared history for chat: {} by user {} - Database deletion: {}", 
                        getChatDisplayName(chat), currentUser.getUsername(), deleteSuccess ? "successful" : "failed");
                        
                } catch (Exception e) {
                    logger.error("Error clearing chat history", e);
                    showErrorAlert("Error", "Failed to clear chat history: " + e.getMessage());
                }
            }
        });
    }
    
    private void deletePrivateChat(Object chat) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete Chat");
        confirmAlert.setHeaderText("What would you like to delete?");
        confirmAlert.setContentText("Choose how you want to delete this chat with " + getChatDisplayName(chat) + ":");
        
        // Custom buttons for different deletion options
        ButtonType deleteChatOnly = new ButtonType("Remove Chat Only", ButtonBar.ButtonData.OTHER);
        ButtonType deleteChatAndMessages = new ButtonType("Delete Chat + All Messages", ButtonBar.ButtonData.OTHER);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        confirmAlert.getButtonTypes().setAll(deleteChatOnly, deleteChatAndMessages, cancel);
        
        // Add detailed explanation
        Label explanation = new Label(
            "* Remove Chat Only: Removes chat from your list but keeps messages in database\n" +
            "* Delete Chat + All Messages: Permanently deletes all messages and removes chat\n\n" +
            "WARNING: Message deletion cannot be undone!"
        );
        explanation.setWrapText(true);
        explanation.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        confirmAlert.getDialogPane().setExpandableContent(explanation);
        confirmAlert.getDialogPane().setExpanded(true);
        
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == cancel) {
                return; // User cancelled
            }
            
            try {
                boolean deleteMessages = (response == deleteChatAndMessages);
                boolean dbSuccess = true;
                
                // Remove from UI first
                chatItems.remove(chat);
                if (currentChat == chat) {
                    currentChat = null;
                    messageContainer.getChildren().clear();
                    messageBoxMap.clear();
                    chatTitleLabel.setText("Select a chat");
                    chatSubtitleLabel.setText("");
                }
                
                // Delete from database if requested
                if (deleteMessages && chat instanceof PrivateChat) {
                    PrivateChat privateChat = (PrivateChat) chat;
                    User currentUser = userService.getCurrentUser();
                    String otherUserId = privateChat.getOtherUserId(currentUser.getUserId());
                    
                    // Delete all messages first
                    boolean messagesDeleted = messageService.clearPrivateChatHistory(currentUser.getUserId(), otherUserId);
                    
                    // Then delete the chat itself
                    boolean chatDeleted = chatService.deletePrivateChat(privateChat.getChatId());
                    
                    dbSuccess = messagesDeleted && chatDeleted;
                    
                    if (!dbSuccess) {
                        logger.warn("Database deletion partially failed - Messages: {}, Chat: {}", messagesDeleted, chatDeleted);
                    }
                }
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Chat Deleted");
                alert.setHeaderText(null);
                
                if (deleteMessages) {
                    alert.setContentText(dbSuccess ? 
                        "Chat and all messages with " + getChatDisplayName(chat) + " have been permanently deleted." :
                        "WARNING: Chat removed from list, but there may have been issues with database deletion. Check logs for details.");
                } else {
                    alert.setContentText("Chat with " + getChatDisplayName(chat) + " has been removed from your list.\n\n" +
                        "Messages remain in the database and chat can be restored by sending a new message.");
                }
                
                alert.showAndWait();
                logger.info("Deleted private chat: {} - Delete messages: {} - DB success: {}", 
                    getChatDisplayName(chat), deleteMessages, dbSuccess);
                    
            } catch (Exception e) {
                logger.error("Error deleting chat", e);
                showErrorAlert("Error", "Failed to delete chat: " + e.getMessage());
            }
        });
    }
    
    private void leaveGroup(GroupChat group) {
        try {
            Alert confirmAlert = new Alert(Alert.AlertType.WARNING);
            confirmAlert.setTitle("Leave Group");
            confirmAlert.setHeaderText("Leave " + group.getGroupName() + "?");
            confirmAlert.setContentText("Are you sure you want to leave this group?\n\n" +
                "This will:\n" +
                "• Remove you from the group\n" +
                "• Stop receiving new messages\n" +
                "• Remove the group from your chat list\n" +
                "• Notify other members that you left\n\n" +
                "You can be re-added by any admin later.");
            
            confirmAlert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        // TODO: Implement actual group leave functionality
                        // Remove from chat list immediately for UI feedback
                        chatItems.remove(group);
                        if (currentChat == group) {
                            currentChat = null;
                            messageContainer.getChildren().clear();
                            chatTitleLabel.setText("Select a chat");
                            chatSubtitleLabel.setText("");
                        }
                        
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Left Group");
                        alert.setHeaderText(null);
                        alert.setContentText("You have left " + group.getGroupName() + ".\n\n" +
                            "The group has been removed from your chat list.\n\n" +
                            "Note: Backend group membership update will be implemented in a future update.");
                        alert.showAndWait();
                        
                        logger.info("Left group: {} by user {}", group.getGroupName(), 
                            userService.getCurrentUser().getUsername());
                    } catch (Exception e) {
                        logger.error("Error leaving group", e);
                        showErrorAlert("Error", "Failed to leave group: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            logger.error("Error in leave group dialog", e);
            showErrorAlert("Error", "Failed to show leave group dialog: " + e.getMessage());
        }
    }
    
    private void leaveChannel(Channel channel) {
        try {
            Alert confirmAlert = new Alert(Alert.AlertType.WARNING);
            confirmAlert.setTitle("Leave Channel");
            confirmAlert.setHeaderText("Leave " + channel.getChannelName() + "?");
            confirmAlert.setContentText("Are you sure you want to leave this channel?\n\n" +
                "This will:\n" +
                "• Unsubscribe you from the channel\n" +
                "• Stop receiving new posts\n" +
                "• Remove the channel from your chat list\n" +
                "• Remove access to channel history\n\n" +
                (channel.isPrivate() ? 
                    "This is a private channel - you'll need a new invite link to rejoin." :
                    "You can rejoin this public channel anytime using the channel link."));
            
            confirmAlert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        // TODO: Implement actual channel unsubscribe functionality
                        // Remove from chat list immediately for UI feedback
                        chatItems.remove(channel);
                        if (currentChat == channel) {
                            currentChat = null;
                            messageContainer.getChildren().clear();
                            chatTitleLabel.setText("Select a chat");
                            chatSubtitleLabel.setText("");
                        }
                        
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Left Channel");
                        alert.setHeaderText(null);
                        alert.setContentText("You have left " + channel.getChannelName() + ".\n\n" +
                            "The channel has been removed from your chat list.\n\n" +
                            "Note: Backend channel subscription update will be implemented in a future update.");
                        alert.showAndWait();
                        
                        logger.info("Left channel: {} by user {}", channel.getChannelName(), 
                            userService.getCurrentUser().getUsername());
                    } catch (Exception e) {
                        logger.error("Error leaving channel", e);
                        showErrorAlert("Error", "Failed to leave channel: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            logger.error("Error in leave channel dialog", e);
            showErrorAlert("Error", "Failed to show leave channel dialog: " + e.getMessage());
        }
    }
    
    // Unified Management Methods
    private void showGroupManagement(GroupChat group) {
        try {
            // Check if user has permission to manage the group
            User currentUser = userService.getCurrentUser();
            if (currentUser == null) {
                showAlert("Error", "User not authenticated");
                return;
            }
            
            // Check user role in group
            boolean isOwner = group.getCreatorId().equals(currentUser.getUserId());
            boolean isAdmin = group.getAdminIds().contains(currentUser.getUserId());
            boolean canManageGroup = isOwner || isAdmin;
            
            if (canManageGroup) {
                showGroupAdminView(group);
            } else {
                showGroupMemberView(group);
            }
            
            logger.info("Opened group {} view for user: {} (admin: {})", 
                group.getGroupName(), currentUser.getUsername(), canManageGroup);
            
        } catch (Exception e) {
            logger.error("Error showing group management", e);
            showErrorAlert("Error", "Failed to open group management: " + e.getMessage());
        }
    }    
    private void showGroupAdminView(GroupChat group) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Group Management - " + group.getGroupName());
        dialog.setWidth(700);
        dialog.setHeight(600);
        
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // Group Info Tab
        Tab infoTab = new Tab("Group Info");
        ScrollPane infoScrollPane = new ScrollPane();
        infoScrollPane.setFitToWidth(true);
        VBox infoContent = createGroupInfoContent(group);
        infoScrollPane.setContent(infoContent);
        infoTab.setContent(infoScrollPane);
        
        // Members Management Tab
        Tab membersTab = new Tab("Manage Members");
        ScrollPane membersScrollPane = new ScrollPane();
        membersScrollPane.setFitToWidth(true);
        VBox membersContent = createGroupMembersContent(group);
        membersScrollPane.setContent(membersContent);
        membersTab.setContent(membersScrollPane);
        
        tabPane.getTabs().addAll(infoTab, membersTab);
        
        Scene scene = new Scene(tabPane);
        dialog.setScene(scene);
        dialog.show();
    }
    
    private void showGroupMemberView(GroupChat group) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(group.getGroupName());
        dialog.setWidth(600);
        dialog.setHeight(500);
        
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // Group Info Tab (simplified for members)
        Tab infoTab = new Tab("About");
        VBox infoContent = createMemberGroupInfoContent(group);
        ScrollPane infoScrollPane = new ScrollPane(infoContent);
        infoScrollPane.setFitToWidth(true);
        infoTab.setContent(infoScrollPane);
        
        tabPane.getTabs().add(infoTab);
        
        Scene scene = new Scene(tabPane);
        dialog.setScene(scene);
        dialog.show();
    }

    private void showChannelManagement(Channel channel) {
        try {
            // Check user permissions for channel
            User currentUser = userService.getCurrentUser();
            boolean isOwner = channel.getOwnerId().equals(currentUser.getUserId());
            boolean isAdmin = channel.getAdminIds().contains(currentUser.getUserId());
            boolean canManageChannel = isOwner || isAdmin;
            
            if (canManageChannel) {
                showChannelAdminView(channel);
            } else {
                showChannelSubscriberView(channel);
            }
            
            logger.info("Opened channel {} view for user: {} (admin: {})", 
                channel.getChannelName(), currentUser.getUsername(), canManageChannel);
        } catch (Exception e) {
            logger.error("Error showing channel management", e);
            showErrorAlert("Error", "Failed to open channel management: " + e.getMessage());
        }
    }
    
    private void showChannelAdminView(Channel channel) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Channel Management - " + channel.getChannelName());
        dialog.setWidth(700);
        dialog.setHeight(600);
        
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // Channel Info Tab
        Tab infoTab = new Tab("Channel Info");
        ScrollPane infoScrollPane = new ScrollPane();
        infoScrollPane.setFitToWidth(true);
        VBox infoContent = createChannelInfoContent(channel);
        infoScrollPane.setContent(infoContent);
        infoTab.setContent(infoScrollPane);
        
        // Subscribers Management Tab
        Tab subscribersTab = new Tab("Manage Subscribers");
        ScrollPane subscribersScrollPane = new ScrollPane();
        subscribersScrollPane.setFitToWidth(true);
        VBox subscribersContent = createChannelSubscribersContent(channel);
        subscribersScrollPane.setContent(subscribersContent);
        subscribersTab.setContent(subscribersScrollPane);
        
        tabPane.getTabs().addAll(infoTab, subscribersTab);
        
        Scene scene = new Scene(tabPane);
        dialog.setScene(scene);
        dialog.show();
    }
    
    private void showChannelSubscriberView(Channel channel) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(channel.getChannelName());
        dialog.setWidth(600);
        dialog.setHeight(500);
        
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        
        // Channel Info Tab (simplified for subscribers)
        Tab infoTab = new Tab("About");
        VBox infoContent = createSubscriberChannelInfoContent(channel);
        ScrollPane infoScrollPane = new ScrollPane(infoContent);
        infoScrollPane.setFitToWidth(true);
        infoTab.setContent(infoScrollPane);
        
        tabPane.getTabs().add(infoTab);
        
        Scene scene = new Scene(tabPane);
        dialog.setScene(scene);
        dialog.show();
    }
    
    // Enhanced search helper methods
    
    /**
     * Get chat display name by chat ID
     */
    private String getChatDisplayNameById(String chatId) {
        Object chat = findChatById(chatId);
        return chat != null ? getChatDisplayName(chat) : "Unknown Chat";
    }
    
    /**
     * Find chat object by ID
     */
    private Object findChatById(String chatId) {
        try {
            User currentUser = userService.getCurrentUser();
            
            // Search in private chats
            List<PrivateChat> privateChats = chatService.getUserPrivateChats(currentUser.getUserId());
            for (PrivateChat chat : privateChats) {
                if (chat.getChatId().equals(chatId)) {
                    return chat;
                }
            }
            
            // Search in group chats
            List<GroupChat> groupChats = chatService.getAllUserGroups(currentUser.getUserId());
            for (GroupChat group : groupChats) {
                if (group.getGroupId().equals(chatId)) {
                    return group;
                }
            }
            
            // Search in channels
            List<Channel> channels = channelService.getAllUserChannels(currentUser.getUserId());
            for (Channel channel : channels) {
                if (channel.getChannelId().equals(chatId)) {
                    return channel;
                }
            }
            
        } catch (Exception e) {
            logger.error("Error finding chat by ID: " + chatId, e);
        }
        return null;
    }
    
    /**
     * Load default profile picture for users
     */
    private Image loadDefaultProfilePicture() {
        try {
            return new Image(getClass().getResourceAsStream("/icons/user-default.png"));
        } catch (Exception e) {
            return createDefaultColoredImage(Color.LIGHTBLUE);
        }
    }
    
    /**
     * Load default group picture
     */
    private Image loadDefaultGroupPicture() {
        try {
            return new Image(getClass().getResourceAsStream("/icons/group-default.png"));
        } catch (Exception e) {
            return createDefaultColoredImage(Color.LIGHTGREEN);
        }
    }
    
    /**
     * Load default channel picture
     */
    private Image loadDefaultChannelPicture() {
        try {
            return new Image(getClass().getResourceAsStream("/icons/channel-default.png"));
        } catch (Exception e) {
            return createDefaultColoredImage(Color.ORANGE);
        }
    }
    
    /**
     * Load default message picture
     */
    private Image loadDefaultMessagePicture() {
        try {
            return new Image(getClass().getResourceAsStream("/icons/message-default.png"));
        } catch (Exception e) {
            return createDefaultColoredImage(Color.LIGHTGRAY);
        }
    }
    
    /**
     * Create a default colored image
     */
    private Image createDefaultColoredImage(Color color) {
        WritableImage image = new WritableImage(24, 24);
        for (int x = 0; x < 24; x++) {
            for (int y = 0; y < 24; y++) {
                image.getPixelWriter().setColor(x, y, color);
            }
        }
        return image;
    }


    // Helper method to get chat display name
    private String getChatDisplayName(Object chat) {
        if (chat instanceof PrivateChat) {
            PrivateChat privateChat = (PrivateChat) chat;
            User currentUser = userService.getCurrentUser();
            String otherUserId = privateChat.getOtherUserId(currentUser.getUserId());
            User otherUser = userService.findUserById(otherUserId);
            return otherUser != null ? otherUser.getProfileName() : "Unknown User";
        } else if (chat instanceof GroupChat) {
            return ((GroupChat) chat).getGroupName();
        } else if (chat instanceof Channel) {
            return ((Channel) chat).getChannelName();
        }
        return "Unknown Chat";
    }

    @FXML
    public void handleSendMessage() {
        String messageText = messageInputField.getText().trim();
        if (messageText.isEmpty()) {
            messageInputField.requestFocus();
            return;
        }
        
        if (currentChat == null) {
            showAlert("No Chat Selected", "Please select a chat before sending a message.");
            return;
        }
        
        try {
            User currentUser = userService.getCurrentUser();
            if (currentUser == null) {
                showAlert("Authentication Error", "Please log in again.");
                return;
            }
            
            // Check channel posting permissions
            if (currentChat instanceof Channel) {
                Channel channel = (Channel) currentChat;
                boolean isOwner = channel.getOwnerId().equals(currentUser.getUserId());
                boolean isAdmin = channel.getAdminIds().contains(currentUser.getUserId());
                
                if (!isOwner && !isAdmin) {
                    showAlert("Permission Denied", "Only channel admins can post messages to this channel.");
                    return;
                }
            }
            
            String receiverId = getChatId(currentChat);
            if (receiverId == null) {
                showAlert("Error", "Invalid chat selected. Please try selecting the chat again.");
                return;
            }
            
            // Validate message length (reasonable limit)
            if (messageText.length() > 4000) {
                showAlert("Message Too Long", "Message is too long. Please keep it under 4000 characters.");
                return;
            }
            
            Message message = new Message(
                currentUser.getUserId(),
                receiverId,
                messageText,
                Message.MessageType.TEXT
            );
            
            boolean sent = messageService.sendMessage(message);
            if (sent) {
                // Add to UI immediately and broadcast to other clients
                addMessageToUI(message);
                messageBroker.broadcastMessage(message);
                
                // Clear the input field and scroll to bottom
                messageInputField.clear();
                scrollToBottom();
                
                // Stop typing indicator since message was sent
                stopTyping();
            } else {
                showAlert("Send Failed", "Failed to send message. Please try again.");
            }
            
        } catch (Exception e) {
            logger.error("Error sending message", e);
            showAlert("Send Error", "An error occurred while sending the message: " + e.getMessage());
        }
    }

    @FXML
    public void handleSearchInCurrentChat() {
        if (currentChat == null) {
            showAlert("No Chat Selected", "Please select a chat to search in.");
            return;
        }
        
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Search in Chat");
        dialog.setHeaderText("Search messages in " + getChatDisplayName(currentChat));
        dialog.setContentText("Enter search term:");
        dialog.getEditor().setPromptText("Type your search query...");
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(searchTerm -> {
            String trimmedTerm = searchTerm.trim();
            if (!trimmedTerm.isEmpty()) {
                if (trimmedTerm.length() < 2) {
                    showAlert("Search Term Too Short", "Please enter at least 2 characters to search.");
                    return;
                }
                searchMessagesInCurrentChat(trimmedTerm);
            }
        });
    }

    @FXML
    public void handleAttachFile() {
        if (currentChat == null) {
            showAlert("No Chat Selected", "Please select a chat before attaching files.");
            return;
        }
        
        // Check if user has permission to send attachments in channels
        if (currentChat instanceof Channel) {
            Channel channel = (Channel) currentChat;
            User currentUser = userService.getCurrentUser();
            
            if (currentUser != null) {
                boolean isOwner = channel.getOwnerId().equals(currentUser.getUserId());
                boolean isAdmin = channel.getAdminIds().contains(currentUser.getUserId());
                boolean canPost = isOwner || isAdmin;
                
                if (!canPost) {
                    showErrorAlert("Permission Denied", "Only admins can send files to this channel.");
                    return;
                }
            }
        }
        
        FileChooser fileChooser = MediaHandler.createFileChooser("Select File to Send");
        File file = fileChooser.showOpenDialog(mainPane.getScene().getWindow());
        
        if (file != null) {
            try {
                // Check if file exists and is readable
                if (!file.exists() || !file.canRead()) {
                    showAlert("File Access Error", "Cannot read the selected file. Please choose another file.");
                    return;
                }
                
                // Check file size (limit to 50MB for demo)
                long maxSize = 50 * 1024 * 1024; // 50MB
                if (file.length() > maxSize) {
                    showAlert("File Too Large", 
                        "File size (" + MediaHandler.formatFileSize(file.length()) + 
                        ") exceeds the maximum allowed size of " + MediaHandler.formatFileSize(maxSize) + 
                        ".\n\nPlease choose a smaller file.");
                    return;
                }
                
                // Check for empty files
                if (file.length() == 0) {
                    showAlert("Empty File", "The selected file is empty. Please choose a different file.");
                    return;
                }
                
                // Simple confirmation dialog for all files
                handleFileAttachment(file);
                
            } catch (Exception e) {
                logger.error("Error handling file attachment", e);
                showAlert("Attachment Error", "Failed to process file attachment: " + e.getMessage());
            }
        }
    }

    /**
     * Handle file attachment with confirmation
     */
    private void handleFileAttachment(File file) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Send File");
        confirmDialog.setHeaderText("Send this file?");
        confirmDialog.setContentText("File: " + file.getName() + "\nSize: " + MediaHandler.formatFileSize(file.length()));

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            processFileUpload(file);
        }
    }

    /**
     * Process the actual file upload and sending
     */
    private void processFileUpload(File file) {
        try {
            // Get user preferences for compression
            boolean compressImages = UserPreferences.getInstance().isCompressImages();
            
            // Create upload task
            Task<String> uploadTask = MediaHandler.uploadFile(file, compressImages);
            
            // Show upload dialog
            Dialog<String> uploadDialog = MediaHandler.showUploadDialog(uploadTask, 
                (Stage) mainPane.getScene().getWindow());
            
            // Handle upload completion
            uploadDialog.setOnHidden(e -> {
                String uploadedPath = uploadDialog.getResult();
                if (uploadedPath != null) {
                    sendMediaMessage(file, uploadedPath);
                }
            });
            
            uploadDialog.show();
            
        } catch (Exception e) {
            logger.error("Error processing file upload", e);
            showAlert("Upload Error", "Failed to process file upload: " + e.getMessage());
        }
    }
    
    /**
     * Send media message after successful upload
     */
    private void sendMediaMessage(File originalFile, String uploadedPath) {
        try {
            User currentUser = userService.getCurrentUser();
            String receiverId = getChatId(currentChat);
            
            // Determine message type based on file extension
            String extension = MediaHandler.getFileExtension(originalFile.getName());
            Message.MessageType messageType;
            
            if (MediaHandler.isImageFile(extension)) {
                messageType = Message.MessageType.IMAGE;
            } else if (MediaHandler.isVideoFile(extension)) {
                messageType = Message.MessageType.VIDEO;
            } else if (MediaHandler.isAudioFile(extension)) {
                messageType = Message.MessageType.AUDIO;
            } else {
                messageType = Message.MessageType.FILE;
            }
            
            // Create message with media
            Message message = new Message(
                currentUser.getUserId(),
                receiverId,
                originalFile.getName() + " (" + MediaHandler.formatFileSize(originalFile.length()) + ")",
                messageType
            );
            message.setMediaPath(uploadedPath);
            
            // Send message to database, add to UI, and broadcast to other clients  
            messageService.sendMessage(message);
            addMessageToUI(message);
            messageBroker.broadcastMessage(message);
            scrollToBottom();
            
            logger.info("Media message sent: {} ({})", originalFile.getName(), messageType);
            
        } catch (Exception e) {
            logger.error("Error sending media message", e);
            showAlert("Send Error", "Failed to send media message: " + e.getMessage());
        }
    }

    @FXML
    public void handleNewPrivateChat() {
        // Simple dialog for now
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("New Private Chat");
        alert.setContentText("New private chat functionality will be implemented soon.");
        alert.showAndWait();
    }

    @FXML 
    public void handleNewGroup() {
        try {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Create New Group");
            dialog.setHeaderText("Create a new group chat");
            dialog.setContentText("Enter group name:");
            
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent() && !result.get().trim().isEmpty()) {
                String groupName = result.get().trim();
                
                // Get description
                TextInputDialog descDialog = new TextInputDialog();
                descDialog.setTitle("Group Description");
                descDialog.setHeaderText("Add a description for " + groupName);
                descDialog.setContentText("Enter description (optional):");
                
                String description = descDialog.showAndWait().orElse("").trim();
                
                // Create group
                User currentUser = userService.getCurrentUser();
                if (currentUser != null) {
                    GroupChat group = chatService.createGroup(groupName, description, currentUser.getUserId());
                    if (group != null) {
                        // Refresh chat list
                        loadUserChats();
                        
                        // Select the new group
                        chatListView.getSelectionModel().select(group);
                        selectChat(group);
                        
                        showSuccessAlert("Group Created", "Group '" + groupName + "' created successfully!");
                        logger.info("Created new group: {} by user: {}", groupName, currentUser.getUsername());
                    } else {
                        showAlert("Error", "Failed to create group. Please try again.");
                    }
                } else {
                    showAlert("Error", "User not authenticated.");
                }
            }
        } catch (Exception e) {
            logger.error("Error creating new group", e);
            showAlert("Error", "Failed to create group: " + e.getMessage());
        }
    }

    @FXML
    public void handleNewChannel() {
        try {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Create New Channel");
            dialog.setHeaderText("Create a new public channel");
            dialog.setContentText("Enter channel name:");
            
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent() && !result.get().trim().isEmpty()) {
                String channelName = result.get().trim();
                
                // Get description
                TextInputDialog descDialog = new TextInputDialog();
                descDialog.setTitle("Channel Description");
                descDialog.setHeaderText("Add a description for " + channelName);
                descDialog.setContentText("Enter description (optional):");
                
                String description = descDialog.showAndWait().orElse("").trim();
                
                // Create channel
                User currentUser = userService.getCurrentUser();
                if (currentUser != null) {
                    Channel channel = new Channel(channelName, currentUser.getUserId());
                    channel.setDescription(description);
                    
                    boolean success = channelService.createChannel(channel);
                    if (success) {
                        // Refresh chat list
                        loadUserChats();
                        
                        // Select the new channel
                        chatListView.getSelectionModel().select(channel);
                        selectChat(channel);
                        
                        showSuccessAlert("Channel Created", "Channel '" + channelName + "' created successfully!");
                        logger.info("Created new channel: {} by user: {}", channelName, currentUser.getUsername());
                    } else {
                        showAlert("Error", "Failed to create channel. Please try again.");
                    }
                } else {
                    showAlert("Error", "User not authenticated.");
                }
            }
        } catch (Exception e) {
            logger.error("Error creating new channel", e);
            showAlert("Error", "Failed to create channel: " + e.getMessage());
        }
    }

    private String formatTimestamp(LocalDateTime timestamp) {
        if (timestamp == null) return "";
        
        LocalDateTime now = LocalDateTime.now();
        if (timestamp.toLocalDate().equals(now.toLocalDate())) {
            return timestamp.format(DateTimeFormatter.ofPattern("HH:mm"));
        } else if (timestamp.toLocalDate().equals(now.toLocalDate().minusDays(1))) {
            return "Yesterday " + timestamp.format(DateTimeFormatter.ofPattern("HH:mm"));
        } else {
            return timestamp.format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"));
        }
    }

    // Helper methods for file handling
    private String extractFileName(String path) {
        if (path == null) return "unknown";
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private String getFileIcon(String fileName) {
        if (fileName == null) return "[FILE]";
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        switch (ext) {
            case "jpg", "jpeg", "png", "gif", "bmp": return "[IMG]";
            case "mp4", "avi", "mov", "mkv": return "[VID]";
            case "mp3", "wav", "flac", "aac": return "[AUD]";
            case "pdf": return "[PDF]";
            case "doc", "docx": return "[DOC]";
            case "xls", "xlsx": return "[XLS]";
            case "ppt", "pptx": return "[PPT]";
            case "zip", "rar", "7z": return "[ZIP]";
            default: return "[FILE]";
        }
    }

    private void openFile(String path) {
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(new File(path));
            }
        } catch (Exception e) {
            logger.error("Error opening file: {}", path, e);
            showAlert("Error", "Failed to open file: " + e.getMessage());
        }
    }
    
    /**
     * Handle voice button press - supports both hold-to-record and tap-to-lock modes
     */
    private void handleVoiceButtonPressed() {
        if (voiceRecorder != null && voiceRecorder.isRecording() && isVoiceRecordingLocked) {
            // Already recording in locked mode - stop and send
            stopAndSendVoiceRecording();
        } else {
            // Start recording
            startVoiceRecording();
        }
    }
    
    /**
     * Handle voice button release - only relevant for hold-to-record mode
     */
    private void handleVoiceButtonReleased() {
        if (voiceRecorder != null && voiceRecorder.isRecording() && !isVoiceRecordingLocked) {
            // Check if it was a quick tap (less than 0.5 seconds) - switch to lock mode
            if (voiceRecorder.getRecordingDuration() < 1) {
                switchToLockMode();
            } else {
                // Normal hold-to-record - send the message
                stopAndSendVoiceRecording();
            }
        }
    }
    
    /**
     * Switch from hold mode to lock mode for longer recordings
     */
    private void switchToLockMode() {
        isVoiceRecordingLocked = true;
        
        Platform.runLater(() -> {
            if (chatSubtitleLabel != null) {
                chatSubtitleLabel.setText("🔒 Recording locked • Tap to stop");
                chatSubtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #00AA00; -fx-font-weight: bold;");
            }
            
            // Update button appearance for locked mode
            voiceButton.setStyle("-fx-background-color: #00AA00; -fx-border-color: transparent; " +
                               "-fx-background-radius: 20px; -fx-pref-width: 50px; -fx-pref-height: 50px; " +
                               "-fx-effect: dropshadow(gaussian, rgba(0,170,0,0.8), 10, 0, 0, 0); " +
                               "-fx-scale-x: 1.0; -fx-scale-y: 1.0;");
        });
        
        logger.info("Voice recording switched to locked mode - tap again to stop and send");
    }
    
    /**
     * Start voice recording (on mouse press)
     */
    private void startVoiceRecording() {
        // Check if user has permission to send voice messages in channels
        if (currentChat instanceof Channel) {
            Channel channel = (Channel) currentChat;
            User currentUser = userService.getCurrentUser();
            
            if (currentUser != null) {
                boolean isOwner = channel.getOwnerId().equals(currentUser.getUserId());
                boolean isAdmin = channel.getAdminIds().contains(currentUser.getUserId());
                boolean canPost = isOwner || isAdmin;
                
                if (!canPost) {
                    showErrorAlert("Permission Denied", "Only admins can send voice messages to this channel.");
                    return;
                }
            }
        }
        
        if (voiceRecorder == null) {
            voiceRecorder = new VoiceRecorder();
        }
        
        isVoiceRecordingActive = true;
        
        // Start recording
        voiceRecorder.startRecording(
            null, // We'll handle sending separately
            this::updateRecordingIndicator
        );
        
        // Update button appearance - make it red and larger to show it's recording
        voiceButton.setStyle("-fx-background-color: #FF4444; -fx-border-color: transparent; " +
                           "-fx-background-radius: 20px; -fx-pref-width: 50px; -fx-pref-height: 50px; " +
                           "-fx-effect: dropshadow(gaussian, rgba(255,68,68,0.8), 10, 0, 0, 0); " +
                           "-fx-scale-x: 1.1; -fx-scale-y: 1.1;");
        
        // Show recording indicator
        showRecordingIndicator();
        
        logger.info("Voice recording started - hold to continue, release to send");
    }
    
    /**
     * Stop recording and send voice message (on mouse release)
     */
    private void stopAndSendVoiceRecording() {
        if (voiceRecorder != null && voiceRecorder.isRecording()) {
            // Get the recording path before stopping
            String voicePath = voiceRecorder.getCurrentRecordingPath();
            int duration = voiceRecorder.getRecordingDuration();
            
            logger.info("Stopping voice recording - duration: {} seconds", duration);
            
            // Stop the recording (this should stop the timer)
            voiceRecorder.stopAndSendRecording();
            
            // Stop recording indicator immediately
            isVoiceRecordingActive = false;
            isVoiceRecordingLocked = false; // Reset lock mode
            
            // Reset UI immediately and ensure cleanup
            Platform.runLater(() -> {
                resetVoiceButton();
                hideRecordingIndicator();
                logger.info("Voice recording UI cleaned up");
            });
            
            // Send the voice message
            if (voicePath != null && duration >= 1) {
                sendVoiceMessage(voicePath);
            }
            
            logger.info("Voice recording completed and sent");
        }
    }
    
    /**
     * Cancel voice recording (on mouse exit while pressed)
     */
    private void cancelVoiceRecording() {
        if (voiceRecorder != null && voiceRecorder.isRecording()) {
            voiceRecorder.cancelRecording();
            
            // Stop recording indicator immediately  
            isVoiceRecordingActive = false;
            isVoiceRecordingLocked = false; // Reset lock mode
            
            // Reset UI immediately
            resetVoiceButton();
            hideRecordingIndicator();
            
            // Show brief feedback that recording was cancelled
            Platform.runLater(() -> {
                chatSubtitleLabel.setText("Voice message cancelled");
                chatSubtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #FF4444;");
                
                // Reset subtitle after 2 seconds
                Timeline resetTimeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
                    updateChatSubtitle();
                }));
                resetTimeline.play();
            });
            
            logger.info("Voice recording cancelled");
        }
    }
    
    /**
     * Send voice message after recording
     */
    private void sendVoiceMessage(String voicePath) {
        try {
            // Immediately hide recording indicator and stop any ongoing callbacks
            isVoiceRecordingActive = false;
            hideRecordingIndicator();
            
            User currentUser = userService.getCurrentUser();
            String receiverId = getChatId(currentChat);
            
            int duration = voiceRecorder.getRecordingDuration();
            String durationText = "🎤 Voice message (" + VoiceRecorder.formatDuration(duration) + ")";
            
            Message message = new Message(
                currentUser.getUserId(),
                receiverId,
                durationText,
                Message.MessageType.VOICE
            );
            message.setMediaPath(voicePath);
            
            messageService.sendMessage(message);
            addMessageToUI(message);
            messageBroker.broadcastMessage(message);
            scrollToBottom();
            
            logger.info("Voice message sent: {} duration: {}", voicePath, duration);
            
        } catch (Exception e) {
            logger.error("Error sending voice message", e);
            showAlert("Send Error", "Failed to send voice message: " + e.getMessage());
        } finally {
            resetVoiceButton();
            hideRecordingIndicator();
        }
    }
    
    /**
     * Update the recording indicator with better UX for longer recordings
     */
    private void updateRecordingIndicator(int seconds) {
        // Additional safeguard - don't update if recording has stopped
        if (!isVoiceRecordingActive || voiceRecorder == null || !voiceRecorder.isRecording()) {
            logger.info("SKIPPING recording indicator update - recording not active, seconds: {}", seconds);
            return;
        }
        
        logger.info("UPDATING recording indicator - {} seconds", seconds);
        
        Platform.runLater(() -> {
            // Double-check again on the UI thread
            if (!isVoiceRecordingActive || chatSubtitleLabel == null || voiceRecorder == null || !voiceRecorder.isRecording()) {
                return;
            }
            
            String formattedTime = VoiceRecorder.formatDuration(seconds);
            String indicator;
            String style;
            
            // Check if recording is locked (tap mode)
            if (isVoiceRecordingLocked) {
                indicator = "🔒 Recording locked • " + formattedTime + " • Tap to stop";
                style = "-fx-font-size: 13px; -fx-text-fill: #00AA00; -fx-font-weight: bold;";
                // Keep button style locked (green)
            } else {
                // Enhanced UX with different indicators based on recording length (hold mode)
                if (seconds < 3) {
                    indicator = "Recording... " + formattedTime + " (Release to send, quick tap to lock)";
                    style = "-fx-font-size: 13px; -fx-text-fill: #FF4444; -fx-font-weight: bold;";
                    updateVoiceButtonStyle(seconds);
                } else if (seconds < 10) {
                    indicator = "Recording... " + formattedTime + " (Release to send)";
                    style = "-fx-font-size: 13px; -fx-text-fill: #FF6600; -fx-font-weight: bold;";
                    updateVoiceButtonStyle(seconds);
                } else {
                    // For longer recordings in hold mode, suggest switching to lock mode
                    indicator = "Recording... " + formattedTime + " (Release to send, or quick tap to lock for long messages)";
                    style = "-fx-font-size: 13px; -fx-text-fill: #0088FF; -fx-font-weight: bold;";
                    updateVoiceButtonStyle(seconds);
                }
            }
            
            chatSubtitleLabel.setText(indicator);
            chatSubtitleLabel.setStyle(style);
        });
    }
    
    /**
     * Show recording indicator in the chat subtitle
     */
    private void showRecordingIndicator() {
        Platform.runLater(() -> {
            if (chatSubtitleLabel != null) {
                chatSubtitleLabel.setText("🔴 Recording...");
                chatSubtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #FF4444; -fx-font-weight: bold;");
            }
        });
    }
    
    /**
     * Hide recording indicator and restore original subtitle
     */
    private void hideRecordingIndicator() {
        isRecordingIndicatorVisible = false;
        Platform.runLater(() -> {
            logger.info("HIDING RECORDING INDICATOR - clearing subtitle");
            if (chatSubtitleLabel != null && currentChat != null) {
                // Force clear the text first
                chatSubtitleLabel.setText("");
                
                // Then restore original subtitle based on chat type
                if (currentChat instanceof User) {
                    User user = (User) currentChat;
                    boolean isOnline = messageBroker.isUserOnline(user.getUserId());
                    String statusText = isOnline ? "Online" : "Last seen recently";
                    chatSubtitleLabel.setText(statusText);
                    chatSubtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + (isOnline ? "#4CAF50" : "#757575") + ";");
                    logger.info("Recording indicator hidden, restored user status: {}", statusText);
                } else if (currentChat instanceof GroupChat) {
                    GroupChat group = (GroupChat) currentChat;
                    String groupText = group.getMemberIds().size() + " members";
                    chatSubtitleLabel.setText(groupText);
                    chatSubtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #757575;");
                    logger.info("Recording indicator hidden, restored group info: {}", groupText);
                } else if (currentChat instanceof Channel) {
                    Channel channel = (Channel) currentChat;
                    String channelText = channel.getSubscriberIds().size() + " subscribers";
                    chatSubtitleLabel.setText(channelText);
                    chatSubtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #757575;");
                    logger.info("Recording indicator hidden, restored channel info: {}", channelText);
                }
            } else {
                logger.warn("Cannot hide recording indicator - chatSubtitleLabel or currentChat is null");
            }
        });
    }
    
    /**
     * Reset voice button to normal appearance with smooth transition
     */
    private void resetVoiceButton() {
        Platform.runLater(() -> {
            voiceButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; " +
                               "-fx-background-radius: 20px; -fx-pref-width: 40px; -fx-pref-height: 40px; " +
                               "-fx-effect: null; -fx-scale-x: 1.0; -fx-scale-y: 1.0;");
        });
    }
    
    /**
     * Update voice button style based on recording duration for enhanced UX
     */
    private void updateVoiceButtonStyle(int seconds) {
        if (voiceButton == null) return;
        
        // Don't update button style if recording is locked
        if (isVoiceRecordingLocked) return;
        
        Platform.runLater(() -> {
            String baseStyle = "-fx-border-color: transparent; -fx-background-radius: 20px; " +
                             "-fx-pref-width: 50px; -fx-pref-height: 50px;";
            String style;
            
            if (seconds < 3) {
                // Initial recording - pulsing red
                style = baseStyle + 
                       "-fx-background-color: #FF4444; " +
                       "-fx-effect: dropshadow(gaussian, rgba(255,68,68,0.8), 10, 0, 0, 0); " +
                       "-fx-scale-x: 1.1; -fx-scale-y: 1.1;";
            } else if (seconds < 10) {
                // Early recording - steady orange
                style = baseStyle + 
                       "-fx-background-color: #FF6600; " +
                       "-fx-effect: dropshadow(gaussian, rgba(255,102,0,0.7), 8, 0, 0, 0); " +
                       "-fx-scale-x: 1.05; -fx-scale-y: 1.05;";
            } else if (seconds < 30) {
                // Good length - green
                style = baseStyle + 
                       "-fx-background-color: #00AA00; " +
                       "-fx-effect: dropshadow(gaussian, rgba(0,170,0,0.6), 6, 0, 0, 0); " +
                       "-fx-scale-x: 1.0; -fx-scale-y: 1.0;";
            } else if (seconds < 45) {
                // Getting long - blue
                style = baseStyle + 
                       "-fx-background-color: #0088FF; " +
                       "-fx-effect: dropshadow(gaussian, rgba(0,136,255,0.6), 6, 0, 0, 0); " +
                       "-fx-scale-x: 1.0; -fx-scale-y: 1.0;";
            } else if (seconds < 55) {
                // Warning - orange with pulsing
                style = baseStyle + 
                       "-fx-background-color: #FF8800; " +
                       "-fx-effect: dropshadow(gaussian, rgba(255,136,0,0.9), 12, 0, 0, 0); " +
                       "-fx-scale-x: 1.1; -fx-scale-y: 1.1;";
            } else {
                // Critical - red with strong effect
                style = baseStyle + 
                       "-fx-background-color: #FF0000; " +
                       "-fx-effect: dropshadow(gaussian, rgba(255,0,0,1.0), 15, 0, 0, 0); " +
                       "-fx-scale-x: 1.15; -fx-scale-y: 1.15;";
            }
            
            voiceButton.setStyle(style);
        });
    }
    
    /**
     * Update slide-to-cancel visual feedback based on drag distance
     */
    private void updateSlideToCancel(double distance) {
        if (!isVoiceRecordingActive || chatSubtitleLabel == null || isVoiceRecordingLocked) return;
        
        Platform.runLater(() -> {
            if (distance > 30) { // Threshold for slide-to-cancel warning
                int duration = voiceRecorder != null ? voiceRecorder.getRecordingDuration() : 0;
                String timeText = VoiceRecorder.formatDuration(duration);
                
                if (distance > 60) {
                    // Very close to cancel
                    chatSubtitleLabel.setText("Slide to cancel... " + timeText);
                    chatSubtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #FF0000; -fx-font-weight: bold; " +
                                             "-fx-effect: dropshadow(gaussian, #FF0000, 3, 0, 0, 0);");
                } else {
                    // Warning about slide to cancel
                    chatSubtitleLabel.setText("Slide to cancel - " + timeText);
                    chatSubtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #FF8800; -fx-font-weight: bold;");
                }
            }
        });
    }
    
    /**
     * Show media gallery for current chat
     */
    @FXML
    public void handleShowMediaGallery() {
        if (currentChat != null) {
            String chatId = getChatId(currentChat);
            MediaHandler.showMediaGallery(chatId, (Stage) mainPane.getScene().getWindow());
        } else {
            showAlert("No Chat Selected", "Please select a chat to view its media gallery.");
        }
    }
    
    /**
     * Enhanced mark messages as read functionality with instant bidirectional updates
     */
    private void markMessagesAsRead() {
        if (currentChat == null) return;
        
        try {
            User currentUser = userService.getCurrentUser();
            String chatId = getChatId(currentChat);
            
            // Get unread messages in the current chat
            List<Message> unreadMessages = messageService.getUnreadMessagesInChat(chatId, currentUser.getUserId());
            
            for (Message message : unreadMessages) {
                // Skip messages sent by current user
                if (message.getSenderId().equals(currentUser.getUserId())) {
                    continue;
                }
                
                // Mark message as read immediately in database
                message.setStatus(Message.MessageStatus.READ);
                messageService.updateMessageStatus(message.getMessageId(), Message.MessageStatus.READ);
                
                // Immediately update UI for this specific message
                Platform.runLater(() -> {
                    updateMessageReadStatusInstant(message.getMessageId(), currentUser.getUserId());
                });
                
                // Instantly notify sender via message broker (this will trigger onMessageRead on sender's side)
                messageBroker.notifyMessageRead(message.getMessageId(), currentUser.getUserId());
                
                logger.debug("Instantly marked message {} as read by user {}", message.getMessageId(), currentUser.getUserId());
            }
            
            if (!unreadMessages.isEmpty()) {
                logger.info("Instantly marked {} messages as read in chat {}", unreadMessages.size(), chatId);
                
                // Update chat list read indicators immediately
                Platform.runLater(() -> {
                    updateChatListItemReadStatus(chatId);
                    // Also notify sender's chat list if they're viewing another chat
                    refreshChatListForBothUsers(chatId);
                });
            }
            
        } catch (Exception e) {
            logger.error("Error marking messages as read", e);
        }
    }
    
    /**
     * Refresh chat list for both users in the conversation to show instant read status
     */
    private void refreshChatListForBothUsers(String chatId) {
        try {
            // This ensures both users see the read status change immediately
            // even if one user is looking at a different chat
            
            // Current user's chat list is updated immediately via updateChatListItemReadStatus
            // For the other user, the onMessageRead callback will trigger their UI update
            
            // Additional: Force a complete refresh of current user's chat list
            refreshChatList();
            
        } catch (Exception e) {
            logger.error("Error refreshing chat list for both users", e);
        }
    }
    
    /**
     * Update chat list item to reflect read status
     */
    private void updateChatListItemReadStatus(String chatId) {
        try {
            // Find the chat item in the list and update its visual state
            for (Object chat : chatItems) {
                if (getChatId(chat).equals(chatId)) {
                    chatListView.refresh();
                    break;
                }
            }
        } catch (Exception e) {
            logger.error("Error updating chat list read status", e);
        }
    }

    // Group Settings Tab Creation Methods
    
    private VBox createGroupGeneralSettings(GroupChat group) {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // Group Name
        Label nameLabel = new Label("Group Name:");
        nameLabel.setStyle("-fx-font-weight: bold;");
        TextField nameField = new TextField(group.getGroupName());
        nameField.setPromptText("Enter group name");
        
        // Group Description
        Label descLabel = new Label("Description:");
        descLabel.setStyle("-fx-font-weight: bold;");
        TextArea descArea = new TextArea(group.getDescription() != null ? group.getDescription() : "");
        descArea.setPromptText("Enter group description (optional)");
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);
        
        // Group Photo
        Label photoLabel = new Label("Group Photo:");
        photoLabel.setStyle("-fx-font-weight: bold;");
        Button changePhotoBtn = new Button("Change Photo");
        changePhotoBtn.setOnAction(e -> {
            // TODO: Implement photo change functionality
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Change Photo");
            alert.setContentText("Photo change functionality will be implemented in a future update.");
            alert.showAndWait();
        });
        
        content.getChildren().addAll(
            nameLabel, nameField,
            descLabel, descArea,
            photoLabel, changePhotoBtn
        );
        
        return content;
    }
    
    private VBox createGroupPrivacySettings(GroupChat group) {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // Group Type
        Label typeLabel = new Label("Group Type:");
        typeLabel.setStyle("-fx-font-weight: bold;");
        RadioButton publicGroup = new RadioButton("Public Group");
        RadioButton privateGroup = new RadioButton("Private Group");
        ToggleGroup typeGroup = new ToggleGroup();
        publicGroup.setToggleGroup(typeGroup);
        privateGroup.setToggleGroup(typeGroup);
        privateGroup.setSelected(true); // Default
        
        Label typeDesc = new Label("Public groups can be found in search and have usernames.\nPrivate groups are invitation-only.");
        typeDesc.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
        typeDesc.setWrapText(true);
        
        // Join Settings
        Label joinLabel = new Label("Who can add members:");
        joinLabel.setStyle("-fx-font-weight: bold;");
        ComboBox<String> joinCombo = new ComboBox<>();
        joinCombo.getItems().addAll("Only Admins", "All Members");
        joinCombo.setValue("Only Admins");
        
        content.getChildren().addAll(
            typeLabel, publicGroup, privateGroup, typeDesc,
            new Separator(),
            joinLabel, joinCombo
        );
        
        return content;
    }
    
    private VBox createGroupPermissionSettings(GroupChat group) {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        Label permLabel = new Label("Member Permissions:");
        permLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        // Send Messages
        CheckBox sendMessages = new CheckBox("Send Messages");
        sendMessages.setSelected(true);
        
        // Send Media
        CheckBox sendMedia = new CheckBox("Send Media Files");
        sendMedia.setSelected(true);
        
        // Send Stickers & GIFs
        CheckBox sendStickers = new CheckBox("Send Stickers & GIFs");
        sendStickers.setSelected(true);
        
        // Add Members
        CheckBox addMembers = new CheckBox("Add New Members");
        addMembers.setSelected(false);
        
        // Pin Messages
        CheckBox pinMessages = new CheckBox("Pin Messages");
        pinMessages.setSelected(false);
        
        // Change Group Info
        CheckBox changeInfo = new CheckBox("Change Group Info");
        changeInfo.setSelected(false);
        
        Label adminLabel = new Label("Administrator Permissions:");
        adminLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        // Delete Messages
        CheckBox deleteMessages = new CheckBox("Delete Messages");
        deleteMessages.setSelected(true);
        
        // Ban Users
        CheckBox banUsers = new CheckBox("Ban Users");
        banUsers.setSelected(true);
        
        // Promote Members
        CheckBox promoteMembers = new CheckBox("Add New Admins");
        promoteMembers.setSelected(false);
        
        content.getChildren().addAll(
            permLabel,
            sendMessages, sendMedia, sendStickers, addMembers, pinMessages, changeInfo,
            new Separator(),
            adminLabel,
            deleteMessages, banUsers, promoteMembers
        );
        
        return content;
    }
    
    // Channel Settings Tab Creation Methods
    
    private VBox createChannelGeneralSettings(Channel channel) {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // Channel Name
        Label nameLabel = new Label("Channel Name:");
        nameLabel.setStyle("-fx-font-weight: bold;");
        TextField nameField = new TextField(channel.getChannelName());
        nameField.setPromptText("Enter channel name");
        
        // Channel Description
        Label descLabel = new Label("Description:");
        descLabel.setStyle("-fx-font-weight: bold;");
        TextArea descArea = new TextArea(channel.getDescription() != null ? channel.getDescription() : "");
        descArea.setPromptText("Enter channel description");
        descArea.setPrefRowCount(4);
        descArea.setWrapText(true);
        
        // Channel Photo
        Label photoLabel = new Label("Channel Photo:");
        photoLabel.setStyle("-fx-font-weight: bold;");
        Button changePhotoBtn = new Button("Change Photo");
        changePhotoBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Change Photo");
            alert.setContentText("Photo change functionality will be implemented in a future update.");
            alert.showAndWait();
        });
        
        content.getChildren().addAll(
            nameLabel, nameField,
            descLabel, descArea,
            photoLabel, changePhotoBtn
        );
        
        return content;
    }
    
    private VBox createChannelPrivacySettings(Channel channel) {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // Channel Type
        Label typeLabel = new Label("Channel Type:");
        typeLabel.setStyle("-fx-font-weight: bold;");
        RadioButton publicChannel = new RadioButton("Public Channel");
        RadioButton privateChannel = new RadioButton("Private Channel");
        ToggleGroup typeGroup = new ToggleGroup();
        publicChannel.setToggleGroup(typeGroup);
        privateChannel.setToggleGroup(typeGroup);
        
        if (channel.isPrivate()) {
            privateChannel.setSelected(true);
        } else {
            publicChannel.setSelected(true);
        }
        
        Label typeDesc = new Label("Public channels can be found in search and have usernames.\nPrivate channels are invitation-only via links.");
        typeDesc.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
        typeDesc.setWrapText(true);
        
        // Username (for public channels)
        Label usernameLabel = new Label("Username:");
        usernameLabel.setStyle("-fx-font-weight: bold;");
        TextField usernameField = new TextField();
        usernameField.setPromptText("@channelname");
        usernameField.setDisable(channel.isPrivate());
        
        // Sign Messages
        CheckBox signMessages = new CheckBox("Sign messages with author name");
        signMessages.setSelected(false);
        
        // Restrict Saving Content
        CheckBox restrictSaving = new CheckBox("Restrict saving content");
        restrictSaving.setSelected(false);
        
        // Update disable state based on selection
        publicChannel.setOnAction(e -> usernameField.setDisable(false));
        privateChannel.setOnAction(e -> usernameField.setDisable(true));
        
        content.getChildren().addAll(
            typeLabel, publicChannel, privateChannel, typeDesc,
            new Separator(),
            usernameLabel, usernameField,
            new Separator(),
            signMessages, restrictSaving
        );
        
        return content;
    }
    
    private VBox createChannelAdminSettings(Channel channel) {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        Label adminLabel = new Label("Administrator Settings:");
        adminLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        // Admin List
        Label adminListLabel = new Label("Current Administrators:");
        adminListLabel.setStyle("-fx-font-weight: bold;");
        
        ListView<String> adminList = new ListView<>();
        adminList.getItems().addAll("You (Creator)", "Admin permissions will be", "implemented in future update");
        adminList.setPrefHeight(100);
        
        Button addAdminBtn = new Button("Add Administrator");
        addAdminBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Add Administrator");
            alert.setContentText("Add administrator functionality will be implemented in a future update.");
            alert.showAndWait();
        });
        
        Button removeAdminBtn = new Button("Remove Administrator");
        removeAdminBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Remove Administrator");
            alert.setContentText("Remove administrator functionality will be implemented in a future update.");
            alert.showAndWait();
        });
        
        HBox adminButtons = new HBox(10);
        adminButtons.getChildren().addAll(addAdminBtn, removeAdminBtn);
        
        content.getChildren().addAll(
            adminLabel,
            adminListLabel, adminList,
            adminButtons
        );
        
        return content;
    }
    
    private VBox createChannelContentSettings(Channel channel) {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        Label title = new Label("Channel Settings");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        content.getChildren().add(title);
        content.getChildren().add(new Label("(Settings UI coming soon)"));
        
        return content;
    }
    
    // Tab content helpers for unified management dialogs
    private VBox createGroupInfoContent(GroupChat group) {
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        try {
            Label name = new Label("Name: " + group.getGroupName());
            Label id = new Label("Group ID: " + group.getGroupId());
            Label desc = new Label("Description: " + (group.getDescription() != null ? group.getDescription() : "No description"));
            List<User> members = chatService.getGroupMembers(group.getGroupId());
            Label memberCount = new Label("Members: " + members.size());
            ListView<String> memberList = new ListView<>();
            for (User member : members) {
                boolean isOnline = messageBroker.isUserOnline(member.getUserId());
                memberList.getItems().add(member.getProfileName() + (isOnline ? " (Online)" : " (Offline)"));
            }
            content.getChildren().addAll(name, id, desc, memberCount, memberList);
        } catch (Exception e) {
            content.getChildren().add(new Label("Error loading group info: " + e.getMessage()));
        }
        return content;
    }

    private VBox createGroupMembersContent(GroupChat group) {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        try {
            // Title
            Label title = new Label("Group Members");
            title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
            
            // Current user info for permission checking
            User currentUser = userService.getCurrentUser();
            boolean isOwner = currentUser != null && group.getCreatorId().equals(currentUser.getUserId());
            boolean isAdmin = currentUser != null && group.getAdminIds().contains(currentUser.getUserId());
            boolean canManageMembers = isOwner || isAdmin;
            
            // Member count info
            List<User> members = chatService.getGroupMembers(group.getGroupId());
            Label memberCount = new Label("Total Members: " + members.size());
            memberCount.setStyle("-fx-font-weight: bold;");
            
            // Members list with roles
            ListView<VBox> memberListView = new ListView<>();
            memberListView.setPrefHeight(300);
            
            for (User member : members) {
                VBox memberItem = createMemberItem(member, group, canManageMembers, currentUser);
                memberListView.getItems().add(memberItem);
            }
            
            // Member management buttons
            HBox buttonContainer = new HBox(10);
            buttonContainer.setAlignment(Pos.CENTER_LEFT);
            
            if (canManageMembers) {
                Button addMemberBtn = new Button("Add Member");
                addMemberBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                addMemberBtn.setOnAction(e -> handleAddMember(group, memberListView));
                
                buttonContainer.getChildren().add(addMemberBtn);
            } else {
                Label permissionInfo = new Label("Only group admins can manage members");
                permissionInfo.setStyle("-fx-text-fill: #666;");
                buttonContainer.getChildren().add(permissionInfo);
            }
            
            content.getChildren().addAll(title, memberCount, memberListView, buttonContainer);
            
        } catch (Exception e) {
            logger.error("Error creating group members content", e);
            content.getChildren().add(new Label("Error loading members: " + e.getMessage()));
        }
        
        return content;
    }
    
    private VBox createMemberItem(User member, GroupChat group, boolean canManageMembers, User currentUser) {
        VBox memberItem = new VBox(5);
        memberItem.setPadding(new Insets(10));
        memberItem.setStyle("-fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-color: #f9f9f9;");
        
        // Member info row
        HBox memberInfo = new HBox(10);
        memberInfo.setAlignment(Pos.CENTER_LEFT);
        
        // Profile picture placeholder
        Circle profilePic = new Circle(20);
        profilePic.setFill(Color.LIGHTBLUE);
        
        // Member details
        VBox memberDetails = new VBox(2);
        Label nameLabel = new Label(member.getProfileName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        Label usernameLabel = new Label("@" + member.getUsername());
        usernameLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        
        memberDetails.getChildren().addAll(nameLabel, usernameLabel);
        
        // Role and status
        VBox statusInfo = new VBox(2);
        statusInfo.setAlignment(Pos.CENTER_RIGHT);
        
        // Determine role
        String role = "Member";
        String roleStyle = "-fx-text-fill: #666;";
        if (group.getCreatorId().equals(member.getUserId())) {
            role = "Owner";
            roleStyle = "-fx-text-fill: #FF9800; -fx-font-weight: bold;";
        } else if (group.getAdminIds().contains(member.getUserId())) {
            role = "Admin";
            roleStyle = "-fx-text-fill: #2196F3; -fx-font-weight: bold;";
        }
        
        Label roleLabel = new Label(role);
        roleLabel.setStyle(roleStyle);
        
        // Online status
        boolean isOnline = messageBroker.isUserOnline(member.getUserId());
        Label statusLabel = new Label(isOnline ? "Online" : "Offline");
        statusLabel.setStyle(isOnline ? "-fx-text-fill: #4CAF50;" : "-fx-text-fill: #999;");
        
        statusInfo.getChildren().addAll(roleLabel, statusLabel);
        
        memberInfo.getChildren().addAll(profilePic, memberDetails);
        
        // Add spacer to push status to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        memberInfo.getChildren().addAll(spacer, statusInfo);
        
        memberItem.getChildren().add(memberInfo);
        
        // Action buttons for member management
        if (canManageMembers && currentUser != null && !member.getUserId().equals(currentUser.getUserId())) {
            HBox actionButtons = new HBox(5);
            actionButtons.setAlignment(Pos.CENTER_RIGHT);
            
            // Don't allow actions on the owner
            if (!group.getCreatorId().equals(member.getUserId())) {
                if (!group.getAdminIds().contains(member.getUserId())) {
                    // Regular member - can promote or remove
                    Button promoteBtn = new Button("Make Admin");
                    promoteBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 10px;");
                    promoteBtn.setOnAction(e -> handlePromoteMember(member, group));
                    actionButtons.getChildren().add(promoteBtn);
                }
                
                Button removeBtn = new Button("Remove");
                removeBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 10px;");
                removeBtn.setOnAction(e -> handleRemoveMember(member, group));
                actionButtons.getChildren().add(removeBtn);
            }
            
            if (actionButtons.getChildren().size() > 0) {
                memberItem.getChildren().add(actionButtons);
            }
        }
        
        return memberItem;
    }

    private VBox createGroupSettingsContent(GroupChat group) {
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        try {
            // Placeholder for group settings UI
            Label title = new Label("Group Settings");
            title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
            content.getChildren().add(title);
            // You can add more settings fields here as needed
            content.getChildren().add(new Label("(Settings UI coming soon)"));
        } catch (Exception e) {
            content.getChildren().add(new Label("Error loading settings: " + e.getMessage()));
        }
        return content;
    }

    private VBox createChannelInfoContent(Channel channel) {
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        try {
            Label name = new Label("Name: " + channel.getChannelName());
            Label id = new Label("Channel ID: " + channel.getChannelId());
            Label desc = new Label("Description: " + (channel.getDescription() != null ? channel.getDescription() : "No description"));
            Label type = new Label("Type: " + (channel.isPrivate() ? "Private" : "Public"));
            content.getChildren().addAll(name, id, desc, type);
        } catch (Exception e) {
            content.getChildren().add(new Label("Error loading channel info: " + e.getMessage()));
        }
        return content;
    }

    private VBox createChannelSubscribersContent(Channel channel) {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        try {
            // Title
            Label title = new Label("Channel Subscribers");
            title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
            
            // Current user info for permission checking
            User currentUser = userService.getCurrentUser();
            boolean isOwner = currentUser != null && channel.getOwnerId().equals(currentUser.getUserId());
            boolean isAdmin = currentUser != null && channel.getAdminIds().contains(currentUser.getUserId());
            boolean canManageSubscribers = isOwner || isAdmin;
            
            // Subscriber count info
            List<User> subscribers = chatService.getChannelSubscribers(channel.getChannelId());
            Label subscriberCount = new Label("Total Subscribers: " + subscribers.size());
            subscriberCount.setStyle("-fx-font-weight: bold;");
            
            // Subscribers list with roles
            ListView<VBox> subscriberListView = new ListView<>();
            subscriberListView.setPrefHeight(300);
            
            for (User subscriber : subscribers) {
                VBox subscriberItem = createSubscriberItem(subscriber, channel, canManageSubscribers, currentUser);
                subscriberListView.getItems().add(subscriberItem);
            }
            
            // Subscriber management buttons
            HBox buttonContainer = new HBox(10);
            buttonContainer.setAlignment(Pos.CENTER_LEFT);
            
            if (canManageSubscribers) {
                Button addSubscriberBtn = new Button("Add Subscriber");
                addSubscriberBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                addSubscriberBtn.setOnAction(e -> handleAddSubscriber(channel, subscriberListView));
                
                buttonContainer.getChildren().add(addSubscriberBtn);
            } else {
                Label permissionInfo = new Label("Only channel admins can manage subscribers");
                permissionInfo.setStyle("-fx-text-fill: #666;");
                buttonContainer.getChildren().add(permissionInfo);
            }
            
            content.getChildren().addAll(title, subscriberCount, subscriberListView, buttonContainer);
            
        } catch (Exception e) {
            logger.error("Error creating channel subscribers content", e);
            content.getChildren().add(new Label("Error loading subscribers: " + e.getMessage()));
        }
        
        return content;
    }
    
    private VBox createSubscriberItem(User subscriber, Channel channel, boolean canManageSubscribers, User currentUser) {
        VBox subscriberItem = new VBox(5);
        subscriberItem.setPadding(new Insets(10));
        subscriberItem.setStyle("-fx-border-color: #ddd; -fx-border-radius: 5; -fx-background-color: #f9f9f9;");
        
        // Subscriber info row
        HBox subscriberInfo = new HBox(10);
        subscriberInfo.setAlignment(Pos.CENTER_LEFT);
        
        // Profile picture placeholder
        Circle profilePic = new Circle(20);
        profilePic.setFill(Color.LIGHTBLUE);
        
        // Subscriber details
        VBox subscriberDetails = new VBox(2);
        Label nameLabel = new Label(subscriber.getProfileName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        Label usernameLabel = new Label("@" + subscriber.getUsername());
        usernameLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 12px;");
        
        subscriberDetails.getChildren().addAll(nameLabel, usernameLabel);
        
        // Role and status
        VBox statusInfo = new VBox(2);
        statusInfo.setAlignment(Pos.CENTER_RIGHT);
        
        // Determine role
        String role = "Subscriber";
        String roleStyle = "-fx-text-fill: #666;";
        if (channel.getOwnerId().equals(subscriber.getUserId())) {
            role = "Owner";
            roleStyle = "-fx-text-fill: #FF9800; -fx-font-weight: bold;";
        } else if (channel.getAdminIds().contains(subscriber.getUserId())) {
            role = "Admin";
            roleStyle = "-fx-text-fill: #2196F3; -fx-font-weight: bold;";
        }
        
        Label roleLabel = new Label(role);
        roleLabel.setStyle(roleStyle);
        
        // Online status
        boolean isOnline = messageBroker.isUserOnline(subscriber.getUserId());
        Label statusLabel = new Label(isOnline ? "Online" : "Offline");
        statusLabel.setStyle(isOnline ? "-fx-text-fill: #4CAF50;" : "-fx-text-fill: #999;");
        
        statusInfo.getChildren().addAll(roleLabel, statusLabel);
        
        subscriberInfo.getChildren().addAll(profilePic, subscriberDetails);
        
        // Add spacer to push status to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        subscriberInfo.getChildren().addAll(spacer, statusInfo);
        
        subscriberItem.getChildren().add(subscriberInfo);
        
        // Action buttons for subscriber management
        if (canManageSubscribers && currentUser != null && !subscriber.getUserId().equals(currentUser.getUserId())) {
            HBox actionButtons = new HBox(5);
            actionButtons.setAlignment(Pos.CENTER_RIGHT);
            
            // Don't allow actions on the owner
            if (!channel.getOwnerId().equals(subscriber.getUserId())) {
                if (!channel.getAdminIds().contains(subscriber.getUserId())) {
                    // Regular subscriber - can promote or remove
                    Button promoteBtn = new Button("Make Admin");
                    promoteBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 10px;");
                    promoteBtn.setOnAction(e -> handlePromoteSubscriber(subscriber, channel));
                    actionButtons.getChildren().add(promoteBtn);
                }
                
                Button removeBtn = new Button("Remove");
                removeBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 10px;");
                removeBtn.setOnAction(e -> handleRemoveSubscriber(subscriber, channel));
                actionButtons.getChildren().add(removeBtn);
            }
            
            if (actionButtons.getChildren().size() > 0) {
                subscriberItem.getChildren().add(actionButtons);
            }
        }
        
        return subscriberItem;
    }

    private VBox createChannelSettingsContent(Channel channel) {
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        try {
            Label title = new Label("Channel Settings");
            title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
            content.getChildren().add(title);
            content.getChildren().add(new Label("(Settings UI coming soon)"));
        } catch (Exception e) {
            content.getChildren().add(new Label("Error loading settings: " + e.getMessage()));
        }
        return content;
    }

    /**
     * Apply current theme settings to the UI
     */
    private void applyCurrentTheme() {
        try {
            // This will be called after settings dialog closes to apply any theme changes
            UserPreferences userPreferences = UserPreferences.getInstance();
            String theme = userPreferences.getTheme();
            
            // Get the current scene
            Scene scene = mainPane.getScene();
            if (scene != null) {
                // Clear existing stylesheets
                scene.getStylesheets().clear();
                
                // Apply theme-specific stylesheet
                switch (theme) {
                    case "Light":
                        scene.getStylesheets().add(getClass().getResource("/css/telegram-theme.css").toExternalForm());
                        break;
                    case "Dark":
                        scene.getStylesheets().add(getClass().getResource("/css/telegram-theme.css").toExternalForm());
                        scene.getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());
                        break;
                    case "Auto":
                    case "System":
                        // For now, default to light theme
                        scene.getStylesheets().add(getClass().getResource("/css/telegram-theme.css").toExternalForm());
                        break;
                    default:
                        scene.getStylesheets().add(getClass().getResource("/css/telegram-theme.css").toExternalForm());
                        break;
                }
                
                logger.info("Applied theme: {}", theme);
            }
        } catch (Exception e) {
            logger.error("Error applying theme", e);
        }
    }

    /**
     * Creates a circular profile image from the source image
     * Properly crops and centers the image to avoid distortion
     */
    private Image createCircularProfileImage(Image sourceImage, int size) {
        if (sourceImage == null) return null;
        
        try {
            // Calculate dimensions to crop a square from the center of the image
            double sourceWidth = sourceImage.getWidth();
            double sourceHeight = sourceImage.getHeight();
            double cropSize = Math.min(sourceWidth, sourceHeight);
            
            // Calculate offset to center the crop
            double offsetX = (sourceWidth - cropSize) / 2.0;
            double offsetY = (sourceHeight - cropSize) / 2.0;
            
            // Create square cropped image
            WritableImage croppedImage = new WritableImage((int) cropSize, (int) cropSize);
            var sourceReader = sourceImage.getPixelReader();
            var croppedWriter = croppedImage.getPixelWriter();
            
            for (int y = 0; y < cropSize; y++) {
                for (int x = 0; x < cropSize; x++) {
                    Color pixel = sourceReader.getColor((int) (x + offsetX), (int) (y + offsetY));
                    croppedWriter.setColor(x, y, pixel);
                }
            }
            
            // Create circular image from the square crop
            WritableImage circularImage = new WritableImage(size, size);
            var circularWriter = circularImage.getPixelWriter();
            
            double centerX = size / 2.0;
            double centerY = size / 2.0;
            double radius = size / 2.0;
            
            // Resize the cropped image to fit target size
            ImageView tempView = new ImageView(croppedImage);
            tempView.setFitWidth(size);
            tempView.setFitHeight(size);
            tempView.setPreserveRatio(false);
            
            WritableImage resizedImage = new WritableImage(size, size);
            tempView.snapshot(null, resizedImage);
            var resizedReader = resizedImage.getPixelReader();
            
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    double distance = Math.sqrt((x - centerX) * (x - centerX) + (y - centerY) * (y - centerY));
                    
                    if (distance <= radius) {
                        // Inside circle - copy pixel
                        Color pixelColor = resizedReader.getColor(x, y);
                        circularWriter.setColor(x, y, pixelColor);
                    } else {
                        // Outside circle - transparent
                        circularWriter.setColor(x, y, Color.TRANSPARENT);
                    }
                }
            }
            
            return circularImage;
        } catch (Exception e) {
            logger.error("Error creating circular profile image: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Creates a circular profile ImageView for the chat list
     */
    private ImageView createCircularProfileImageView(Image sourceImage, int size) {
        Image circularImage = createCircularProfileImage(sourceImage, size);
        if (circularImage == null) return null;
        
        ImageView imageView = new ImageView(circularImage);
        imageView.setFitWidth(size);
        imageView.setFitHeight(size);
        imageView.setPreserveRatio(false);
        
        return imageView;
    }
    
    // ===============================
    // GROUP MEMBER VIEW METHODS
    // ===============================
    
    private VBox createMemberGroupInfoContent(GroupChat group) {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // Group header
        Label titleLabel = new Label(group.getGroupName());
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");
        
        // Description
        if (group.getDescription() != null && !group.getDescription().trim().isEmpty()) {
            Label descLabel = new Label(group.getDescription());
            descLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
            descLabel.setWrapText(true);
            content.getChildren().add(descLabel);
        }
        
        Separator separator1 = new Separator();
        
        // Basic group info
        VBox infoBox = new VBox(10);
        infoBox.setPadding(new Insets(15));
        infoBox.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 8;");
        
        Label membersLabel = new Label("👥 Members: " + getGroupMemberCount(group));
        membersLabel.setStyle("-fx-font-size: 14px;");
        
        Label createdLabel = new Label("Created: " + formatChannelDate(group.getCreatedAt()));
        createdLabel.setStyle("-fx-font-size: 14px;");
        
        Label typeLabel = new Label("Type: Private Group");
        typeLabel.setStyle("-fx-font-size: 14px;");
        
        infoBox.getChildren().addAll(membersLabel, createdLabel, typeLabel);
        
        Separator separator2 = new Separator();
        
        // Member info
        Label memberInfoLabel = new Label("As a group member, you can:");
        memberInfoLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        VBox benefitsBox = new VBox(8);
        benefitsBox.setPadding(new Insets(10));
        
        Label benefit1 = new Label("* Send messages and participate in discussions");
        Label benefit2 = new Label("* Share files, images, and documents");
        Label benefit3 = new Label("* View group history and media");
        Label benefit4 = new Label("* Get notifications for group activity");
        
        benefit1.setStyle("-fx-font-size: 14px;");
        benefit2.setStyle("-fx-font-size: 14px;");
        benefit3.setStyle("-fx-font-size: 14px;");
        benefit4.setStyle("-fx-font-size: 14px;");
        
        benefitsBox.getChildren().addAll(benefit1, benefit2, benefit3, benefit4);
        
        content.getChildren().addAll(titleLabel, separator1, infoBox, separator2, memberInfoLabel, benefitsBox);
        return content;
    }
    
    private VBox createMemberSettingsContent(GroupChat group) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        
        Label titleLabel = new Label("Member Settings");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // Notification settings
        VBox notificationBox = new VBox(10);
        notificationBox.setPadding(new Insets(15));
        notificationBox.setStyle("-fx-background-color: #f9f9f9; -fx-background-radius: 8;");
        
        Label notifLabel = new Label("🔔 Notifications");
        notifLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        CheckBox enableNotifications = new CheckBox("Enable notifications for this group");
        enableNotifications.setSelected(true);
        CheckBox soundNotifications = new CheckBox("Play sound for notifications");
        soundNotifications.setSelected(false);
        CheckBox onlyMentions = new CheckBox("Only notify when mentioned");
        onlyMentions.setSelected(false);
        
        notificationBox.getChildren().addAll(notifLabel, enableNotifications, soundNotifications, onlyMentions);
        
        Separator separator = new Separator();
        
        // Actions
        VBox actionsBox = new VBox(10);
        actionsBox.setPadding(new Insets(15));
        actionsBox.setStyle("-fx-background-color: #fff3e0; -fx-background-radius: 8;");
        
        Label actionsLabel = new Label("Actions");
        actionsLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        Button muteButton = new Button("Mute Group");
        muteButton.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white;");
        muteButton.setPrefWidth(200);
        
        Button leaveButton = new Button("Leave Group");
        leaveButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        leaveButton.setPrefWidth(200);
        leaveButton.setOnAction(e -> leaveGroup(group));
        
        actionsBox.getChildren().addAll(actionsLabel, muteButton, leaveButton);
        
        content.getChildren().addAll(titleLabel, notificationBox, separator, actionsBox);
        return content;
    }
    
    private int getGroupMemberCount(GroupChat group) {
        try {
            List<User> members = chatService.getGroupMembers(group.getGroupId());
            return members != null ? members.size() : 0;
        } catch (Exception e) {
            logger.error("Error getting group member count", e);
            return 0;
        }
    }

    // ===============================
    // SUBSCRIBER VIEW METHODS
    // ===============================
    
    private VBox createSubscriberChannelInfoContent(Channel channel) {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        
        // Channel header
        Label titleLabel = new Label(channel.getChannelName());
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2196F3;");
        
        // Description
        if (channel.getDescription() != null && !channel.getDescription().trim().isEmpty()) {
            Label descLabel = new Label(channel.getDescription());
            descLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
            descLabel.setWrapText(true);
            content.getChildren().add(descLabel);
        }
        
        Separator separator1 = new Separator();
        
        // Basic channel info
        VBox infoBox = new VBox(10);
        infoBox.setPadding(new Insets(15));
        infoBox.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 8;");
        
        Label subscribersLabel = new Label("📊 Subscribers: " + getChannelSubscriberCount(channel));
        subscribersLabel.setStyle("-fx-font-size: 14px;");
        
        Label createdLabel = new Label("Created: " + formatChannelDate(channel.getCreatedAt()));
        createdLabel.setStyle("-fx-font-size: 14px;");
        
        Label typeLabel = new Label("Type: Public Channel");
        typeLabel.setStyle("-fx-font-size: 14px;");
        
        infoBox.getChildren().addAll(subscribersLabel, createdLabel, typeLabel);
        
        Separator separator2 = new Separator();
        
        // Subscriber info
        Label subscriberInfoLabel = new Label("As a subscriber, you can:");
        subscriberInfoLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        VBox benefitsBox = new VBox(8);
        benefitsBox.setPadding(new Insets(10));
        
        Label benefit1 = new Label("* Receive all channel updates and posts");
        Label benefit2 = new Label("* View channel history and media");
        Label benefit3 = new Label("* Get notifications for new content");
        Label benefit4 = new Label("* Access shared files and documents");
        
        benefit1.setStyle("-fx-font-size: 14px;");
        benefit2.setStyle("-fx-font-size: 14px;");
        benefit3.setStyle("-fx-font-size: 14px;");
        benefit4.setStyle("-fx-font-size: 14px;");
        
        benefitsBox.getChildren().addAll(benefit1, benefit2, benefit3, benefit4);
        
        content.getChildren().addAll(titleLabel, separator1, infoBox, separator2, subscriberInfoLabel, benefitsBox);
        return content;
    }
    
    private VBox createSubscriberSettingsContent(Channel channel) {
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        
        Label titleLabel = new Label("Subscription Settings");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        
        // Notification settings
        VBox notificationBox = new VBox(10);
        notificationBox.setPadding(new Insets(15));
        notificationBox.setStyle("-fx-background-color: #f9f9f9; -fx-background-radius: 8;");
        
        Label notifLabel = new Label("🔔 Notifications");
        notifLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        CheckBox enableNotifications = new CheckBox("Enable notifications for this channel");
        enableNotifications.setSelected(true);
        CheckBox soundNotifications = new CheckBox("Play sound for notifications");
        soundNotifications.setSelected(false);
        CheckBox onlyMentions = new CheckBox("Only notify when mentioned");
        onlyMentions.setSelected(false);
        
        notificationBox.getChildren().addAll(notifLabel, enableNotifications, soundNotifications, onlyMentions);
        
        Separator separator = new Separator();
        
        // Actions
        VBox actionsBox = new VBox(10);
        actionsBox.setPadding(new Insets(15));
        actionsBox.setStyle("-fx-background-color: #fff3e0; -fx-background-radius: 8;");
        
        Label actionsLabel = new Label("Actions");
        actionsLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        Button muteButton = new Button("Mute Channel");
        muteButton.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white;");
        muteButton.setPrefWidth(200);
        
        Button leaveButton = new Button("Leave Channel");
        leaveButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        leaveButton.setPrefWidth(200);
        leaveButton.setOnAction(e -> leaveChannel(channel));
        
        actionsBox.getChildren().addAll(actionsLabel, muteButton, leaveButton);
        
        content.getChildren().addAll(titleLabel, notificationBox, separator, actionsBox);
        return content;
    }
    
    private int getChannelSubscriberCount(Channel channel) {
        try {
            return (int) channelService.getSubscriberCount(channel.getChannelId());
        } catch (Exception e) {
            logger.error("Error getting channel subscriber count", e);
            return 0;
        }
    }
    
    /**
     * Update the group member count label in the management dialog
     */
    private void updateGroupMemberCountLabel(GroupChat group, javafx.scene.Node parentNode) {
        try {
            // Find the member count label in the parent container and update it
            if (parentNode instanceof javafx.scene.Parent) {
                updateLabelInContainer((javafx.scene.Parent) parentNode, "Total Members:", 
                    "Total Members: " + getGroupMemberCount(group));
            }
        } catch (Exception e) {
            logger.error("Error updating group member count label", e);
        }
    }
    
    /**
     * Update the channel subscriber count label in the management dialog
     */
    private void updateChannelSubscriberCountLabel(Channel channel, javafx.scene.Node parentNode) {
        try {
            // Find the subscriber count label in the parent container and update it
            if (parentNode instanceof javafx.scene.Parent) {
                updateLabelInContainer((javafx.scene.Parent) parentNode, "Total Subscribers:", 
                    "Total Subscribers: " + getChannelSubscriberCount(channel));
            }
        } catch (Exception e) {
            logger.error("Error updating channel subscriber count label", e);
        }
    }
    
    /**
     * Helper method to find and update a label in a container
     */
    private void updateLabelInContainer(javafx.scene.Parent container, String labelPrefix, String newText) {
        try {
            for (javafx.scene.Node node : container.getChildrenUnmodifiable()) {
                if (node instanceof Label) {
                    Label label = (Label) node;
                    if (label.getText().startsWith(labelPrefix)) {
                        label.setText(newText);
                        
                        // Add a brief highlight animation to show the update
                        Timeline highlightTimeline = new Timeline();
                        KeyFrame frame1 = new KeyFrame(Duration.millis(0), 
                            new KeyValue(label.scaleXProperty(), 1.0),
                            new KeyValue(label.scaleYProperty(), 1.0));
                        KeyFrame frame2 = new KeyFrame(Duration.millis(200), 
                            new KeyValue(label.scaleXProperty(), 1.1),
                            new KeyValue(label.scaleYProperty(), 1.1));
                        KeyFrame frame3 = new KeyFrame(Duration.millis(400), 
                            new KeyValue(label.scaleXProperty(), 1.0),
                            new KeyValue(label.scaleYProperty(), 1.0));
                        highlightTimeline.getKeyFrames().addAll(frame1, frame2, frame3);
                        highlightTimeline.play();
                        return;
                    }
                } else if (node instanceof javafx.scene.Parent) {
                    // Recursively search in child containers
                    updateLabelInContainer((javafx.scene.Parent) node, labelPrefix, newText);
                }
            }
        } catch (Exception e) {
            logger.error("Error updating label in container", e);
        }
    }
    
    /**
     * Refresh group management UI by closing and reopening the dialog
     */
    private void refreshGroupManagementUI(GroupChat group) {
        try {
            // This is a simple approach - ideally we'd update the specific ListView
            // but for now we'll trigger a refresh by showing an updated dialog
            Platform.runLater(() -> {
                // Close any open dialogs for this group and reopen
                // This ensures the UI reflects the current state
                showGroupInfo(group);
            });
        } catch (Exception e) {
            logger.error("Error refreshing group management UI", e);
        }
    }
    
    /**
     * Refresh channel management UI by closing and reopening the dialog
     */
    private void refreshChannelManagementUI(Channel channel) {
        try {
            Platform.runLater(() -> {
                // Close any open dialogs for this channel and reopen
                showChannelInfo(channel);
            });
        } catch (Exception e) {
            logger.error("Error refreshing channel management UI", e);
        }
    }
    
    private String formatChannelDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "Unknown";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        return dateTime.format(formatter);
    }

    // ===============================
    // MEMBER MANAGEMENT METHODS
    // ===============================
    
    private void handleAddMember(GroupChat group, ListView<VBox> memberListView) {
        try {
            // Create dialog to select user to add
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Add Member to " + group.getGroupName());
            dialog.setHeaderText("Select a user to add to the group");
            
            // Create the custom dialog content
            VBox content = new VBox(10);
            content.setPadding(new Insets(20));
            
            Label instructionLabel = new Label("Enter username or search for users:");
            TextField usernameField = new TextField();
            usernameField.setPromptText("Enter username...");
            
            ListView<User> userListView = new ListView<>();
            userListView.setPrefHeight(200);
            
            // Search functionality
            usernameField.textProperty().addListener((obs, oldText, newText) -> {
                if (newText.length() > 2) {
                    searchUsersForGroup(newText, group, userListView);
                } else {
                    userListView.getItems().clear();
                }
            });
            
            // Custom cell factory for user display
            userListView.setCellFactory(param -> new ListCell<User>() {
                @Override
                protected void updateItem(User user, boolean empty) {
                    super.updateItem(user, empty);
                    if (empty || user == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(user.getProfileName() + " (@" + user.getUsername() + ")");
                    }
                }
            });
            
            content.getChildren().addAll(instructionLabel, usernameField, new Label("Search Results:"), userListView);
            dialog.getDialogPane().setContent(content);
            
            // Add buttons
            ButtonType addButtonType = new ButtonType("Add Member", ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
            
            // Enable add button only when user is selected
            Button addButton = (Button) dialog.getDialogPane().lookupButton(addButtonType);
            addButton.setDisable(true);
            
            userListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                addButton.setDisable(newSelection == null);
            });
            
            // Result converter
            dialog.setResultConverter(buttonType -> {
                if (buttonType == addButtonType) {
                    User selectedUser = userListView.getSelectionModel().getSelectedItem();
                    return selectedUser != null ? selectedUser.getUserId() : null;
                }
                return null;
            });
            
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                String userIdToAdd = result.get();
                
                // Add the member to the group
                boolean success = chatService.addMemberToGroup(group.getGroupId(), userIdToAdd);
                
                if (success) {
                    // Get the added user info
                    User addedUser = userService.getUserById(userIdToAdd);
                    User currentUser = userService.getCurrentUser();
                    
                    // Add the new member to the ListView immediately
                    VBox memberItem = createMemberItem(addedUser, group, true, currentUser);
                    memberListView.getItems().add(memberItem);
                    
                    // Update member count label immediately
                    updateGroupMemberCountLabel(group, memberListView.getParent());
                    
                    // Update chat subtitle if this is the current chat
                    if (currentChat != null && currentChat instanceof GroupChat && 
                        ((GroupChat) currentChat).getGroupId().equals(group.getGroupId())) {
                        updateChatSubtitle();
                    }
                    
                    // Update chat list to reflect new member count
                    Platform.runLater(() -> {
                        refreshChatList();
                    });
                    
                    showAlert("Success", "Member added successfully!");
                } else {
                    showAlert("Error", "Failed to add member. They might already be in the group.");
                }
            }
            
        } catch (Exception e) {
            logger.error("Error adding member to group", e);
            showErrorAlert("Error", "Failed to add member: " + e.getMessage());
        }
    }
    
    private void searchUsersForGroup(String searchText, GroupChat group, ListView<User> userListView) {
        try {
            // Get all users and filter by search text
            List<User> allUsers = userService.searchUsers(searchText);
            List<User> groupMembers = chatService.getGroupMembers(group.getGroupId());
            
            // Filter out users who are already in the group
            List<User> availableUsers = allUsers.stream()
                    .filter(user -> groupMembers.stream().noneMatch(member -> member.getUserId().equals(user.getUserId())))
                    .toList();
            
            Platform.runLater(() -> {
                userListView.getItems().clear();
                userListView.getItems().addAll(availableUsers);
            });
            
        } catch (Exception e) {
            logger.error("Error searching users for group", e);
        }
    }
    
    private void handleRemoveMember(User member, GroupChat group) {
        try {
            // Confirmation dialog
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Remove Member");
            confirmation.setHeaderText("Remove " + member.getProfileName() + " from " + group.getGroupName() + "?");
            confirmation.setContentText("This action cannot be undone.");
            
            Optional<ButtonType> result = confirmation.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                
                User currentUser = userService.getCurrentUser();
                boolean success = chatService.removeMemberFromGroup(group.getGroupId(), member.getUserId(), currentUser.getUserId());
                
                if (success) {
                    // Find and refresh all open group management windows
                    refreshGroupManagementUI(group);
                    
                    // Update chat subtitle if this is the current chat
                    if (currentChat != null && currentChat instanceof GroupChat && 
                        ((GroupChat) currentChat).getGroupId().equals(group.getGroupId())) {
                        updateChatSubtitle();
                    }
                    
                    // Update chat list to reflect new member count
                    Platform.runLater(() -> {
                        refreshChatList();
                    });
                    
                    showAlert("Success", member.getProfileName() + " has been removed from the group.");
                } else {
                    showAlert("Error", "Failed to remove member from group.");
                }
            }
            
        } catch (Exception e) {
            logger.error("Error removing member from group", e);
            showAlert("Error", "Failed to remove member: " + e.getMessage());
        }
    }
    
    private void handlePromoteMember(User member, GroupChat group) {
        try {
            // Confirmation dialog
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Promote Member");
            confirmation.setHeaderText("Make " + member.getProfileName() + " an admin?");
            confirmation.setContentText("This will give them permission to manage group members and settings.");
            
            Optional<ButtonType> result = confirmation.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                
                User currentUser = userService.getCurrentUser();
                boolean success = chatService.makeGroupAdmin(group.getGroupId(), member.getUserId(), currentUser.getUserId());
                
                if (success) {
                    showAlert("Success", member.getProfileName() + " is now an admin. Please reopen the dialog to see changes.");
                } else {
                    showErrorAlert("Error", "Failed to promote member to admin.");
                }
            }
            
        } catch (Exception e) {
            logger.error("Error promoting member", e);
            showErrorAlert("Error", "Failed to promote member: " + e.getMessage());
        }
    }
    
    // ===============================
    // CHANNEL SUBSCRIBER MANAGEMENT METHODS
    // ===============================
    
    private void handleAddSubscriber(Channel channel, ListView<VBox> subscriberListView) {
        try {
            // Create dialog to select user to add
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Add Subscriber to " + channel.getChannelName());
            dialog.setHeaderText("Select a user to add to the channel");
            
            // Create the custom dialog content
            VBox content = new VBox(10);
            content.setPadding(new Insets(20));
            
            Label instructionLabel = new Label("Enter username or search for users:");
            TextField usernameField = new TextField();
            usernameField.setPromptText("Enter username...");
            
            ListView<User> userListView = new ListView<>();
            userListView.setPrefHeight(200);
            
            // Search functionality
            usernameField.textProperty().addListener((obs, oldText, newText) -> {
                if (newText.length() > 2) {
                    searchUsersForChannel(newText, channel, userListView);
                } else {
                    userListView.getItems().clear();
                }
            });
            
            // Custom cell factory for user display
            userListView.setCellFactory(param -> new ListCell<User>() {
                @Override
                protected void updateItem(User user, boolean empty) {
                    super.updateItem(user, empty);
                    if (empty || user == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(user.getProfileName() + " (@" + user.getUsername() + ")");
                    }
                }
            });
            
            content.getChildren().addAll(instructionLabel, usernameField, new Label("Search Results:"), userListView);
            dialog.getDialogPane().setContent(content);
            
            // Add buttons
            ButtonType addButtonType = new ButtonType("Add Subscriber", ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);
            
            // Enable add button only when user is selected
            Button addButton = (Button) dialog.getDialogPane().lookupButton(addButtonType);
            addButton.setDisable(true);
            
            userListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                addButton.setDisable(newSelection == null);
            });
            
            // Result converter
            dialog.setResultConverter(buttonType -> {
                if (buttonType == addButtonType) {
                    User selectedUser = userListView.getSelectionModel().getSelectedItem();
                    return selectedUser != null ? selectedUser.getUserId() : null;
                }
                return null;
            });
            
            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                String userIdToAdd = result.get();
                
                // Add the subscriber to the channel
                boolean success = chatService.addSubscriberToChannel(channel.getChannelId(), userIdToAdd);
                
                if (success) {
                    // Get the added user info
                    User addedUser = userService.getUserById(userIdToAdd);
                    User currentUser = userService.getCurrentUser();
                    
                    // Add the new subscriber to the ListView immediately
                    VBox subscriberItem = createSubscriberItem(addedUser, channel, true, currentUser);
                    subscriberListView.getItems().add(subscriberItem);
                    
                    // Update subscriber count label immediately
                    updateChannelSubscriberCountLabel(channel, subscriberListView.getParent());
                    
                    // Update chat subtitle if this is the current chat
                    if (currentChat != null && currentChat instanceof Channel && 
                        ((Channel) currentChat).getChannelId().equals(channel.getChannelId())) {
                        updateChatSubtitle();
                    }
                    
                    // Update chat list to reflect new subscriber count
                    Platform.runLater(() -> {
                        refreshChatList();
                    });
                    
                    showAlert("Success", "Subscriber added successfully!");
                } else {
                    showAlert("Error", "Failed to add subscriber. They might already be subscribed.");
                }
            }
            
        } catch (Exception e) {
            logger.error("Error adding subscriber to channel", e);
            showErrorAlert("Error", "Failed to add subscriber: " + e.getMessage());
        }
    }
    
    private void searchUsersForChannel(String searchText, Channel channel, ListView<User> userListView) {
        try {
            // Get all users and filter by search text
            List<User> allUsers = userService.searchUsers(searchText);
            List<User> channelSubscribers = chatService.getChannelSubscribers(channel.getChannelId());
            
            // Filter out users who are already subscribed to the channel
            List<User> availableUsers = allUsers.stream()
                    .filter(user -> channelSubscribers.stream().noneMatch(subscriber -> subscriber.getUserId().equals(user.getUserId())))
                    .toList();
            
            Platform.runLater(() -> {
                userListView.getItems().clear();
                userListView.getItems().addAll(availableUsers);
            });
            
        } catch (Exception e) {
            logger.error("Error searching users for channel", e);
        }
    }
    
    private void handleRemoveSubscriber(User subscriber, Channel channel) {
        try {
            // Confirmation dialog
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Remove Subscriber");
            confirmation.setHeaderText("Remove " + subscriber.getProfileName() + " from " + channel.getChannelName() + "?");
            confirmation.setContentText("This action cannot be undone.");
            
            Optional<ButtonType> result = confirmation.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                
                User currentUser = userService.getCurrentUser();
                boolean success = chatService.removeSubscriberFromChannel(channel.getChannelId(), subscriber.getUserId(), currentUser.getUserId());
                
                if (success) {
                    // Find and refresh all open channel management windows
                    refreshChannelManagementUI(channel);
                    
                    // Update chat subtitle if this is the current chat
                    if (currentChat != null && currentChat instanceof Channel && 
                        ((Channel) currentChat).getChannelId().equals(channel.getChannelId())) {
                        updateChatSubtitle();
                    }
                    
                    // Update chat list to reflect new subscriber count
                    Platform.runLater(() -> {
                        refreshChatList();
                    });
                    
                    showAlert("Success", subscriber.getProfileName() + " has been removed from the channel.");
                } else {
                    showAlert("Error", "Failed to remove subscriber from channel.");
                }
            }
            
        } catch (Exception e) {
            logger.error("Error removing subscriber from channel", e);
            showAlert("Error", "Failed to remove subscriber: " + e.getMessage());
        }
    }
    
    private void handlePromoteSubscriber(User subscriber, Channel channel) {
        try {
            // Confirmation dialog
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Promote Subscriber");
            confirmation.setHeaderText("Make " + subscriber.getProfileName() + " an admin?");
            confirmation.setContentText("This will give them permission to manage channel subscribers and post content.");
            
            Optional<ButtonType> result = confirmation.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                
                User currentUser = userService.getCurrentUser();
                boolean success = chatService.makeChannelAdmin(channel.getChannelId(), subscriber.getUserId(), currentUser.getUserId());
                
                if (success) {
                    showAlert("Success", subscriber.getProfileName() + " is now an admin. Please reopen the dialog to see changes.");
                } else {
                    showErrorAlert("Error", "Failed to promote subscriber to admin.");
                }
            }
            
        } catch (Exception e) {
            logger.error("Error promoting subscriber", e);
            showErrorAlert("Error", "Failed to promote subscriber: " + e.getMessage());
        }
    }
    
    @Override
    public void onUserOnlineStatusChanged(String userId, boolean isOnline) {
        Platform.runLater(() -> {
            try {
                // Update online status indicators throughout the UI
                updateUserOnlineStatus(userId, isOnline);
                
                // Update chat header if this is the current chat user
                if (currentChat instanceof PrivateChat) {
                    PrivateChat privateChat = (PrivateChat) currentChat;
                    User currentUser = userService.getCurrentUser();
                    String otherUserId = privateChat.getOtherUserId(currentUser.getUserId());
                    
                    if (otherUserId.equals(userId)) {
                        updateChatHeaderOnlineStatus(userId, isOnline);
                    }
                }
                
                // Update chat list indicators
                chatListView.refresh();
                
                User user = userService.getUserById(userId);
                if (user != null) {
                    logger.info("User {} is now {}", user.getDisplayName(), isOnline ? "online" : "offline");
                }
                
            } catch (Exception e) {
                logger.error("Error handling online status change", e);
            }
        });
    }
    
    /**
     * Update user online status throughout the UI
     */
    private void updateUserOnlineStatus(String userId, boolean isOnline) {
        try {
            // Update online status indicator in chat header
            if (onlineStatusIndicator != null && currentChat instanceof PrivateChat) {
                PrivateChat privateChat = (PrivateChat) currentChat;
                User currentUser = userService.getCurrentUser();
                String otherUserId = privateChat.getOtherUserId(currentUser.getUserId());
                
                if (otherUserId.equals(userId)) {
                    onlineStatusIndicator.setFill(isOnline ? Color.GREEN : Color.GRAY);
                    onlineStatusIndicator.setVisible(true);
                }
            }
            
            // Update typing indicator area with last seen info
            if (typingIndicatorLabel != null && !typingIndicatorLabel.isVisible()) {
                updateLastSeenDisplay(userId, isOnline);
            }
            
        } catch (Exception e) {
            logger.error("Error updating user online status in UI", e);
        }
    }
    
    /**
     * Update chat header online status
     */
    private void updateChatHeaderOnlineStatus(String userId, boolean isOnline) {
        try {
            if (chatSubtitleLabel != null) {
                User user = userService.getUserById(userId);
                if (user != null) {
                    String statusText;
                    String statusColor;
                    
                    if (isOnline) {
                        statusText = "🟢 Online";
                        statusColor = "#4CAF50";
                    } else {
                        // Show last seen time
                        statusText = getLastSeenText(user);
                        statusColor = "#757575";
                    }
                    
                    chatSubtitleLabel.setText(statusText);
                    chatSubtitleLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + statusColor + ";");
                }
            }
        } catch (Exception e) {
            logger.error("Error updating chat header online status", e);
        }
    }
    
    /**
     * Get last seen text for user
     */
    private String getLastSeenText(User user) {
        try {
            // Get last seen time from user service or message broker
            String lastSeenText = userService.getLastSeenTime(user.getUserId());
            return lastSeenText;
        } catch (Exception e) {
            logger.error("Error getting last seen text", e);
            return "Last seen recently";
        }
    }
    
    /**
     * Format last seen time
     */
    private String formatLastSeen(LocalDateTime lastSeen) {
        try {
            LocalDateTime now = LocalDateTime.now();
            long minutesAgo = java.time.Duration.between(lastSeen, now).toMinutes();
            
            if (minutesAgo < 1) {
                return "Last seen just now";
            } else if (minutesAgo < 60) {
                return "Last seen " + minutesAgo + " minute" + (minutesAgo == 1 ? "" : "s") + " ago";
            } else if (minutesAgo < 24 * 60) {
                long hoursAgo = minutesAgo / 60;
                return "Last seen " + hoursAgo + " hour" + (hoursAgo == 1 ? "" : "s") + " ago";
            } else {
                long daysAgo = minutesAgo / (24 * 60);
                if (daysAgo == 1) {
                    return "Last seen yesterday";
                } else if (daysAgo < 7) {
                    return "Last seen " + daysAgo + " days ago";
                } else {
                    return "Last seen a long time ago";
                }
            }
        } catch (Exception e) {
            logger.error("Error formatting last seen time", e);
            return "Last seen recently";
        }
    }
    
    /**
     * Update last seen display in typing indicator area
     */
    private void updateLastSeenDisplay(String userId, boolean isOnline) {
        try {
            if (currentChat instanceof PrivateChat) {
                PrivateChat privateChat = (PrivateChat) currentChat;
                User currentUser = userService.getCurrentUser();
                String otherUserId = privateChat.getOtherUserId(currentUser.getUserId());
                
                if (otherUserId.equals(userId) && typingIndicatorLabel != null) {
                    if (isOnline) {
                        typingIndicatorLabel.setText("🟢 Online");
                        typingIndicatorLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-size: 12px;");
                    } else {
                        User user = userService.getUserById(userId);
                        if (user != null) {
                            typingIndicatorLabel.setText(getLastSeenText(user));
                            typingIndicatorLabel.setStyle("-fx-text-fill: #757575; -fx-font-size: 12px;");
                        }
                    }
                    typingIndicatorLabel.setVisible(true);
                }
            }
        } catch (Exception e) {
            logger.error("Error updating last seen display", e);
        }
    }

    private void updateChatHeader(Object chat) {
        try {
            logger.info("Updating chat header for chat: {}", chat != null ? chat.getClass().getSimpleName() : "null");
            
            if (chat == null) {
                chatTitleLabel.setText("");
                chatSubtitleLabel.setText("");
                if (onlineStatusIndicator != null) {
                    onlineStatusIndicator.setVisible(false);
                }
                if (chatProfileImageView != null) {
                    chatProfileImageView.setImage(null);
                    chatProfileImageView.setVisible(false);
                }
                return;
            }

            String chatName = getChatDisplayName(chat);
            chatTitleLabel.setText(chatName);
            logger.info("Set chat title to: {}", chatName);

            if (chat instanceof PrivateChat) {
                PrivateChat privateChat = (PrivateChat) chat;
                String otherUserId = privateChat.getParticipantIds().stream()
                    .filter(id -> !id.equals(userService.getCurrentUser().getUserId()))
                    .findFirst()
                    .orElse(null);

                logger.info("Private chat with user ID: {}", otherUserId);

                if (otherUserId != null) {
                    User otherUser = userService.findUserById(otherUserId);
                    logger.info("Found other user: {}, profile path: {}", 
                        otherUser != null ? otherUser.getUsername() : "null",
                        otherUser != null ? otherUser.getProfilePicturePath() : "null");
                    
                    if (otherUser != null) {
                        // Set subtitle based on online status
                        boolean isOnline = LocalMessageBroker.getInstance().isUserOnline(otherUserId);
                        if (isOnline) {
                            chatSubtitleLabel.setText("online");
                            chatSubtitleLabel.setStyle("-fx-text-fill: #4CAF50;"); // Green for online
                        } else {
                            chatSubtitleLabel.setText("last seen recently");
                            chatSubtitleLabel.setStyle("-fx-text-fill: #9E9E9E;"); // Gray for offline
                        }

                        // Show/hide online status indicator
                        if (onlineStatusIndicator != null) {
                            onlineStatusIndicator.setVisible(isOnline);
                            if (isOnline) {
                                onlineStatusIndicator.setStyle("-fx-fill: #4CAF50;"); // Green dot
                            }
                        }

                        // Update profile image
                        logger.info("Calling updateChatProfileImage with path: {}", otherUser.getProfilePicturePath());
                        updateChatProfileImage(otherUser.getProfilePicturePath());
                    }
                }
            } else if (chat instanceof GroupChat) {
                GroupChat groupChat = (GroupChat) chat;
                int memberCount = groupChat.getMemberIds().size();
                int onlineCount = (int) groupChat.getMemberIds().stream()
                    .filter(LocalMessageBroker.getInstance()::isUserOnline)
                    .count();
                
                chatSubtitleLabel.setText(memberCount + " members" + 
                    (onlineCount > 0 ? ", " + onlineCount + " online" : ""));
                chatSubtitleLabel.setStyle("-fx-text-fill: #9E9E9E;");

                // Hide online indicator for groups
                if (onlineStatusIndicator != null) {
                    onlineStatusIndicator.setVisible(false);
                }

                // Update group profile image
                updateChatProfileImage(groupChat.getProfilePicturePath());
            } else if (chat instanceof Channel) {
                Channel channel = (Channel) chat;
                chatSubtitleLabel.setText("channel");
                chatSubtitleLabel.setStyle("-fx-text-fill: #9E9E9E;");

                // Hide online indicator for channels
                if (onlineStatusIndicator != null) {
                    onlineStatusIndicator.setVisible(false);
                }

                // Update channel profile image
                updateChatProfileImage(channel.getProfilePicturePath());
            }

        } catch (Exception e) {
            logger.error("Error updating chat header", e);
        }
    }

    private void updateChatProfileImage(String imagePath) {
        try {
            if (chatProfileImageView != null) {
                if (imagePath != null && !imagePath.isEmpty()) {
                    // Decode URL-encoded path (fix %20 to spaces, etc.)
                    String decodedPath = java.net.URLDecoder.decode(imagePath, "UTF-8");
                    File imageFile = new File(decodedPath);
                    logger.info("Loading chat profile image: {} -> {}, exists: {}", imagePath, decodedPath, imageFile.exists());
                    
                    if (imageFile.exists()) {
                        Image image = new Image(imageFile.toURI().toString());
                        
                        // Set the image with proper sizing
                        chatProfileImageView.setImage(image);
                        chatProfileImageView.setFitWidth(40);
                        chatProfileImageView.setFitHeight(40);
                        chatProfileImageView.setPreserveRatio(true);
                        
                        // Create circular clip for chat profile picture
                        Circle chatClip = new Circle(20, 20, 20);
                        chatProfileImageView.setClip(chatClip);
                        
                        chatProfileImageView.setVisible(true);
                        logger.info("Successfully loaded profile image: {}", decodedPath);
                        return;
                    } else {
                        logger.warn("Profile image file does not exist: {}", decodedPath);
                    }
                } else {
                    logger.info("No profile image path provided, using default");
                }
                
                // Set default profile image or hide
                try {
                    // Try to load a default profile image from resources
                    Image defaultImage = new Image(getClass().getResourceAsStream("/images/default-profile.png"));
                    chatProfileImageView.setImage(defaultImage);
                    chatProfileImageView.setFitWidth(40);
                    chatProfileImageView.setFitHeight(40);
                    chatProfileImageView.setPreserveRatio(true);
                    Circle defaultClip = new Circle(20, 20, 20);
                    chatProfileImageView.setClip(defaultClip);
                    chatProfileImageView.setVisible(true);
                    logger.info("Loaded default profile image");
                } catch (Exception e) {
                    logger.warn("Could not load default profile image, hiding image view: {}", e.getMessage());
                    chatProfileImageView.setImage(null);
                    chatProfileImageView.setVisible(false);
                }
            }
        } catch (Exception e) {
            logger.error("Error updating chat profile image: {}", e.getMessage(), e);
            if (chatProfileImageView != null) {
                chatProfileImageView.setImage(null);
                chatProfileImageView.setVisible(false);
            }
        }
    }
    
    private void updateChatOptionsMenu(Object chat) {
        try {
            if (chatOptionsMenuButton == null) {
                logger.warn("Chat options menu button is null, cannot update menu");
                return;
            }
            
            // Clear existing menu items
            chatOptionsMenuButton.getItems().clear();
            
            if (chat == null) {
                chatOptionsMenuButton.setVisible(false);
                return;
            }
            
            chatOptionsMenuButton.setVisible(true);
            User currentUser = userService.getCurrentUser();
            
            if (chat instanceof PrivateChat) {
                // Standard menu for private chats
                addMenuItem("Search in Chat", this::handleSearchInCurrentChat);
                addMenuItem("Media Gallery", this::handleShowMediaGallery);
                addSeparatorMenuItem();
                addMenuItem("View Profile", this::handleChatHeaderClick);
                addMenuItem("Mute Notifications", null); // TODO: implement
                addSeparatorMenuItem();
                addMenuItem("Clear History", null); // TODO: implement  
                addMenuItem("Delete Chat", null); // TODO: implement
                
            } else if (chat instanceof GroupChat) {
                GroupChat groupChat = (GroupChat) chat;
                boolean isAdmin = groupChat.isAdmin(currentUser.getUserId());
                boolean isCreator = groupChat.getCreatorId().equals(currentUser.getUserId());
                
                // Common menu items for all group members
                addMenuItem("Search in Chat", this::handleSearchInCurrentChat);
                addMenuItem("Media Gallery", this::handleShowMediaGallery);
                addSeparatorMenuItem();
                addMenuItem("Group Info", this::handleChatHeaderClick);
                
                if (isAdmin || isCreator) {
                    // Admin-only options
                    addMenuItem("Manage Members", null); // TODO: implement
                    addMenuItem("Group Settings", null); // TODO: implement
                    addSeparatorMenuItem();
                }
                
                addMenuItem("Mute Notifications", null); // TODO: implement
                
                if (isCreator) {
                    addMenuItem("Delete Group", null); // TODO: implement
                } else {
                    addMenuItem("Leave Group", null); // TODO: implement
                }
                
            } else if (chat instanceof Channel) {
                Channel channel = (Channel) chat;
                boolean isAdmin = channel.isAdmin(currentUser.getUserId());
                boolean isOwner = channel.getOwnerId().equals(currentUser.getUserId());
                
                // Common menu items for all channel subscribers
                addMenuItem("Search in Channel", this::handleSearchInCurrentChat);
                addMenuItem("Media Gallery", this::handleShowMediaGallery);
                addSeparatorMenuItem();
                addMenuItem("Channel Info", this::handleChatHeaderClick);
                
                if (isAdmin || isOwner) {
                    // Admin-only options
                    addMenuItem("Manage Subscribers", null); // TODO: implement
                    addMenuItem("Channel Settings", null); // TODO: implement
                    addSeparatorMenuItem();
                }
                
                addMenuItem("Mute Notifications", null); // TODO: implement
                
                if (isOwner) {
                    addMenuItem("Delete Channel", null); // TODO: implement
                } else {
                    addMenuItem("Unsubscribe", null); // TODO: implement
                }
            }
            
            logger.info("Updated chat options menu for {} with {} items", 
                chat.getClass().getSimpleName(), chatOptionsMenuButton.getItems().size());
                
        } catch (Exception e) {
            logger.error("Error updating chat options menu", e);
        }
    }
    
    private void addMenuItem(String text, Runnable action) {
        MenuItem menuItem = new MenuItem(text);
        if (action != null) {
            menuItem.setOnAction(e -> action.run());
        } else {
            // Disable items that don't have implementation yet
            menuItem.setDisable(true);
            menuItem.setStyle("-fx-text-fill: #9E9E9E;");
        }
        chatOptionsMenuButton.getItems().add(menuItem);
    }
    
    private void addSeparatorMenuItem() {
        chatOptionsMenuButton.getItems().add(new SeparatorMenuItem());
    }


}
