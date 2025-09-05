package com.telegram.ui;

import com.telegram.models.Message;
import com.telegram.utils.MediaHandler;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Enhanced media component for displaying different types of media in messages
 */
public class MediaMessageComponent {
    private static final Logger logger = LoggerFactory.getLogger(MediaMessageComponent.class);
    
    private final Message message;
    private final boolean isFromCurrentUser;
    private final Stage parentStage;
    private VoicePlayer voicePlayer;
    
    public MediaMessageComponent(Message message, boolean isFromCurrentUser, Stage parentStage) {
        this.message = message;
        this.isFromCurrentUser = isFromCurrentUser;
        this.parentStage = parentStage;
        this.voicePlayer = new VoicePlayer();
    }
    
    /**
     * Create media component based on message type
     */
    public VBox createMediaComponent() {
        if (message.getMediaPath() == null || message.getMediaPath().isEmpty()) {
            return null;
        }
        
        String extension = MediaHandler.getFileExtension(new File(message.getMediaPath()).getName());
        
        if (MediaHandler.isImageFile(extension)) {
            return createImageComponent();
        } else if (MediaHandler.isVideoFile(extension)) {
            return createVideoComponent();
        } else if (MediaHandler.isAudioFile(extension) || message.getType() == Message.MessageType.VOICE) {
            return createAudioComponent();
        } else {
            return createFileComponent();
        }
    }
    
    /**
     * Create enhanced image preview component
     */
    private VBox createImageComponent() {
        VBox imageContainer = new VBox(5);
        imageContainer.setAlignment(Pos.CENTER);
        
        try {
            File imageFile = new File(message.getMediaPath());
            if (!imageFile.exists()) {
                return createErrorComponent("Image not found");
            }
            
            Image image = new Image(imageFile.toURI().toString());
            ImageView imageView = new ImageView(image);
            
            // Calculate appropriate size while maintaining aspect ratio
            double maxWidth = 300;
            double maxHeight = 200;
            
            double imageWidth = image.getWidth();
            double imageHeight = image.getHeight();
            
            if (imageWidth > maxWidth || imageHeight > maxHeight) {
                double widthRatio = maxWidth / imageWidth;
                double heightRatio = maxHeight / imageHeight;
                double ratio = Math.min(widthRatio, heightRatio);
                
                imageView.setFitWidth(imageWidth * ratio);
                imageView.setFitHeight(imageHeight * ratio);
            } else {
                imageView.setFitWidth(imageWidth);
                imageView.setFitHeight(imageHeight);
            }
            
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            
            // Add rounded corners
            Rectangle clip = new Rectangle(imageView.getFitWidth(), imageView.getFitHeight());
            clip.setArcWidth(10);
            clip.setArcHeight(10);
            imageView.setClip(clip);
            
            // Add styling
            imageView.setStyle("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 2);");
            
            // Click to open full-screen viewer
            imageView.setOnMouseClicked(e -> {
                e.consume(); // Prevent event bubbling to parent message
                MediaHandler.showImageViewer(message.getMediaPath(), parentStage);
            });
            
            // Hover effect
            imageView.setOnMouseEntered(e -> imageView.setOpacity(0.9));
            imageView.setOnMouseExited(e -> imageView.setOpacity(1.0));
            
            imageContainer.getChildren().add(imageView);
            
            // Add image info
            File file = new File(message.getMediaPath());
            String fileSize = MediaHandler.formatFileSize(file.length());
            Label infoLabel = new Label(file.getName() + " • " + fileSize);
            infoLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #888; -fx-font-style: italic;");
            imageContainer.getChildren().add(infoLabel);
            
            // Add click hint
            Label hintLabel = new Label("Click to view full size");
            hintLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #aaa; -fx-font-style: italic;");
            imageContainer.getChildren().add(hintLabel);
            
        } catch (Exception e) {
            logger.error("Error creating image component", e);
            return createErrorComponent("Failed to load image");
        }
        
        return imageContainer;
    }
    
