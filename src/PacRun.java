import java.awt.*;
import java.awt.event.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import javax.swing.*;
import java.awt.AlphaComposite;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;



public class PacRun extends JPanel implements ActionListener, KeyListener {

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (gameOver || levelComplete) {
            // Only handle restart/next level on keyReleased to avoid multiple triggers
            return;
        }

        // Movement for Pac-Man (Player 1)
        int keyCode = e.getKeyCode();

        // Handle regular or reversed controls
        if (!controlsReversed) {
            // Normal controls
            if (keyCode == KeyEvent.VK_UP) {
                requestedDirection = 'U';
            } else if (keyCode == KeyEvent.VK_DOWN) {
                requestedDirection = 'D';
            } else if (keyCode == KeyEvent.VK_LEFT) {
                requestedDirection = 'L';
            } else if (keyCode == KeyEvent.VK_RIGHT) {
                requestedDirection = 'R';
            }
        } else {
            // Reversed controls
            if (keyCode == KeyEvent.VK_UP) {
                requestedDirection = 'D';
            } else if (keyCode == KeyEvent.VK_DOWN) {
                requestedDirection = 'U';
            } else if (keyCode == KeyEvent.VK_LEFT) {
                requestedDirection = 'R';
            } else if (keyCode == KeyEvent.VK_RIGHT) {
                requestedDirection = 'L';
            }
        }

        // Movement for Ghost (Player 2) in Hunter mode
        if (hunterMode && playerGhost != null) {
            if (keyCode == KeyEvent.VK_W) {
                ghostRequestedDirection = 'U';
            } else if (keyCode == KeyEvent.VK_S) {
                ghostRequestedDirection = 'D';
            } else if (keyCode == KeyEvent.VK_A) {
                ghostRequestedDirection = 'L';
            } else if (keyCode == KeyEvent.VK_D) {
                ghostRequestedDirection = 'R';
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // Return to main menu with ESC key
// Return to main menu with ESC key
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            // Stop the game loop
            if (gameLoop.isRunning()) {
                gameLoop.stop();
            }

            // Play menu music when returning to the menu
            AudioPlayer.playMusic("menu");

            // Close the current game window
            Window window = SwingUtilities.windowForComponent(this);
            if (window instanceof JFrame) {
                window.dispose();
            }

            // Create and show a new menu screen
            EventQueue.invokeLater(() -> {
                new MenuScreen();
            });
            return;
        }

        // Handle game over restart
        if (gameOver && e.getKeyCode() == KeyEvent.VK_SPACE) {
            loadLevel(1); // Restart from level 1
            gameLoop.start();
            return;
        }

// Handle level complete - space to go to next level
        if (levelComplete && e.getKeyCode() == KeyEvent.VK_SPACE) {
            boolean startLoopAfter = true; // Assume we need to start the loop

            if (currentLevel == 5) {
                // After bonus level, go back to level 1
                loadLevel(1);
            } else if (currentLevel == 4) {
                // After completing level 4, start bonus level
                startBonusLevel();
                startLoopAfter = false; // Don't restart main loop, startBonusLevel handles it
            } else if (currentLevel < 4) {
                // Normal progression for levels 1-3
                loadLevel(currentLevel + 1);
            } else {
                // After completing all levels (or other cases?), restart from level 1
                loadLevel(1);
            }

            if (startLoopAfter && gameLoop != null) { // Start loop only if needed and exists
                if(!gameLoop.isRunning()){ // Check if not already running
                    gameLoop.start();
                }
            }
            return; // Keep the return here to exit after handling space
        }

        // Level shortcuts (only in single player mode)
        if (!hunterMode) {
            if (e.getKeyCode() == KeyEvent.VK_1) {
                loadLevel(1);
                gameLoop.start();
                return;
            } else if (e.getKeyCode() == KeyEvent.VK_2) {
                loadLevel(2);
                gameLoop.start();
                return;
            } else if (e.getKeyCode() == KeyEvent.VK_3) {
                loadLevel(3);
                gameLoop.start();
                return;
            } else if (e.getKeyCode() == KeyEvent.VK_4) {
                loadLevel(4);
                gameLoop.start();
                return;
            } else if (e.getKeyCode() == KeyEvent.VK_B) {
                startBonusLevel();
                return;
            }
        }
    }

    // Inner class representing game entities such as walls, ghosts, and Pac-Man
    public class Block {
        int x;
        int y;
        int width;
        int height;
        Image image;
        Image originalImage; // Store the original image for restoration

        int startX;   // Starting X position for reset
        int startY;   // Starting Y position for reset
        char direction = 'U'; // Direction (U = Up, D = Down, L = Left, R = Right)
        int velocityX = 0;    // Horizontal velocity
        int velocityY = 0;    // Vertical velocity
        boolean vulnerable = false; // For ghost vulnerability during power-up
        boolean eaten = false;        // Track if the ghost has been eaten
        long eatenTime = 0;           // When the ghost was eaten
        long respawnDelay = 5000;     // 5 seconds before respawning

        // Constructor to initialize a Block object
        Block(Image image, int x, int y, int width, int height) {
            this.image = image;
            this.originalImage = image; // Store original image
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.startX = x;
            this.startY = y;
        }
        public Block(int x, int y, int width, int height) {
            // Call the existing constructor, passing null for the image
            this(null, x, y, width, height);
        }

        // Updates the direction and velocity of the Block
        void updateDirection(char direction) {
            char prevDirection = this.direction;
            this.direction = direction;
            updateVelocity();
            this.x += this.velocityX;
            this.y += this.velocityY;
            // Check for collisions with walls and revert movement if needed
            for (Block wall : walls) {
                if (collision(this, wall)) {
                    this.x -= this.velocityX;
                    this.y -= this.velocityY;
                    this.direction = prevDirection;
                    updateVelocity();
                }
            }
        }

        // Sets velocity based on the current direction and speed boost
        void updateVelocity() {
            int divisor = 4; // Normal speed divisor

            // Apply speed boost if active (only for Pac-Man)
            if (this == pacman && speedBoostActive) {
                divisor = 2; // Faster speed with boost
            }

            if (this.direction == 'U') {
                this.velocityX = 0;
                this.velocityY = -tileSize / divisor;
            } else if (this.direction == 'D') {
                this.velocityX = 0;
                this.velocityY = tileSize / divisor;
            } else if (this.direction == 'L') {
                this.velocityX = -tileSize / divisor;
                this.velocityY = 0;
            } else if (this.direction == 'R') {
                this.velocityX = tileSize / divisor;
                this.velocityY = 0;
            }
        }

        // Resets the Block's position to the starting coordinates
        void reset() {
            this.x = this.startX;
            this.y = this.startY;
        }
    }

    // Inner class for power pellet consumption effect
    static class PowerPelletEffect {
        int x, y;
        int radius = 5;
        int maxRadius = 40;
        float opacity = 1.0f;

        PowerPelletEffect(int x, int y) {
            this.x = x;
            this.y = y;
        }

        boolean update() {
            radius += 2;
            opacity -= 0.05f;
            return radius <= maxRadius && opacity > 0;
        }

        void draw(Graphics2D g2d) {
            g2d.setColor(new Color(255, 165, 0, (int) (255 * opacity))); // Orange color
            g2d.setStroke(new BasicStroke(2f));
            g2d.drawOval(x - radius, y - radius, radius * 2, radius * 2);
        }
    }

    // Game properties - KEEPING HARDCODED DIMENSIONS
     int rowCount = 21;
    int columnCount = 19;
     int tileSize = 32;  // Hardcoded tile size for consistent movement
     int boardWidth = columnCount * tileSize;
    int boardHeight = rowCount * tileSize;
   int offsetX;  // For centering the game horizontally
     int offsetY;  // For centering the game vertically

    // Images for different game entities
     Image wallImage;
    Image blueGhostImage;
     Image orangeGhostImage;
     Image pinkGhostImage;
    Image redGhostImage;
   Image pacmanUpImage;
     Image pacmanDownImage;
   Image pacmanLeftImage;
    Image pacmanRightImage;
    Image cherryImage;  // Image for cherry power-up
  Image appleImage;   // Image for apple power-up
    Image orangeImage;  // Image for orange power-up
   Image scaredGhostImage; // Image for scared ghosts

    // Hunter vs. Hunted mode variables
     boolean hunterMode = false;  // Flag for Hunter vs. Hunted mode
    Block playerGhost = null;    // The ghost controlled by Player 2
     int ghostPlayerScore = 0;    // Player 2's score
    int ghostLives = 2;
     char ghostRequestedDirection = ' '; // Direction requested by ghost player
   boolean ghostCamping = false;  // Flag to detect ghost camping
    long lastGhostMoveTime = 0;    // Time of last ghost movement
     final long CAMPING_THRESHOLD = 3000; // 3 seconds threshold for clamping
    long lastPositionChangeTime = 0; // Time of last significant position change
   int lastGhostX = 0;           // Last recorded X position of ghost
     int lastGhostY = 0;           // Last recorded Y position of ghost
     boolean showingGhostDeathAnimation = false; // Flag for ghost death animation
    float ghostDeathAnimationAlpha = 0.0f;      // Alpha for ghost death animation
    long ghostDeathAnimationStartTime = 0;      // Start time for ghost death animation
    // Position swap variables
    boolean swapEnabled = true;
     int swapsPerRound = 2;
    int swapsRemaining = 2;
    long nextSwapTime = 0;
     boolean swapWarningActive = false;
     long swapWarningStartTime = 0;
   final long SWAP_WARNING_DURATION = 3000; // 3 second warning
     boolean showingSwapEffect = false;
   float swapEffectAlpha = 0.0f;
     long swapEffectStartTime = 0;
     final long SWAP_EFFECT_DURATION = 1000; // 1 second animation

    // Death animation variables
     boolean showingDeathAnimation = false;
     float deathAnimationAlpha = 0.0f;
     long deathAnimationStartTime = 0;
     static final long DEATH_ANIMATION_DURATION = 1500; // 1.5 seconds for the animation
    // Hunter vs. Hunted mode variables

    // Movement improvement variables
    char requestedDirection = ' '; // Direction player wants to go

    boolean isAligned(int position, int gridSize) {
        // Check if position is aligned with the grid (allowing small offset)
        int offset = position % gridSize;
        return offset <= 3 || offset >= gridSize - 3;
    }

    // Tile map representing the game layout
    // Level 1: Basic gameplay - navigate the maze and collect pellets while avoiding ghosts
     String[] tileMap = {
            "XXXXXXXXXXXXXXXXXXX",
            "X        X        X",
            "X XX XXX X XXX XX X",
            "X                 X",
            "X XX X XXXXX X XX X",
            "X    X       X    X",
            "XXXX XXXX XXXX XXXX",
            "   X X       X X   ",
            "XXXX X XXrXX X XXXX",
            "X       bpo       X",
            "XXXX X XXXXX X XXXX",
            "   X X       X X   ",
            "XXXX X XXXXX X XXXX",
            "X        X        X",
            "X XX XXX X XXX XX X",
            "X  X     P     X  X",
            "XX X X XXXXX X X XX",
            "X    X   X   X    X",
            "X XXXXXX X XXXXXX X",
            "X                 X",
            "XXXXXXXXXXXXXXXXXXX"
    };

    // Level 2: Cherries provide speed boosts to outrun ghosts
     String[] level2Map = {
            "XXXXXXXXXXXXXXXXXXX",
            "X        X        X",
            "X XX XXX X XXX XX X",
            "X    C         C  X", // Two cherries on the top row
            "X XX X XXXXX X XX X",
            "X    X       X    X",
            "XXXX XXXX XXXX XXXX",
            "   X X       X X   ",
            "XXXX X XXrXX X XXXX",
            "X       bpo       X",
            "XXXX X XXXXX X XXXX",
            "   X X       X X   ",
            "XXXX X XXXXX X XXXX",
            "X        X        X",
            "X XX XXX X XXX XX X",
            "X  X     P     X  X",
            "XX X X XXXXX X X XX",
            "X    X   X   X    X",
            "X XXXXXX X XXXXXX X",
            "X                 X",
            "XXXXXXXXXXXXXXXXXXX"
    };

