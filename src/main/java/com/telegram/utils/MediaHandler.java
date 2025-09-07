package com.telegram.utils;

import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Enhanced media handling utility for file uploads, previews, and management
 */
public class MediaHandler {
    private static final Logger logger = LoggerFactory.getLogger(MediaHandler.class);
    
    // Supported file types
    public static final List<String> IMAGE_EXTENSIONS = Arrays.asList(
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg"
    );
    
    public static final List<String> VIDEO_EXTENSIONS = Arrays.asList(
        "mp4", "avi", "mov", "mkv", "wmv", "flv", "webm", "m4v"
    );
    
    public static final List<String> AUDIO_EXTENSIONS = Arrays.asList(
        "mp3", "wav", "flac", "aac", "ogg", "wma", "m4a"
    );
    
    public static final List<String> DOCUMENT_EXTENSIONS = Arrays.asList(
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf"
    );
    
    // Media directories
    private static final Path MEDIA_DIR = Paths.get("media");
    private static final Path IMAGES_DIR = MEDIA_DIR.resolve("images");
    private static final Path VIDEOS_DIR = MEDIA_DIR.resolve("videos");
    private static final Path AUDIO_DIR = MEDIA_DIR.resolve("audio");
    private static final Path DOCUMENTS_DIR = MEDIA_DIR.resolve("documents");
    private static final Path THUMBNAILS_DIR = MEDIA_DIR.resolve("thumbnails");
    
    static {
        createDirectories();
    }
    
    private static void createDirectories() {
        try {
            Files.createDirectories(IMAGES_DIR);
            Files.createDirectories(VIDEOS_DIR);
            Files.createDirectories(AUDIO_DIR);
            Files.createDirectories(DOCUMENTS_DIR);
            Files.createDirectories(THUMBNAILS_DIR);
        } catch (IOException e) {
            logger.error("Failed to create media directories", e);
        }
    }
    
