import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.AlphaComposite;
import java.awt.geom.RoundRectangle2D;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.AffineTransform;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.Shape;

public class MenuScreen extends JFrame {
    // Animation variables
    private Timer animationTimer;
    private int pacX = 50, pacY = 50;
    private int pacDirection = 1; // 1 = right, -1 = left
    private int ghostX = 700, ghostY = 100;
    // Animation variables for enhanced menu
    private ArrayList<MenuGhost> ghosts;
    private ArrayList<MazeElement> mazeElements;
    private Image redGhostImage, blueGhostImage, pinkGhostImage, orangeGhostImage;
    private Random random = new Random();

    // Enhanced background effects
    private ArrayList<Star> stars = new ArrayList<>();
    private ArrayList<Pellet> pellets = new ArrayList<>();
    private Color[] backgroundColors = {
            new Color(0, 0, 40),    // Dark blue
            new Color(20, 0, 60),   // Medium blue
            new Color(0, 0, 15)     // Very dark blue
    };
    private float colorTransitionTime = 0;

    // Font for the entire menu
    private Font pacmanFont;
    private Font pacmanTitleFont = null;

    // Level selection components
    private JDialog levelSelectDialog;
    private JPanel thumbnailPanel;
    private boolean levelSelectOpen = false;
    private int selectedLevel = 1;

    // Level thumbnail images - updated to include Level 4
    private BufferedImage[] levelThumbnails = new BufferedImage[5];


    // Button declarations - added here
    private JButton selectModeButton;
    private JButton howToPlayButton;
    private JButton exitButton;

    public MenuScreen() {
        // Basic window setup
        setTitle("PacRun - Main Menu");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


        setUndecorated(true);

        // Get screen dimensions for full screen
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(screenSize.width, screenSize.height);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        try {
            // Keep loading your original font
            pacmanFont = Font.createFont(Font.TRUETYPE_FONT,
                    getClass().getResourceAsStream("/pac-font.ttf"));

            // Also load the new title-specific font
            pacmanTitleFont = Font.createFont(Font.TRUETYPE_FONT,
                    getClass().getResourceAsStream("/pacman-title-font.ttf"));

            System.out.println("Title font loaded successfully");
        } catch (Exception e) {
            System.out.println("Could not load font: " + e.getMessage());
            e.printStackTrace();
            pacmanTitleFont = null;
        }
        // Load ghost images
        try {
            redGhostImage = new ImageIcon(getClass().getResource("/redGhost.png")).getImage();
            blueGhostImage = new ImageIcon(getClass().getResource("/blueGhost.png")).getImage();
            pinkGhostImage = new ImageIcon(getClass().getResource("/pinkGhost.png")).getImage();
            orangeGhostImage = new ImageIcon(getClass().getResource("/orangeGhost.png")).getImage();
        } catch (Exception e) {
            System.out.println("Could not load ghost images: " + e.getMessage());
        }
        // Initialize menu animations
        initializeMenuAnimations();

        // Initialize star field effect (150-250 stars)
        int numStars = 150 + random.nextInt(100);
        for (int i = 0; i < numStars; i++) {
            stars.add(new Star(screenSize.width, screenSize.height));
        }

        // Add some floating Pac-Man pellets (30-50)
        int numPellets = 30 + random.nextInt(20);
        for (int i = 0; i < numPellets; i++) {
            pellets.add(new Pellet(screenSize.width, screenSize.height));
        }

        // main panel with an enhanced gradient background
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Create a cycling gradient background that slowly shifts over time
                int colorIndex = (int) colorTransitionTime;
                float fraction = colorTransitionTime - colorIndex;

                Color color1 = backgroundColors[colorIndex % backgroundColors.length];
                Color color2 = backgroundColors[(colorIndex + 1) % backgroundColors.length];

                // Interpolate between the two colors
                int r = (int) (color1.getRed() * (1 - fraction) + color2.getRed() * fraction);
                int gr = (int) (color1.getGreen() * (1 - fraction) + color2.getGreen() * fraction);
                int b = (int) (color1.getBlue() * (1 - fraction) + color2.getBlue() * fraction);

                Color topColor = new Color(r, gr, b);
                Color bottomColor = new Color(Math.max(0, r - 20), Math.max(0, gr - 20), Math.max(0, b - 20));

                GradientPaint gradient = new GradientPaint(
                        0, 0, topColor,
                        0, getHeight(), bottomColor
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // Draw animated stars first (behind everything)
                for (Star star : stars) {
                    star.draw(g2d);
                }

                // Draw maze-like patterns in the background
                g2d.setColor(new Color(20, 20, 100, 20));
                for (int x = 0; x < getWidth(); x += 100) {
                    for (int y = 0; y < getHeight(); y += 100) {
                        g2d.drawRoundRect(x, y, 80, 80, 20, 20);
                    }
                }

                // Draw floating pellets
                for (Pellet pellet : pellets) {
                    pellet.draw(g2d);
                }

// Draw ghosts
                if (ghosts != null) {
                    for (MenuGhost ghost : ghosts) {
                        ghost.draw(g2d);
                    }
                }

            }
        };
        mainPanel.setLayout(new BorderLayout());

        // Create a panel for the Pac-Man style logo
        PacManLogoPanel logoPanel = new PacManLogoPanel();
        logoPanel.setPreferredSize(new Dimension(screenSize.width, 300)); // Increased from 200
        logoPanel.setOpaque(false);

        // Create the buttons BEFORE using them
        selectModeButton = createStyledButton("Select Mode", Color.YELLOW);
        howToPlayButton = createStyledButton("How to Play", Color.CYAN);
        exitButton = createStyledButton("Exit", new Color(255, 100, 100));

        // Set up the button panel with the already-created buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setOpaque(false);

// Make buttons center-aligned horizontally
        selectModeButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        howToPlayButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        exitButton.setAlignmentX(Component.CENTER_ALIGNMENT);

// Add vertical spacing between components
        buttonPanel.add(Box.createVerticalGlue()); // Add space at top
        buttonPanel.add(selectModeButton);
        buttonPanel.add(Box.createVerticalStrut(40)); // Increased from 20 pixels spacing
        buttonPanel.add(howToPlayButton);
        buttonPanel.add(Box.createVerticalStrut(40)); // Increased from 20 pixels spacing
        buttonPanel.add(exitButton);
        buttonPanel.add(Box.createVerticalGlue()); // Add space at bottom
        // Add action listeners
        selectModeButton.addActionListener(e -> {
            showModeSelectDialog();
        });

        // Replace it with:
        howToPlayButton.addActionListener(e -> {
            showEnhancedHowToPlayDialog();
        });

        exitButton.addActionListener(e -> System.exit(0));

        // Add components to the main panel
        mainPanel.add(logoPanel, BorderLayout.NORTH);

        // Center the buttons in the middle of the screen
        JPanel centeringPanel = new JPanel(new GridBagLayout());
        centeringPanel.setOpaque(false);
        centeringPanel.add(buttonPanel);
        mainPanel.add(centeringPanel, BorderLayout.CENTER);

        // Add main panel to frame
        add(mainPanel);

        // Set up animation timer with more frequent updates for smoother animations
        animationTimer = new Timer(40, e -> {
            // Update Pac-Man position
            pacX += 5 * pacDirection;
            if (pacX > getWidth()) pacX = -50;
            if (pacX < -50) pacX = getWidth();

            // Update ghost position
            ghostX -= 3;
            if (ghostX < -50) ghostX = getWidth();

            // Update ghost animations
            if (ghosts != null) {
                for (MenuGhost ghost : ghosts) {
                    ghost.update();
                }
            }

            // Update star positions
            for (Star star : stars) {
                star.update();
            }

            // Update pellet positions
            for (Pellet pellet : pellets) {
                pellet.update();
            }

            // Cycle background colors
            colorTransitionTime += 0.005f;
            if (colorTransitionTime > 3) {
                colorTransitionTime = 0;
            }

            // Request repaint to update animations
            mainPanel.repaint();

            // Update level select dialog if it's open
            if (levelSelectOpen && levelSelectDialog != null && levelSelectDialog.isVisible()) {
                thumbnailPanel.repaint();
            }
        });
        animationTimer.start();