    /**
     * Create video preview component
     */
    private VBox createVideoComponent() {
        VBox videoContainer = new VBox(8);
        videoContainer.setAlignment(Pos.CENTER_LEFT);
        videoContainer.setPadding(new Insets(10));
        videoContainer.setStyle("-fx-background-color: rgba(0,0,0,0.1); -fx-background-radius: 8px;");
        
        File videoFile = new File(message.getMediaPath());
        
        // Video icon and info
        HBox videoHeader = new HBox(10);
        videoHeader.setAlignment(Pos.CENTER_LEFT);
        
        Label videoIcon = new Label("[VID]");
        videoIcon.setStyle("-fx-font-size: 24px;");
        
        VBox videoInfo = new VBox(2);
        Label nameLabel = new Label(videoFile.getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        
        Label sizeLabel = new Label(MediaHandler.formatFileSize(videoFile.length()));
        sizeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        
        videoInfo.getChildren().addAll(nameLabel, sizeLabel);
        videoHeader.getChildren().addAll(videoIcon, videoInfo);
        
        // Play button
        HBox controlsBox = new HBox(10);
        controlsBox.setAlignment(Pos.CENTER_LEFT);
        
        Label playButton = new Label("Play Video");
        playButton.setStyle("-fx-background-color: #007AFF; -fx-text-fill: white; " +
                          "-fx-padding: 8px 16px; -fx-background-radius: 5px; -fx-cursor: hand;");
        
        playButton.setOnMouseClicked(e -> openVideoPlayer());
        playButton.setOnMouseEntered(e -> playButton.setStyle(
            "-fx-background-color: #0056CC; -fx-text-fill: white; " +
            "-fx-padding: 8px 16px; -fx-background-radius: 5px; -fx-cursor: hand;"));
        playButton.setOnMouseExited(e -> playButton.setStyle(
            "-fx-background-color: #007AFF; -fx-text-fill: white; " +
            "-fx-padding: 8px 16px; -fx-background-radius: 5px; -fx-cursor: hand;"));
        
        controlsBox.getChildren().add(playButton);
        
        videoContainer.getChildren().addAll(videoHeader, controlsBox);
        return videoContainer;
    }
    
    /**
     * Create audio preview component
     */
    private VBox createAudioComponent() {
        VBox audioContainer = new VBox(8);
        audioContainer.setAlignment(Pos.CENTER_LEFT);
        audioContainer.setPadding(new Insets(12));
        
        boolean isVoiceMessage = message.getType() == Message.MessageType.VOICE;
        
        // Enhanced styling for voice messages
        if (isVoiceMessage) {
            String backgroundColor = isFromCurrentUser ? "rgba(255,255,255,0.15)" : "rgba(70,130,180,0.08)";
            audioContainer.setStyle("-fx-background-color: " + backgroundColor + "; -fx-background-radius: 12px; " +
                                  "-fx-border-color: rgba(70,130,180,0.2); -fx-border-width: 1px; -fx-border-radius: 12px;");
        } else {
            audioContainer.setStyle("-fx-background-color: rgba(138,43,226,0.1); -fx-background-radius: 8px;");
        }
        
        File audioFile = new File(message.getMediaPath());
        
        // Audio header with enhanced voice message styling
        HBox audioHeader = new HBox(12);
        audioHeader.setAlignment(Pos.CENTER_LEFT);
        
        // Use the play circle icon for voice messages
        if (isVoiceMessage) {
            try {
                ImageView playIcon = new ImageView();
                Image iconImage = new Image(getClass().getResourceAsStream("/icons/play_circle_24dp_E3E3E3_FILL0_wght400_GRAD0_opsz24.png"));
                playIcon.setImage(iconImage);
                playIcon.setFitWidth(32);
                playIcon.setFitHeight(32);
                playIcon.setPreserveRatio(true);
                
                // Add subtle hover effect
                playIcon.setOnMouseEntered(e -> playIcon.setOpacity(0.8));
                playIcon.setOnMouseExited(e -> playIcon.setOpacity(1.0));
                
                VBox audioInfo = new VBox(3);
                Label nameLabel = new Label("Voice Message");
                nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; " + 
                                (isFromCurrentUser ? "-fx-text-fill: rgba(255,255,255,0.95);" : "-fx-text-fill: #2c3e50;"));
                
                String duration = VoiceRecorder.formatDuration(0);
                String fileSize = MediaHandler.formatFileSize(audioFile.length());
                Label infoLabel = new Label(duration + " • " + fileSize);
                infoLabel.setStyle("-fx-font-size: 12px; " + 
                                (isFromCurrentUser ? "-fx-text-fill: rgba(255,255,255,0.8);" : "-fx-text-fill: #7f8c8d;"));
                
                audioInfo.getChildren().addAll(nameLabel, infoLabel);
                audioHeader.getChildren().addAll(playIcon, audioInfo);
                
                // Enhanced controls for voice messages
                HBox controlsBox = new HBox(12);
                controlsBox.setAlignment(Pos.CENTER_LEFT);
                controlsBox.setPadding(new Insets(5, 0, 0, 44)); // Align with text
                
                // Custom styled progress bar for voice messages
                ProgressBar progressBar = new ProgressBar(0);
                progressBar.setPrefWidth(140);
                progressBar.setPrefHeight(4);
                progressBar.setStyle("-fx-accent: " + (isFromCurrentUser ? "#ffffff" : "#4682B4") + "; " +
                                   "-fx-background-color: " + (isFromCurrentUser ? "rgba(255,255,255,0.3)" : "rgba(70,130,180,0.2)") + "; " +
                                   "-fx-background-radius: 2px;");
                
                Label timeLabel = new Label("0:00");
                timeLabel.setStyle("-fx-font-size: 11px; -fx-font-family: 'SF Pro Text', -apple-system, system-ui; " + 
                                 (isFromCurrentUser ? "-fx-text-fill: rgba(255,255,255,0.8);" : "-fx-text-fill: #95a5a6;"));
                
                // Add click handler for voice message playback
                playIcon.setOnMouseClicked(e -> playVoiceMessage(playIcon, progressBar, timeLabel));
                
                controlsBox.getChildren().addAll(progressBar, timeLabel);
                audioContainer.getChildren().addAll(audioHeader, controlsBox);
                
            } catch (Exception e) {
                logger.warn("Could not load play circle icon, falling back to text icon");
                // Fallback to original implementation
                return createOriginalAudioComponent(audioContainer, audioFile, isVoiceMessage);
            }
        } else {
            // Regular audio file styling (unchanged)
            String iconText = "[AUD]";
            Label audioIcon = new Label(iconText);
            audioIcon.setStyle("-fx-font-size: 24px;");
            
            VBox audioInfo = new VBox(2);
            Label nameLabel = new Label(audioFile.getName());
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
            
            String infoText = MediaHandler.formatFileSize(audioFile.length());
            Label sizeLabel = new Label(infoText);
            sizeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
            
            audioInfo.getChildren().addAll(nameLabel, sizeLabel);
            audioHeader.getChildren().addAll(audioIcon, audioInfo);
            
            // Audio controls
            HBox controlsBox = new HBox(10);
            controlsBox.setAlignment(Pos.CENTER_LEFT);
            
            Label playButton = new Label("Play");
            playButton.setStyle("-fx-background-color: #8A2BE2; -fx-text-fill: white; " +
                              "-fx-padding: 6px 12px; -fx-background-radius: 20px; -fx-cursor: hand;");
            
            ProgressBar progressBar = new ProgressBar(0);
            progressBar.setPrefWidth(150);
            
            Label timeLabel = new Label("0:00");
            timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
            
            playButton.setOnMouseClicked(e -> playAudio(playButton, progressBar, timeLabel));
            
            controlsBox.getChildren().addAll(playButton, progressBar, timeLabel);
            audioContainer.getChildren().addAll(audioHeader, controlsBox);
        }
        
        return audioContainer;
    }
    
