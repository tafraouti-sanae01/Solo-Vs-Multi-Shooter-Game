# Solo-Vs-Multi-Shooter-Game

This project is a shooting game developed in Java, using Swing for the graphical interface. The game features a well-structured architecture with several key components:

1. **Game Architecture**:
   - A main interface (`JeuInterface.java`) that manages the core gameplay
   - An audio management system (`AudioManager.java`) for sound effects and music
   - A score database (`ScoreDatabase.java`) for result persistence
   - A real-time communication system for integrated chat

2. **Main Features**:
   - Progressive level system (Beginner to Master level)
   - Four different aircraft types with unique characteristics (MiG-51S, F/A-28A, Su-55, Su-51K)
   - Lives system with temporary invincibility after collision
   - Dynamic scoring system with bonuses for successful hits
   - Integrated chat interface for player communication
   - Smooth animations for projectiles, collisions, and visual effects

3. **Technical Aspects**:
   - Thread utilization for movement and collision management
   - Optimized collision system with adjusted collision rectangles
   - Efficient management of graphical and sound resources
   - Client-server architecture for real-time chat
   - Responsive user interface with smooth animations

4. **Multimedia Resources**:
   - Custom images for aircraft, projectiles, and visual effects
   - Sound effects for shooting, collisions, and level changes
   - Modern user interface with transitions and animations

5. **Security and Performance**:
   - Secure network connection management
   - Performance optimization using `CopyOnWriteArrayList` for projectiles
   - Proper resource cleanup on game closure

The project demonstrates excellent object-oriented architecture with clear separation of responsibilities, efficient resource management, and particular attention to user experience details. It integrates advanced features while maintaining a maintainable and extensible codebase.

## Technologies Used
- Java
- Swing
- Java Sound API
- Java Networking
- SQLite (for score persistence)
- Radmin VPN (for virtual network connection)

## Features
- Real-time multiplayer chat
- Dynamic difficulty progression
- Multiple aircraft types with unique characteristics
- Score tracking and persistence
- Sound effects and background music
- Smooth animations and visual effects

## Network Configuration
For multiplayer mode and real-time chat, the game uses **Radmin VPN** to establish a secure virtual network connection between players. This solution provides:
- Stable and secure connection between players
- Minimal latency for smooth gameplay experience
- Simplified network configuration for users
- Support for remote multiplayer sessions

## Authors
This project was developed by:
- TAFRAOUTI Sanae
- ESSEBAIY Aya
- ELMESSAOUDI Fatima
