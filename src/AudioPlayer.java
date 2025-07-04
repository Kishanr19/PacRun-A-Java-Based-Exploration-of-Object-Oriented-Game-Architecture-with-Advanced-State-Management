import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple audio player for the PacRun game.
 * Handles loading and playing sound effects and background music.
 */
public class AudioPlayer {
    // Maps to store our audio clips
    private static Map<String, Clip> clips = new HashMap<>();

    // Currently playing background music
    private static Clip currentMusic;

    // Audio enabled flags
    private static boolean soundEnabled = true;
    private static boolean musicEnabled = true;

    static {
        // Use the correct path that includes the Resources directory
        loadClip("menu", "Resources/pacman_beginning.wav");
        loadClip("munch", "Resources/pacman_chomp.wav");
        loadClip("power", "Resources/pacman_eatfruit.wav");
        loadClip("death", "Resources/pacman_death.wav");
    }

    private static void loadClip(String name, String path) {
        try {
            URL url = AudioPlayer.class.getResource(path);
            if (url == null) {
                System.err.println("Could not find sound file: " + path);
                return;
            }

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(url);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clips.put(name, clip);
            System.out.println("Successfully loaded audio: " + name);

        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Failed to load sound: " + path);
            e.printStackTrace();
        }
    }


    public static void playSound(String name) {
        if (!soundEnabled) return;

        Clip clip = clips.get(name);
        if (clip == null) {
            System.err.println("Sound not found: " + name);
            return;
        }

        // Stop if already playing and reset position
        if (clip.isRunning()) {
            clip.stop();
        }
        clip.setFramePosition(0);
        clip.start();
    }


    public static void playMusic(String name) {
        if (!musicEnabled) return;

        // First stop any currently playing music
        stopMusic();

        Clip clip = clips.get(name);
        if (clip == null) {
            System.err.println("Music not found: " + name);
            return;
        }

        clip.setFramePosition(0);
        clip.loop(Clip.LOOP_CONTINUOUSLY);
        currentMusic = clip;
    }

    /**
     * Stop the currently playing background music
     */
    public static void stopMusic() {
        if (currentMusic != null && currentMusic.isRunning()) {
            currentMusic.stop();
        }
    }


    public static void setSoundEnabled(boolean enabled) {
        soundEnabled = enabled;
    }

    public static void setMusicEnabled(boolean enabled) {
        musicEnabled = enabled;
        if (!enabled) {
            stopMusic();
        }
    }
    public static boolean isSoundEnabled() {
        return soundEnabled;
    }

    public static boolean isMusicEnabled() {
        return musicEnabled;
    }



    public static void cleanup() {
        for (Clip clip : clips.values()) {
            if (clip != null) {
                clip.stop();
                clip.close();
            }
        }
        clips.clear();
        currentMusic = null;
    }
}