    /**
     * Fallback method for original audio component styling
     */
    private VBox createOriginalAudioComponent(VBox audioContainer, File audioFile, boolean isVoiceMessage) {
        // Audio icon and info
        HBox audioHeader = new HBox(10);
        audioHeader.setAlignment(Pos.CENTER_LEFT);
        
        String iconText = isVoiceMessage ? "[MIC]" : "[AUD]";
        Label audioIcon = new Label(iconText);
        audioIcon.setStyle("-fx-font-size: 24px;");
        
        VBox audioInfo = new VBox(2);
        String nameText = isVoiceMessage ? "Voice Message" : audioFile.getName();
        Label nameLabel = new Label(nameText);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        
        String infoText = isVoiceMessage ? 
            VoiceRecorder.formatDuration(0) + " • " + MediaHandler.formatFileSize(audioFile.length()) :
            MediaHandler.formatFileSize(audioFile.length());
        Label sizeLabel = new Label(infoText);
        sizeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        
        audioInfo.getChildren().addAll(nameLabel, sizeLabel);
        audioHeader.getChildren().addAll(audioIcon, audioInfo);
        
        // Audio controls
        HBox controlsBox = new HBox(10);
        controlsBox.setAlignment(Pos.CENTER_LEFT);
        
        Label playButton = new Label("Play");
        String buttonColor = isVoiceMessage ? "#4682B4" : "#8A2BE2";
        playButton.setStyle("-fx-background-color: " + buttonColor + "; -fx-text-fill: white; " +
                          "-fx-padding: 6px 12px; -fx-background-radius: 20px; -fx-cursor: hand;");
        
        // Progress bar (placeholder)
        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(isVoiceMessage ? 120 : 150);
        
        Label timeLabel = new Label("0:00");
        timeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        
        // Add click handler for voice message playback
        if (isVoiceMessage) {
            playButton.setOnMouseClicked(e -> playVoiceMessage(playButton, progressBar, timeLabel));
        } else {
            playButton.setOnMouseClicked(e -> playAudio(playButton, progressBar, timeLabel));
        }
        
        controlsBox.getChildren().addAll(playButton, progressBar, timeLabel);
        
        audioContainer.getChildren().addAll(audioHeader, controlsBox);
        return audioContainer;
    }
    
