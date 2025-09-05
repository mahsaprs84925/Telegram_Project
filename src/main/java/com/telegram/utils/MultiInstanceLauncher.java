package com.telegram.utils;

import com.telegram.TelegramApp;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * Utility to launch multiple instances of Telegram Clone for testing real-time features
 */
public class MultiInstanceLauncher {
    
    public static void main(String[] args) {
        System.out.println("Launching multiple Telegram Clone instances for real-time testing...");
        
        // Launch first instance normally
        new Thread(() -> {
            System.setProperty("telegram.instance", "1");
            Application.launch(TelegramApp.class);
        }).start();
        
        // Wait a bit for first instance to start
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Launch second instance
        new Thread(() -> {
            System.setProperty("telegram.instance", "2");
            Platform.runLater(() -> {
                try {
                    new TelegramApp().start(new Stage());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }).start();
        
        // Wait a bit more
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Launch third instance
        new Thread(() -> {
            System.setProperty("telegram.instance", "3");
            Platform.runLater(() -> {
                try {
                    new TelegramApp().start(new Stage());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }).start();
        
        System.out.println("All instances launched! You can now test real-time messaging between different users.");
    }
}