    // Level 3: Apples reverse controls, creating an additional challenge
     String[] level3Map = {
            "XXXXXXXXXXXXXXXXXXX",
            "X        X        X",
            "X XX XXX X XXX XX X",
            "X        A        X", // 'A' is an apple power-up
            "X XX X XXXXX X XX X",
            "X    X       X    X",
            "XXXX XXXX XXXX XXXX",
            "   X X       X X   ",
            "XXXX X XXrXX X XXXX",
            "X       bpo       X",
            "XXXX X XXXXX X XXXX",
            "   X X       X X   ",
            "XXXX X XXXXX X XXXX",
            "X        X        X",
            "X XX XXX X XXX XX X",
            "X  X     P     X  X",
            "XX X X XXXXX X X XX",
            "X    X   X   X    X",
            "X XXXXXX X XXXXXX X",
            "X        A        X", // Another apple power-up
            "XXXXXXXXXXXXXXXXXXX"
    };

    // Level 4: Oranges allow Pac-Man to hunt and eat ghosts for points
     String[] level4Map = {
            "XXXXXXXXXXXXXXXXXXX",
            "X        X        X",
            "X XX XXX X XXX XX X",
            "XG               GX", // Orange power-ups in corners (using 'G')
            "X XX X XXXXX X XX X",
            "X    X       X    X",
            "XXXX XXXX XXXX XXXX",
            "   X X       X X   ", // Side tunnels
            "XXXX X XXrXX X XXXX",
            "X       bpo       X",
            "XXXX X XXXXX X XXXX",
            "   X X       X X   ", // Side tunnels
            "XXXX X XXXXX X XXXX",
            "X        X        X",
            "X XX XXX X XXX XX X",
            "X  X     P     X  X",
            "XX X X XXXXX X X XX",
            "X    X   X   X    X",
            "X XXXXXX X XXXXXX X",
            "XG               GX", // Orange power-ups in corners
            "XXXXXXXXXXXXXXXXXXX"
    };

    // Bonus Level: Timed challenge to collect as many pellets as possible in 20 seconds
    String[] bonusLevelMap = {
            "XXXXXXXXXXXXXXXXXXX",
            "X        X        X",
            "X XX XXX X XXX XX X",
            "X                 X",
            "X XX X XXXXX X XX X",
            "X    X       X    X",
            "XXXX XXXX XXXX XXXX",
            "   X X       X X   ",
            "XXXX X XXrXX X XXXX",
            "X       bpo       X",
            "XXXX X XXXXX X XXXX",
            "   X X       X X   ",
            "XXXX X XXXXX X XXXX",
            "X        X        X",
            "X XX XXX X XXX XX X",
            "X  X     P     X  X",
            "XX X X XXXXX X X XX",
            "X    X   X   X    X",
            "X XXXXXX X XXXXXX X",
            "X                 X",
            "XXXXXXXXXXXXXXXXXXX"
    };

    // Game entities
    HashSet<Block> walls;
    HashSet<Block> foods;
    HashSet<Block> ghosts;
    HashSet<Block> powerUps;  // Collection for cherry power-ups
    HashSet<Block> apples;    // Collection for apple power-ups
    HashSet<Block> orangePowerUps; // Collection for orange power-ups
    ArrayList<PowerPelletEffect> pelletEffects = new ArrayList<>();
    Block pacman;

    // Game control variables
    Timer gameLoop;   // Timer to update the game periodically
    char[] directions = {'U', 'D', 'L', 'R'}; // Possible movement directions
    Random random = new Random();
    int score = 0;    // Player score
    int lives = 3;    // Player lives
    boolean gameOver = false;

    // Level tracking
    boolean levelComplete = false;
    int currentLevel = 1;

    // Speed boost variables
    boolean speedBoostActive = false;
    long speedBoostStartTime = 0;
    long SPEED_BOOST_DURATION = 5000; // 5 seconds in milliseconds

    // Reverse controls variables
    boolean controlsReversed = false;
    boolean ghostsSlowed = false;
    long reverseControlStartTime = 0;
    long REVERSE_CONTROL_DURATION = 8000; // 8 seconds in milliseconds

    // Power-up variables
    boolean poweredUp = false;
    long powerUpStartTime = 0;
    private final long POWER_UP_DURATION = 8000; // 8 seconds

    // Bonus level variables
    boolean isBonusLevel = false;
     long bonusLevelStartTime = 0;
    final long BONUS_LEVEL_DURATION = 20000; // 20 seconds
     int bonusScore = 0;
    int bonusHighScore = 0;
    int finalBonusScore = 0;
     int pelletCombo = 0;
     int maxCombo = 0;
   boolean immuneToGhosts = false;
     long immunityStartTime = 0;
    final long IMMUNITY_DURATION = 1500; // 1.5 seconds

    HashSet<Block> respawnedPellets = new HashSet<>();
     Timer pelletRespawnTimer;

    // Constructor for the game panel that accepts a level parameter
    PacRun(int level) {
        // Get screen dimensions
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        // Calculate offsets to center the game
        offsetX = (screenSize.width - boardWidth) / 2;
        offsetY = (screenSize.height - boardHeight) / 2;

        // Set panel size to full screen
        setPreferredSize(screenSize);
        setBackground(Color.BLACK);
        addKeyListener(this);
        setFocusable(true);

        // Load images for different game entities
        loadImages();

        AudioPlayer.stopMusic();

        // Initialize the game with the specified level
        loadLevel(level);

        // Start game timer (20 frames per second)
        gameLoop = new Timer(50, this);
        gameLoop.start();
    }

    // Default constructor that starts at level 1
    PacRun() {
        this(1); // Call the parameterized constructor with level 1
    }

    // Constructor for Hunter vs. Hunted mode
    public PacRun(boolean hunterMode) {
        // Get screen dimensions
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        // Calculate offsets to center the game
        offsetX = (screenSize.width - boardWidth) / 2;
        offsetY = (screenSize.height - boardHeight) / 2;

        // Set panel size to full screen
        setPreferredSize(screenSize);
        setBackground(Color.BLACK);
        addKeyListener(this);
        setFocusable(true);

        // Set the hunter mode flag
        this.hunterMode = hunterMode;

        // Load images for different game entities
        loadImages();

        // Initialize the game with level 1
        loadLevel(1);

        // Start game timer (20 frames per second)
        gameLoop = new Timer(50, this);
        gameLoop.start();
    }

    private void loadImages() {
        wallImage = new ImageIcon(getClass().getResource("/wall tile.png")).getImage();
        blueGhostImage = new ImageIcon(getClass().getResource("/blueGhost.png")).getImage();
        orangeGhostImage = new ImageIcon(getClass().getResource("/orangeGhost.png")).getImage();
        pinkGhostImage = new ImageIcon(getClass().getResource("/pinkGhost.png")).getImage();
        redGhostImage = new ImageIcon(getClass().getResource("/redGhost.png")).getImage();
        pacmanUpImage = new ImageIcon(getClass().getResource("/pacmanUp.png")).getImage();
        pacmanDownImage = new ImageIcon(getClass().getResource("/pacmanDown.png")).getImage();
        pacmanLeftImage = new ImageIcon(getClass().getResource("/pacmanLeft.png")).getImage();
        pacmanRightImage = new ImageIcon(getClass().getResource("/pacmanRight.png")).getImage();

        // Load cherry image
        try {
            cherryImage = new ImageIcon(getClass().getResource("/cherry.png")).getImage();
        } catch (Exception e) {
            // If cherry image fails to load, create a simple red circle as fallback
            cherryImage = createSimpleCherryImage();
            System.out.println("Warning: Could not load cherry.png - using fallback image");
        }

        // Load apple image
        try {
            appleImage = new ImageIcon(getClass().getResource("/apple.png")).getImage();
        } catch (Exception e) {
            // If apple image fails to load, create a simple apple as fallback
            appleImage = createSimpleAppleImage();
            System.out.println("Warning: Could not load apple.png - using fallback image");
        }

        // Load orange power-up image
        try {
            orangeImage = new ImageIcon(getClass().getResource("/orange.png")).getImage();
        } catch (Exception e) {
            // Create a simple orange if image fails to load
            orangeImage = createSimpleOrangeImage();
            System.out.println("Warning: Could not load orange.png - using fallback image");
        }

        // Load the scared ghost image
        try {
            scaredGhostImage = new ImageIcon(getClass().getResource("/scaredGhost.png")).getImage();
            System.out.println("Successfully loaded scared ghost image");
        } catch (Exception e) {
            // Create a simple blue ghost as fallback
            scaredGhostImage = createSimpleBlueGhostImage();
            System.out.println("Warning: Could not load scaredGhost.png - using fallback image: " + e.getMessage());
        }
    }