    /**
     * Create generic file component
     */
    private VBox createFileComponent() {
        VBox fileContainer = new VBox(5);
        fileContainer.setAlignment(Pos.CENTER_LEFT);
        fileContainer.setPadding(new Insets(10));
        fileContainer.setStyle("-fx-background-color: rgba(0,0,0,0.05); -fx-background-radius: 8px; -fx-cursor: hand;");
        
        File file = new File(message.getMediaPath());
        
        HBox fileHeader = new HBox(10);
        fileHeader.setAlignment(Pos.CENTER_LEFT);
        
        // File icon
        String fileIcon = MediaHandler.getFileIcon(file.getName());
        Label iconLabel = new Label(fileIcon);
        iconLabel.setStyle("-fx-font-size: 24px;");
        
        // File info
        VBox fileInfo = new VBox(2);
        Label nameLabel = new Label(file.getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        
        String extension = MediaHandler.getFileExtension(file.getName()).toUpperCase();
        String sizeInfo = extension + " • " + MediaHandler.formatFileSize(file.length());
        Label infoLabel = new Label(sizeInfo);
        infoLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        
        fileInfo.getChildren().addAll(nameLabel, infoLabel);
        fileHeader.getChildren().addAll(iconLabel, fileInfo);
        
        // Download/Open action
        Label actionLabel = new Label("Download");
        actionLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #007AFF; -fx-cursor: hand;");
        actionLabel.setOnMouseClicked(e -> downloadFile());
        
        fileContainer.getChildren().addAll(fileHeader, actionLabel);
        
        // Click to open
        fileContainer.setOnMouseClicked(e -> {
            try {
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().open(file);
                }
            } catch (Exception ex) {
                logger.error("Error opening file", ex);
            }
        });
        
        // Hover effect
        fileContainer.setOnMouseEntered(e -> 
            fileContainer.setStyle("-fx-background-color: rgba(0,0,0,0.1); -fx-background-radius: 8px; -fx-cursor: hand;"));
        fileContainer.setOnMouseExited(e -> 
            fileContainer.setStyle("-fx-background-color: rgba(0,0,0,0.05); -fx-background-radius: 8px; -fx-cursor: hand;"));
        
        return fileContainer;
    }
    
    /**
     * Create error component for failed media
     */
    private VBox createErrorComponent(String errorMessage) {
        VBox errorContainer = new VBox(5);
        errorContainer.setAlignment(Pos.CENTER);
        errorContainer.setPadding(new Insets(15));
        errorContainer.setStyle("-fx-background-color: rgba(255,0,0,0.1); -fx-background-radius: 8px;");
        
        Label errorIcon = new Label("ERROR");
        errorIcon.setStyle("-fx-font-size: 20px;");
        
        Label errorLabel = new Label(errorMessage);
        errorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #cc0000;");
        
        errorContainer.getChildren().addAll(errorIcon, errorLabel);
        return errorContainer;
    }
    
