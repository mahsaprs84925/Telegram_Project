package com.telegram.utils;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * Profile picture cropper utility for consistent sizing like Telegram
 */
public class ProfilePictureCropper {
    private static final Logger logger = LoggerFactory.getLogger(ProfilePictureCropper.class);
    
    // Standard profile picture size (same as Telegram)
    public static final int PROFILE_PICTURE_SIZE = 160;
    
    private Image originalImage;
    private ImageView imageView;
    private Circle cropArea;
    private Pane imageContainer;
    private double startX, startY;
    private boolean isDragging = false;
    private Stage cropStage;
    private CompletableFuture<File> resultFuture;
    
    /**
     * Show profile picture cropper dialog
     * @param parentStage Parent stage
     * @param imageFile Source image file
     * @return CompletableFuture with cropped image file, or null if cancelled
     */
    public static CompletableFuture<File> showCropDialog(Stage parentStage, File imageFile) {
        ProfilePictureCropper cropper = new ProfilePictureCropper();
        return cropper.showDialog(parentStage, imageFile);
    }
    
    private CompletableFuture<File> showDialog(Stage parentStage, File imageFile) {
        resultFuture = new CompletableFuture<>();
        
        try {
            // Load the original image
            originalImage = new Image(imageFile.toURI().toString());
            
            if (originalImage.isError()) {
                logger.error("Failed to load image: {}", imageFile.getAbsolutePath());
                resultFuture.complete(null);
                return resultFuture;
            }
            
            setupCropDialog(parentStage);
            
        } catch (Exception e) {
            logger.error("Error setting up crop dialog", e);
            resultFuture.complete(null);
        }
        
        return resultFuture;
    }
    
    private void setupCropDialog(Stage parentStage) {
        cropStage = new Stage();
        cropStage.setTitle("Crop Profile Picture");
        cropStage.initModality(Modality.APPLICATION_MODAL);
        cropStage.initOwner(parentStage);
        cropStage.setResizable(false);
        
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(20));
        
        // Instructions
        Label instructionLabel = new Label("Drag to adjust the crop area. The cropped image will be used as your profile picture.");
        instructionLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
        instructionLabel.setWrapText(true);
        
        // Image container with crop overlay
        setupImageContainer();
        
        // Buttons
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        buttonBox.setPadding(new Insets(20, 0, 0, 0));
        
        Button cropButton = new Button("Crop & Save");
        cropButton.setStyle("-fx-background-color: #0088cc; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 10 20;");
        cropButton.setOnAction(e -> handleCrop());
        
        Button cancelButton = new Button("Cancel");
        cancelButton.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: #333; -fx-padding: 10 20;");
        cancelButton.setOnAction(e -> handleCancel());
        
        buttonBox.getChildren().addAll(cropButton, cancelButton);
        
        // Layout
        VBox topContainer = new VBox(10);
        topContainer.getChildren().addAll(instructionLabel, imageContainer);
        
        root.setTop(topContainer);
        root.setBottom(buttonBox);
        
        Scene scene = new Scene(root, 600, 700);
        cropStage.setScene(scene);
        cropStage.show();
        