        AudioPlayer.playMusic("menu");

        // Make sure the window is visible
        setVisible(true);
    }

    // Initialize menu animations with ghosts and maze elements
    private void initializeMenuAnimations() {
        // Create animated Pac-Man character that moves across the screen
        pacX = -50; // Start off-screen
        pacY = getHeight() / 3;
        pacDirection = 1; // Moving right

        // Create multiple ghosts with different starting positions and speeds
        ghosts = new ArrayList<>();

        // Use your existing ghost images
        Image[] ghostImages = {
                redGhostImage,
                blueGhostImage,
                pinkGhostImage,
                orangeGhostImage
        };

        // Add 4 ghosts with different positions
        for (int i = 0; i < 4; i++) {
            MenuGhost ghost = new MenuGhost();
            ghost.image = ghostImages[i];
            ghost.x = random.nextInt(getWidth());
            ghost.y = 100 + random.nextInt(getHeight() / 2);
            ghost.speed = 2 + random.nextInt(3); // Random speed between 2-4
            ghost.direction = random.nextBoolean() ? 1 : -1; // Random direction
            ghosts.add(ghost);
        }

        // Create maze-like patterns for the background
        mazeElements = new ArrayList<>();
        for (int i = 0; i < 15; i++) { // Add 15 maze elements
            MazeElement maze = new MazeElement();
            maze.x = random.nextInt(getWidth());
            maze.y = random.nextInt(getHeight());
            maze.width = 100 + random.nextInt(150);
            maze.height = 100 + random.nextInt(150);
            maze.cornerRadius = 20 + random.nextInt(15);
            maze.opacity = 0.1f + (random.nextFloat() * 0.15f); // Subtle opacity between 0.1-0.25
            mazeElements.add(maze);
        }
    }

    private void createLevelThumbnails() {
        // Create level 1 thumbnail with number in title
        levelThumbnails[0] = createLevelThumbnail(1,
                "Navigate the maze and collect all pellets while avoiding ghosts",
                new Color(50, 50, 200));

        // Create level 2 thumbnail with number in title
        levelThumbnails[1] = createLevelThumbnail(2,
                "Find cherries for speed boosts to outrun ghosts",
                new Color(50, 150, 50));

        // Create level 3 thumbnail with number in title
        levelThumbnails[2] = createLevelThumbnail(3,
                "Watch out! Apples reverse your controls",
                new Color(150, 50, 150));

        // Create level 4 thumbnail with number in title
        levelThumbnails[3] = createLevelThumbnail(4,
                "Collect oranges to hunt and eat ghosts for points",
                new Color(255, 165, 0));

        // Create bonus level thumbnail
        levelThumbnails[4] = createLevelThumbnail(5,
                "BONUS ROUND: Collect as many pellets as possible in 20 seconds!",
                new Color(255, 215, 0)); // Gold color for bonus level
    }
    private BufferedImage createLevelThumbnail(int level, String description, Color bgColor) {
        // Create consistent thumbnails with the same dimensions
        int width = 280;
        int height = 240;
        BufferedImage thumbnail = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = thumbnail.createGraphics();

        // Enable high-quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        // Background with smooth gradient
        GradientPaint gradient;
        if (level == 5) {
            // Special golden gradient for bonus level
            gradient = new GradientPaint(
                    0, 0, new Color(255, 215, 0),
                    0, height, new Color(180, 150, 0));
        } else {
            // Regular gradient for normal levels
            gradient = new GradientPaint(
                    0, 0, bgColor.brighter(),
                    0, height, bgColor.darker().darker());
        }
        g2d.setPaint(gradient);
        g2d.fillRoundRect(0, 0, width, height, 20, 20);

        // Add consistently styled border
        g2d.setColor(Color.WHITE);
        g2d.setStroke(new BasicStroke(4f));
        g2d.drawRoundRect(4, 4, width - 8, height - 8, 16, 16);

        // Draw level title with SMALLER font size to ensure it fits properly
        if (pacmanFont != null) {
            // Set font consistently for all levels
            g2d.setFont(pacmanFont.deriveFont(Font.BOLD, 32f));

            // Prepare the level text
            String levelText = (level == 5) ? "BONUS" : "LEVEL " + level;
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(levelText);
            int textX = (width - textWidth) / 2;

            // Draw shadow for all levels consistently
            g2d.setColor(Color.BLACK);
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    if (i != 0 || j != 0) { // Skip center position (will be white text)
                        g2d.drawString(levelText, textX + i, 55 + j);
                    }
                }
            }

            // Draw the main text in white for all levels
            g2d.setColor(Color.WHITE);
            g2d.drawString(levelText, textX, 55);
        } else {
            // Fallback font - smaller size
            g2d.setFont(new Font("Arial", Font.BOLD, 32));
            String levelText = (level == 5) ? "BONUS" : "LEVEL " + level;
            FontMetrics fm = g2d.getFontMetrics();
            int textWidth = fm.stringWidth(levelText);
            int textX = (width - textWidth) / 2;

            // Draw shadow with fallback font too
            g2d.setColor(Color.BLACK);
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    if (i != 0 || j != 0) {
                        g2d.drawString(levelText, textX + i, 55 + j);
                    }
                }
            }

            // Draw the main text
            g2d.setColor(Color.WHITE);
            g2d.drawString(levelText, textX, 55);
        }

        // Draw description with proper centering
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        FontMetrics fm = g2d.getFontMetrics();

        // Create a text area for description with padding
        int textAreaWidth = width - 30;

        // Split description into lines
        String[] lines = wrapText(description, textAreaWidth, fm);

        // Draw description centered
        int lineHeight = fm.getHeight();
        int y = 85; // Start text a bit higher to make room for feature illustrations

        for (String line : lines) {
            int lineWidth = fm.stringWidth(line);
            int x = (width - lineWidth) / 2;
            g2d.drawString(line, x, y);
            y += lineHeight;
        }

        // Feature area dimensions
        int featureX = 20;
        int featureY = 140;
        int featureWidth = width - 40;
        int featureHeight = 70;

        // Draw feature area based on level
        try {
            // Blue background for feature area
            g2d.setColor(new Color(0, 0, 150));
            g2d.fillRoundRect(featureX, featureY, featureWidth, featureHeight, 10, 10);

            // Create a common layout structure for all levels
            // We'll use a consistent image size and position
            int imageSize = 32; // Standard image size
            int imageX = featureX + 20; // Left side position for icon
            int imageY = featureY + (featureHeight - imageSize) / 2; // Vertically centered
            int labelY = featureY + featureHeight/2 + 5; // Vertically centered text

            // Define color for the basic layout elements
            g2d.setColor(Color.WHITE);

            switch (level) {
                case 1: // Level 1: Basic gameplay
                    // Try to load pacman and ghost images
                    Image pacmanImage = null;
                    Image ghostImage = null;
                    try {
                        pacmanImage = new ImageIcon(getClass().getResource("/pacmanRight.png")).getImage();
                        ghostImage = new ImageIcon(getClass().getResource("/redGhost.png")).getImage();
                    } catch (Exception e) {
                        System.out.println("Could not load game images: " + e.getMessage());
                    }

                    // Draw pacman (either image or fallback)
                    if (pacmanImage != null) {
                        g2d.drawImage(pacmanImage, imageX, imageY, imageSize, imageSize, null);
                    } else {
                        g2d.setColor(Color.YELLOW);
                        g2d.fillArc(imageX, imageY, imageSize, imageSize, 30, 300);
                    }

                    // Draw pellets
                    g2d.setColor(Color.WHITE);
                    for (int i = 0; i < 5; i++) {
                        g2d.fillRect(imageX + imageSize + 15 + (i * 12), labelY - 2, 4, 4);
                    }

                    // Draw ghost (either image or fallback)
                    if (ghostImage != null) {
                        g2d.drawImage(ghostImage, featureX + featureWidth - imageSize - 20, imageY, imageSize, imageSize, null);
                    } else {
                        g2d.setColor(Color.RED);
                        int ghostX = featureX + featureWidth - imageSize - 20;
                        g2d.fillRect(ghostX, imageY, imageSize, imageSize - 10);
                        g2d.fillArc(ghostX, imageY, imageSize, imageSize, 0, 180);
                    }
                    break;

                case 2: // Level 2: Cherries
                    // Try to load cherry image
                    Image cherryImage = null;
                    try {
                        cherryImage = new ImageIcon(getClass().getResource("/cherry.png")).getImage();
                    } catch (Exception e) {
                        System.out.println("Could not load cherry image: " + e.getMessage());
                    }

                    // Draw cherry (either image or fallback)
                    if (cherryImage != null) {
                        g2d.drawImage(cherryImage, imageX, imageY, imageSize, imageSize, null);
                    } else {
                        // Draw fallback cherry
                        g2d.setColor(Color.RED);
                        g2d.fillOval(imageX, imageY + 5, imageSize/2, imageSize/2);
                        g2d.fillOval(imageX + imageSize/2, imageY + 10, imageSize/2, imageSize/2);
                        g2d.setColor(new Color(0, 150, 0));
                        g2d.fillRect(imageX + imageSize/4, imageY, 3, 10);
                    }

                    // Draw arrow
                    int arrowStartX = imageX + imageSize + 10;
                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(new BasicStroke(2f));
                    g2d.drawLine(arrowStartX, labelY, arrowStartX + 30, labelY);
                    g2d.drawLine(arrowStartX + 30, labelY, arrowStartX + 20, labelY - 10);
                    g2d.drawLine(arrowStartX + 30, labelY, arrowStartX + 20, labelY + 10);

                    // Draw "SPEED BOOST" text - ensuring it fits
                    g2d.setFont(new Font("Arial", Font.BOLD, 14));
                    g2d.drawString("SPEED BOOST", arrowStartX + 40, labelY + 5);
                    break;

                case 3: // Level 3: Apples
                    // Try to load apple image
                    Image appleImage = null;
                    try {
                        appleImage = new ImageIcon(getClass().getResource("/apple.png")).getImage();
                    } catch (Exception e) {
                        System.out.println("Could not load apple image: " + e.getMessage());
                    }

                    // Draw apple (either image or fallback)
                    if (appleImage != null) {
                        g2d.drawImage(appleImage, imageX, imageY, imageSize, imageSize, null);
                    } else {
                        // Draw fallback apple
                        g2d.setColor(new Color(200, 0, 0));
                        g2d.fillOval(imageX, imageY, imageSize, imageSize);
                        g2d.setColor(new Color(0, 150, 0));
                        g2d.fillRect(imageX + imageSize/2 - 1, imageY - 5, 3, 10);
                    }

                    // Draw arrow
                    arrowStartX = imageX + imageSize + 10;
                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(new BasicStroke(2f));
                    g2d.drawLine(arrowStartX, labelY, arrowStartX + 30, labelY);
                    g2d.drawLine(arrowStartX + 30, labelY, arrowStartX + 20, labelY - 10);
                    g2d.drawLine(arrowStartX + 30, labelY, arrowStartX + 20, labelY + 10);

                    // Draw "REVERSE" text - using smaller font to ensure it fits
                    g2d.setFont(new Font("Arial", Font.BOLD, 12));
                    g2d.drawString("REVERSE CONTROLS", arrowStartX + 40, labelY + 5);
                    break;

                case 4: // Level 4: Oranges
                    // Try to load orange image
                    Image orangeImage = null;
                    try {
                        orangeImage = new ImageIcon(getClass().getResource("/orange.png")).getImage();
                    } catch (Exception e) {
                        System.out.println("Could not load orange image: " + e.getMessage());
                    }

                    // Draw orange (either image or fallback)
                    if (orangeImage != null) {
                        g2d.drawImage(orangeImage, imageX, imageY, imageSize, imageSize, null);
                    } else {
                        // Draw fallback orange
                        g2d.setColor(new Color(255, 165, 0));
                        g2d.fillOval(imageX, imageY, imageSize, imageSize);
                    }

                    // Draw arrow
                    arrowStartX = imageX + imageSize + 10;
                    g2d.setColor(Color.WHITE);
                    g2d.setStroke(new BasicStroke(2f));
                    g2d.drawLine(arrowStartX, labelY, arrowStartX + 30, labelY);
                    g2d.drawLine(arrowStartX + 30, labelY, arrowStartX + 20, labelY - 10);
                    g2d.drawLine(arrowStartX + 30, labelY, arrowStartX + 20, labelY + 10);

                    // Draw "HUNT GHOSTS" text
                    g2d.setFont(new Font("Arial", Font.BOLD, 14));
                    g2d.drawString("HUNT GHOSTS", arrowStartX + 40, labelY + 5);
                    break;

                case 5: // Bonus level: Timer and special pellets
                    // Draw timer indicator (20s)
                    g2d.setColor(new Color(0, 255, 255)); // Cyan
                    g2d.fillRect(featureX + 10, featureY + 15, featureWidth - 20, 5);
                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("Arial", Font.BOLD, 12));
                    g2d.drawString("20s", featureX + featureWidth - 30, featureY + 15);

                    // Try to load power pellet image
                    Image powerPelletImage = null;
                    try {
                        powerPelletImage = new ImageIcon(getClass().getResource("/powerFood.png")).getImage();
                    } catch (Exception e) {
                        System.out.println("Could not load power pellet image: " + e.getMessage());
                    }

                    // Draw power pellets
                    int pelletSize = 16;
                    int pelletY = featureY + featureHeight/2 - pelletSize/2 + 10;
                    if (powerPelletImage != null) {
                        g2d.drawImage(powerPelletImage, featureX + 20, pelletY, pelletSize, pelletSize, null);
                        g2d.drawImage(powerPelletImage, featureX + featureWidth - 40, pelletY, pelletSize, pelletSize, null);
                    } else {
                        g2d.setColor(Color.WHITE);
                        g2d.fillOval(featureX + 20, pelletY, pelletSize, pelletSize);
                        g2d.fillOval(featureX + featureWidth - 40, pelletY, pelletSize, pelletSize);
                    }

                    // Draw combo text
                    g2d.setColor(Color.YELLOW);
                    g2d.setFont(new Font("Arial", Font.BOLD, 16));
                    g2d.drawString("3x COMBO!", featureX + featureWidth/2 - 45, labelY + 10);
                    break;
            }
        } catch (Exception e) {
            System.out.println("Error drawing level features: " + e.getMessage());
            e.printStackTrace();

            // Fallback - if feature drawing fails, at least display text
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
            String fallbackText = "";
            switch (level) {
                case 1: fallbackText = "Collect pellets, avoid ghosts"; break;
                case 2: fallbackText = "Cherries give speed boosts"; break;
                case 3: fallbackText = "Apples reverse controls"; break;
                case 4: fallbackText = "Oranges let you hunt ghosts"; break;
                case 5: fallbackText = "Timed bonus level"; break;
            }

            int textWidth = g2d.getFontMetrics().stringWidth(fallbackText);
            g2d.drawString(fallbackText, featureX + (featureWidth - textWidth)/2, featureY + featureHeight/2 + 5);
        }

        g2d.dispose();
        return thumbnail;
    }

    // Helper method to wrap text for descriptions
    private String[] wrapText(String text, int maxWidth, FontMetrics metrics) {
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        ArrayList<String> lines = new ArrayList<>();

        for (String word : words) {
            if (metrics.stringWidth(currentLine + " " + word) < maxWidth) {
                if (currentLine.length() > 0) currentLine.append(" ");
                currentLine.append(word);
            } else {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            }
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines.toArray(new String[0]);
    }

    private void showLevelSelectDialog() {
        // If dialog already exists, just show it
        if (levelSelectDialog != null) {
            levelSelectDialog.setVisible(true);
            levelSelectOpen = true;
            return;
        }

        System.out.println("Creating level selection dialog...");

        // Create thumbnails if they haven't been created yet
        createLevelThumbnails();

        // Create dialog with increased width for better margins
        levelSelectDialog = new JDialog(this, "", true);

        // Remove the title bar completely
        levelSelectDialog.setUndecorated(true);

        levelSelectDialog.setSize(1900, 700);  // Wider dialog for better margins
        levelSelectDialog.setLocationRelativeTo(this);
        levelSelectDialog.setResizable(false);

        // Custom panel with gradient background - this will be our main content panel
        thumbnailPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Create dark blue gradient background
                GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(0, 0, 30),
                        0, getHeight(), new Color(0, 0, 10)
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // Draw subtle maze-like patterns
                g2d.setColor(new Color(20, 20, 100, 15));
                for (int x = 0; x < getWidth(); x += 100) {
                    for (int y = 0; y < getHeight(); y += 100) {
                        g2d.drawRoundRect(x, y, 70, 70, 15, 15);
                    }
                }

                // Draw Pac-Man themed border (matching the How To Play screen)
                g2d.setColor(new Color(255, 215, 0, 80)); // Semi-transparent gold (same as How To Play)
                g2d.setStroke(new BasicStroke(3f)); // Same thickness as How To Play
                g2d.drawRoundRect(10, 10, getWidth() - 20, getHeight() - 20, 20, 20); // Same rounded rectangle as How To Play

// Draw "SELECT A LEVEL" text at the top
                if (pacmanFont != null) {
                    g2d.setFont(pacmanFont.deriveFont(Font.BOLD, 36f)); // Match font size with other menu titles
                } else {
                    g2d.setFont(new Font("Arial", Font.BOLD, 36)); // Same fallback as other titles
                }
                g2d.setColor(Color.YELLOW); // Same yellow color as other titles

                String title = "SELECT A LEVEL";
                FontMetrics fm = g2d.getFontMetrics();
                int titleWidth = fm.stringWidth(title);

// Position the title consistently with other menu titles
                int titleX = (getWidth() - titleWidth) / 2;
                int titleY = 60; // Same vertical position as other menu titles

// Draw the title text directly without glow effects - just like other menu titles
                g2d.drawString(title, titleX, titleY);

// Draw highlight for selected level with enhanced effect
                if (selectedLevel >= 1 && selectedLevel <= 5 && getComponentCount() >= selectedLevel) {
                    int index = selectedLevel - 1;

                    try {
                        // Get the selected thumbnail position
                        JLabel thumbnail = (JLabel) getComponent(index);
                        Rectangle bounds = thumbnail.getBounds();

                        // Pulsing effect
                        float pulse = (float) (1.0 + 0.2 * Math.sin(System.currentTimeMillis() / 300.0));
                        int glowSize = (int) (12 * pulse);

                        // Draw enhanced glow effect
                        for (int i = glowSize; i > 0; i -= 3) {
                            g2d.setColor(new Color(255, 255, 0, 100 - (i * 5)));
                            g2d.drawRoundRect(bounds.x - i, bounds.y - i,
                                    bounds.width + i * 2, bounds.height + i * 2,
                                    20, 20);
                        }

                        // Draw selection border
                        g2d.setColor(Color.YELLOW);
                        g2d.setStroke(new BasicStroke(5f));
                        g2d.drawRoundRect(bounds.x - 5, bounds.y - 5,
                                bounds.width + 10, bounds.height + 10,
                                20, 20);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        System.out.println("Warning: Component not available yet");
                    }
                }
            }
        };
        thumbnailPanel.setLayout(null); // Use absolute positioning

        // Calculate perfectly distributed thumbnails
        int thumbnailWidth = 280;
        int thumbnailHeight = 240;

        // Calculate the positions to perfectly distribute thumbnails
        int totalThumbnails = levelThumbnails.length;
        int spacing = 110; // Consistent spacing between thumbnails
        int totalWidth = (thumbnailWidth * totalThumbnails) + (spacing * (totalThumbnails - 1));
        int startX = (levelSelectDialog.getWidth() - totalWidth) / 2;
        int startY = 150; // Positioned below the title with adequate space

        // Add level thumbnails with proper spacing and size
        for (int i = 0; i < levelThumbnails.length; i++) {
            final int level = i + 1;
            JLabel thumbnail = new JLabel(new ImageIcon(levelThumbnails[i]));

            // Position with perfect spacing
            int x = startX + i * (thumbnailWidth + spacing);
            thumbnail.setBounds(x, startY, thumbnailWidth, thumbnailHeight);

            // Add hover and click effects
            thumbnail.addMouseListener(new MouseAdapter() {
                private Timer hoverTimer;
                private float scale = 1.0f;
                private boolean hovering = false;
                private final float MAX_SCALE = 1.05f;

                @Override
                public void mouseEntered(MouseEvent e) {
                    thumbnail.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    hovering = true;

                    // Add white border on hover
                    thumbnail.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createEmptyBorder(2, 2, 2, 2),
                            BorderFactory.createLineBorder(Color.WHITE, 3, true)
                    ));

                    // Start scale animation timer if not already running
                    if (hoverTimer == null || !hoverTimer.isRunning()) {
                        hoverTimer = new Timer(20, evt -> {
                            if (hovering && scale < MAX_SCALE) {
                                scale += 0.01f;
                                updateScale();
                            } else if (!hovering && scale > 1.0f) {
                                scale -= 0.01f;
                                updateScale();
                            } else if (!hovering && scale <= 1.0f) {
                                hoverTimer.stop();
                                scale = 1.0f;
                                updateScale();
                            }
                        });
                        hoverTimer.start();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    thumbnail.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                    hovering = false;
                    thumbnail.setBorder(null);

                    // Scale animation handles return to normal size in the timer
                }

                private void updateScale() {
                    int origWidth = thumbnailWidth;
                    int origHeight = thumbnailHeight;
                    int newWidth = (int)(origWidth * scale);
                    int newHeight = (int)(origHeight * scale);
                    int newX = x - (newWidth - origWidth)/2;
                    int newY = startY - (newHeight - origHeight)/2;
                    thumbnail.setBounds(newX, newY, newWidth, newHeight);
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    selectedLevel = level;
                    thumbnailPanel.repaint();
                }
            });

            thumbnailPanel.add(thumbnail);
        }

        // Add control buttons with improved positioning
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 100, 20));

        // Position buttons with more vertical spacing from thumbnails
        buttonPanel.setBounds(0, startY + thumbnailHeight + 70, levelSelectDialog.getWidth(), 120);

        // Create styled buttons
        JButton playButton = createStyledButton("PLAY", new Color(0, 180, 0));
        playButton.setPreferredSize(new Dimension(280, 80));

        JButton cancelButton = createStyledButton("CANCEL", new Color(200, 60, 60));
        cancelButton.setPreferredSize(new Dimension(280, 80));

        // Add button actions
        playButton.addActionListener(e -> {
            levelSelectDialog.dispose();
            levelSelectOpen = false;
            dispose(); // Close the menu

            if (selectedLevel == 5) {
                // Start bonus round directly
                new MainApplication(1, true);
            } else {
                // Start normal level
                new MainApplication(selectedLevel);
            }
        });

        cancelButton.addActionListener(e -> {
            levelSelectDialog.dispose();
            levelSelectOpen = false;
        });

        buttonPanel.add(playButton);
        buttonPanel.add(cancelButton);
        thumbnailPanel.add(buttonPanel);

        // Set the content pane directly to our main panel
        levelSelectDialog.setContentPane(thumbnailPanel);

        // Show dialog
        levelSelectDialog.setVisible(true);
        levelSelectOpen = true;
    }
    // First, add this overloaded version of the method that accepts a font size parameter
    private JButton createStyledButton(String text, Color accentColor, float fontSize) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                // Create enhanced button gradient with accent color influence
                Color topColor = new Color(
                        Math.min(80 + accentColor.getRed() / 3, 255),
                        Math.min(80 + accentColor.getGreen() / 3, 255),
                        Math.min(80 + accentColor.getBlue() / 3, 255)
                );
                Color bottomColor = new Color(
                        Math.min(40 + accentColor.getRed() / 5, 255),
                        Math.min(40 + accentColor.getGreen() / 5, 255),
                        Math.min(40 + accentColor.getBlue() / 5, 255)
                );

                // Create a more pronounced gradient for better visual depth
                GradientPaint gradient = new GradientPaint(
                        0, 0, topColor,
                        0, getHeight(), bottomColor
                );
                g2d.setPaint(gradient);

                // Draw rounded button background with slightly larger corners
                RoundRectangle2D roundedRectangle = new RoundRectangle2D.Float(
                        0, 0, getWidth() - 1, getHeight() - 1, 30, 30);
                g2d.fill(roundedRectangle);

                // Add a subtle inner shadow/glow effect
                int shadowSize = 8;
                Color innerShadow = new Color(0, 0, 0, 40);
                g2d.setColor(innerShadow);
                g2d.setStroke(new BasicStroke(shadowSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.draw(new RoundRectangle2D.Float(
                        shadowSize / 2, shadowSize / 2,
                        getWidth() - shadowSize, getHeight() - shadowSize,
                        20, 20));

                // Enhanced hover effect - more pronounced glow with pulsating effect
                float baseAlpha = isRolloverEnabled() && getModel().isRollover() ? 0.9f : 0.5f;

                // Add slight pulsating effect when hovered
                if (isRolloverEnabled() && getModel().isRollover()) {
                    long currentTime = System.currentTimeMillis();
                    float pulse = (float) (0.1 * Math.sin(currentTime / 200.0));
                    baseAlpha += pulse;
                }

                // Pressed state effect
                if (getModel().isPressed()) {
                    baseAlpha = 1.0f;
                    g2d.setColor(new Color(
                            accentColor.getRed(),
                            accentColor.getGreen(),
                            accentColor.getBlue(),
                            180));
                } else {
                    g2d.setColor(new Color(
                            accentColor.getRed(),
                            accentColor.getGreen(),
                            accentColor.getBlue(),
                            (int) (160 * baseAlpha)));
                }

                // Draw border with enhanced glow
                g2d.setStroke(new BasicStroke(3f));
                g2d.draw(roundedRectangle);

                // Add outer glow when hovered
                if (isRolloverEnabled() && getModel().isRollover()) {
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
                    g2d.setColor(accentColor);
                    g2d.setStroke(new BasicStroke(5f));
                    g2d.draw(new RoundRectangle2D.Float(
                            -2, -2, getWidth() + 3, getHeight() + 3, 32, 32));
                    g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                }

                // Use the font that's already set on the button
                g2d.setFont(getFont());

                FontMetrics metrics = g2d.getFontMetrics();
                int x = (getWidth() - metrics.stringWidth(text)) / 2;
                int y = ((getHeight() - metrics.getHeight()) / 2) + metrics.getAscent();

                // Enhanced text shadow for better visibility
                g2d.setColor(new Color(0, 0, 0, 120));

                // Multiple shadow effect for more depth
                for (int i = -3; i <= 3; i++) {
                    for (int j = -3; j <= 3; j++) {
                        if (i != 0 || j != 0) {
                            g2d.drawString(text, x + i, y + j);
                        }
                    }
                }

                // Draw the actual text with a slight glow
                if (getModel().isPressed()) {
                    // Slightly darker text when pressed
                    g2d.setColor(new Color(220, 220, 220));
                    g2d.drawString(text, x + 1, y + 1);  // Slight offset when pressed
                } else {
                    g2d.setColor(Color.WHITE);
                    g2d.drawString(text, x, y);
                }
            }
        };

        // Button styling
        if (pacmanFont != null) {
            button.setFont(pacmanFont.deriveFont(Font.BOLD, fontSize)); // Now using the parameter
        } else {
            button.setFont(new Font("Arial", Font.BOLD, (int)fontSize)); // Now using the parameter
        }
        button.setForeground(Color.WHITE);
        button.setPreferredSize(new Dimension(400, 80));
        button.setMaximumSize(new Dimension(400, 80));
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);

        // Add hover effect with scaling animation
        button.addMouseListener(new MouseAdapter() {
            private Timer hoverTimer;
            private float hoverScale = 1.0f;
            private final float MAX_SCALE = 1.08f;
            private boolean hovering = false;

            @Override
            public void mouseEntered(MouseEvent e) {
                hovering = true;
                button.setCursor(new Cursor(Cursor.HAND_CURSOR));

                // Create animation timer for smooth scaling
                if (hoverTimer != null && hoverTimer.isRunning()) {
                    hoverTimer.stop();
                }

                hoverTimer = new Timer(20, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        if (hovering && hoverScale < MAX_SCALE) {
                            hoverScale += 0.008f;
                            updateButtonScale();
                        } else if (!hovering && hoverScale > 1.0f) {
                            hoverScale -= 0.008f;
                            updateButtonScale();
                        } else {
                            hoverTimer.stop();
                        }
                    }
                });
                hoverTimer.start();

                // Increase the font size more smoothly with the animation
                if (pacmanFont != null) {
                    button.setFont(pacmanFont.deriveFont(Font.BOLD, fontSize + 2)); // Made slightly larger
                } else {
                    button.setFont(new Font("Arial", Font.BOLD, (int)fontSize + 2)); // Made slightly larger
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovering = false;
                button.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

                // Only start animation if it's not already running
                if (hoverTimer == null || !hoverTimer.isRunning()) {
                    hoverTimer = new Timer(20, new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent evt) {
                            if (!hovering && hoverScale > 1.0f) {
                                hoverScale -= 0.008f;
                                updateButtonScale();
                            } else {
                                hoverTimer.stop();
                            }
                        }
                    });
                    hoverTimer.start();
                }

                // Return to normal font size
                if (pacmanFont != null) {
                    button.setFont(pacmanFont.deriveFont(Font.BOLD, fontSize));
                } else {
                    button.setFont(new Font("Arial", Font.BOLD, (int)fontSize));
                }
            }

            private void updateButtonScale() {
                int originalWidth = 400;
                int originalHeight = 80;
                int newWidth = (int) (originalWidth * hoverScale);
                int newHeight = (int) (originalHeight * hoverScale);

                button.setPreferredSize(new Dimension(newWidth, newHeight));
                button.setMaximumSize(new Dimension(newWidth, newHeight));
                button.revalidate();
                button.repaint();
            }
        });

        return button;
    }

    // Then add this overloaded version for backward compatibility with existing code
    private JButton createStyledButton(String text, Color accentColor) {
        // Default to the original font size (32f)
        return createStyledButton(text, accentColor, 32f);
    }
    private void showModeSelectDialog() {
        // Create the dialog
        JDialog modeDialog = new JDialog(this, "", true);
        modeDialog.setUndecorated(true);
        modeDialog.setSize(600, 400);
        modeDialog.setLocationRelativeTo(this);
        modeDialog.setResizable(false);

        // Custom panel with gradient background
        JPanel modePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Create dark blue gradient background
                GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(0, 0, 40),
                        0, getHeight(), new Color(0, 0, 15)
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // Draw maze-like patterns in the background
                if (mazeElements != null) {
                    for (MazeElement maze : mazeElements) {
                        maze.draw(g2d);
                    }
                }

                // Draw "SELECT MODE" text at the top
                if (pacmanFont != null) {
                    g2d.setFont(pacmanFont.deriveFont(Font.BOLD, 36f));
                } else {
                    g2d.setFont(new Font("Arial", Font.BOLD, 36));
                }
                g2d.setColor(Color.YELLOW);

                String title = "SELECT MODE";
                FontMetrics fm = g2d.getFontMetrics();
                int titleWidth = fm.stringWidth(title);
                g2d.drawString(title, (getWidth() - titleWidth) / 2, 60);

                // Draw a border around the dialog
                g2d.setColor(new Color(255, 215, 0, 80)); // Semi-transparent gold
                g2d.setStroke(new BasicStroke(3f));
                g2d.drawRoundRect(10, 10, getWidth() - 20, getHeight() - 20, 20, 20);
            }
        };

        // Use BoxLayout for more control over button sizes
        modePanel.setLayout(new BoxLayout(modePanel, BoxLayout.Y_AXIS));
        modePanel.setBorder(BorderFactory.createEmptyBorder(100, 50, 100, 50));

        // Create mode buttons with smaller font size (22f)
        JButton singlePlayerButton = createStyledButton("Single Player", Color.GREEN, 22f);
        JButton multiplayerButton = createStyledButton("Multiplayer", Color.MAGENTA, 22f);

        // Center-align buttons horizontally
        singlePlayerButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        multiplayerButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Add action listeners for mode buttons
        singlePlayerButton.addActionListener(e -> {
            modeDialog.dispose();
            showLevelSelectDialog();
        });

        multiplayerButton.addActionListener(e -> {
            modeDialog.dispose();
            dispose(); // Close menu
            new MainApplication(true); // Start multiplayer game
        });

        // Add buttons to panel with proper spacing
        modePanel.add(Box.createVerticalGlue()); // Space at top
        modePanel.add(singlePlayerButton);
        modePanel.add(Box.createVerticalStrut(30)); // Fixed space between buttons
        modePanel.add(multiplayerButton);
        modePanel.add(Box.createVerticalGlue()); // Space at bottom

        // Add panel to dialog
        modeDialog.setContentPane(modePanel);
        modeDialog.setVisible(true);
    }
    private void showEnhancedHowToPlayDialog() {
        // Create a custom dialog with significantly increased size
        JDialog howToPlayDialog = new JDialog(this, "", true);
        howToPlayDialog.setUndecorated(true);
        howToPlayDialog.setSize(1100, 800);
        ;
        howToPlayDialog.setLocationRelativeTo(this);
        howToPlayDialog.setResizable(false);

        // Create a panel with custom background painting
        JPanel contentPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Create dark blue gradient background like the main menu
                GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(0, 0, 40),
                        0, getHeight(), new Color(0, 0, 15)
                );
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());

                // Draw maze-like patterns in the background
                g2d.setColor(new Color(20, 20, 100, 20));
                for (int x = 0; x < getWidth(); x += 60) {
                    for (int y = 0; y < getHeight(); y += 60) {
                        g2d.drawRoundRect(x, y, 40, 40, 10, 10);
                    }
                }

                // Draw Pac-Man themed border
                g2d.setColor(new Color(255, 215, 0, 80)); // Semi-transparent gold
                g2d.setStroke(new BasicStroke(3f));
                g2d.drawRoundRect(10, 10, getWidth() - 20, getHeight() - 20, 20, 20);
            }
        };

        contentPanel.setLayout(new BorderLayout(10, 10));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        // Main title at the top
        JLabel titleLabel = new JLabel("HOW TO PLAY");
        titleLabel.setForeground(Color.YELLOW);
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        if (pacmanFont != null) {
            titleLabel.setFont(pacmanFont.deriveFont(Font.BOLD, 36f));
        } else {
            titleLabel.setFont(new Font("Arial", Font.BOLD, 36));
        }
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
        contentPanel.add(titleLabel, BorderLayout.NORTH);

        // Create a scrollable panel for the content to ensure all items are visible
        JPanel mainContent = new JPanel(null);
        mainContent.setOpaque(false);

        // Left Column - Basic Controls and Power-ups
        // BASIC CONTROLS header
        JLabel basicControlsLabel = createSectionTitle("BASIC CONTROLS", new Color(255, 0, 0)); // Red
        basicControlsLabel.setBounds(20, 0, 400, 30);
        mainContent.add(basicControlsLabel);

        // Basic controls instruction items
        JPanel pacmanControlItem = createInstructionItemWithImage("Use arrow keys to move Pac-Man", "pacmanRight.png");
        pacmanControlItem.setBounds(20, 35, 400, 25);
        mainContent.add(pacmanControlItem);

        JPanel pelletControlItem = createInstructionItemWithImage("Eat all pellets to complete the level", "powerFood.png");
        pelletControlItem.setBounds(20, 65, 400, 25);
        mainContent.add(pelletControlItem);

        JPanel ghostControlItem = createInstructionItemWithImage("Avoid ghosts or lose a life", "redGhost.png");
        ghostControlItem.setBounds(20, 95, 400, 25);
        mainContent.add(ghostControlItem);

        // GAME OBJECTIVES header
        JLabel objectivesLabel = createSectionTitle("GAME OBJECTIVES", new Color(255, 255, 0)); // Yellow
        objectivesLabel.setBounds(20, 135, 400, 30);
        mainContent.add(objectivesLabel);

        // Game objectives items
        JPanel singlePlayerObj = createInstructionItem("Single Player: Complete all 4 levels to unlock Bonus Round");
        singlePlayerObj.setBounds(20, 170, 400, 25);
        mainContent.add(singlePlayerObj);

        JPanel progressionObj = createInstructionItem("Progress through levels with increasing difficulty");
        progressionObj.setBounds(20, 200, 400, 25);
        mainContent.add(progressionObj);

        JPanel survivalObj = createInstructionItem("Survive with your 3 lives and reach the highest score");
        survivalObj.setBounds(20, 230, 400, 25);
        mainContent.add(survivalObj);

        // POWER-UPS header
        JLabel powerUpsLabel = createSectionTitle("POWER-UPS", new Color(255, 105, 180)); // Pink
        powerUpsLabel.setBounds(20, 270, 400, 30);
        mainContent.add(powerUpsLabel);

        // Power-ups instruction items
        JPanel cherryItem = createInstructionItemWithImage("Collect cherries for speed boost", "cherry.png");
        cherryItem.setBounds(20, 305, 400, 25);
        mainContent.add(cherryItem);

        JPanel appleItem = createInstructionItemWithImage("Collect apples in level 3 for reversed controls", "apple.png");
        appleItem.setBounds(20, 335, 400, 25);
        mainContent.add(appleItem);

        // Added explanation about reversed controls
        JPanel reverseExplanation = createInstructionItem("(Arrow keys will move in opposite directions)");
        reverseExplanation.setBounds(20, 365, 400, 25);
        mainContent.add(reverseExplanation);

        JPanel orangeItem = createInstructionItemWithImage("Collect oranges in level 4 to hunt and eat ghosts", "orange.png");
        orangeItem.setBounds(20, 395, 400, 25);
        mainContent.add(orangeItem);

        JPanel bonusUnlockItem = createInstructionItemWithImage("Complete level 4 to unlock the Bonus Round!", "powerFood.png");
        bonusUnlockItem.setBounds(20, 425, 400, 25);
        mainContent.add(bonusUnlockItem);

        // CONTROLS GUIDE header
        JLabel controlsGuideLabel = createSectionTitle("CONTROLS GUIDE", new Color(255, 255, 100)); // Yellow
        controlsGuideLabel.setBounds(20, 465, 400, 30);
        mainContent.add(controlsGuideLabel);

        // Controls guide items
        JPanel normalControlsItem = createInstructionItem("Normal: ↑ = up, ↓ = down, ← = left, → = right");
        normalControlsItem.setBounds(20, 500, 400, 25);
        mainContent.add(normalControlsItem);

        JPanel reversedControlsItem = createInstructionItem("Reversed: ↑ = down, ↓ = up, ← = right, → = left");
        reversedControlsItem.setBounds(20, 530, 400, 25);
        mainContent.add(reversedControlsItem);

        JPanel wasdControlsItem = createInstructionItem("Player 2: W = up, S = down, A = left, D = right");
        wasdControlsItem.setBounds(20, 560, 400, 25);
        mainContent.add(wasdControlsItem);

        // Middle Column - Bonus Round and Multiplayer
        // BONUS ROUND header
        JLabel bonusRoundLabel = createSectionTitle("BONUS ROUND", new Color(0, 191, 255)); // Blue
        bonusRoundLabel.setBounds(470, 0, 400, 30);
        mainContent.add(bonusRoundLabel);

        // Bonus round instruction items
        JPanel collectPelletsItem = createInstructionItem("Collect as many pellets as possible in 20 seconds");
        collectPelletsItem.setBounds(470, 35, 450, 25);
        mainContent.add(collectPelletsItem);

        JPanel ghostCollisionsItem = createInstructionItem("Ghost collisions reduce your score instead of lives");
        ghostCollisionsItem.setBounds(470, 65, 450, 25);
        mainContent.add(ghostCollisionsItem);

        JPanel combosItem = createInstructionItem("Build combos for score multipliers (2x and 3x)");
        combosItem.setBounds(470, 95, 450, 25);
        mainContent.add(combosItem);

        JPanel bonusObjective = createInstructionItem("Objective: Get the highest possible score in time limit");
        bonusObjective.setBounds(470, 125, 450, 25);
        mainContent.add(bonusObjective);

        // MULTIPLAYER header
        JLabel multiplayerLabel = createSectionTitle("MULTIPLAYER", new Color(255, 165, 0)); // Orange
        multiplayerLabel.setBounds(470, 165, 400, 30);
        mainContent.add(multiplayerLabel);

        // Multiplayer instruction items
        JPanel controlsItem = createInstructionItem("Player 1 uses Arrow Keys, Player 2 uses WASD");
        controlsItem.setBounds(470, 200, 450, 25);
        mainContent.add(controlsItem);

        JPanel competeItem = createInstructionItem("Compete to collect the most pellets and score");
        competeItem.setBounds(470, 230, 450, 25);
        mainContent.add(competeItem);

        // Added explanation about multiplayer mode
        JPanel multiplayerExplanation = createInstructionItem("Players compete in the same maze to score points");
        multiplayerExplanation.setBounds(470, 260, 450, 25);
        mainContent.add(multiplayerExplanation);

        JPanel multiplayerWin = createInstructionItem("Highest score at the end of the round wins!");
        multiplayerWin.setBounds(470, 290, 450, 25);
        mainContent.add(multiplayerWin);

        JPanel multiplayerObj = createInstructionItem("Objective: Outmaneuver your opponent and score more");
        multiplayerObj.setBounds(470, 320, 450, 25);
        mainContent.add(multiplayerObj);

        // SHORTCUTS header
        JLabel shortcutsLabel = createSectionTitle("SHORTCUTS", new Color(0, 255, 127)); // Green
        shortcutsLabel.setBounds(470, 360, 400, 30);
        mainContent.add(shortcutsLabel);

        // Shortcuts instruction items
        JPanel level2Item = createInstructionItem("Press 'L' for level 2");
        level2Item.setBounds(470, 395, 450, 25);
        mainContent.add(level2Item);

        JPanel level3Item = createInstructionItem("Press 'K' for level 3");
        level3Item.setBounds(470, 425, 450, 25);
        mainContent.add(level3Item);

        JPanel level4Item = createInstructionItem("Press 'J' for level 4");
        level4Item.setBounds(470, 455, 450, 25);
        mainContent.add(level4Item);

        JPanel bonusItem = createInstructionItem("Press 'B' for Bonus Round");
        bonusItem.setBounds(470, 485, 450, 25);
        mainContent.add(bonusItem);

        JPanel escItem = createInstructionItem("Press 'ESC' to exit game at any time");
        escItem.setBounds(470, 515, 450, 25);
        mainContent.add(escItem);

        // DIFFICULTY PROGRESSION header
        JLabel difficultyLabel = createSectionTitle("DIFFICULTY PROGRESSION", new Color(128, 0, 128));
        difficultyLabel.setBounds(470, 555, 500, 30); // Increased width from 400 to 500
        mainContent.add(difficultyLabel);

        // Difficulty progression items
        JPanel level1Diff = createInstructionItem("Level 1: Standard maze, basic ghost AI");
        level1Diff.setBounds(470, 590, 450, 25);
        mainContent.add(level1Diff);

        JPanel level2Diff = createInstructionItem("Level 2: Faster ghosts, collect cherries for boosts");
        level2Diff.setBounds(470, 620, 450, 25);
        mainContent.add(level2Diff);

        JPanel level3Diff = createInstructionItem("Level 3: Control reversal challenges with apple power-ups");
        level3Diff.setBounds(470, 650, 450, 25);
        mainContent.add(level3Diff);

        JPanel level4Diff = createInstructionItem("Level 4: Hunt ghosts with orange power-ups for bonus points");
        level4Diff.setBounds(470, 680, 450, 25);
        mainContent.add(level4Diff);

        // Create a scroll pane for the main content
        JScrollPane scrollPane = new JScrollPane(mainContent);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);

        // Set a preferred size for the main content to ensure all elements are included
        mainContent.setPreferredSize(new Dimension(940, 730));
        // After creating your scrollPane, add these customization lines:
        scrollPane.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override
            protected void configureScrollBarColors() {
                this.thumbColor = new Color(0, 220, 0); // Green color matching your OK button
                this.trackColor = new Color(0, 0, 60); // Dark blue matching your background
                this.thumbDarkShadowColor = null;
                this.thumbHighlightColor = null;
                this.thumbLightShadowColor = null;
                this.trackHighlightColor = null;
            }

            @Override
            protected JButton createDecreaseButton(int orientation) {
                return createZeroButton();
            }

            @Override
            protected JButton createIncreaseButton(int orientation) {
                return createZeroButton();
            }

            private JButton createZeroButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                button.setMinimumSize(new Dimension(0, 0));
                button.setMaximumSize(new Dimension(0, 0));
                return button;
            }

            @Override
            protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
                if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) {
                    return;
                }

                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                // Draw rounded rectangle for thumb
                g2.setColor(thumbColor);
                g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2,
                        thumbBounds.width - 4, thumbBounds.height - 4,
                        10, 10);
                g2.dispose();
            }

            @Override
            protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(trackColor);
                g2.fillRect(trackBounds.x, trackBounds.y,
                        trackBounds.width, trackBounds.height);
                g2.dispose();
            }
        });