    // Helper method to create tinted versions of images
    Image createTintedImage(Image sourceImage, Color tintColor) {
        // Create a buffered image with the EXACT same dimensions as the source
        BufferedImage tintedImage = new BufferedImage(
                sourceImage.getWidth(null),
                sourceImage.getHeight(null),
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D g2d = tintedImage.createGraphics();

        // Use high quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // Draw the original image at its full size
        g2d.drawImage(sourceImage, 0, 0, null);

        // Apply tint
        g2d.setComposite(AlphaComposite.SrcAtop.derive(0.3f));
        g2d.setColor(tintColor);
        g2d.fillRect(0, 0, sourceImage.getWidth(null), sourceImage.getHeight(null));

        g2d.dispose();
        return tintedImage;
    }

    // Create a simple cherry image as fallback if loading fails
     Image createSimpleCherryImage() {
        // Create a buffered image instead of using createImage()
        BufferedImage img = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // Draw a red circle
        g.setColor(Color.RED);
        g.fillOval(4, 4, tileSize - 8, tileSize - 8);

        // Add a green stem
        g.setColor(Color.GREEN);
        g.fillRect(tileSize / 2, 0, 2, 8);

        g.dispose();
        return img;
    }

    // Create a simple apple image as fallback if loading fails
     Image createSimpleAppleImage() {
        // Create a buffered image
        BufferedImage img = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // Draw a red apple shape
        g.setColor(new Color(220, 0, 0)); // Red
        g.fillOval(2, 6, tileSize - 4, tileSize - 8);

        // Add a green stem
        g.setColor(new Color(0, 128, 0)); // Dark green
        g.fillRect(tileSize / 2 - 1, 0, 3, 6);

        // Add a small leaf
        g.setColor(new Color(0, 180, 0)); // Lighter green
        g.fillOval(tileSize / 2 + 2, 3, 5, 3);

        g.dispose();
        return img;
    }

    // Create a simple orange image as fallback
    Image createSimpleOrangeImage() {
        BufferedImage img = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // Draw an orange circle
        g.setColor(new Color(255, 165, 0)); // Orange color
        g.fillOval(2, 2, tileSize - 4, tileSize - 4);

        // Add a green stem
        g.setColor(new Color(0, 128, 0)); // Green
        g.fillRect(tileSize / 2 - 1, 0, 3, 4);

        g.dispose();
        return img;
    }

    // Create a simple blue ghost image as fallback
     Image createSimpleBlueGhostImage() {
        BufferedImage img = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // Draw a blue ghost shape
        g.setColor(new Color(0, 0, 200));
        g.fillRect(2, 2, tileSize - 4, tileSize - 4);

        // Add white eyes
        g.setColor(Color.WHITE);
        g.fillOval(tileSize / 4, tileSize / 3, 4, 4);
        g.fillOval(3 * tileSize / 4 - 2, tileSize / 3, 4, 4);

        g.dispose();
        return img;
    }

    // Method to load different levels
// Find this section in your loadLevel method (around line 425-440)
// This is where levels are initialized

     void loadLevel(int level) {
        // Stop any menu music that might be playing
        AudioPlayer.stopMusic();
        currentLevel = level;
        score = 0;
        ghostPlayerScore = 0; // Reset ghost player score for Hunter mode
        if (hunterMode) {
            lives = 2; // 2 lives in multiplayer
            ghostLives = 2; // Initialize ghost lives to 2 in multiplayer
        } else {
            lives = 3; // 3 lives in single player
        }
        gameOver = false;
        levelComplete = false;
        speedBoostActive = false;
        controlsReversed = false;
        ghostsSlowed = false;
        poweredUp = false;
        showingDeathAnimation = false;
        showingGhostDeathAnimation = false;
        ghostCamping = false;

        // Select the appropriate map
        if (level == 1) {
            // Use default level 1 map


            if (hunterMode) {
                // Create a modified map where red ghost (r) is at the top of the map
                tileMap = new String[]{
                        "XXXXXXXXXXXXXXXXXXX",
                        "X        X        X",
                        "X XX XXX X XXX XX X",
                        "X        r        X", // Red ghost (Player 2) now spawns at the top
                        "X XX X XXXXX X XX X",
                        "X    X       X    X",
                        "XXXX XXXX XXXX XXXX",
                        "   X X       X X   ",
                        "XXXX X XX XX X XXXX",
                        "X        p        X", // Only pink ghost in center
                        "XXXX X XXXXX X XXXX",
                        "   X X       X X   ",
                        "XXXX X XXXXX X XXXX",
                        "X        X        X",
                        "X XX XXX X XXX XX X",
                        "X  X     P     X  X", // Pac-Man position unchanged
                        "XX X X XXXXX X X XX",
                        "X    X   X   X    X",
                        "X XXXXXX X XXXXXX X",
                        "X                 X",
                        "XXXXXXXXXXXXXXXXXXX"
                };
            }
        } else if (level == 2) {
            tileMap = level2Map;
        } else if (level == 3) {
            tileMap = level3Map;
        } else if (level == 4) {
            tileMap = level4Map;
        }

        loadMap();
    }

    // Loads the map layout and initializes game entities
    public void loadMap() {
        walls = new HashSet<>();
        foods = new HashSet<>();
        ghosts = new HashSet<>();
        powerUps = new HashSet<>();
        apples = new HashSet<>();
        orangePowerUps = new HashSet<>();
        pelletEffects.clear();
        playerGhost = null; // Reset player ghost reference

        // Populate walls, ghosts, and Pac-Man based on tileMap layout
        for (int r = 0; r < rowCount; r++) {
            for (int c = 0; c < columnCount; c++) {
                char tile = tileMap[r].charAt(c);
                int x = c * tileSize;
                int y = r * tileSize;

                if (tile == 'X') {
                    walls.add(new Block(wallImage, x, y, tileSize, tileSize));
                } else if (tile == 'p') {
                    // Always load pink ghost in both modes
                    ghosts.add(new Block(pinkGhostImage, x, y, tileSize, tileSize));
                } else if (tile == 'b' && !hunterMode) {
                    // Only load blue ghost in single player mode
                    ghosts.add(new Block(blueGhostImage, x, y, tileSize, tileSize));
                } else if (tile == 'o' && !hunterMode) {
                    // Only load orange ghost in single player mode
                    ghosts.add(new Block(orangeGhostImage, x, y, tileSize, tileSize));
                } else if (tile == 'r') {
                    Block redGhost = new Block(redGhostImage, x, y, tileSize, tileSize);
                    ghosts.add(redGhost);
                    // In Hunter mode, set the red ghost to be player-controlled
                    if (hunterMode) {
                        playerGhost = redGhost;
                        lastGhostX = x;
                        lastGhostY = y;
                        lastPositionChangeTime = System.currentTimeMillis();
                    }
                } else if (tile == 'P') {
                    pacman = new Block(pacmanRightImage, x, y, tileSize, tileSize);
                    pacman.startX = x;
                    pacman.startY = y;
                } else if (tile == ' ') {
                    // Create small food pellets only for empty spaces within the maze
                    int foodSize = tileSize / 8;
                    int foodOffset = (tileSize - foodSize) / 2;

                    // Only add food pellets in areas that are part of the main maze
                    // Check that this isn't in one of those isolated areas by verifying
                    // it's not in rows 7-8 or 11-12 in columns 0-2 or 16-18
                    boolean isIsolatedArea =
                            ((r == 7 || r == 8 || r == 11 || r == 12) &&
                                    (c <= 2 || c >= 16));

                    if (!isIsolatedArea) {
                        foods.add(new Block(null, x + foodOffset, y + foodOffset, foodSize, foodSize));
                    }
                } else if (tile == 'C') {
                    // Cherry power-up
                    powerUps.add(new Block(cherryImage, x, y, tileSize, tileSize));
                    System.out.println("Added cherry at position: " + x + "," + y);
                } else if (tile == 'A') {
                    // Apple power-up
                    apples.add(new Block(appleImage, x, y, tileSize, tileSize));

                    // Still add a regular food pellet here so the player must collect everything
                    int foodSize = tileSize / 8;
                    int foodOffset = (tileSize - foodSize) / 2;
                    foods.add(new Block(null, x + foodOffset, y + foodOffset, foodSize, foodSize));

                    System.out.println("Added apple at position: " + x + "," + y);
                } else if (tile == 'G') {
                    // Orange power-up (using 'G' to avoid conflict with side tunnels)
                    orangePowerUps.add(new Block(orangeImage, x, y, tileSize, tileSize));

                    // Also add a regular food pellet in the same spot
                    int foodSize = tileSize / 8;
                    int foodOffset = (tileSize - foodSize) / 2;
                    foods.add(new Block(null, x + foodOffset, y + foodOffset, foodSize, foodSize));

                    System.out.println("Added orange at position: " + x + "," + y);
                }
            }
        }

        // Set random directions for ghosts at the start
        for (Block ghost : ghosts) {
            // First set basic properties
            ghost.vulnerable = false;
            ghost.eaten = false;

            // Assign specific behaviors per ghost type
            if (ghost.image == redGhostImage && !hunterMode) {
                // Red ghost always moves down initially
                ghost.direction = 'D';
                ghost.velocityX = 0;
                ghost.velocityY = tileSize / 4;
                System.out.println("Red ghost initialized with direction D");
            } else if (ghost.image == pinkGhostImage) {
                // Pink ghost always moves up initially (in both modes)
                ghost.direction = 'U';
                ghost.velocityX = 0;
                ghost.velocityY = -tileSize / 4;
                System.out.println("Pink ghost initialized with direction U");
            } else if (ghost.image == blueGhostImage) {
                // Blue ghost starts moving left
                ghost.direction = 'L';
                ghost.velocityX = -tileSize / 4;
                ghost.velocityY = 0;
                System.out.println("Blue ghost initialized with direction L");
            } else if (ghost.image == orangeGhostImage) {
                // Orange ghost starts moving right
                ghost.direction = 'R';
                ghost.velocityX = tileSize / 4;
                ghost.velocityY = 0;
                System.out.println("Orange ghost initialized with direction R");
            } else {
                // Any other ghost gets random direction
                char newDirection = directions[random.nextInt(4)];
                ghost.direction = newDirection;
                ghost.updateVelocity();
                System.out.println("Other ghost initialized with random direction: " + newDirection);
            }
        }

        // If in Hunter mode but couldn't find the red ghost, use the first ghost
        if (hunterMode && playerGhost == null && !ghosts.isEmpty()) {
            playerGhost = ghosts.iterator().next();
            playerGhost.image = redGhostImage; // Make it visually distinct
            lastGhostX = playerGhost.x;
            lastGhostY = playerGhost.y;
            lastPositionChangeTime = System.currentTimeMillis();
            System.out.println("Hunter mode: Using first ghost as player ghost");
        }

        // Initialize position swap for Hunter mode
        if (hunterMode) {
            initializePositionSwap();

            // Remove all power-ups in multiplayer mode
            powerUps.clear();
            apples.clear();
            orangePowerUps.clear();
        }

        // Debug info
        System.out.println("Level " + currentLevel + " loaded with " + powerUps.size() +
                " cherries, " + apples.size() + " apples, and " + orangePowerUps.size() + " oranges.");
        if (hunterMode) {
            System.out.println("Hunter mode active with " + ghosts.size() + " ghosts (" +
                    (ghosts.size() - 1) + " AI + 1 player-controlled)");
        }
        // Make sure ghosts start moving immediately in hunter mode
        if (hunterMode) {
            for (Block ghost : ghosts) {
                // Skip player ghost
                if (ghost == playerGhost) continue;

                // Ensure ghost has velocity
                if (ghost.velocityX == 0 && ghost.velocityY == 0) {
                    // Double-check velocity is set properly
                    ghost.updateVelocity();

                    // If still no velocity, force a direction
                    if (ghost.velocityX == 0 && ghost.velocityY == 0) {
                        if (ghost.image == pinkGhostImage) {
                            ghost.direction = 'U';  // Pink ghost starts going up
                        } else {
                            // Random direction for other ghosts
                            ghost.direction = directions[random.nextInt(4)];
                        }
                        ghost.updateVelocity();
                        System.out.println("Force-initialized ghost velocity: " + ghost.velocityX + "," + ghost.velocityY);
                    }
                }
            }
        }
    }

    //THE initializePositionSwap METHOD
     void initializePositionSwap() {
        // Initialize swap variables
        swapsRemaining = swapsPerRound;
        swapWarningActive = false;
        showingSwapEffect = false;

        // Set the first swap to occur between 30-45 seconds into the round
        nextSwapTime = System.currentTimeMillis() + 30000 + random.nextInt(15000);

        System.out.println("Position swap feature initialized. First swap in 30-45 seconds.");
    }

     void handlePositionSwaps() {
        if (!hunterMode || !swapEnabled || swapsRemaining <= 0) return;

        long currentTime = System.currentTimeMillis();

        // Check if we should start warning abouMore consistent velocity management between different game entitiesvet an upcoming swap
        if (!swapWarningActive && !showingSwapEffect && currentTime >= nextSwapTime - SWAP_WARNING_DURATION) {
            swapWarningActive = true;
            swapWarningStartTime = currentTime;
            System.out.println("Position swap warning started!");
        }

        // Check if warning period is over' and it's time to perform the swap
        if (swapWarningActive && currentTime >= nextSwapTime) {
            performPositionSwap();
            swapWarningActive = false;
            swapsRemaining--;

            // Schedule next swap (if any remaining) for 45-75 seconds later
            if (swapsRemaining > 0) {
                nextSwapTime = currentTime + 45000 + random.nextInt(30000);
                System.out.println("Next position swap scheduled in 45-75 seconds. " +
                        swapsRemaining + " swaps remaining.");
            }
        }

        // Update swap effect animation
        if (showingSwapEffect) {
            long elapsed = System.currentTimeMillis() - swapEffectStartTime;
            swapEffectAlpha = (float) elapsed / SWAP_EFFECT_DURATION;

            if (swapEffectAlpha >= 1.0f) {
                showingSwapEffect = false;
            }
        }
    }

    void performPositionSwap() {
        // Start the swap effect animation
        showingSwapEffect = true;
        swapEffectStartTime = System.currentTimeMillis();

        // Save the original positions
        int tempX = pacman.x;
        int tempY = pacman.y;

        // Swap positions
        pacman.x = playerGhost.x;
        pacman.y = playerGhost.y;
        playerGhost.x = tempX;
        playerGhost.y = tempY;

        // Reset velocities to prevent characters from moving through walls
        pacman.velocityX = 0;
        pacman.velocityY = 0;
        playerGhost.velocityX = 0;
        playerGhost.velocityY = 0;

        System.out.println("POSITION SWAP executed!");
    }

     void spawnRandomPellet() {
        // Find an empty position (not occupied by walls or ghosts)
        boolean validPosition = false;
        int x = 0, y = 0;

        // Try up to 10 times to find a valid position
        for (int attempt = 0; attempt < 10; attempt++) {
            int randomCol = 1 + random.nextInt(columnCount - 2);
            int randomRow = 1 + random.nextInt(rowCount - 2);

            x = randomCol * tileSize;
            y = randomRow * tileSize;

            // Check if this position collides with any walls
            boolean collision = false;
            for (Block wall : walls) {
                if (x >= wall.x && x < wall.x + wall.width &&
                        y >= wall.y && y < wall.y + wall.height) {
                    collision = true;
                    break;
                }
            }

            // Also check if this position already has a food pellet
            for (Block food : foods) {
                if (x >= food.x - tileSize && x < food.x + tileSize &&
                        y >= food.y - tileSize && y < food.y + tileSize) {
                    collision = true;
                    break;
                }
            }

            if (!collision) {
                validPosition = true;
                break;
            }
        }

        if (validPosition) {
            // Create a special bonus pellet that's worth more points
            int pelletSize = tileSize / 4; // Slightly larger than regular pellets
            int pelletOffset = (tileSize - pelletSize) / 2;
            Block bonusPellet = new Block(null, x + pelletOffset, y + pelletOffset, pelletSize, pelletSize);
            foods.add(bonusPellet);
            respawnedPellets.add(bonusPellet); // Track respawned pellets separately
        }
    }

     void spawnRandomPowerUp() {
        if (!isBonusLevel) return;

        // Find an empty position
        boolean validPosition = false;
        int x = 0, y = 0;

        // Try up to 10 times to find a valid position
        for (int attempt = 0; attempt < 10; attempt++) {
            int randomCol = 1 + random.nextInt(columnCount - 2);
            int randomRow = 1 + random.nextInt(rowCount - 2);

            x = randomCol * tileSize;
            y = randomRow * tileSize;

            // Check if this position collides with walls or existing items
            boolean collision = false;
            for (Block wall : walls) {
                if (x >= wall.x && x < wall.x + wall.width &&
                        y >= wall.y && y < wall.y + wall.height) {
                    collision = true;
                    break;
                }
            }

            if (!collision) {
                validPosition = true;
                break;
            }
        }

        if (validPosition) {
            // Randomly choose which power-up to spawn
            int powerUpType = random.nextInt(3);
            Block powerUp = null;

            if (powerUpType == 0) {
                // Cherry (speed boost)
                powerUp = new Block(cherryImage, x, y, tileSize, tileSize);
                powerUps.add(powerUp);
            } else if (powerUpType == 1) {
                // Apple (reverse controls)
                powerUp = new Block(appleImage, x, y, tileSize, tileSize);
                apples.add(powerUp);
            } else {
                // Orange (power up)
                powerUp = new Block(orangeImage, x, y, tileSize, tileSize);
                orangePowerUps.add(powerUp);
            }
        }
    }

   void endBonusLevel() {
        // save the final score before any resets
        finalBonusScore = bonusScore;

        // Update high score if needed
        if (bonusScore > bonusHighScore) {
            bonusHighScore = bonusScore;
        }

        // Mark bonus level as ended
        isBonusLevel = false;

        // Stop pellet respawn timer
        if (pelletRespawnTimer != null) {
            pelletRespawnTimer.stop();
            pelletRespawnTimer = null;
        }

        //Set levelComplete to true to trigger results screen
        levelComplete = true;

        // Stop the game loop to pause and display results
        gameLoop.stop();

        // Trigger a repaint to immediately show the results
        repaint();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // This method needs to handle timer events

        // First, handle any ongoing animations
        if (showingDeathAnimation) {
            long elapsed = System.currentTimeMillis() - deathAnimationStartTime;
            deathAnimationAlpha = (float) elapsed / DEATH_ANIMATION_DURATION;

            if (deathAnimationAlpha >= 1.0f) {
                // Animation completed
                showingDeathAnimation = false;
                resetPositions();  // Reset positions after death animation
                speedBoostActive = false;
                controlsReversed = false;
                ghostsSlowed = false;
                poweredUp = false;
            }

            repaint();  // Redraw to show animation progress
            return;  // Skip normal game updates during animation
        }

        // Handle ghost death animation if active
        if (showingGhostDeathAnimation) {
            long elapsed = System.currentTimeMillis() - ghostDeathAnimationStartTime;
            ghostDeathAnimationAlpha = (float) elapsed / DEATH_ANIMATION_DURATION;

            if (ghostDeathAnimationAlpha >= 1.0f) {
                showingGhostDeathAnimation = false;
            }

            repaint();
            return;
        }

        // Check for bonus level timer expiration
        if (isBonusLevel) {
            long elapsed = System.currentTimeMillis() - bonusLevelStartTime;
            if (elapsed > BONUS_LEVEL_DURATION) {
                endBonusLevel();  // This will handle everything including showing results
                return;
            }
        }

        // Handle position swaps in Hunter mode
        handlePositionSwaps();

// Normal game update code
        move();       // Update game state
        repaint();    // Redraw the screen


        // Check for game over condition
        if (gameOver) {
            gameLoop.stop();
        }
    }

     void drawBonusTimerBar(Graphics2D g2d) {
        // Calculate time remaining
        long elapsed = System.currentTimeMillis() - bonusLevelStartTime;
        long remaining = Math.max(0, BONUS_LEVEL_DURATION - elapsed);
        float percentRemaining = (float) remaining / BONUS_LEVEL_DURATION;

        // Calculate the width of the timer bar
        int maxWidth = boardWidth - 40;
        int currentWidth = (int) (maxWidth * percentRemaining);

        // Choose color based on time remaining (green -> yellow -> red)
        Color barColor;
        if (percentRemaining > 0.6f) {
            barColor = Color.GREEN;
        } else if (percentRemaining > 0.3f) {
            barColor = Color.YELLOW;
        } else {
            // Flashing red when time is running out
            if ((elapsed / 250) % 2 == 0) {
                barColor = Color.RED;
            } else {
                barColor = new Color(255, 150, 150); // Light red
            }
        }

        // Draw background
        g2d.setColor(new Color(50, 50, 50));
        g2d.fillRoundRect(offsetX + 20, offsetY - 20, maxWidth, 15, 10, 10);

        // Draw filled portion
        g2d.setColor(barColor);
        g2d.fillRoundRect(offsetX + 20, offsetY - 20, currentWidth, 15, 10, 10);

        // Draw border
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawRoundRect(offsetX + 20, offsetY - 20, maxWidth, 15, 10, 10);

        // Draw time text
        int secondsLeft = (int) (remaining / 1000);
        String timeText = secondsLeft + "s";
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(timeText);
        g2d.drawString(timeText, offsetX + 20 + (maxWidth - textWidth) / 2, offsetY - 5);
    }

     void drawBonusComboIndicator(Graphics2D g2d) {
        if (pelletCombo <= 1) return; // Don't show for just 1 pellet

        // Position at top right
        int x = offsetX + boardWidth - 100;
        int y = offsetY - 75;

        // Determine color and size based on combo tier
        float scale = Math.min(1.5f, 1.0f + pelletCombo / 20.0f);

        Color comboColor;
        if (pelletCombo >= 30) {
            comboColor = new Color(255, 0, 255); // Purple for ultimate tier
        } else if (pelletCombo >= 25) {
            comboColor = new Color(255, 50, 50); // Red for tier 5
        } else if (pelletCombo >= 20) {
            comboColor = new Color(255, 100, 0); // Dark orange for tier 4
        } else if (pelletCombo >= 15) {
            comboColor = new Color(255, 165, 0); // Orange for tier 3
        } else if (pelletCombo >= 10) {
            comboColor = new Color(255, 215, 0); // Gold for tier 2
        } else if (pelletCombo >= 6) {
            comboColor = Color.YELLOW; // Yellow for tier 1
        } else {
            comboColor = new Color(200, 200, 50); // Pale yellow for base tier
        }

        // Draw combo text with pulsing effect
        float pulse = (float) (1.0 + 0.2 * Math.sin(System.currentTimeMillis() / 150.0));
        int fontSize = (int) (18 * scale * pulse);

        g2d.setFont(new Font("Arial", Font.BOLD, fontSize));
        g2d.setColor(comboColor);

        // Display the combo count without the misleading 'x'
        String comboText = pelletCombo + " COMBO!";
        g2d.drawString(comboText, x, y);

        // Draw multiplier info (now matching the actual multiplier)
        String multiplierText;
        if (pelletCombo >= 30) {
            multiplierText = "8x POINTS!";
        } else if (pelletCombo >= 25) {
            multiplierText = "7x POINTS!";
        } else if (pelletCombo >= 20) {
            multiplierText = "6x POINTS!";
        } else if (pelletCombo >= 15) {
            multiplierText = "5x POINTS!";
        } else if (pelletCombo >= 10) {
            multiplierText = "4x POINTS!";
        } else if (pelletCombo >= 6) {
            multiplierText = "3x POINTS!";
        } else if (pelletCombo >= 3) {
            multiplierText = "2x POINTS!";
        } else {
            multiplierText = "";
        }

        if (!multiplierText.isEmpty()) {
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            g2d.drawString(multiplierText, x + 20, y + 20);
        }

        // The problematic line has been removed

        // Add visual effects for tier transitions
        if (pelletCombo == 3 || pelletCombo == 6 || pelletCombo == 10 ||
                pelletCombo == 15 || pelletCombo == 20 || pelletCombo == 25 ||
                pelletCombo == 30) {
            // Draw a special effect when reaching a new tier
            g2d.setColor(new Color(255, 255, 255, 150));
            g2d.fillRect(offsetX, offsetY, boardWidth, 5); // Flash effect at edges
        }
    }

    void drawBonusResults(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        // Semi-transparent overlay
        g2d.setColor(new Color(0, 0, 50, 200));
        g2d.fillRect(offsetX, offsetY, boardWidth, boardHeight);

        // Create a panel-like background for results
        int panelWidth = boardWidth - 80;
        int panelHeight = boardHeight - 160;
        int panelX = offsetX + 40;
        int panelY = offsetY + 80;

        // Draw panel background with gradient
        GradientPaint gradient = new GradientPaint(
                panelX, panelY, new Color(0, 0, 100),
                panelX, panelY + panelHeight, new Color(0, 0, 60)
        );
        g2d.setPaint(gradient);
        g2d.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 20, 20);

        // Draw panel border
        g2d.setColor(new Color(100, 100, 255));
        g2d.setStroke(new BasicStroke(3f));
        g2d.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 20, 20);

