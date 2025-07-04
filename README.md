# PacRun
PacRun is a Java-based reimagining of the classic arcade game Pac-Man. Players navigate a maze to collect pellets while avoiding ghosts, with enhanced gameplay features including multiple power-ups, distinct ghost AI behaviors, and both single-player and multiplayer modes.

## Notable Features

- Four progressive levels with unique power-ups (speed boost, control reversal, ghost hunting)
- Distinct ghost AI personalities based on the original Pac-Man design patterns
- Competitive "Pellet Race" multiplayer mode with position-swapping mechanics
- Bonus Round with combo-based scoring system and time constraints
- Custom animations and visual feedback systems
- Responsive menu interface with level selection and tutorial screens
- Complete audio system with sound effects and background music

## Setup and Running Instructions (Minimum Screen Size: 1024x768)
### Prerequisites

- Java Development Kit (JDK) 8 or higher
- Any Java IDE (IntelliJ IDEA recommended)
- Screen resolution of at least 1024x768 pixels

### Compilation Instructions

- Open the project in your IDE
- Ensure that the Resources folder is properly included in the project structure
- Compile all Java files in the src directory
- Run the MainApplication class from your IDE

## Project Structure
```
PacRun/
├── src/                      # Source code directory
│   ├── AudioPlayer.java      # Sound management system
│   ├── MainApplication.java  # Game window and entry point
│   ├── MenuScreen.java       # Interactive menu interface
│   ├── PacRun.java           # Core game class and logic
│   └── Resources/            # Game assets directory
│       ├── wall tile.png     # Wall texture
│       ├── pacmanRight.png   # Pac-Man sprite
│       ├── redGhost.png      # Ghost sprites
│       ├── cherry.png        # Power-up sprites
│       ├── pacman_beginning.wav # Sound effects
│       ├── pac-font.ttf      # Custom fonts
│       └── ...               # Additional textures
├── test/                     # Test directory
│   ├── AudioPlayerTest.java  # Unit tests for audio system
│   └── PacRunTest.java       # Game logic tests
└── README.md                 # This documentation file

```

## System Features
### Sophisticated Game Mechanics

- **Object-Oriented Architecture**: Unified Block entity system representing all game elements
- **State Management System**: Boolean flags with timestamp tracking for power-up effects
- **Grid-Aligned Movement**: Precise corner-turning system with tolerance thresholds
- **Personality-Based Ghost AI**: Each ghost implements unique targeting algorithms:

#### Ghost AI :
- **Red Ghost**: Direct pursuit using shortest path calculation
- **Pink Ghost**: Ambush targeting (4 tiles ahead of Pac-Man)
- **Blue Ghost**: Semi-random movement with pursuit phases
- **Orange Ghost**: Proximity-aware behavior (chase when far, retreat when close)

#### Power-Up System: Three distinct mechanics that alter gameplay:

- Cherry: Temporary speed increase (5 seconds)
- Apple: Control reversal with ghost slowdown (8 seconds)
- Orange: Role reversal allowing ghost consumption (8 seconds)

#### Realtime Features :
- Collision Detection: Optimized AABB (Axis-Aligned Bounding Box) system
- Visual Feedback: Real-time status indicators and animations
- Bonus Round System: Respawning pellets with combo multipliers (up to 8x)
- Multiplayer System: Two-player competitive mode with position swapping

### Technical Implementation

- Java Swing Graphics: Custom rendering pipeline without external game engines
- Event-Driven Architecture: Timer-based game loop with input event handling
- Sound Management: Resource loading with error handling and fallback options
- Menu Interface: Animated components with interactive elements
- Level Management: String-based map representation for easy level design

## How to Play
### Controls

- Arrow Keys: Control Pac-Man (Single Player and Player 1 in Multiplayer)
- WASD Keys: Control Ghost (Player 2 in Multiplayer)
- ESC: Return to main menu
- SPACE: Continue to next level after completion

### Gameplay Objectives
- **Main Goal**: Navigate through the maze collecting all pellets to complete each level while avoiding ghosts
- **Scoring System**:
    - 10 points for each regular pellet
    - 50 points for each power-up collected
    - 200 points for eating a ghost (when powered up by an Orange)
- **Lives**: You start with 3 lives; lose a life when a ghost catches you
- **Level Completion**: A level is completed when all pellets have been collected
- **Game Over**: Occurs when all lives are lost

## Gameplay Modes


### Single Player Mode

- Level 1: Standard gameplay - navigate the maze, collect all pellets, and avoid ghosts to advance
- Level 2: Introduces Cherry power-ups that provide temporary speed boosts to help you outrun ghosts
- Level 3: Introduces Apple power-ups that reverse your controls but also slow down ghosts, requiring adaptive gameplay
- Level 4: Introduces Orange power-ups that allow you to hunt and eat ghosts for bonus points
- Bonus Round: Time-limited challenge where you must collect as many pellets as possible within 20 seconds, with respawning pellets and combo multipliers

### Multiplayer "Pellet Race" Mode

- Both players compete simultaneously in the same maze
- Player 1 controls Pac-Man using arrow keys, trying to collect pellets for points
- Player 2 controls a user-controlled ghost using WASD keys, who can also collect pellets
- Players each have 2 lives in this mode (instead of 3 in single player)
- Position swapping occurs periodically with warning indicators to keep gameplay dynamic
- Winning condition: The player with the highest score when one player loses all lives, or when all pellets are collected

## Known Issues and Limitations

- Fixed maze layouts (no procedural generation)
- Limited screen resolution support (requires minimum 1024x768)
- Limited game save functionality (no persistent high scores)


## Important Notes

- The game requires a relatively modern Java runtime for optimal performance
- Ensure all resource files are correctly included in the project structure
- Audio files must be accessible from the Resources directory
- For best experience, run in fullscreen mode
- Teacher/Examiner Mode: Use number keys (1-4) to quickly access specific levels
- Bonus level can be accessed directly with the 'B' key
- In multiplayer mode, both players must agree on when to exit (ESC key)
- If experiencing performance issues, try closing other applications first
