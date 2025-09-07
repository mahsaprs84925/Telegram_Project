package com.telegram.ui;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Voice message recorder with real audio recording capability for Telegram-style hold-to-record functionality
 */
public class VoiceRecorder {
    private static final Logger logger = LoggerFactory.getLogger(VoiceRecorder.class);
    
    private Timeline recordingTimer;
    private boolean isRecording = false;
    private int recordingSeconds = 0;
    private String currentRecordingPath;
    private Consumer<String> onVoiceRecorded;
    private Consumer<Integer> onRecordingProgress;
    
    // Audio recording components
    private TargetDataLine microphone;
    private AudioFileFormat.Type fileType = AudioFileFormat.Type.WAVE;
    private AudioFormat audioFormat;
    private ByteArrayOutputStream audioBuffer;
    
    // Voice recordings directory
    private static final Path VOICE_RECORDINGS_DIR = Paths.get("media", "voice");
    
    static {
        try {
            Files.createDirectories(VOICE_RECORDINGS_DIR);
        } catch (Exception e) {
            LoggerFactory.getLogger(VoiceRecorder.class).error("Failed to create voice recordings directory", e);
        }
    }
    
    public VoiceRecorder() {
        setupAudioFormat();
    }
    
    /**
     * Setup the audio format for recording
     */
    private void setupAudioFormat() {
        // Standard audio format: 16-bit, 16 kHz, mono
        // This provides good quality for voice messages while keeping file size reasonable
        audioFormat = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            16000.0f,    // 16 kHz sample rate
            16,          // 16-bit samples
            1,           // mono (1 channel)
            2,           // frame size (16-bit = 2 bytes)
            16000.0f,    // frame rate (same as sample rate for PCM)
            false        // little-endian byte order
        );
    }
    
    /**
     * Start recording voice message
     */
    public void startRecording(Consumer<String> onComplete, Consumer<Integer> onProgress) {
        if (isRecording) return;
        
        this.onVoiceRecorded = onComplete;
        this.onRecordingProgress = onProgress;
        
        try {
            isRecording = true;
            recordingSeconds = 0;
            
            // Generate unique filename
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            currentRecordingPath = VOICE_RECORDINGS_DIR.resolve("voice_" + timestamp + ".wav").toString();
            
            // Start actual audio recording
            if (!startAudioRecording()) {
                isRecording = false;
                logger.error("Failed to start audio recording");
                return;
            }
            
            // Start timer for duration tracking
            recordingTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
                // Double-check we're still recording before updating progress
                if (isRecording) {
                    recordingSeconds++;
                    if (onRecordingProgress != null) {
                        onRecordingProgress.accept(recordingSeconds);
                    }
                    
                    // No auto-stop limit - allow unlimited recording duration
                    // Users can record as long as needed for comprehensive voice messages
                }
            }));
            recordingTimer.setCycleCount(Timeline.INDEFINITE);
            recordingTimer.play();
            
            logger.info("Voice recording started: {}", currentRecordingPath);
            
        } catch (Exception e) {
            logger.error("Error starting voice recording", e);
            isRecording = false;
        }
    }
    
    /**
     * Stop recording and send the voice message
     */
    public void stopAndSendRecording() {
        if (!isRecording) return;
        
        isRecording = false;
        
        // Stop timer first to prevent any more callbacks
        if (recordingTimer != null) {
            recordingTimer.stop();
            recordingTimer = null;
        }
        
        // Clear the progress callback to prevent any lingering calls
        onRecordingProgress = null;
        
        // Stop audio recording and save file
        stopAudioRecording();
        
        // Only send if recording is at least 1 second
        if (recordingSeconds >= 1 && onVoiceRecorded != null && currentRecordingPath != null) {
            onVoiceRecorded.accept(currentRecordingPath);
            logger.info("Voice message sent: {} seconds", recordingSeconds);
        } else {
            cancelRecording();
        }
    }
    
    /**
     * Get the current recording path
     */
    public String getCurrentRecordingPath() {
        return currentRecordingPath;
    }
    
    /**
     * Cancel recording (slide away or short press)
     */
    public void cancelRecording() {
        if (!isRecording) return;
        
        isRecording = false;
        
        // Stop timer and clear callback
        if (recordingTimer != null) {
            recordingTimer.stop();
            recordingTimer = null;
        }
        onRecordingProgress = null;
        
        // Stop audio recording without saving
        stopAudioRecording();
        
        // Delete the recording file
        if (currentRecordingPath != null) {
            try {
                Files.deleteIfExists(Paths.get(currentRecordingPath));
                logger.info("Voice recording cancelled and deleted");
            } catch (Exception e) {
                logger.warn("Failed to delete cancelled recording: {}", currentRecordingPath);
            }
        }
    }
    
    /**
     * Check if currently recording
     */
    public boolean isRecording() {
        return isRecording;
    }
    
    /**
     * Get current recording duration
     */
    public int getRecordingDuration() {
        return recordingSeconds;
    }
    
    /**
     * Format duration for display (e.g., "0:05")
     */
    public static String formatDuration(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", minutes, secs);
    }
    
    /**
     * Start actual audio recording from microphone
     */
    private boolean startAudioRecording() {
        try {
            // Get microphone line
            DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
            
            if (!AudioSystem.isLineSupported(micInfo)) {
                logger.warn("Microphone not supported with the specified audio format");
                return false;
            }
            
            microphone = (TargetDataLine) AudioSystem.getLine(micInfo);
            microphone.open(audioFormat);
            microphone.start();
            
            // Initialize audio buffer
            audioBuffer = new ByteArrayOutputStream();
            
            // Start recording in a background thread
            Thread recordingThread = new Thread(this::recordAudio);
            recordingThread.setDaemon(true);
            recordingThread.start();
            
            return true;
            
        } catch (LineUnavailableException e) {
            logger.error("Microphone line unavailable", e);
            return false;
        } catch (Exception e) {
            logger.error("Error starting audio recording", e);
            return false;
        }
    }
    
    /**
     * Record audio data from microphone to buffer
     */
    private void recordAudio() {
        byte[] buffer = new byte[4096];
        
        try {
            while (isRecording && microphone != null) {
                int bytesRead = microphone.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    audioBuffer.write(buffer, 0, bytesRead);
                }
            }
        } catch (Exception e) {
            logger.error("Error during audio recording", e);
        }
    }
    
    /**
     * Stop audio recording and save to file
     */
    private void stopAudioRecording() {
        try {
            if (microphone != null) {
                microphone.stop();
                microphone.close();
            }
            
            if (audioBuffer != null && currentRecordingPath != null) {
                // Save audio buffer to WAV file
                saveAudioToFile();
            }
            
        } catch (Exception e) {
            logger.error("Error stopping audio recording", e);
        } finally {
            microphone = null;
            audioBuffer = null;
        }
    }
    
    /**
     * Save recorded audio buffer to WAV file
     */
    private void saveAudioToFile() throws IOException {
        if (audioBuffer == null || currentRecordingPath == null) return;
        
        byte[] audioData = audioBuffer.toByteArray();
        
        // Create audio input stream from buffer
        AudioInputStream audioInputStream = new AudioInputStream(
            new java.io.ByteArrayInputStream(audioData),
            audioFormat,
            audioData.length / audioFormat.getFrameSize()
        );
        
        // Write to WAV file
        File outputFile = new File(currentRecordingPath);
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputFile);
        
        audioInputStream.close();
        logger.info("Audio saved to: {} ({} bytes)", currentRecordingPath, audioData.length);
    }
}