        // Draw title
        g2d.setFont(new Font("Arial", Font.BOLD, 32));
        g2d.setColor(Color.YELLOW);
        String title = "BONUS ROUND RESULTS";
        FontMetrics fm = g2d.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        g2d.drawString(title, panelX + (panelWidth - titleWidth) / 2, panelY + 50);

        // Draw results text
        g2d.setFont(new Font("Arial", Font.BOLD, 24));
        g2d.setColor(Color.WHITE);

        int textY = panelY + 120;
        int leftMargin = panelX + 60;

        // Just show the score
        g2d.drawString("Score: " + finalBonusScore, leftMargin, textY);
        textY += 40;


        // Draw star rating based on score
        int stars = 0;
        if (finalBonusScore >= 1000) stars = 5;
        else if (finalBonusScore >= 750) stars = 4;
        else if (finalBonusScore >= 500) stars = 3;
        else if (finalBonusScore >= 250) stars = 2;
        else if (finalBonusScore > 0) stars = 1;

        g2d.drawString("Rating: ", leftMargin, textY);

        // Draw stars
        for (int i = 0; i < 5; i++) {
            if (i < stars) {
                // Filled star
                g2d.setColor(Color.YELLOW);
            } else {
                // Empty star
                g2d.setColor(Color.GRAY);
            }
            drawStar(g2d, leftMargin + 120 + i * 40, textY - 10, 15);
        }

