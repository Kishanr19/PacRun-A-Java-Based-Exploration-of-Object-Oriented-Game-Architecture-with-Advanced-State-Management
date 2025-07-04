import javax.swing.JFrame;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MainApplication {
    // Constructor that accepts a level parameter
    public MainApplication(int level) {
        // Create the main game window
        JFrame gameWindow = new JFrame("PacRun");

        // Remove window decorations for full screen
        gameWindow.setUndecorated(true);

        // Get the graphics environment and device for full screen setup
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();

        // Configure basic window properties
        gameWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gameWindow.setResizable(false);

        // Create the game instance with the specified level
        PacRun pacRunInstance = new PacRun(level);
        gameWindow.add(pacRunInstance);

        // Make the window full screen
        gd.setFullScreenWindow(gameWindow);

        // Add window listener for audio cleanup
        gameWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                AudioPlayer.cleanup();
            }
        });

        // Ensure the game has keyboard focus
        pacRunInstance.requestFocus();

        // Add key listener for navigation
        pacRunInstance.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                //PacRun class will now handle the ESC key
            }
        });
    }

    // Constructor for multiplayer mode
    public MainApplication(boolean multiplayer) {
        // Create the main game window
        JFrame gameWindow = new JFrame("PacRun - 2 Player Mode");

        // Remove window decorations for full screen
        gameWindow.setUndecorated(true);

        // Get the graphics environment and device for full screen setup
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();

        // Configure basic window properties
        gameWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gameWindow.setResizable(false);

        // Create the game instance in multiplayer mode
        PacRun pacRunInstance = new PacRun(multiplayer);
        gameWindow.add(pacRunInstance);

        // Make the window full screen
        gd.setFullScreenWindow(gameWindow);

        // Add window listener for audio cleanup
        gameWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                AudioPlayer.cleanup();
            }
        });

        // Ensure the game has keyboard focus
        pacRunInstance.requestFocus();

        // Add key listener for navigation
        pacRunInstance.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                // Empty - PacRun class will now handle the ESC key
            }
        });
    }

    // New constructor for bonus level
    public MainApplication(int level, boolean startBonusMode) {
        // Create the main game window
        JFrame gameWindow = new JFrame("PacRun - Bonus Round");

        // Remove window decorations for full screen
        gameWindow.setUndecorated(true);

        // Get the graphics environment and device for full screen setup
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();

        // Configure basic window properties
        gameWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        gameWindow.setResizable(false);

        // Create the game instance
        PacRun pacRunInstance = new PacRun(1); // Create with level 1
        gameWindow.add(pacRunInstance);

        // Make the window full screen
        gd.setFullScreenWindow(gameWindow);

        // Add window listener for audio cleanup
        gameWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                AudioPlayer.cleanup();
            }
        });

        // Ensure the game has keyboard focus
        pacRunInstance.requestFocus();

        // Start bonus level
        pacRunInstance.startBonusLevel();

        // Add key listener for navigation
        pacRunInstance.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                // Empty - PacRun class will now handle the ESC key
            }
        });
    }

    // Default constructor that starts at level 1
    public MainApplication() {
        this(1); // Call the parameterized constructor with level 1
    }

    // Main method to start the application
    public static void main(String[] args) {
        // Ensure we run on the Event Dispatch Thread
        EventQueue.invokeLater(() -> {
            new MenuScreen(); // Start with the menu screen
        });
    }
}