// Make scrollbar thinner to be less intrusive
        scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));

        contentPanel.add(scrollPane, BorderLayout.CENTER);

        // Create a panel for the OK button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setOpaque(false);

        // Create a stylized OK button
        JButton okButton = new JButton("OK") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Draw button background
                g2d.setColor(new Color(0, 120, 0));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);

                // Draw button border
                g2d.setColor(new Color(0, 220, 0));
                g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);

                // Draw text
                g2d.setColor(Color.WHITE);
                if (pacmanFont != null) {
                    g2d.setFont(pacmanFont.deriveFont(Font.BOLD, 24f));
                } else {
                    g2d.setFont(new Font("Arial", Font.BOLD, 24));
                }

                FontMetrics fm = g2d.getFontMetrics();
                int textX = (getWidth() - fm.stringWidth("OK")) / 2;
                int textY = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();

                g2d.drawString("OK", textX, textY);
            }
        };

        okButton.setPreferredSize(new Dimension(150, 50));
        okButton.setFocusPainted(false);
        okButton.setBorderPainted(false);
        okButton.setContentAreaFilled(false);
        okButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        okButton.addActionListener(e -> howToPlayDialog.dispose());

        buttonPanel.add(okButton);
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Set the content pane and show dialog
        howToPlayDialog.setContentPane(contentPanel);
        howToPlayDialog.setVisible(true);
    }

    // Helper method to create section titles with custom font and color
    private JLabel createSectionTitle(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setForeground(color);

        // Use a slightly larger, bold font
        if (pacmanFont != null) {
            label.setFont(pacmanFont.deriveFont(Font.BOLD, 20f));
        } else {
            label.setFont(new Font("Arial", Font.BOLD, 20));
        }

        return label;
    }

    // Helper method for instruction items with actual game images
    private JPanel createInstructionItemWithImage(String text, String imagePath) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);

        // Try to load actual game image
        JLabel iconLabel = new JLabel();
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource("/" + imagePath));
            // Scale image to an appropriate size
            Image img = icon.getImage().getScaledInstance(24, 24, Image.SCALE_SMOOTH);
            iconLabel.setIcon(new ImageIcon(img));
        } catch (Exception e) {
            // Fallback to a colored circle if image can't be loaded
            iconLabel.setText("•");
            iconLabel.setForeground(Color.YELLOW);
            iconLabel.setFont(new Font("Arial", Font.BOLD, 24));
        }

        // Text label with custom font
        JLabel textLabel = new JLabel(text);
        textLabel.setForeground(Color.WHITE);
        textLabel.setFont(new Font("Arial", Font.PLAIN, 14));

        panel.add(iconLabel, BorderLayout.WEST);
        panel.add(textLabel, BorderLayout.CENTER);

        return panel;
    }

    // Helper method for instruction items without images
    private JPanel createInstructionItem(String text) {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setOpaque(false);

        // Use a yellow dot as bullet point
        JLabel bulletLabel = new JLabel("•");
        bulletLabel.setForeground(Color.YELLOW);
        bulletLabel.setFont(new Font("Arial", Font.BOLD, 24));

        // Text label with custom font
        JLabel textLabel = new JLabel(text);
        textLabel.setForeground(Color.WHITE);
        textLabel.setFont(new Font("Arial", Font.PLAIN, 14));

        panel.add(bulletLabel, BorderLayout.WEST);
        panel.add(textLabel, BorderLayout.CENTER);

        return panel;
    }


    private class PacManLogoPanel extends JPanel {
        private Image logoImage;

        public PacManLogoPanel() {
            // Load the logo image when the panel is created
            try {
                logoImage = new ImageIcon(getClass().getResource("/pac-run-logo.png")).getImage();
                System.out.println("Logo image loaded successfully");
            } catch (Exception e) {
                System.out.println("Could not load logo image: " + e.getMessage());
                e.printStackTrace();
                logoImage = null;
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw animated stars and other background elements if needed

            // If we have a logo image, display it centered
            if (logoImage != null) {
                // Calculate position to center the image
                int imgWidth = logoImage.getWidth(null);
                int imgHeight = logoImage.getHeight(null);

                // If you want to scale the image to a specific width while maintaining aspect ratio
                int displayWidth = 600;  // Adjust this value as needed
                int displayHeight = (int)(imgHeight * ((double)displayWidth / imgWidth));

                int x = (getWidth() - displayWidth) / 2;
                int y = 80;  // Adjust vertical position as needed

                // Draw the image
                g2d.drawImage(logoImage, x, y, displayWidth, displayHeight, null);
            } else {
                // Fallback to original drawing method if image failed to load
                // You can keep a simplified version of your original code here as a backup
                g2d.setColor(Color.YELLOW);
                g2d.setFont(new Font("Arial", Font.BOLD, 48));
                String text = "PAC-RUN";
                FontMetrics fm = g2d.getFontMetrics();
                int textX = (getWidth() - fm.stringWidth(text)) / 2;
                g2d.drawString(text, textX, 140);
            }
        }
    }

    // Star class for starfield background effect
    private class Star {
        private float x, y;
        private float size;
        private float brightness;
        private float speed;

        public Star(int screenWidth, int screenHeight) {
            x = (float) (Math.random() * screenWidth);
            y = (float) (Math.random() * screenHeight);
            size = (float) (Math.random() * 2.5f + 0.5f);
            brightness = (float) (Math.random() * 0.5f + 0.5f);
            speed = (float) (Math.random() * 0.7f + 0.3f);
        }

        public void update() {
            // Make stars "twinkle" by slightly changing brightness
            brightness += (Math.random() - 0.5f) * 0.05f;
            if (brightness < 0.3f) brightness = 0.3f;
            if (brightness > 1.0f) brightness = 1.0f;

            // Make stars move slightly
            y += speed;
            if (y > getHeight()) {
                y = 0;
                x = (float) (Math.random() * getWidth());
            }
        }

        public void draw(Graphics2D graphics) {
            graphics.setColor(new Color(1f, 1f, 1f, brightness));
            graphics.fillOval((int) x, (int) y, (int) size, (int) size);
        }
    }
    // Inner class for ghost characters
    private class MenuGhost {
        Image image;
        int x, y;
        int speed;
        int direction; // 1 = right, -1 = left
        float bounce = 0; // For a floating effect

        void update() {
            // Move horizontally
            x += speed * direction;

            // Wrap around screen
            if (x > getWidth() + 50) x = -50;
            if (x < -50) x = getWidth() + 50;

            // Add a gentle floating effect
            bounce += 0.05f;
            if (bounce > 2 * Math.PI) bounce -= 2 * Math.PI;
        }

        void draw(Graphics2D g2d) {
            // Apply floating effect
            int yOffset = (int)(Math.sin(bounce) * 5);

            // Draw the ghost
            g2d.drawImage(image, x, y + yOffset, 40, 40, null);
        }
    }

    // Inner class for decorative maze elements
    private class MazeElement {
        int x, y, width, height, cornerRadius;
        float opacity;

        void draw(Graphics2D g2d) {
            Composite originalComposite = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
            g2d.setColor(new Color(0, 0, 180)); // Blue color for maze
            g2d.drawRoundRect(x, y, width, height, cornerRadius, cornerRadius);
            g2d.setComposite(originalComposite);
        }
    }

    // Pellet class for floating pellet effect
    private class Pellet {
        private float x, y;
        private float size;
        private float speedX, speedY;
        private Color color;

        public Pellet(int screenWidth, int screenHeight) {
            x = (float) (Math.random() * screenWidth);
            y = (float) (Math.random() * screenHeight);
            size = (float) (Math.random() * 6 + 4);
            speedX = (float) (Math.random() * 2 - 1) * 0.5f;
            speedY = (float) (Math.random() * 2 - 1) * 0.5f;

            // Occasionally create a power pellet (larger, with pulsing effect)
            if (Math.random() < 0.2) {
                size *= 2;
                color = new Color(220, 220, 255); // Power pellet color (slight blue tint)
            } else {
                color = new Color(255, 255, 255); // Regular pellet color (white)
            }
        }

        public void update() {
            x += speedX;
            y += speedY;

            // Bounce off edges
            if (x < 0 || x > getWidth()) speedX *= -1;
            if (y < 0 || y > getHeight()) speedY *= -1;

            // Ensure pellets stay within bounds
            if (x < 0) x = 0;
            if (x > getWidth()) x = getWidth();
            if (y < 0) y = 0;
            if (y > getHeight()) y = getHeight();
        }

        public void draw(Graphics2D graphics) {
            // For power pellets, add pulsing effect
            if (size > 8) {
                float pulse = (float) (0.7 + 0.3 * Math.sin(System.currentTimeMillis() / 200.0));
                graphics.setColor(new Color(
                        (int) (color.getRed() * pulse),
                        (int) (color.getGreen() * pulse),
                        (int) (color.getBlue() * pulse)
                ));
            } else {
                graphics.setColor(color);
            }

            graphics.fillOval((int) x, (int) y, (int) size, (int) size);
        }
    }

    public static void main(String[] args) {
        // Ensure the menu runs on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> new MenuScreen());
    }
}