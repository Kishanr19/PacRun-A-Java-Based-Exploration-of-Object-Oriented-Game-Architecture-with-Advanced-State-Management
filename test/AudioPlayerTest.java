import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;



public class AudioPlayerTest {

    // Store initial states
    private static boolean initialSoundState;
    private static boolean initialMusicState;

    @Before
    public void setUp() {
        // Record initial state ONLY ONCE if needed, but safer to set defaults
        // if (!initialStateRecorded) {
        //     initialSoundState = AudioPlayer.isSoundEnabled();
        //     initialMusicState = AudioPlayer.isMusicEnabled();
        //     initialStateRecorded = true;
        // }

        // Ensure a known state before each test
        AudioPlayer.setMusicEnabled(true);
        AudioPlayer.setSoundEnabled(true);
        AudioPlayer.stopMusic(); // Stop potential leftovers
    }

    @Test
    public void testInitialStateValues() {
        // Test the state set in setUp
        assertTrue("Sound should be enabled after setUp", AudioPlayer.isSoundEnabled());
        assertTrue("Music should be enabled after setUp", AudioPlayer.isMusicEnabled());
    }

    @Test
    public void testDisableSound() {
        AudioPlayer.setSoundEnabled(false);
        assertFalse("Sound should be disabled after calling setSoundEnabled(false)", AudioPlayer.isSoundEnabled());
    }

    @Test
    public void testEnableSoundAfterDisabling() {
        AudioPlayer.setSoundEnabled(false); // Ensure it starts disabled for this test
        AudioPlayer.setSoundEnabled(true);
        assertTrue("Sound should be enabled after calling setSoundEnabled(true)", AudioPlayer.isSoundEnabled());
    }

    @Test
    public void testDisableMusic() {
        AudioPlayer.setMusicEnabled(false);
        assertFalse("Music should be disabled after calling setMusicEnabled(false)", AudioPlayer.isMusicEnabled());
        // Cannot easily assert side effect (music actually stopping) in unit test
    }

    @Test
    public void testEnableMusicAfterDisabling() {
        AudioPlayer.setMusicEnabled(false); // Ensure it starts disabled
        AudioPlayer.setMusicEnabled(true);
        assertTrue("Music should be enabled after calling setMusicEnabled(true)", AudioPlayer.isMusicEnabled());
    }

    @After
    public void tearDown() {
        // Reset state after each test to avoid interference for subsequent tests
        AudioPlayer.setMusicEnabled(true); // Or restore initialMusicState if recorded
        AudioPlayer.setSoundEnabled(true); // Or restore initialSoundState if recorded
        AudioPlayer.stopMusic();
    }
}