        // Draw continue message
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        g2d.setColor(new Color(100, 255, 100));
        String continueMsg = "Press SPACE to continue";
        fm = g2d.getFontMetrics();
        int msgWidth = fm.stringWidth(continueMsg);
        g2d.drawString(continueMsg, panelX + (panelWidth - msgWidth) / 2, panelY + panelHeight - 40);
    }

    // Helper method to draw a star
     void drawStar(Graphics2D g, int x, int y, int size) {
        int[] xPoints = new int[10];
        int[] yPoints = new int[10];

        for (int i = 0; i < 10; i++) {
            double angle = Math.PI / 2 + i * Math.PI / 5;
            int r = (i % 2 == 0) ? size : size / 2;
            xPoints[i] = x + (int) (r * Math.cos(angle));
            yPoints[i] = y - (int) (r * Math.sin(angle));
        }

        g.fillPolygon(xPoints, yPoints, 10);
    }

    // Modified method for drawing the Pac-Man style logo - positioned higher and larger
     void drawPacRunHeader(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Make the logo wider and taller
        int width = 250; // Increased from 200
        int height = 60; // Increased from 50
        int x = offsetX + (boardWidth - width) / 2;

        // Position logo much higher up to avoid any overlap with game info
        // This will place it well above the score/level/lives row
        int y = offsetY - height - 80; // Increased distance from board

        // Only draw if there's space above the board
        if (y > 0) {
            // Draw orange background rectangle
            g2d.setColor(new Color(255, 140, 0)); // Orange
            g2d.fillRoundRect(x, y, width, height, 15, 15);

            // Draw red border - made slightly thicker
            g2d.setColor(Color.RED);
            g2d.setStroke(new BasicStroke(5f)); // Increased from 4f
            g2d.drawRoundRect(x, y, width, height, 15, 15);

            // Draw the "PACRUN" text in Pac-Man style with larger font
            g2d.setColor(Color.YELLOW);
            Font pacmanFont = null;
            try {
                // Try to load the Pac-Man font with larger size
                pacmanFont = Font.createFont(Font.TRUETYPE_FONT,
                        getClass().getResourceAsStream("/pac-font.ttf"));
                pacmanFont = pacmanFont.deriveFont(Font.BOLD, 38f); // Increased from 30f
                g2d.setFont(pacmanFont);
            } catch (Exception e) {
                // Fallback to Arial if font loading fails
                g2d.setFont(new Font("Arial", Font.BOLD, 38)); // Increased from 30
            }

            // Draw text with outline
            FontMetrics fm = g2d.getFontMetrics();
            String text = "PAC-RUN";
            int textX = x + width / 2 - fm.stringWidth(text) / 2;
            int textY = y + height / 2 + fm.getAscent() / 3;

            // Draw outline - made slightly thicker
            g2d.setColor(Color.BLACK);
            for (int i = -3; i <= 3; i++) { // Increased range from -2/2 to -3/3
                for (int j = -3; j <= 3; j++) {
                    if (i != 0 || j != 0) {
                        g2d.drawString(text, textX + i, textY + j);
                    }
                }
            }

            // Draw text
            g2d.setColor(Color.YELLOW);
            g2d.drawString(text, textX, textY);
        }
    }

     void drawLivesDisplay(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Position the HUD row right below the title but above the maze
        int hudY = offsetY - 35;

        // Set font for all text elements
        g2d.setFont(new Font("Arial", Font.BOLD, 18));
        g2d.setColor(Color.WHITE);

        // Draw Score on the left side
        String scoreText = "Score: " + score;
        g2d.drawString(scoreText, offsetX + 10, hudY);

        // Draw Level in the center
        String levelText;
        if (isBonusLevel) {
            levelText = "BONUS ROUND";
            // Use a special color for this text
            g2d.setColor(new Color(255, 215, 0)); // Gold
        } else {
            levelText = "Level: " + currentLevel;
        }
        FontMetrics fm = g2d.getFontMetrics();
        int levelX = offsetX + (boardWidth - fm.stringWidth(levelText)) / 2;
        g2d.drawString(levelText, levelX, hudY);

        // Restore color if we changed it
        if (isBonusLevel) {
            g2d.setColor(Color.WHITE);
        }

        // Only draw lives if NOT in bonus level
        if (!isBonusLevel) {
            // Draw Lives text on the right
            String livesText = "Lives:";
            int livesTextWidth = fm.stringWidth(livesText);

            // Fixed width calculation for lives display - this prevents shifting
            int iconSize = tileSize - 10; // Slightly smaller than tile size
            int maxLives = 5; // Maximum number of lives to show
            int displayedLives = Math.min(lives, maxLives);
            int totalIconsWidth = displayedLives * iconSize;
            int spaceBetween = 5 * (displayedLives - 1);
            int totalLivesWidth = livesTextWidth + 10 + totalIconsWidth + spaceBetween;

            // Fix the position to be static, not dependent on the number of lives
            int livesX = offsetX + boardWidth - 200; // Fixed position, adjust as needed

            g2d.drawString(livesText, livesX, hudY);

            // Draw Pac-Man icons for each life
            int iconsStartX = livesX + livesTextWidth + 10;

            for (int i = 0; i < displayedLives; i++) {
                int x = iconsStartX + (i * (iconSize + 5));

                // Use Pac-Man image if available, otherwise draw yellow circle with mouth
                if (pacmanRightImage != null) {
                    g2d.drawImage(pacmanRightImage, x, hudY - 15, iconSize, iconSize, null);
                } else {
                    g2d.setColor(Color.YELLOW);
                    g2d.fillArc(x, hudY - 15, iconSize, iconSize, 30, 300);
                }
            }
        }
    }
    // Method to draw Hunter vs. Hunted UI
     void drawHunterModeUI(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Set font
        Font scoreFont = new Font("Arial", Font.BOLD, 16);
        g2d.setFont(scoreFont);

        // Draw mode indicator
        g2d.setColor(Color.WHITE);
        String modeText = "PELLET RACE MODE";
        int boardCenter = offsetX + (boardWidth / 2);
        int modeX = boardCenter - (g2d.getFontMetrics().stringWidth(modeText) / 2);
        g2d.drawString(modeText, modeX, offsetY - 35);

        // Draw Player 1 (Pac-Man) info
        g2d.setColor(Color.YELLOW);
        String p1Text = "Pac-Man: " + score;
        g2d.drawString(p1Text, offsetX + 10, offsetY - 35);

        // Draw Player 1 lives
        int iconSize = tileSize - 10;
        int iconSpacing = 5;
        int p1IconsStartX = offsetX + 10 + g2d.getFontMetrics().stringWidth(p1Text) + 10;

        for (int i = 0; i < lives; i++) {
            int x = p1IconsStartX + (i * (iconSize + iconSpacing));
            g2d.drawImage(pacmanRightImage, x, offsetY - 50, iconSize, iconSize, null);
        }

        // Draw Player 2 (Ghost) info
        g2d.setColor(Color.RED);  // Red for the ghost player
        String p2Text = "Ghost: " + ghostPlayerScore;
        int p2TextWidth = g2d.getFontMetrics().stringWidth(p2Text);
        int p2TextX = offsetX + boardWidth - p2TextWidth - 10;
        g2d.drawString(p2Text, p2TextX, offsetY - 35);

        // Draw Player 2 lives (ghost icons)
        int p2IconsStartX = p2TextX - ((iconSize + iconSpacing) * 2) - 10; // Position to the left of score

        // Draw the actual number of ghost lives
        for (int i = 0; i < ghostLives; i++) {
            int x = p2IconsStartX + (i * (iconSize + iconSpacing));
            // Use the red ghost image for the lives indicator
            g2d.drawImage(redGhostImage, x, offsetY - 50, iconSize, iconSize, null);
        }

        // Draw ghost camping indicator if needed
        if (ghostCamping && playerGhost != null) {
            g2d.setColor(new Color(255, 0, 0, 150));  // Semi-transparent red
            String campingText = "CAMPING DETECTED!";
            int campX = offsetX + (boardWidth - g2d.getFontMetrics().stringWidth(campingText)) / 2;
            g2d.drawString(campingText, campX, offsetY - 10);

            // Draw fading effect around the ghost
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            g2d.fillOval(
                    playerGhost.x + offsetX - 5,
                    playerGhost.y + offsetY - 5,
                    playerGhost.width + 10,
                    playerGhost.height + 10
            );
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        }
    }
     void drawSwapWarningAndEffect(Graphics g) {
        if (!hunterMode) return;

        Graphics2D g2d = (Graphics2D) g;

        // Draw swap warning
        if (swapWarningActive) {
            // Calculate warning intensity (pulsing effect)
            long elapsed = System.currentTimeMillis() - swapWarningStartTime;
            float pulseIntensity = (float) Math.abs(Math.sin(elapsed / 250.0));

            // Draw warning border around the screen
            g2d.setColor(new Color(255, 50, 50, (int) (100 + 155 * pulseIntensity)));
            g2d.setStroke(new BasicStroke(5.0f));
            g2d.drawRect(offsetX, offsetY, boardWidth, boardHeight);

            // Draw warning text
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            g2d.setColor(new Color(255, 50, 50, (int) (150 + 105 * pulseIntensity)));
            String warning = "POSITION SWAP IMMINENT!";
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(warning);
            g2d.drawString(warning, offsetX + (boardWidth - textWidth) / 2, offsetY - 10);
        }

        // Draw swap effect animation
        if (showingSwapEffect) {
            // Save original composite
            Composite originalComposite = g2d.getComposite();

            // Flash effect across the screen
            float flash = Math.min(1.0f, 1.0f - Math.abs(swapEffectAlpha - 0.5f) * 2);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, flash));
            g2d.setColor(new Color(255, 0, 255)); // Purple flash
            g2d.fillRect(offsetX, offsetY, boardWidth, boardHeight);

            // Draw "SWAPPED!" text
            if (swapEffectAlpha > 0.3f && swapEffectAlpha < 0.7f) {
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                g2d.setFont(new Font("Arial", Font.BOLD, 36));
                g2d.setColor(Color.WHITE);
                String message = "POSITIONS SWAPPED!";
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(message);
                g2d.drawString(message, offsetX + (boardWidth - textWidth) / 2, offsetY + boardHeight / 2);
            }

            // Restore composite
            g2d.setComposite(originalComposite);
        }
    }

    // Draws all game entities
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    public void draw(Graphics g) {
        // Create a gradient background instead of plain black
        Graphics2D g2d = (Graphics2D) g;
        GradientPaint gradient = new GradientPaint(
                0, 0, new Color(0, 0, 40), // Very dark blue at top
                0, getHeight(), new Color(0, 0, 15) // Even darker blue at bottom
        );
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Draw the PAC-RUN logo header
        drawPacRunHeader(g);

        // Display UI based on game mode
        if (hunterMode) {
            drawHunterModeUI(g);
            // Draw position swap warning and effect in Hunter mode
            drawSwapWarningAndEffect(g);
        } else {
            // Original single player UI
            drawLivesDisplay(g);


            // Add bonus level UI elements if in bonus level
            if (isBonusLevel) {
                // Draw timer bar at the top
                drawBonusTimerBar(g2d);

                // Draw bonus score
                g2d.setColor(Color.YELLOW);
                g2d.setFont(new Font("Arial", Font.BOLD, 18));
                g2d.drawString("BONUS: " + bonusScore, offsetX + 10, offsetY - 50);

                // Draw combo indicator
                drawBonusComboIndicator(g2d);

                // If player is immune, draw a shield effect around Pac-Man
                if (immuneToGhosts) {
                    // Calculate shield opacity based on remaining immunity time
                    long elapsed = System.currentTimeMillis() - immunityStartTime;
                    float opacity = 1.0f - (float) elapsed / IMMUNITY_DURATION;

                    g2d.setColor(new Color(100, 100, 255, (int) (200 * opacity)));
                    int shieldSize = tileSize + 10;
                    g2d.fillOval(
                            pacman.x + offsetX - 5,
                            pacman.y + offsetY - 5,
                            shieldSize, shieldSize
                    );
                }
            }
        }

        // Draw walls with offset
        for (Block wall : walls) {
            g.drawImage(wall.image, wall.x + offsetX, wall.y + offsetY, wall.width, wall.height, null);
        }

        // Draw food pellets with offset
        g.setColor(Color.WHITE);
        for (Block food : foods) {
            g.fillRect(food.x + offsetX, food.y + offsetY, food.width, food.height);
        }

        // Draw power-ups with offset
        for (Block powerUp : powerUps) {
            g.drawImage(powerUp.image, powerUp.x + offsetX, powerUp.y + offsetY, powerUp.width, powerUp.height, null);
        }

        // Draw apples with offset
        for (Block apple : apples) {
            g.drawImage(apple.image, apple.x + offsetX, apple.y + offsetY, apple.width, apple.height, null);
        }

        // Draw orange power-ups with offset
        for (Block orange : orangePowerUps) {
            g.drawImage(orange.image, orange.x + offsetX, orange.y + offsetY, orange.width, orange.height, null);
        }

        // Draw power pellet consumption effects
        Iterator<PowerPelletEffect> effectIterator = pelletEffects.iterator();
        while (effectIterator.hasNext()) {
            PowerPelletEffect effect = effectIterator.next();
            if (!effect.update()) {
                effectIterator.remove();
            } else {
                effect.draw(g2d);
            }
        }

        // Draw ghosts with offset - apply visual effect if slowed
        for (Block ghost : ghosts) {
            // Skip drawing eaten ghosts
            if (ghost.eaten) {
                continue;
            }

            // Always draw the ghost using its current image property
            g.drawImage(ghost.image, ghost.x + offsetX, ghost.y + offsetY, ghost.width, ghost.height, null);
            // For the player-controlled ghost in Hunter mode
            if (hunterMode && ghost == playerGhost) {
                // Add a pulsing glow effect to show it's player-controlled
                float pulse = (float) (0.5 + 0.3 * Math.sin(System.currentTimeMillis() / 200.0));
                g2d.setColor(new Color(255, 0, 0, (int) (150 * pulse)));
                int glowSize = ghost.width + 10;
                g2d.fillOval(
                        ghost.x + offsetX - 5,
                        ghost.y + offsetY - 5,
                        glowSize, glowSize
                );

                // Add a "P2" indicator above the ghost
                g2d.setFont(new Font("Arial", Font.BOLD, 10));
                g2d.setColor(Color.WHITE);
                g2d.drawString("P2", ghost.x + offsetX + 11, ghost.y + offsetY - 5);
            }

            // Add special effects on top as needed

            // For slowed ghosts that aren't vulnerable, add blue tint
            if (ghostsSlowed && !ghost.vulnerable) {
                g.setColor(new Color(0, 0, 255, 80));
                g.fillRect(ghost.x + offsetX, ghost.y + offsetY, ghost.width, ghost.height);
            }

            // For vulnerable ghosts when power-up is about to end, add flashing
            if (ghost.vulnerable && poweredUp) {
                long elapsed = System.currentTimeMillis() - powerUpStartTime;
                if (elapsed > POWER_UP_DURATION - 2000) { // Last 2 seconds
                    // Flash between blue and white every 250ms
                    if ((elapsed / 250) % 2 == 0) {
                        g.setColor(new Color(255, 255, 255, 100));
                        g.fillRect(ghost.x + offsetX, ghost.y + offsetY, ghost.width, ghost.height);
                    }
                }
            }

            // For the player-controlled ghost in Hunter mode
            if (hunterMode && ghost == playerGhost) {
                // Add a subtle glow effect to show it's player-controlled
                g2d.setColor(new Color(255, 0, 0, 50));
                int glowSize = ghost.width + 10;
                g2d.fillOval(
                        ghost.x + offsetX - 5,
                        ghost.y + offsetY - 5,
                        glowSize, glowSize
                );
            }
        }

        // Draw Pac-Man with offset
        if (pacman != null) {
            g.drawImage(pacman.image, pacman.x + offsetX, pacman.y + offsetY, pacman.width, pacman.height, null);
        }

        // Draw death animation if active
        if (showingDeathAnimation) {
            drawDeathAnimation(g);
        }

        // Draw ghost death animation if active
        if (showingGhostDeathAnimation) {
            drawGhostDeathAnimation(g);
        }

        // Display game over message if needed
        if (gameOver) {
            g.setFont(new Font("Arial", Font.BOLD, 18));
            g.setColor(Color.WHITE);
            g.drawString("Game Over: " + score, offsetX + tileSize / 2, offsetY + tileSize / 2);
            g.drawString("Press any key to restart", offsetX + tileSize / 2, offsetY + tileSize + tileSize / 2);
        } else {
            // Display speed boost indicator when active
            if (speedBoostActive) {
                g.setColor(Color.YELLOW);
                g.drawString("SPEED BOOST!", offsetX + boardWidth - 150, offsetY + tileSize / 2);

                // Draw boost timer bar
                long elapsed = System.currentTimeMillis() - speedBoostStartTime;
                long remaining = SPEED_BOOST_DURATION - elapsed;
                int barWidth = (int) (remaining * 100 / SPEED_BOOST_DURATION);
                g.fillRect(offsetX + boardWidth - 150, offsetY + tileSize, barWidth, 8);
            }

            // Display reversed controls indicator
            if (controlsReversed) {
                // Add a purple border to indicate reversed controls
                g.setColor(new Color(128, 0, 128, 120));
                g.fillRect(offsetX, offsetY, boardWidth, 5); // Top border
                g.fillRect(offsetX, offsetY + boardHeight - 5, boardWidth, 5); // Bottom border
                g.fillRect(offsetX, offsetY, 5, boardHeight); // Left border
                g.fillRect(offsetX + boardWidth - 5, offsetY, 5, boardHeight); // Right border

                // Draw "CONTROLS REVERSED" text
                g.setColor(new Color(255, 0, 255));
                g.setFont(new Font("Arial", Font.BOLD, 14));
                g.drawString("CONTROLS REVERSED", offsetX + 10, offsetY + boardHeight - 10);

                // Draw timer bar for reverse controls
                long elapsed = System.currentTimeMillis() - reverseControlStartTime;
                long remaining = REVERSE_CONTROL_DURATION - elapsed;
                int barWidth = (int) (remaining * 100 / REVERSE_CONTROL_DURATION);
                g.fillRect(offsetX + 170, offsetY + boardHeight - 10, barWidth, 5);
            }

            // Display power-up indicator when active
            if (poweredUp) {
                g.setColor(new Color(255, 165, 0)); // Orange color
                g.drawString("GHOST HUNT!", offsetX + 10, offsetY + tileSize / 2);

                // Draw power-up timer bar
                long elapsed = System.currentTimeMillis() - powerUpStartTime;
                long remaining = POWER_UP_DURATION - elapsed;
                int barWidth = (int) (remaining * 100 / POWER_UP_DURATION);
                g.fillRect(offsetX + 10, offsetY + tileSize, barWidth, 8);
            }
        }

        // Draw level complete screen
        if (levelComplete) {
            drawLevelComplete(g);
        }
    }

    // Draw death animation
     void drawDeathAnimation(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        // Save the original composite
        Composite originalComposite = g2d.getComposite();

        // Calculate the position for the center of the animation
        int centerX = pacman.x + offsetX + (pacman.width / 2);
        int centerY = pacman.y + offsetY + (pacman.height / 2);

        // First phase: Pac-Man spin and expand effect (first half of animation)
        if (deathAnimationAlpha < 0.5f) {
            float phase = deathAnimationAlpha * 2; // 0 to 1 during first half

            // Rotate and grow Pac-Man
            int size = pacman.width + (int) (phase * 20); // Grows by 20 pixels
            int angle = (int) (phase * 720) % 360; // Two full rotations

            g2d.setColor(Color.YELLOW);
            g2d.fillArc(centerX - size / 2, centerY - size / 2, size, size, angle, 330 - (int) (phase * 330));
        }

        // Second phase: Fade out with expanding circles (second half of animation)
        else {
            float phase = (deathAnimationAlpha - 0.5f) * 2; // 0 to 1 during second half

            // Draw expanding circles with decreasing opacity
            int numCircles = 5;
            for (int i = 0; i < numCircles; i++) {
                float circlePhase = phase + ((float) i / numCircles);
                if (circlePhase > 1.0f) circlePhase = 1.0f;

                int size = pacman.width + (int) (circlePhase * 100); // Expand up to 100 pixels
                float opacity = 1.0f - circlePhase;

                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
                g2d.setColor(Color.YELLOW);
                g2d.fillOval(centerX - size / 2, centerY - size / 2, size, size);
            }
        }

        // Overlay a fading white flash across the screen
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                Math.max(0, 0.7f - deathAnimationAlpha)));
        g2d.setColor(Color.WHITE);
        g2d.fillRect(offsetX, offsetY, boardWidth, boardHeight);

        // Restore the original composite
        g2d.setComposite(originalComposite);
    }

    // Draw ghost death animation
    void drawGhostDeathAnimation(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        // Save the original composite
        Composite originalComposite = g2d.getComposite();

        // Calculate the position for the center of the animation
        int centerX = playerGhost.x + offsetX + (playerGhost.width / 2);
        int centerY = playerGhost.y + offsetY + (playerGhost.height / 2);

        // Animation effects similar to Pac-Man death but with red color
        float phase = ghostDeathAnimationAlpha;

        // Draw expanding circles with decreasing opacity
        int numCircles = 5;
        for (int i = 0; i < numCircles; i++) {
            float circlePhase = phase + ((float) i / numCircles);
            if (circlePhase > 1.0f) circlePhase = 1.0f;

            int size = playerGhost.width + (int) (circlePhase * 100); // Expand up to 100 pixels
            float opacity = 1.0f - circlePhase;

            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
            g2d.setColor(Color.RED);
            g2d.fillOval(centerX - size / 2, centerY - size / 2, size, size);
        }

        // Overlay a fading white flash across the screen
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                Math.max(0, 0.5f - ghostDeathAnimationAlpha)));
        g2d.setColor(Color.WHITE);
        g2d.fillRect(offsetX, offsetY, boardWidth, boardHeight);

        // Restore the original composite
        g2d.setComposite(originalComposite);
    }

     void drawLevelComplete(Graphics g) {
        // For Hunter vs. Hunted mode
        if (hunterMode) {
            // Draw Hunter vs. Hunted results
            g.setColor(new Color(0, 0, 0, 180));
            g.fillRect(offsetX, offsetY, boardWidth, boardHeight);

            // Text for game end
            String message;
            if (gameOver) {
                // Determine which player lost all lives
                if (lives <= 0) {
                    message = "Pac-Man Lost All Lives!";
                    g.setColor(Color.RED); // Ghost color for Pac-Man's loss
                } else if (ghostLives <= 0) {
                    message = "Ghost Lost All Lives!";
                    g.setColor(Color.YELLOW); // Pac-Man color for Ghost's loss
                } else {
                    message = "Game Over!";
                    g.setColor(Color.WHITE);
                }
            } else {
                message = "Round Complete!";
                g.setColor(Color.WHITE);
            }

            // Draw message
            g.setFont(new Font("Arial", Font.BOLD, 40));
            FontMetrics fm = g.getFontMetrics();
            int messageX = offsetX + (boardWidth - fm.stringWidth(message)) / 2;
            int messageY = offsetY + boardHeight / 2 - 100;
            g.drawString(message, messageX, messageY);

            // Draw Player 1 results
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.setColor(Color.YELLOW);
            String p1Result = "Pac-Man: " + score + " points";
            int p1X = offsetX + (boardWidth - fm.stringWidth(p1Result)) / 2;
            g.drawString(p1Result, p1X, offsetY + boardHeight / 2 - 40);

            // Draw Player 2 results
            g.setColor(Color.RED);
            String p2Result = "Ghost: " + ghostPlayerScore + " points";
            int p2X = offsetX + (boardWidth - fm.stringWidth(p2Result)) / 2;
            g.drawString(p2Result, p2X, offsetY + boardHeight / 2);

            // Determine and display the winner based on score
            g.setFont(new Font("Arial", Font.BOLD, 32));
            String winnerText;

            if (score > ghostPlayerScore) {
                g.setColor(Color.YELLOW);
                winnerText = "Pac-Man Wins!";
            } else if (ghostPlayerScore > score) {
                g.setColor(Color.RED);
                winnerText = "Ghost Wins!";
            } else {
                g.setColor(Color.WHITE);
                winnerText = "It's a Tie!";
            }

            int winnerX = offsetX + (boardWidth - g.getFontMetrics().stringWidth(winnerText)) / 2;
            g.drawString(winnerText, winnerX, offsetY + boardHeight / 2 + 60);

            // Return to menu message
            g.setFont(new Font("Arial", Font.BOLD, 20));
            g.setColor(Color.WHITE);
            String returnMsg = "Press SPACE to return to menu";
            int msgX = offsetX + (boardWidth - g.getFontMetrics().stringWidth(returnMsg)) / 2;
            g.drawString(returnMsg, msgX, offsetY + boardHeight / 2 + 120);
            return;
        }


        // For single player mode
        if (isBonusLevel) {
            // Draw the bonus level results instead
            drawBonusResults(g);
            return;
        }

        // Original level complete code for regular levels
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(offsetX, offsetY, boardWidth, boardHeight);

        // "Level Complete" message
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.setColor(Color.YELLOW);
        String message = "Level Complete!";
        FontMetrics fm = g.getFontMetrics();
        int messageX = offsetX + (boardWidth - fm.stringWidth(message)) / 2;
        int messageY = offsetY + boardHeight / 2 - 40;
        g.drawString(message, messageX, messageY);

        // Next level or game complete message
        g.setFont(new Font("Arial", Font.BOLD, 24));
        String nextMessage;
        if (currentLevel < 4) {
            nextMessage = "Press SPACE for next level";
        } else if (currentLevel == 4) {
            nextMessage = "Press SPACE for bonus round!";
        } else {
            nextMessage = "Game Complete! Press SPACE to restart";
        }

        fm = g.getFontMetrics();
        int nextX = offsetX + (boardWidth - fm.stringWidth(nextMessage)) / 2;
        int nextY = offsetY + boardHeight / 2 + 40;
        g.drawString(nextMessage, nextX, nextY);
    }

    // Moves Pac-Man and updates the game state
    public void move() {
        // Move the player-controlled ghost in Hunter mode
        if (hunterMode && playerGhost != null) {
            // Use the same turn mechanism as Pac-Man
            tryGhostTurn();

            // Apply movement directly like we do for Pac-Man
            playerGhost.x += playerGhost.velocityX;
            playerGhost.y += playerGhost.velocityY;

            // Handle wall collisions exactly like Pac-Man
            for (Block wall : walls) {
                if (collision(playerGhost, wall)) {
                    playerGhost.x -= playerGhost.velocityX;
                    playerGhost.y -= playerGhost.velocityY;
                    break;
                }
            }

            ghostCamping = false; // Always set to false in Pellet Race mode
        }

        // Check if speed boost has expired
        if (speedBoostActive) {
            long elapsed = System.currentTimeMillis() - speedBoostStartTime;
            if (elapsed > SPEED_BOOST_DURATION) {
                speedBoostActive = false;
                // Update velocity with normal speed
                if (pacman != null) {
                    pacman.updateVelocity();
                }
            }
        }

        // Check if reverse controls have expired
        if (controlsReversed || ghostsSlowed) {
            long elapsed = System.currentTimeMillis() - reverseControlStartTime;
            if (elapsed > REVERSE_CONTROL_DURATION) {
                controlsReversed = false;
                ghostsSlowed = false;
            }
        }

        // Check if power-up has expired
        if (poweredUp) {
            long elapsed = System.currentTimeMillis() - powerUpStartTime;
            if (elapsed > POWER_UP_DURATION) {
                // End power-up
                poweredUp = false;

                // Reset ghosts to normal
                for (Block ghost : ghosts) {
                    ghost.vulnerable = false;
                    ghost.image = ghost.originalImage; // Restore original image
                }
            }
        }

        // Try to turn in the requested direction if one exists
        if (requestedDirection != ' ' && pacman != null) {
            tryTurn();
        }

        // Move Pac-Man if it exists
        if (pacman != null) {
            pacman.x += pacman.velocityX;
            pacman.y += pacman.velocityY;

            // Handle wall collisions for Pac-Man
            for (Block wall : walls) {
                if (collision(pacman, wall)) {
                    pacman.x -= pacman.velocityX;
                    pacman.y -= pacman.velocityY;
                    break;
                }
            }
        }
        // Only check for player ghost pellet collection if the ghost exists
        if (hunterMode && playerGhost != null) {
            Block p2FoodEaten = null;
            for (Block food : foods) {
                if (collision(playerGhost, food)) {
                    p2FoodEaten = food;
                    ghostPlayerScore += 10; // Same points as Player 1
                    break;
                }
            }

            if (p2FoodEaten != null) {
                foods.remove(p2FoodEaten);
            }
        }
        // Check if Pac-Man eats a food pellet
        Block foodEaten = null;
        if (pacman != null) {
            for (Block food : foods) {
                if (collision(pacman, food)) {
                    foodEaten = food;

                    // Add this line to play the munching sound
                    AudioPlayer.playSound("munch");

                    if (isBonusLevel) {
                        // In bonus level, pellets are worth more points
                        int pelletPoints = respawnedPellets.contains(food) ? 25 : 15;

                        // Extended tier system for consecutive pellets
                        if (pelletCombo >= 30) {
                            // Ultimate tier - 8x multiplier for exceptional play
                            pelletPoints *= 8;
                        } else if (pelletCombo >= 25) {
                            // Tier 5 - 7x multiplier
                            pelletPoints *= 7;
                        } else if (pelletCombo >= 20) {
                            // Tier 4 - 6x multiplier
                            pelletPoints *= 6;
                        } else if (pelletCombo >= 15) {
                            // Tier 3 - 5x multiplier
                            pelletPoints *= 5;
                        } else if (pelletCombo >= 10) {
                            // Tier 2 - 4x multiplier
                            pelletPoints *= 4;
                        } else if (pelletCombo >= 6) {
                            // Tier 1 - 3x multiplier (adjusted threshold)
                            pelletPoints *= 3;
                        } else if (pelletCombo >= 3) {
                            // Base tier - 2x multiplier
                            pelletPoints *= 2;
                        }
                        // Default is 1x (no multiplier)

                        score += pelletPoints;
                        bonusScore += pelletPoints;

                        // Increment combo
                        pelletCombo++;
                        if (pelletCombo > maxCombo) {
                            maxCombo = pelletCombo;
                        }

                        // Spawn new power-up occasionally based on combo
                        if (pelletCombo % 10 == 0) {
                            spawnRandomPowerUp();
                        }
                    } else {
                        score += 10;
                    }
                    break;
                }
            }
        }

        if (foodEaten != null) {
            foods.remove(foodEaten);
            respawnedPellets.remove(foodEaten);
        }

        // Check if Pac-Man collects a power-up (cherry)
        Block powerUpCollected = null;
        if (pacman != null) {
            for (Block powerUp : powerUps) {
                if (collision(pacman, powerUp)) {
                    powerUpCollected = powerUp;
                    activateSpeedBoost();
                    score += 50; // Extra points for cherry
                    break;
                }
            }
        }

        if (powerUpCollected != null) {
            powerUps.remove(powerUpCollected);
        }

        // Check if Pac-Man collects an apple (reverse controls power-up)
        Block appleCollected = null;
        if (pacman != null) {
            for (Block apple : apples) {
                if (collision(pacman, apple)) {
                    appleCollected = apple;
                    activateReverseControls();
                    score += 30; // Extra points for the challenge
                    break;
                }
            }
        }

        if (appleCollected != null) {
            apples.remove(appleCollected);
        }

        // Check if Pac-Man collects an orange power-up
        Block orangeCollected = null;
        if (pacman != null) {
            for (Block orange : orangePowerUps) {
                if (collision(pacman, orange)) {
                    orangeCollected = orange;
                    activatePowerUp();
                    score += 50; // Extra points for orange power-up

                    // Add visual effect at the position of the collected orange
                    pelletEffects.add(new PowerPelletEffect(
                            orange.x + orange.width / 2,
                            orange.y + orange.height / 2
                    ));
                    break;
                }
            }
        }

        if (orangeCollected != null) {
            orangePowerUps.remove(orangeCollected);
        }
// Check collisions between Pac-Man and ghosts
        for (Block ghost : ghosts) {
            // Check if this is the player-controlled ghost in Hunter mode
            boolean isPlayerGhost = (hunterMode && ghost == playerGhost);

            // Skip collision checks for eaten ghosts
            if (ghost.eaten) {
                continue;
            }

            // Player 1 (Pac-Man) collision with ghosts
            if (pacman != null && collision(ghost, pacman)) {
                if (poweredUp && ghost.vulnerable && !ghost.eaten) {
                    // Pac-Man eats the ghost
                    ghost.eaten = true;
                    ghost.eatenTime = System.currentTimeMillis();

                    // Bonus for eating ghost
                    int ghostPoints = isBonusLevel ? 300 : 200;
                    score += ghostPoints;
                    if (isBonusLevel) bonusScore += ghostPoints;

                    if (isPlayerGhost) {
                        // Player 2's ghost was eaten
                        ghostPlayerScore -= 50; // Penalty for being eaten

                        // Show ghost death animation
                        showingGhostDeathAnimation = true;
                        ghostDeathAnimationStartTime = System.currentTimeMillis();
                        ghostDeathAnimationAlpha = 0.0f;

                        // Add visual effect
                        pelletEffects.add(new PowerPelletEffect(
                                ghost.x + ghost.width / 2,
                                ghost.y + ghost.height / 2
                        ));
                    }
                } else if (!ghost.eaten && !immuneToGhosts) {
                    // Ghost catches Pac-Man
                    if (isPlayerGhost) {
                        // In multiplayer, P2 (player ghost) should NOT kill Pac-Man
                        // They should just pass through each other
                        System.out.println("Pac-Man and P2 collided - no effect in multiplayer mode");
                    } else if (isBonusLevel) {
                        // BONUS LEVEL: Just lose points and get brief immunity
                        int penalty = Math.min(100, bonusScore);
                        bonusScore -= penalty;
                        score -= penalty;
                        pelletCombo = 0;

                        // isual effect
                        pelletEffects.add(new PowerPelletEffect(
                                ghost.x + ghost.width / 2,
                                ghost.y + ghost.height / 2
                        ));

                        // Give temporary immunity
                        immuneToGhosts = true;
                        immunityStartTime = System.currentTimeMillis();
                    }
                    else {
                        // Normal AI ghost caught Pac-Man - lose a life
                        lives--;
                        AudioPlayer.playSound("death");

                        // **KEY CHANGE: Set the animation flag *before* checking game over**
                        showingDeathAnimation = true;
                        deathAnimationStartTime = System.currentTimeMillis();
                        deathAnimationAlpha = 0.0f; // Reset animation progress

                        // Stop Pac-Man movement
                        if (pacman != null) {
                            pacman.velocityX = 0;
                            pacman.velocityY = 0;
                        }

                        // Pause ghost movement
                        for (Block g : ghosts) {
                            if (g != null) {
                                g.velocityX = 0;
                                g.velocityY = 0;
                            }
                        }

                        // Check for game over *after* setting animation flag and stopping movement
                        if (lives <= 0) {
                            gameOver = true;
                            // No return needed here, the main return below handles it
                        }

                        // Return now, as Pac-Man died in this tick and movement/other logic should stop.
                        // The animation will proceed via actionPerformed checking showingDeathAnimation.
                        return;
                    }
                }
            }

            // In your move() method, find the section where Player 2 (ghost) collides with AI ghosts
// P2 (player ghost) collision with AI ghosts
            if (hunterMode && playerGhost != null && ghost != playerGhost) {
                // Check if player ghost collides with AI ghost (pink ghost)
                if (collision(ghost, playerGhost) && !isPlayerGhost) {
                    // Player ghost gets caught by AI ghost - lose a life instead of just points
                    ghostLives--; // Decrement ghost lives

                    // Check if ghost has run out of lives
                    if (ghostLives <= 0) {
                        // Game over for ghost player
                        gameOver = true;
                        return;
                    }

                    // Show ghost death animation
                    showingGhostDeathAnimation = true;
                    ghostDeathAnimationStartTime = System.currentTimeMillis();
                    ghostDeathAnimationAlpha = 0.0f;

                    // Add visual effect at collision point
                    pelletEffects.add(new PowerPelletEffect(
                            playerGhost.x + playerGhost.width / 2,
                            playerGhost.y + playerGhost.height / 2
                    ));

                    // Reset player ghost position
                    playerGhost.reset();

                    // Also reset ghost's velocity to prevent moving right after respawn
                    playerGhost.velocityX = 0;
                    playerGhost.velocityY = 0;
                    ghostRequestedDirection = ' ';

                    // Penalty for losing a life - no longer needed since we're tracking lives
                    // ghostPlayerScore -= 50; // Remove or reduce this penalty

                    // If P2 has a negative score, it can't go below 0
                    if (ghostPlayerScore < 0) {
                        ghostPlayerScore = 0;
                    }

                    break;
                }
            }

            // Check if eaten ghost should respawn
            if (ghost.eaten) {
                long elapsedSinceEaten = System.currentTimeMillis() - ghost.eatenTime;
                if (elapsedSinceEaten > ghost.respawnDelay) {
                    // Time to respawn
                    ghost.eaten = false;
                    ghost.reset();  // Move back to starting position

                    // Make vulnerable again if power-up is still active
                    ghost.vulnerable = poweredUp;
                    ghost.image = poweredUp ? scaredGhostImage : ghost.originalImage;
                }

                // Skip movement for eaten ghosts
                continue;
            }

            // Skip AI movement for player-controlled ghost
            if (isPlayerGhost) {
                continue;
            }

            // Determine target based on ghost personality and game state
            Block target = pacman; // Default target is Pac-Man

            // Add this in your move() method, in the ghost personality section
// If ghost is vulnerable during power-up, make it try to run away
            if (ghost.vulnerable) {
                // Target a position that's in the opposite direction from Pac-Man
                int fleeX = ghost.x * 2 - pacman.x;
                int fleeY = ghost.y * 2 - pacman.y;
                // Create a temporary block that ghosts will move toward (away from Pac-Man)
                Block fleeTarget = new Block(null, fleeX, fleeY, tileSize, tileSize);
                target = fleeTarget;
                System.out.println("Ghost is fleeing from Pac-Man (vulnerable state)");
            }
// Otherwise, each ghost has its own personality based on its image
            else if (ghost.image == redGhostImage) {
                // Red ghost - directly chases Pac-Man (already set as default)
                if (!hunterMode || ghost != playerGhost) {
                    System.out.println("Red ghost chasing");
                }
            } else if (ghost.image == pinkGhostImage) {
                // Pink ghost - aims ahead of Pac-Man's direction
                int lookAheadTiles = 4;
                int targetX = pacman.x;
                int targetY = pacman.y;

                if (pacman.direction == 'U') targetY -= lookAheadTiles * tileSize;
                else if (pacman.direction == 'D') targetY += lookAheadTiles * tileSize;
                else if (pacman.direction == 'L') targetX -= lookAheadTiles * tileSize;
                else if (pacman.direction == 'R') targetX += lookAheadTiles * tileSize;

                Block aheadTarget = new Block(null, targetX, targetY, tileSize, tileSize);
                target = aheadTarget;
                System.out.println("Pink ghost ambushing");
            } else if (ghost.image == blueGhostImage) {
                // Blue ghost - more random, but still somewhat pursues Pac-Man
                if (random.nextInt(3) == 0) { // 1/3 chance of random movement
                    int randX = random.nextInt(boardWidth);
                    int randY = random.nextInt(boardHeight);
                    Block randomTarget = new Block(null, randX, randY, tileSize, tileSize);
                    target = randomTarget;
                    System.out.println("Blue ghost wandering");
                } else {
                    System.out.println("Blue ghost chasing");
                }
            } else if (ghost.image == orangeGhostImage) {
                // Orange ghost - chases directly if far, acts randomly if close
                double distance = calculateDistance(
                        ghost.x / tileSize, ghost.y / tileSize,
                        pacman.x / tileSize, pacman.y / tileSize);

                if (distance > 8) {
                    // Far away, chase directly (already set as default)
                    System.out.println("Orange ghost chasing from afar");
                } else {
                    // Close to Pac-Man, scatter to a corner
                    int cornerX = (random.nextBoolean() ? 1 : columnCount - 2) * tileSize;
                    int cornerY = (random.nextBoolean() ? 1 : rowCount - 2) * tileSize;
                    Block cornerTarget = new Block(null, cornerX, cornerY, tileSize, tileSize);
                    target = cornerTarget;
                    System.out.println("Orange ghost retreating");
                }
            }

            // Get the best direction for this ghost to move
            char newDirection = getGhostDirection(ghost, target);

            // Only update direction if we're changing it
            if (newDirection != ghost.direction) {
                ghost.updateDirection(newDirection);
            }

            // Apply ghost slowdown effect if active
            float speedMultiplier = ghostsSlowed ? 0.5f : 1.0f;
            float adjustedVelocityX = ghost.velocityX * speedMultiplier;
            float adjustedVelocityY = ghost.velocityY * speedMultiplier;

            // Move the ghost
            ghost.x += adjustedVelocityX;
            ghost.y += adjustedVelocityY;

            // Check for wall collisions (this should rarely happen now, but kept as safety)
            for (Block wall : walls) {
                if (collision(ghost, wall)) {
                    ghost.x -= adjustedVelocityX;
                    ghost.y -= adjustedVelocityY;
                    break;
                }
            }
        }

        // Check if immunity has expired
        if (immuneToGhosts) {
            long immunityElapsed = System.currentTimeMillis() - immunityStartTime;
            if (immunityElapsed > IMMUNITY_DURATION) {
                immuneToGhosts = false;
            }
        }

        // Check if level is complete (all food eaten)
        if (foods.isEmpty()) {
            if (isBonusLevel) {
                // In bonus level, don't end early when all pellets are eaten
                // Just spawn more pellets
                for (int i = 0; i < 5; i++) {
                    spawnRandomPellet();
                }
            } else {
                levelComplete = true;
                gameLoop.stop();
            }
        }
    }



    // Pac-Man turn method
     void tryTurn() {
        if (pacman == null) return;

        char originalDirection = pacman.direction;
        boolean canTurn = false;

        // First, check if Pac-Man is aligned with the grid for turning
        boolean alignedX = isAligned(pacman.x, tileSize);
        boolean alignedY = isAligned(pacman.y, tileSize);

        // Determine if we can turn based on alignment and requested direction
        if ((requestedDirection == 'U' || requestedDirection == 'D') && alignedX) {
            canTurn = true;
            // Slightly adjust X position to align with grid
            pacman.x = Math.round(pacman.x / tileSize) * tileSize;
        } else if ((requestedDirection == 'L' || requestedDirection == 'R') && alignedY) {
            canTurn = true;
            // Slightly adjust Y position to align with grid
            pacman.y = Math.round(pacman.y / tileSize) * tileSize;
        }

        if (canTurn) {
            // Try to change direction
            if (requestedDirection == 'U') {
                pacman.updateDirection('U');
                pacman.image = pacmanUpImage;
            } else if (requestedDirection == 'D') {
                pacman.updateDirection('D');
                pacman.image = pacmanDownImage;
            } else if (requestedDirection == 'L') {
                pacman.updateDirection('L');
                pacman.image = pacmanLeftImage;
            } else if (requestedDirection == 'R') {
                pacman.updateDirection('R');
                pacman.image = pacmanRightImage;
            }

            // If we couldn't actually turn (due to wall), revert to original direction
            if (pacman.direction != originalDirection) {
                // We successfully turned - clear the requested direction
                requestedDirection = ' ';
            }
        }
    }

     void tryGhostTurn() {
        if (playerGhost == null || ghostRequestedDirection == ' ') return;

        char originalDirection = playerGhost.direction;
        boolean canTurn = false;

        // Use exactly the same alignment checks as Pac-Man
        boolean alignedX = isAligned(playerGhost.x, tileSize);
        boolean alignedY = isAligned(playerGhost.y, tileSize);

        // Apply the same turn logic as Pac-Man
        if ((ghostRequestedDirection == 'U' || ghostRequestedDirection == 'D') && alignedX) {
            canTurn = true;
            // Align exactly the same way as Pac-Man
            playerGhost.x = Math.round(playerGhost.x / tileSize) * tileSize;
        } else if ((ghostRequestedDirection == 'L' || ghostRequestedDirection == 'R') && alignedY) {
            canTurn = true;
            // Align exactly the same way as Pac-Man
            playerGhost.y = Math.round(playerGhost.y / tileSize) * tileSize;
        }

        if (canTurn) {
            // Try to change direction using the same method Pac-Man uses
            playerGhost.updateDirection(ghostRequestedDirection);

            // Only clear request on successful turn, just like with Pac-Man
            if (playerGhost.direction != originalDirection) {
                ghostRequestedDirection = ' ';
                lastGhostMoveTime = System.currentTimeMillis();
                lastGhostX = playerGhost.x;
                lastGhostY = playerGhost.y;
                lastPositionChangeTime = System.currentTimeMillis();
                ghostCamping = false;
            }
        }
    }

    // Activate speed boost
     void activateSpeedBoost() {
        speedBoostActive = true;
        speedBoostStartTime = System.currentTimeMillis();
        // Play power-up sound
        AudioPlayer.playSound("power");
        // Update velocity with boosted speed
        if (pacman != null) {
            pacman.updateVelocity();
        }
    }

    // Activate reverse controls and ghost slowdown
     void activateReverseControls() {
        controlsReversed = true;
        ghostsSlowed = true;
        reverseControlStartTime = System.currentTimeMillis();

        // Play power-up sound
        AudioPlayer.playSound("power");
    }
    // Activate power-up for ghost hunting
     void activatePowerUp() {
        poweredUp = true;
        powerUpStartTime = System.currentTimeMillis();

        // Play power-up sound
        AudioPlayer.playSound("power");

        System.out.println("Activating power-up: changing " + ghosts.size() + " ghosts to scared appearance");

        // Make all ghosts vulnerable
        for (Block ghost : ghosts) {
            // Skip player ghost in hunter mode
            if (hunterMode && ghost == playerGhost) {
                continue;
            }

            // The original image is already stored in ghost.originalImage
            ghost.image = scaredGhostImage; // Change to scared appearance
            ghost.vulnerable = true;
        }
    }

    // Checks if two blocks collide
    public boolean collision(Block a, Block b) {
        // Add null checks to prevent NullPointerException
        if (a == null || b == null) {
            return false;
        }

        return a.x < b.x + b.width &&
                a.x + a.width > b.x &&
                a.y < b.y + b.height &&
                a.y + a.height > b.y;
    }

    // Helper method to calculate distance between two points
     double calculateDistance(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    // Helper method to check if a tile is a wall
     boolean isWallAt(int tileX, int tileY) {
        // Make sure we don't go out of bounds
        if (tileX < 0 || tileX >= columnCount || tileY < 0 || tileY >= rowCount) {
            return true;
        }

        // Check if the tile is a wall in your map
        return tileMap[tileY].charAt(tileX) == 'X';
    }

    // Method to determine the best direction for a ghost to move
    char getGhostDirection(Block ghost, Block target) {
        // Only consider changing direction at grid intersections
        if (!isAligned(ghost.x, tileSize) || !isAligned(ghost.y, tileSize)) {
            return ghost.direction; // Keep current direction until aligned
        }

        // Round to ,the nearest tile for calculations
        int ghostTileX = Math.round(ghost.x / tileSize);
        int ghostTileY = Math.round(ghost.y / tileSize);
        int targetTileX = Math.round(target.x / tileSize);
        int targetTileY = Math.round(target.y / tileSize);

        // Calculate which directions are valid (not walls)
        boolean canGoUp = !isWallAt(ghostTileX, ghostTileY - 1);
        boolean canGoDown = !isWallAt(ghostTileX, ghostTileY + 1);
        boolean canGoLeft = !isWallAt(ghostTileX - 1, ghostTileY);
        boolean canGoRight = !isWallAt(ghostTileX + 1, ghostTileY);

        // Don't allow reversing direction (prevents ghosts from going back and forth)
        if (ghost.direction == 'D') canGoUp = false;
        if (ghost.direction == 'U') canGoDown = false;
        if (ghost.direction == 'R') canGoLeft = false;
        if (ghost.direction == 'L') canGoRight = false;

        // If no valid directions, allow reversing as a last resort
        if (!canGoUp && !canGoDown && !canGoLeft && !canGoRight) {
            if (ghost.direction == 'D') canGoUp = !isWallAt(ghostTileX, ghostTileY - 1);
            if (ghost.direction == 'U') canGoDown = !isWallAt(ghostTileX, ghostTileY + 1);
            if (ghost.direction == 'R') canGoLeft = !isWallAt(ghostTileX - 1, ghostTileY);
            if (ghost.direction == 'L') canGoRight = !isWallAt(ghostTileX + 1, ghostTileY);
        }

        // Calculate distance for each valid direction
        double currentMinDistance = Double.MAX_VALUE;
        char bestDirection = ghost.direction;

        if (canGoUp) {
            double distance = calculateDistance(ghostTileX, ghostTileY - 1, targetTileX, targetTileY);
            if (distance < currentMinDistance) {
                currentMinDistance = distance;
                bestDirection = 'U';
            }
        }

        if (canGoDown) {
            double distance = calculateDistance(ghostTileX, ghostTileY + 1, targetTileX, targetTileY);
            if (distance < currentMinDistance) {
                currentMinDistance = distance;
                bestDirection = 'D';
            }
        }

        if (canGoLeft) {
            double distance = calculateDistance(ghostTileX - 1, ghostTileY, targetTileX, targetTileY);
            if (distance < currentMinDistance) {
                currentMinDistance = distance;
                bestDirection = 'L';
            }
        }

        if (canGoRight) {
            double distance = calculateDistance(ghostTileX + 1, ghostTileY, targetTileX, targetTileY);
            if (distance < currentMinDistance) {
                currentMinDistance = distance;
                bestDirection = 'R';
            }
        }

        // Occasionally make a random valid move (for variety)
        if (random.nextInt(20) == 0) {
            ArrayList<Character> validDirections = new ArrayList<>();
            if (canGoUp) validDirections.add('U');
            if (canGoDown) validDirections.add('D');
            if (canGoLeft) validDirections.add('L');
            if (canGoRight) validDirections.add('R');

            if (!validDirections.isEmpty()) {
                bestDirection = validDirections.get(random.nextInt(validDirections.size()));
            }
        }

        return bestDirection;
    }

    // Resets positions of Pac-Man and ghosts
    public void resetPositions() {
        if (pacman != null) {
            pacman.reset();
            pacman.velocityX = 0;
            pacman.velocityY = 0;
        }

        for (Block ghost : ghosts) {
            ghost.reset();
            ghost.updateDirection(directions[random.nextInt(4)]);
        }
    }

    public void startBonusLevel() {
        // Stop any music that might be playing
        AudioPlayer.stopMusic();
        // Add this safety check at the beginning of the method
        if (hunterMode) {
            System.out.println("Bonus level not available in multiplayer mode");
            return;
        }

        isBonusLevel = true;
        currentLevel = 5; // Track as level 5 for progression
        bonusLevelStartTime = System.currentTimeMillis();
        bonusScore = 0;
        pelletCombo = 0;
        maxCombo = 0;
        immuneToGhosts = false;

        // Reset positions
        resetPositions();

        // Clear existing power-ups and pellets
        powerUps.clear();
        apples.clear();
        orangePowerUps.clear();
        respawnedPellets.clear();

        // Load the bonus level map
        tileMap = bonusLevelMap;
        loadMap();

        // Start pellet respawn timer
        if (pelletRespawnTimer != null) {
            pelletRespawnTimer.stop();
        }
        pelletRespawnTimer = new Timer(2000, e -> {
            if (isBonusLevel) {
                spawnRandomPellet();
            }
        });
        pelletRespawnTimer.start();

        // Randomly place some power-ups at the start
        spawnRandomPowerUp();
        spawnRandomPowerUp();

        // Start game timer if not already running
        if (!gameLoop.isRunning()) {
            gameLoop.start();
        }

        System.out.println("Bonus level started!");
    }
}

