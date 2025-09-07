package com.telegram.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Window;

import java.util.function.Consumer;

/**
 * Simple emoji picker for quick access to common emojis
 */
public class EmojiPicker {
    
    // Common emoji categories (Windows-compatible ASCII alternatives)
    private static final String[] SMILEYS = {
        ":)", ":D", ":P", ";)", ":(", ":|", ":o", ":s", ":/", ":x", 
        "<3", "</3", ":*", "^_^", "-_-", "O_O", "T_T", "XD", ":3", "=)",
        "B)", "8)", ":>", ":<", "=D", "=P", "=|", "=/", "=o", "=s"
    };
    
    private static final String[] GESTURES = {
        "+1", "-1", "OK", "V", "?", "!", "@", "#", "<", ">",
        "^", "v", "~", "*", "wave", "hi", "hand", "stop", "clap", "pray",
        "thanks", "shake", "write", "strong", "flex", "point", "walk", "kick", "run", "jump"
    };
    
    private static final String[] HEARTS = {
        "<3", "</3", "[heart]", "[broken heart]", "[love]", "[like]", "[dislike]", "[hug]", "[kiss]", "[gift]",
        "[sparkle]", "[double heart]", "[ribbon]", "[star]", "[rose]", "[flower]", "[music]", "[mail]", "[smile]", "[wink]"
    };

    private static final String[] OBJECTS = {
        "[phone]", "[laptop]", "[watch]", "[camera]", "[video]", "[call]", "[pager]", "[fax]", "[tv]", "[radio]",
        "[music note]", "[bell]", "[mute]", "[speaker]", "[megaphone]", "[horn]", "[volume up]", "[volume down]", "[volume off]", "[sound]"
    };
    
    private Popup popup;
    private Consumer<String> onEmojiSelected;
    
    public EmojiPicker(Consumer<String> onEmojiSelected) {
        this.onEmojiSelected = onEmojiSelected;
        createPopup();
    }
    
    private void createPopup() {
        popup = new Popup();
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);
        
        VBox content = new VBox(10);
        content.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                        "-fx-padding: 12; -fx-border-color: #e0e0e0; -fx-border-width: 1; " +
                        "-fx-border-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 8, 0, 0, 2);");
        content.setPrefWidth(300);
        content.setPrefHeight(250);
        
        // Title
        Label titleLabel = new Label("Emoji Picker");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        content.getChildren().add(titleLabel);
        
        // Emoji grid in scroll pane
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(200);
        scrollPane.setStyle("-fx-background-color: transparent;");
        
        VBox emojiContainer = new VBox(10);
        
        // Add emoji categories
        emojiContainer.getChildren().add(createEmojiSection("Smileys", SMILEYS));
        emojiContainer.getChildren().add(createEmojiSection("Gestures", GESTURES));
        emojiContainer.getChildren().add(createEmojiSection("Hearts", HEARTS));
        emojiContainer.getChildren().add(createEmojiSection("Objects", OBJECTS));
        
        scrollPane.setContent(emojiContainer);
        content.getChildren().add(scrollPane);
        
        popup.getContent().add(content);
    }
    
    private VBox createEmojiSection(String title, String[] emojis) {
        VBox section = new VBox(5);
        
        // Section title
        Label sectionTitle = new Label(title);
        sectionTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: #666;");
        section.getChildren().add(sectionTitle);
        
        // Emoji flow pane
        FlowPane emojiPane = new FlowPane(5, 5);
        emojiPane.setAlignment(Pos.CENTER_LEFT);
        
        for (String emoji : emojis) {
            Button emojiButton = new Button(emoji);
            emojiButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; " +
                               "-fx-font-size: 18px; -fx-padding: 5; -fx-cursor: hand;");
            emojiButton.setMinSize(35, 35);
            emojiButton.setMaxSize(35, 35);
            
            emojiButton.setOnMouseEntered(e -> 
                emojiButton.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: transparent; " +
                                   "-fx-font-size: 18px; -fx-padding: 5; -fx-cursor: hand; -fx-background-radius: 4;"));
            
            emojiButton.setOnMouseExited(e -> 
                emojiButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; " +
                                   "-fx-font-size: 18px; -fx-padding: 5; -fx-cursor: hand;"));
            
            emojiButton.setOnAction(e -> {
                if (onEmojiSelected != null) {
                    onEmojiSelected.accept(emoji);
                }
                popup.hide();
            });
            
            emojiPane.getChildren().add(emojiButton);
        }
        
        section.getChildren().add(emojiPane);
        return section;
    }
    
    public void show(Window owner, double x, double y) {
        popup.show(owner, x, y);
    }
    
    public void hide() {
        popup.hide();
    }
}