        // Center the crop area initially
        centerCropArea();
    }
    
    private void setupImageContainer() {
        imageContainer = new Pane();
        imageContainer.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #ddd; -fx-border-width: 1;");
        
        // Calculate display size while maintaining aspect ratio
        double maxDisplaySize = 400;
        double imageWidth = originalImage.getWidth();
        double imageHeight = originalImage.getHeight();
        double scale = Math.min(maxDisplaySize / imageWidth, maxDisplaySize / imageHeight);
        
        double displayWidth = imageWidth * scale;
        double displayHeight = imageHeight * scale;
        
        imageContainer.setPrefSize(displayWidth, displayHeight);
        imageContainer.setMaxSize(displayWidth, displayHeight);
        
        // Image view
        imageView = new ImageView(originalImage);
        imageView.setFitWidth(displayWidth);
        imageView.setFitHeight(displayHeight);
        imageView.setPreserveRatio(true);
        
        // Circular crop area
        double cropRadius = Math.min(displayWidth, displayHeight) * 0.3; // 60% diameter = 30% radius
        cropArea = new Circle(cropRadius);
        cropArea.setFill(Color.TRANSPARENT);
        cropArea.setStroke(Color.WHITE);
        cropArea.setStrokeWidth(3);
        cropArea.getStrokeDashArray().addAll(5d, 5d);
        
        // Add drag functionality to crop area
        setupCropAreaDragging();
        
        imageContainer.getChildren().addAll(imageView, cropArea);
    }
    
    private void setupCropAreaDragging() {
        cropArea.setOnMousePressed(this::handleMousePressed);
        cropArea.setOnMouseDragged(this::handleMouseDragged);
        cropArea.setOnMouseReleased(this::handleMouseReleased);
        
        // Also handle dragging on the image itself
        imageView.setOnMousePressed(this::handleMousePressed);
        imageView.setOnMouseDragged(this::handleMouseDragged);
        imageView.setOnMouseReleased(this::handleMouseReleased);
    }
    
    private void handleMousePressed(MouseEvent event) {
        startX = event.getX();
        startY = event.getY();
        isDragging = true;
        event.consume();
    }
    
    private void handleMouseDragged(MouseEvent event) {
        if (!isDragging) return;
        
        double deltaX = event.getX() - startX;
        double deltaY = event.getY() - startY;
        
        double newCenterX = cropArea.getCenterX() + deltaX;
        double newCenterY = cropArea.getCenterY() + deltaY;
        
        // Constrain to image bounds (keep circle within image)
        double radius = cropArea.getRadius();
        double minX = radius;
        double maxX = imageView.getFitWidth() - radius;
        double minY = radius;
        double maxY = imageView.getFitHeight() - radius;
        
        newCenterX = Math.max(minX, Math.min(newCenterX, maxX));
        newCenterY = Math.max(minY, Math.min(newCenterY, maxY));
        
        cropArea.setCenterX(newCenterX);
        cropArea.setCenterY(newCenterY);
        
        startX = event.getX();
        startY = event.getY();
        event.consume();
    }
    
    private void handleMouseReleased(MouseEvent event) {
        isDragging = false;
        event.consume();
    }
    
    private void centerCropArea() {
        double imageWidth = imageView.getFitWidth();
        double imageHeight = imageView.getFitHeight();
        
        double centerX = imageWidth / 2;
        double centerY = imageHeight / 2;
        
        cropArea.setCenterX(centerX);
        cropArea.setCenterY(centerY);
    }
    
    private void handleCrop() {
        try {
            // Calculate crop coordinates in original image scale
            double scaleX = originalImage.getWidth() / imageView.getFitWidth();
            double scaleY = originalImage.getHeight() / imageView.getFitHeight();
            
            // Get circle center and radius in original image coordinates
            double centerX = cropArea.getCenterX() * scaleX;
            double centerY = cropArea.getCenterY() * scaleY;
            double radius = cropArea.getRadius() * Math.min(scaleX, scaleY);
            
            // Create a square crop area that contains the circle
            int cropSize = (int) (radius * 2);
            int cropX = (int) (centerX - radius);
            int cropY = (int) (centerY - radius);
            
            // Ensure crop area is within image bounds
            cropX = Math.max(0, Math.min(cropX, (int) originalImage.getWidth() - cropSize));
            cropY = Math.max(0, Math.min(cropY, (int) originalImage.getHeight() - cropSize));
            cropSize = Math.min(cropSize, Math.min((int) originalImage.getWidth() - cropX, (int) originalImage.getHeight() - cropY));
            
            logger.info("Circular cropping image: {}x{} at ({}, {}) from original {}x{}", 
                cropSize, cropSize, cropX, cropY, 
                (int) originalImage.getWidth(), (int) originalImage.getHeight());
            
            // Create square cropped image first
            WritableImage squareCrop = new WritableImage(
                originalImage.getPixelReader(),
                cropX, cropY, cropSize, cropSize
            );
            
            // Create circular mask and apply it
            WritableImage circularImage = createCircularImage(squareCrop, PROFILE_PICTURE_SIZE);
            
            // Save to temporary file
            File tempFile = saveCroppedImage(circularImage);
            
            cropStage.close();
            resultFuture.complete(tempFile);
            
        } catch (Exception e) {
            logger.error("Error cropping image", e);
            resultFuture.complete(null);
        }
    }
    
    private void handleCancel() {
        cropStage.close();
        resultFuture.complete(null);
    }
    
    private File saveCroppedImage(WritableImage croppedImage) throws IOException {
        // Create temporary file
        File tempFile = File.createTempFile("profile_crop_", ".png");
        tempFile.deleteOnExit();
        
        // Convert to BufferedImage and save
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(croppedImage, null);
        ImageIO.write(bufferedImage, "png", tempFile);
        
        logger.info("Cropped profile picture saved to: {}", tempFile.getAbsolutePath());
        return tempFile;
    }
    
    /**
     * Quick resize method for existing profile pictures to standard size
     * @param inputFile Input image file
     * @param outputFile Output image file
     * @return true if successful
     */
    public static boolean resizeToStandardSize(File inputFile, File outputFile) {
        try {
            Image image = new Image(inputFile.toURI().toString());
            
            if (image.isError()) {
                return false;
            }
            
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(PROFILE_PICTURE_SIZE);
            imageView.setFitHeight(PROFILE_PICTURE_SIZE);
            imageView.setPreserveRatio(false);
            
            WritableImage resizedImage = new WritableImage(PROFILE_PICTURE_SIZE, PROFILE_PICTURE_SIZE);
            imageView.snapshot(null, resizedImage);
            
            BufferedImage bufferedImage = SwingFXUtils.fromFXImage(resizedImage, null);
            ImageIO.write(bufferedImage, "png", outputFile);
            
            return true;
        } catch (Exception e) {
            logger.error("Error resizing image to standard size", e);
            return false;
        }
    }
    
    private WritableImage createCircularImage(WritableImage sourceImage, int size) {
        // Create a new image with transparent background
        WritableImage circularImage = new WritableImage(size, size);
        
        // Resize the source image to fit the target size
        ImageView sourceView = new ImageView(sourceImage);
        sourceView.setFitWidth(size);
        sourceView.setFitHeight(size);
        sourceView.setPreserveRatio(false);
        
        WritableImage resizedSource = new WritableImage(size, size);
        sourceView.snapshot(null, resizedSource);
        
        // Create circular mask by checking distance from center
        var pixelReader = resizedSource.getPixelReader();
        var pixelWriter = circularImage.getPixelWriter();
        
        double centerX = size / 2.0;
        double centerY = size / 2.0;
        double radius = size / 2.0;
        
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double distance = Math.sqrt((x - centerX) * (x - centerX) + (y - centerY) * (y - centerY));
                
                if (distance <= radius) {
                    // Inside circle - copy pixel
                    Color pixelColor = pixelReader.getColor(x, y);
                    pixelWriter.setColor(x, y, pixelColor);
                } else {
                    // Outside circle - transparent
                    pixelWriter.setColor(x, y, Color.TRANSPARENT);
                }
            }
        }
        
        return circularImage;
    }
}