    /**
     * Open video player (placeholder)
     */
    private void openVideoPlayer() {
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(new File(message.getMediaPath()));
            }
        } catch (Exception e) {
            logger.error("Error opening video player", e);
        }
    }
    
    /**
     * Play voice message with real audio playback
     */
    private void playVoiceMessage(Label playButton, ProgressBar progressBar, Label timeLabel) {
        try {
            if (voicePlayer.isPlaying()) {
                // Stop if already playing
                voicePlayer.stopPlayback();
                playButton.setText("Play");
                progressBar.setProgress(0);
                timeLabel.setText("0:00");
            } else {
                // Start playing
                playButton.setText("Pause");
                progressBar.setVisible(true);
                
                voicePlayer.playVoiceMessage(
                    message.getMediaPath(),
                    currentTime -> {
                        // Update progress on JavaFX thread
                        Platform.runLater(() -> {
                            int totalDuration = voicePlayer.getTotalDurationSeconds();
                            if (totalDuration > 0) {
                                progressBar.setProgress((double) currentTime / totalDuration);
                            }
                            timeLabel.setText(VoicePlayer.formatTime(currentTime));
                        });
                    },
                    isPlaying -> {
                        // Update play/pause button on JavaFX thread
                        Platform.runLater(() -> {
                            if (!isPlaying) {
                                playButton.setText(">");
                                progressBar.setProgress(0);
                                timeLabel.setText("0:00");
                            }
                        });
                    }
                );
            }
        } catch (Exception e) {
            logger.error("Error playing voice message", e);
            playButton.setText("X");
            timeLabel.setText("Error");
        }
    }
    
    /**
     * Overloaded method for ImageView play button (enhanced voice messages)
     */
    private void playVoiceMessage(ImageView playIcon, ProgressBar progressBar, Label timeLabel) {
        try {
            if (voicePlayer.isPlaying()) {
                // Stop if already playing - change back to play icon
                voicePlayer.stopPlayback();
                try {
                    Image playImage = new Image(getClass().getResourceAsStream("/icons/play_circle_24dp_E3E3E3_FILL0_wght400_GRAD0_opsz24.png"));
                    playIcon.setImage(playImage);
                    playIcon.setOpacity(1.0);
                    playIcon.setStyle(""); // Remove any effects
                } catch (Exception e) {
                    logger.warn("Could not load play icon");
                }
                progressBar.setProgress(0);
                timeLabel.setText("0:00");
            } else {
                // Start playing - change to pause icon
                progressBar.setVisible(true);
                
                // Try to load pause icon first, fallback to visual effects if not available
                try {
                    Image pauseImage = new Image(getClass().getResourceAsStream("/icons/pause_circle_24dp_E3E3E3_FILL0_wght400_GRAD0_opsz24.png"));
                    playIcon.setImage(pauseImage);
                    playIcon.setOpacity(1.0);
                    playIcon.setStyle("");
                } catch (Exception e) {
                    logger.warn("Could not load pause icon, using visual effects instead");
                    // Fallback to visual effects if pause icon is not available
                    playIcon.setOpacity(0.8);
                    playIcon.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,122,255,0.4), 4, 0, 0, 0);");
                }
                
                voicePlayer.playVoiceMessage(
                    message.getMediaPath(),
                    currentTime -> {
                        // Update progress on JavaFX thread
                        Platform.runLater(() -> {
                            int totalDuration = voicePlayer.getTotalDurationSeconds();
                            if (totalDuration > 0) {
                                progressBar.setProgress((double) currentTime / totalDuration);
                            }
                            timeLabel.setText(VoicePlayer.formatTime(currentTime));
                        });
                    },
                    isPlaying -> {
                        // Update play icon on JavaFX thread when playback finishes
                        Platform.runLater(() -> {
                            if (!isPlaying) {
                                try {
                                    Image playImage = new Image(getClass().getResourceAsStream("/icons/play_circle_24dp_E3E3E3_FILL0_wght400_GRAD0_opsz24.png"));
                                    playIcon.setImage(playImage);
                                } catch (Exception e) {
                                    logger.warn("Could not load play icon");
                                }
                                playIcon.setOpacity(1.0);
                                playIcon.setStyle("");
                                progressBar.setProgress(0);
                                timeLabel.setText("0:00");
                            }
                        });
                    }
                );
            }
        } catch (Exception e) {
            logger.error("Error playing voice message", e);
            playIcon.setOpacity(0.5); // Error state visual
            playIcon.setStyle("-fx-effect: dropshadow(gaussian, rgba(255,0,0,0.3), 4, 0, 0, 0);");
            timeLabel.setText("Error");
        }
    }
    
    /**
     * Play audio (placeholder - can be enhanced similarly to voice messages)
     */
    private void playAudio(Label playButton, ProgressBar progressBar, Label timeLabel) {
        // TODO: Implement actual audio playback similar to voice messages
        playButton.setText("||");
        progressBar.setVisible(true);
        progressBar.setProgress(0.3); // Placeholder progress
        timeLabel.setText("1:23");
        
        // Simulate playing
        playButton.setOnMouseClicked(e -> {
            playButton.setText("> ");
            progressBar.setVisible(false);
            timeLabel.setText("0:00");
            playButton.setOnMouseClicked(ev -> playAudio(playButton, progressBar, timeLabel));
        });
    }
    
    /**
     * Download file to user's preferred location
     */
    private void downloadFile() {
        // TODO: Implement file download/save as functionality
        logger.info("Download requested for file: {}", message.getMediaPath());
    }
}
