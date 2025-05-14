package org.ensate;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AudioManager {
    private static AudioManager instance;
    private Clip backgroundMusic;
    private boolean isMusicPlaying = false;
    private ExecutorService soundExecutor;
    private boolean isShutdown = false;
    
    private Map<String, CachedSound> soundCache;
    
    private static class CachedSound {
        private final byte[] audioData;
        private final AudioFormat format;
        
        public CachedSound(byte[] audioData, AudioFormat format) {
            this.audioData = audioData;
            this.format = format;
        }
    }

    private AudioManager() {
        initExecutor();
        soundCache = new HashMap<>();
        preloadSounds();
    }

    private void preloadSounds() {
        try {
            loadAndCacheSound("tir.wav");
            loadAndCacheSound("collision.wav");
            loadAndCacheSound("mort.wav");
            loadAndCacheSound("levelup.wav");
        } catch (Exception e) {
            System.err.println("Erreur lors du préchargement des sons: " + e.getMessage());
        }
    }

    private void loadAndCacheSound(String soundFile) {
        try {
            File file = new File("src/main/resources/son/" + soundFile);
            if (!file.exists()) {
                System.err.println("Fichier son non trouvé: " + soundFile);
                return;
            }

            AudioInputStream audioIn = AudioSystem.getAudioInputStream(file);
            AudioFormat baseFormat = audioIn.getFormat();

            AudioFormat targetFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                22050.0f,
                16,
                1,
                2,
                22050.0f,
                false
            );

            AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, audioIn);
            
            byte[] audioData = convertedStream.readAllBytes();
            
            soundCache.put(soundFile, new CachedSound(audioData, targetFormat));
            
            convertedStream.close();
            audioIn.close();
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement du son " + soundFile + ": " + e.getMessage());
        }
    }

    private synchronized void initExecutor() {
        if (soundExecutor == null || soundExecutor.isShutdown()) {
            soundExecutor = Executors.newCachedThreadPool();
            isShutdown = false;
        }
    }

    public static synchronized AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }

    public synchronized void playSoundEffect(String soundFile) {
        if (isShutdown) return;
        
        CachedSound cachedSound = soundCache.get(soundFile);
        if (cachedSound == null) {
            System.err.println("Son non trouvé dans le cache: " + soundFile);
            return;
        }

        initExecutor();
        soundExecutor.execute(() -> {
            try {
                AudioInputStream audioStream = new AudioInputStream(
                    new ByteArrayInputStream(cachedSound.audioData),
                    cachedSound.format,
                    cachedSound.audioData.length / cachedSound.format.getFrameSize()
                );

                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);
                clip.start();

                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        clip.close();
                        try {
                            audioStream.close();
                        } catch (IOException e) {
                            // Ignorer
                        }
                    }
                });
            } catch (Exception e) {
                System.err.println("Erreur lors de la lecture du son " + soundFile + ": " + e.getMessage());
            }
        });
    }

    public synchronized void startBackgroundMusic() {
        if (isShutdown || isMusicPlaying) return;

        initExecutor();
        soundExecutor.execute(() -> {
            try {
                File file = new File("src/main/resources/son/background_music.wav");
                if (!file.exists()) {
                    System.err.println("Musique de fond non trouvée");
                    return;
                }

                AudioInputStream audioIn = AudioSystem.getAudioInputStream(file);
                AudioFormat baseFormat = audioIn.getFormat();

                AudioFormat targetFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    22050.0f,
                    16,
                    1,
                    2,
                    22050.0f,
                    false
                );

                AudioInputStream convertedStream = AudioSystem.getAudioInputStream(targetFormat, audioIn);
                
                backgroundMusic = AudioSystem.getClip();
                backgroundMusic.open(convertedStream);
                backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
                isMusicPlaying = true;

                convertedStream.close();
                audioIn.close();
            } catch (Exception e) {
                System.err.println("Erreur lors du chargement de la musique de fond: " + e.getMessage());
            }
        });
    }

    public synchronized void stopBackgroundMusic() {
        if (isMusicPlaying && backgroundMusic != null) {
            backgroundMusic.stop();
            backgroundMusic.close();
            isMusicPlaying = false;
        }
    }

    public synchronized void pauseBackgroundMusic() {
        if (isMusicPlaying && backgroundMusic != null) {
            backgroundMusic.stop();
            isMusicPlaying = false;
        }
    }

    public synchronized void resumeBackgroundMusic() {
        if (!isMusicPlaying && backgroundMusic != null) {
            backgroundMusic.start();
            isMusicPlaying = true;
        }
    }

    public boolean isMusicPlaying() {
        return isMusicPlaying;
    }

    public void playShootSound() {
        playSoundEffect("tir.wav");
    }

    public void playCollisionSound() {
        playSoundEffect("collision.wav");
    }

    public void playDeathSound() {
        playSoundEffect("mort.wav");
    }

    public void playLevelUpSound() {
        playSoundEffect("levelup.wav");
    }

    public synchronized void cleanup() {
        isShutdown = true;
        stopBackgroundMusic();
        
        if (soundExecutor != null && !soundExecutor.isShutdown()) {
            try {
                soundExecutor.shutdown();
                if (!soundExecutor.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                    soundExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                soundExecutor.shutdownNow();
            }
        }
        
        soundCache.clear();
    }

    public synchronized void reset() {
        cleanup();
        isShutdown = false;
        initExecutor();
        preloadSounds();
    }
} 