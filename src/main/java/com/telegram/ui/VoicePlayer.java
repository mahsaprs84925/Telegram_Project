package com.telegram.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * Simple voice message player for audio playback
 */
public class VoicePlayer {
    private static final Logger logger = LoggerFactory.getLogger(VoicePlayer.class);
    
    private Clip audioClip;
    private boolean isPlaying = false;
    private Timeline progressTimer;
    private Consumer<Integer> onProgressUpdate;
    private Consumer<Boolean> onPlaybackStateChange;
    private int totalDurationSeconds = 0;
    
    /**
     * Play a voice message file
     */
    public void playVoiceMessage(String audioFilePath, Consumer<Integer> progressCallback, Consumer<Boolean> stateCallback) {
        if (isPlaying) {
            stopPlayback();
        }
        
        this.onProgressUpdate = progressCallback;
        this.onPlaybackStateChange = stateCallback;
        
        try {
            File audioFile = new File(audioFilePath);
            if (!audioFile.exists()) {
                logger.error("Audio file does not exist: {}", audioFilePath);
                if (stateCallback != null) {
                    stateCallback.accept(false);
                }
                return;
            }
            
            logger.info("Attempting to play audio file: {} (size: {} bytes)", audioFilePath, audioFile.length());
            
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
            audioClip = AudioSystem.getClip();
            audioClip.open(audioInputStream);
            
            // Calculate total duration
            long frameLength = audioClip.getFrameLength();
            AudioFormat format = audioClip.getFormat();
            totalDurationSeconds = (int) (frameLength / format.getFrameRate());
            
            logger.info("Audio file loaded successfully. Duration: {} seconds, Format: {}", 
                totalDurationSeconds, format.toString());
            
            // Add listener for when playback completes
            audioClip.addLineListener(event -> {
                logger.debug("Audio line event: {}", event.getType());
                if (event.getType() == LineEvent.Type.STOP) {
                    if (audioClip.getFramePosition() >= audioClip.getFrameLength()) {
                        // Playback completed naturally
                        logger.info("Voice message playback completed naturally");
                        stopPlayback();
                    }
                }
            });
            
            audioClip.start();
            isPlaying = true;
            
            if (onPlaybackStateChange != null) {
                onPlaybackStateChange.accept(true);
            }
            
            // Start progress timer
            startProgressTimer();
            
            logger.info("Started playing voice message: {} ({} seconds)", audioFilePath, totalDurationSeconds);
            
        } catch (UnsupportedAudioFileException e) {
            logger.error("Unsupported audio file format: {} - Error: {}", audioFilePath, e.getMessage());
            if (stateCallback != null) {
                stateCallback.accept(false);
            }
        } catch (IOException e) {
            logger.error("Error reading audio file: {} - Error: {}", audioFilePath, e.getMessage());
            if (stateCallback != null) {
                stateCallback.accept(false);
            }
        } catch (LineUnavailableException e) {
            logger.error("Audio line unavailable for playback - Error: {}", e.getMessage());
            if (stateCallback != null) {
                stateCallback.accept(false);
            }
        } catch (Exception e) {
            logger.error("Unexpected error during voice message playback", e);
            if (stateCallback != null) {
                stateCallback.accept(false);
            }
        }
    }
    
    /**
     * Stop current playback
     */
    public void stopPlayback() {
        isPlaying = false;
        
        if (progressTimer != null) {
            progressTimer.stop();
        }
        
        if (audioClip != null) {
            audioClip.stop();
            audioClip.close();
            audioClip = null;
        }
        
        if (onPlaybackStateChange != null) {
            onPlaybackStateChange.accept(false);
        }
        
        logger.info("Voice message playback stopped");
    }
    
    /**
     * Pause/resume playback
     */
    public void togglePlayback() {
        if (audioClip == null) return;
        
        if (isPlaying) {
            audioClip.stop();
            isPlaying = false;
            if (progressTimer != null) {
                progressTimer.pause();
            }
        } else {
            audioClip.start();
            isPlaying = true;
            if (progressTimer != null) {
                progressTimer.play();
            }
        }
        
        if (onPlaybackStateChange != null) {
            onPlaybackStateChange.accept(isPlaying);
        }
    }
    
    /**
     * Seek to a specific position (0.0 to 1.0)
     */
    public void seekTo(double position) {
        if (audioClip == null) return;
        
        position = Math.max(0.0, Math.min(1.0, position));
        long framePosition = (long) (audioClip.getFrameLength() * position);
        audioClip.setFramePosition((int) framePosition);
    }
    
    /**
     * Get current playback position (0.0 to 1.0)
     */
    public double getCurrentPosition() {
        if (audioClip == null) return 0.0;
        return (double) audioClip.getFramePosition() / audioClip.getFrameLength();
    }
    
    /**
     * Get current playback time in seconds
     */
    public int getCurrentTimeSeconds() {
        if (audioClip == null) return 0;
        
        long framePosition = audioClip.getFramePosition();
        AudioFormat format = audioClip.getFormat();
        return (int) (framePosition / format.getFrameRate());
    }
    
    /**
     * Check if currently playing
     */
    public boolean isPlaying() {
        return isPlaying;
    }
    
    /**
     * Get total duration in seconds
     */
    public int getTotalDurationSeconds() {
        return totalDurationSeconds;
    }
    
    /**
     * Start timer to track playback progress
     */
    private void startProgressTimer() {
        progressTimer = new Timeline(new KeyFrame(Duration.millis(100), e -> {
            if (onProgressUpdate != null && audioClip != null) {
                int currentSeconds = getCurrentTimeSeconds();
                onProgressUpdate.accept(currentSeconds);
            }
        }));
        progressTimer.setCycleCount(Timeline.INDEFINITE);
        progressTimer.play();
    }
    
    /**
     * Format time for display (e.g., "0:05")
     */
    public static String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", minutes, secs);
    }
}