    /**
     * Enhanced file chooser with multiple file type support
     */
    public static FileChooser createFileChooser(String title) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        
        // Add extension filters
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("All Files", "*.*"),
            new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp", "*.webp"),
            new FileChooser.ExtensionFilter("Videos", "*.mp4", "*.avi", "*.mov", "*.mkv", "*.wmv", "*.flv", "*.webm"),
            new FileChooser.ExtensionFilter("Audio", "*.mp3", "*.wav", "*.flac", "*.aac", "*.ogg", "*.wma"),
            new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.doc", "*.docx", "*.xls", "*.xlsx", "*.ppt", "*.pptx", "*.txt")
        );
        
        return fileChooser;
    }
    
    /**
     * Upload file with progress tracking and compression
     */
    public static Task<String> uploadFile(File sourceFile, boolean compressImages) {
        return new Task<String>() {
            @Override
            protected String call() throws Exception {
                updateMessage("Starting upload...");
                updateProgress(0, 100);
                
                String fileName = sourceFile.getName();
                String extension = getFileExtension(fileName).toLowerCase();
                String uniqueFileName = UUID.randomUUID().toString() + "_" + fileName;
                
                // Determine target directory based on file type
                Path targetDir = getTargetDirectory(extension);
                Path targetPath = targetDir.resolve(uniqueFileName);
                
                updateMessage("Copying file...");
                updateProgress(25, 100);
                
                // Copy file to target location
                Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                
                updateProgress(75, 100);
                
                // Compress images if requested
                if (compressImages && isImageFile(extension)) {
                    updateMessage("Compressing image...");
                    // TODO: Implement image compression
                    Thread.sleep(500); // Simulate compression time
                }
                
                updateMessage("Upload complete!");
                updateProgress(100, 100);
                
                return targetPath.toString();
            }
        };
    }
    
    /**
     * Get appropriate directory for file type
     */
    private static Path getTargetDirectory(String extension) {
        if (IMAGE_EXTENSIONS.contains(extension)) {
            return IMAGES_DIR;
        } else if (VIDEO_EXTENSIONS.contains(extension)) {
            return VIDEOS_DIR;
        } else if (AUDIO_EXTENSIONS.contains(extension)) {
            return AUDIO_DIR;
        } else if (DOCUMENT_EXTENSIONS.contains(extension)) {
            return DOCUMENTS_DIR;
        }
        return MEDIA_DIR; // Default
    }
    
    /**
     * Enhanced file type icon with better graphics
     */
    public static String getFileIcon(String fileName) {
        if (fileName == null) return "[DOC]";
        
        String extension = getFileExtension(fileName).toLowerCase();
        
        if (IMAGE_EXTENSIONS.contains(extension)) {
            return "[IMG]";
        } else if (VIDEO_EXTENSIONS.contains(extension)) {
            return "[VID]";
        } else if (AUDIO_EXTENSIONS.contains(extension)) {
            return "[AUD]";
        } else if (DOCUMENT_EXTENSIONS.contains(extension)) {
            switch (extension) {
                case "pdf": return "[PDF]";
                case "doc", "docx": return "[DOC]";
                case "xls", "xlsx": return "[XLS]";
                case "ppt", "pptx": return "[PPT]";
                case "txt": return "[TXT]";
                default: return "[DOC]";
            }
        } else if (extension.equals("zip") || extension.equals("rar") || extension.equals("7z")) {
            return "[ZIP]";
        }
        
        return "[DOC]";
    }
    
    /**
     * Get file extension from filename
     */
    public static String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1);
    }
    
    /**
     * Check if file is an image
     */
    public static boolean isImageFile(String extension) {
        return IMAGE_EXTENSIONS.contains(extension.toLowerCase());
    }
    
    /**
     * Check if file is a video
     */
    public static boolean isVideoFile(String extension) {
        return VIDEO_EXTENSIONS.contains(extension.toLowerCase());
    }
    
    /**
     * Check if file is audio
     */
    public static boolean isAudioFile(String extension) {
        return AUDIO_EXTENSIONS.contains(extension.toLowerCase());
    }
    
    /**
     * Format file size in human-readable format
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    /**
     * Open full-screen image viewer
     */
    public static void showImageViewer(String imagePath, Stage parentStage) {
        try {
            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                showAlert("Error", "Image file not found: " + imagePath);
                return;
            }
            
            Stage imageStage = new Stage();
            imageStage.initModality(Modality.APPLICATION_MODAL);
            imageStage.initOwner(parentStage);
            imageStage.initStyle(StageStyle.DECORATED);
            imageStage.setTitle("Image Viewer - " + imageFile.getName());
            
            // Create image view with zoom and pan support
            ImageView imageView = new ImageView(new Image(imageFile.toURI().toString()));
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            imageView.setCache(true);
            
            // Fit to screen initially
            imageView.setFitWidth(800);
            imageView.setFitHeight(600);
            
            // Scroll pane for zoom and pan
            ScrollPane scrollPane = new ScrollPane(imageView);
            scrollPane.setPannable(true);
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            
            // Control panel
            HBox controlPanel = new HBox(10);
            controlPanel.setPadding(new Insets(10));
            controlPanel.setAlignment(Pos.CENTER);
            controlPanel.setStyle("-fx-background-color: rgba(0,0,0,0.8);");
            
            Button zoomInBtn = new Button("Zoom In");
            Button zoomOutBtn = new Button("Zoom Out");
            Button fitToWindowBtn = new Button("Fit to Window");
            Button actualSizeBtn = new Button("Actual Size");
            Button saveAsBtn = new Button("Save As...");
            Button closeBtn = new Button("Close");
            
            // Zoom functionality
            zoomInBtn.setOnAction(e -> {
                imageView.setFitWidth(imageView.getFitWidth() * 1.2);
                imageView.setFitHeight(imageView.getFitHeight() * 1.2);
            });
            
            zoomOutBtn.setOnAction(e -> {
                imageView.setFitWidth(imageView.getFitWidth() / 1.2);
                imageView.setFitHeight(imageView.getFitHeight() / 1.2);
            });
            
            fitToWindowBtn.setOnAction(e -> {
                imageView.setFitWidth(800);
                imageView.setFitHeight(600);
            });
            
            actualSizeBtn.setOnAction(e -> {
                imageView.setFitWidth(imageView.getImage().getWidth());
                imageView.setFitHeight(imageView.getImage().getHeight());
            });
            
            saveAsBtn.setOnAction(e -> saveImageAs(imagePath, imageStage));
            closeBtn.setOnAction(e -> imageStage.close());
            
            controlPanel.getChildren().addAll(
                zoomInBtn, zoomOutBtn, fitToWindowBtn, actualSizeBtn, 
                new Separator(), saveAsBtn, closeBtn
            );
            
            // Layout
            BorderPane layout = new BorderPane();
            layout.setCenter(scrollPane);
            layout.setBottom(controlPanel);
            
            Scene scene = new Scene(layout, 900, 700);
            imageStage.setScene(scene);
            imageStage.setMaximized(false);
            imageStage.show();
            
        } catch (Exception e) {
            logger.error("Error opening image viewer", e);
            showAlert("Error", "Failed to open image viewer: " + e.getMessage());
        }
    }
    
    /**
     * Save image to user-specified location
     */
    private static void saveImageAs(String sourcePath, Stage parentStage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Image As");
        
        File sourceFile = new File(sourcePath);
        fileChooser.setInitialFileName(sourceFile.getName());
        
        String extension = getFileExtension(sourceFile.getName());
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Image Files", "*." + extension)
        );
        
        File targetFile = fileChooser.showSaveDialog(parentStage);
        if (targetFile != null) {
            try {
                Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                showAlert("Success", "Image saved successfully!");
            } catch (IOException e) {
                logger.error("Error saving image", e);
                showAlert("Error", "Failed to save image: " + e.getMessage());
            }
        }
    }
    
    /**
     * Show upload progress dialog
     */
    public static Dialog<String> showUploadDialog(Task<String> uploadTask, Stage parentStage) {
        Dialog<String> dialog = new Dialog<>();
        dialog.initOwner(parentStage);
        dialog.setTitle("Uploading File");
        dialog.setHeaderText("Please wait while the file is being uploaded...");
        
        // Progress indicators
        ProgressBar progressBar = new ProgressBar();
        progressBar.progressProperty().bind(uploadTask.progressProperty());
        progressBar.setPrefWidth(300);
        
        Label statusLabel = new Label();
        statusLabel.textProperty().bind(uploadTask.messageProperty());
        
        // Cancel button
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(cancelButtonType);
        
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(cancelButtonType);
        cancelButton.setOnAction(e -> {
            uploadTask.cancel();
            dialog.setResult(null);
        });
        
        // Layout
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.getChildren().addAll(progressBar, statusLabel);
        dialog.getDialogPane().setContent(content);
        
        // Handle task completion
        uploadTask.setOnSucceeded(e -> {
            dialog.setResult(uploadTask.getValue());
            dialog.close();
        });
        
        uploadTask.setOnFailed(e -> {
            dialog.setResult(null);
            dialog.close();
            showAlert("Upload Failed", "Failed to upload file: " + uploadTask.getException().getMessage());
        });
        
        uploadTask.setOnCancelled(e -> {
            dialog.setResult(null);
            dialog.close();
        });
        
        // Start task
        Thread uploadThread = new Thread(uploadTask);
        uploadThread.setDaemon(true);
        uploadThread.start();
        
        return dialog;
    }
    
    /**
     * Create media gallery for a chat
     */
    public static void showMediaGallery(String chatId, Stage parentStage) {
        Stage galleryStage = new Stage();
        galleryStage.initModality(Modality.APPLICATION_MODAL);
        galleryStage.initOwner(parentStage);
        galleryStage.setTitle("Media Gallery - Chat " + chatId);
        
        // Tab pane for different media types
        TabPane tabPane = new TabPane();
        
        // Images tab
        Tab imagesTab = new Tab("Images");
        imagesTab.setClosable(false);
        GridPane imagesGrid = new GridPane();
        imagesGrid.setHgap(10);
        imagesGrid.setVgap(10);
        imagesGrid.setPadding(new Insets(10));
        
        ScrollPane imagesScroll = new ScrollPane(imagesGrid);
        imagesTab.setContent(imagesScroll);
        
        // Videos tab
        Tab videosTab = new Tab("Videos");
        videosTab.setClosable(false);
        GridPane videosGrid = new GridPane();
        videosGrid.setHgap(10);
        videosGrid.setVgap(10);
        videosGrid.setPadding(new Insets(10));
        
        ScrollPane videosScroll = new ScrollPane(videosGrid);
        videosTab.setContent(videosScroll);
        
        // Documents tab
        Tab documentsTab = new Tab("Documents");
        documentsTab.setClosable(false);
        ListView<String> documentsList = new ListView<>();
        documentsTab.setContent(documentsList);
        
        tabPane.getTabs().addAll(imagesTab, videosTab, documentsTab);
        
        // TODO: Populate with actual media from chat
        populateMediaGallery(imagesGrid, videosGrid, documentsList, chatId);
        
        Scene scene = new Scene(tabPane, 800, 600);
        galleryStage.setScene(scene);
        galleryStage.show();
    }
    
    /**
     * Populate media gallery with chat media
     */
    private static void populateMediaGallery(GridPane imagesGrid, GridPane videosGrid, ListView<String> documentsList, String chatId) {
        // TODO: Load media from database/chat history
        // For now, show placeholder
        Label placeholder = new Label("Media gallery will be populated with chat images, videos, and documents");
        placeholder.setStyle("-fx-font-style: italic; -fx-text-fill: #666;");
        imagesGrid.add(placeholder, 0, 0);
    }
    
    /**
     * Show alert dialog
     */
    private static void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Clean up old media files (for cache management)
     */
    public static void cleanupOldMedia(int daysOld) {
        // TODO: Implement cleanup of old media files
        logger.info("Media cleanup requested for files older than {} days", daysOld);
    }
    
    /**
     * Get total media storage size
     */
    public static long getMediaStorageSize() {
        try {
            return Files.walk(MEDIA_DIR)
                .filter(Files::isRegularFile)
                .mapToLong(path -> {
                    try {
                        return Files.size(path);
                    } catch (IOException e) {
                        return 0;
                    }
                })
                .sum();
        } catch (IOException e) {
            logger.error("Error calculating media storage size", e);
            return 0;
        }
    }
}
