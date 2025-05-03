package org.ensate;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.AudioInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class JeuInterface extends JFrame {
    private String joueur;
    private String niveau;
    private String avion;
    private int score = 0;
    private int vies;
    private int currentLevel = 1;
    private Timer collisionTimer;
    private Timer scoreTimer;
    private long startTime;

    private JPanel gamePanel;
    private JLabel scoreLabel;
    private JLabel levelLabel;
    private ArrayList<JLabel> vieLabels;
    private ArrayList<JLabel> viePerdueLabels;
    private JLabel avionLabel;
    private ArrayList<JLabel> bateauLabels;
    private CopyOnWriteArrayList<Projectile> projectiles;

    private BufferedImage avionImage;
    private BufferedImage[] bateauImages;
    private BufferedImage vieImage;
    private BufferedImage viePerdueImage;
    private BufferedImage backgroundImage;
    private BufferedImage tirImage;
    private BufferedImage mortImage;

    private final int GAME_WIDTH = 900;
    private final int GAME_HEIGHT = 700;
    private final int CHAT_WIDTH = 350;
    private int bateauSpeed;
    private final int TIR_SPEED;
    private final int AVION_SPEED;
    private final int VIE_MAX;
    private final int BATEAU_COUNT;

    private JTextPane chatArea;
    private JTextField chatInput;
    private JButton sendButton;
    private StringBuilder chatHistoryHtml = new StringBuilder("<html>");

    private ServerSocket chatServerSocket;
    private Socket chatClientSocket;
    private PrintWriter chatOut;
    private BufferedReader chatIn;
    private boolean isServer = false;
    private Thread chatThread;

    private boolean movingLeft = false;
    private boolean movingRight = false;
    private boolean movingUp = false;
    private boolean movingDown = false;
    private Thread avionMovementThread;

    private boolean isPaused = false;
    private AudioManager audioManager;

    private boolean gameStarted = false;

    private boolean isInvincible = false;
    private static final int INVINCIBILITY_DURATION = 2000;

    public JeuInterface(String joueur, String niveau, String avion) {
        this.joueur = joueur;
        this.niveau = niveau;
        this.avion = avion;
        this.audioManager = AudioManager.getInstance();
        this.audioManager.reset();

        switch (niveau) {
            case "Debutant":
                BATEAU_COUNT = 3;
                bateauSpeed = 3;
                currentLevel = 1;
                break;
            case "Intermediaire":
                BATEAU_COUNT = 4;
                bateauSpeed = 5;
                currentLevel = 2;
                break;
            case "Professionnel":
                BATEAU_COUNT = 5;
                bateauSpeed = 7;
                currentLevel = 3;
                break;
            case "Haut niveau":
                BATEAU_COUNT = 6;
                bateauSpeed = 9;
                currentLevel = 4;
                break;
            default:
                BATEAU_COUNT = 4;
                bateauSpeed = 5;
                currentLevel = 1;
        }

        switch (avion) {
            case "MiG-51S":
                AVION_SPEED = 8;
                TIR_SPEED = 8;
                VIE_MAX = 4;
                break;
            case "F/A-28A":
                AVION_SPEED = 7;
                TIR_SPEED = 9;
                VIE_MAX = 5;
                break;
            case "Su-55":
                AVION_SPEED = 9;
                TIR_SPEED = 7;
                VIE_MAX = 4;
                break;
            case "Su-51K":
                AVION_SPEED = 6;
                TIR_SPEED = 10;
                VIE_MAX = 6;
                break;
            default:
                AVION_SPEED = 7;
                TIR_SPEED = 8;
                VIE_MAX = 4;
        }

        vies = VIE_MAX;

        setTitle("Jeu de Tir - " + joueur);
        setSize(GAME_WIDTH + CHAT_WIDTH, GAME_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);

        initComponents();
        loadImages();
        setupGame();
        startScoreTimer();
        initializeChat();
    }

    private void startScoreTimer() {
        startTime = System.currentTimeMillis();
        scoreTimer = new Timer();
        scoreTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!isPaused) {
                score += 1;
                scoreLabel.setText("Score: " + score);
                    checkLevelUp();
                }
            }
        }, 0, 1000);
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        
        gamePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                }
            }
        };
        gamePanel.setLayout(null);
        gamePanel.setPreferredSize(new Dimension(GAME_WIDTH, GAME_HEIGHT));
        mainPanel.add(gamePanel, BorderLayout.CENTER);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(null);
        infoPanel.setBounds(0, 0, GAME_WIDTH, 40);
        infoPanel.setOpaque(false);
        gamePanel.add(infoPanel);


        JLabel playerLabel = new JLabel("Joueur: " + joueur);
        playerLabel.setFont(new Font("Arial", Font.BOLD, 16));
        playerLabel.setForeground(Color.WHITE);
        playerLabel.setBounds(10, 10, 200, 20);
        infoPanel.add(playerLabel);


        scoreLabel = new JLabel("Score: 0");
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 16));
        scoreLabel.setForeground(Color.WHITE);
        scoreLabel.setBounds(220, 10, 100, 20);
        infoPanel.add(scoreLabel);


        levelLabel = new JLabel("Niveau: " + niveau);
        levelLabel.setFont(new Font("Arial", Font.BOLD, 16));
        levelLabel.setForeground(Color.WHITE);
        levelLabel.setBounds(340, 10, 200, 20);
        infoPanel.add(levelLabel);


        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setPreferredSize(new Dimension(CHAT_WIDTH, GAME_HEIGHT));

        JPanel chatHeaderPanel = new JPanel(new BorderLayout());
        JLabel chatTitle = new JLabel("Chat du jeu", SwingConstants.CENTER);
        chatTitle.setFont(new Font("Arial", Font.BOLD, 14));
        chatHeaderPanel.add(chatTitle, BorderLayout.CENTER);
        chatPanel.add(chatHeaderPanel, BorderLayout.NORTH);

        chatArea = new JTextPane();
        chatArea.setContentType("text/html");
        chatArea.setEditable(false);
        chatArea.setText(chatHistoryHtml.toString() + "</html>");
        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatPanel.add(chatScroll, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        chatInput = new JTextField();
        sendButton = new JButton("Envoyer");
        inputPanel.add(chatInput, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);
        mainPanel.add(chatPanel, BorderLayout.EAST);

        sendButton.addActionListener(e -> sendChatMessage());
        chatInput.addActionListener(e -> sendChatMessage());

        vieLabels = new ArrayList<>();
        viePerdueLabels = new ArrayList<>();
        int vieY = GAME_HEIGHT - 80;
        
        for (int i = 0; i < VIE_MAX; i++) {
            JLabel vieLabel = new JLabel();
            vieLabel.setBounds(GAME_WIDTH - 150 + i * 35, vieY, 30, 30);
            vieLabel.setOpaque(false);
            gamePanel.add(vieLabel);
            vieLabels.add(vieLabel);

            JLabel viePerdueLabel = new JLabel();
            viePerdueLabel.setBounds(GAME_WIDTH - 150 + i * 35, vieY, 30, 30);
            viePerdueLabel.setOpaque(false);
            viePerdueLabel.setVisible(false);
            gamePanel.add(viePerdueLabel);
            viePerdueLabels.add(viePerdueLabel);
        }

        avionLabel = new JLabel();
        avionLabel.setBounds(GAME_WIDTH/2 - 50, GAME_HEIGHT - 150, 100, 100);
        gamePanel.add(avionLabel);

        bateauLabels = new ArrayList<>();
        for (int i = 0; i < BATEAU_COUNT; i++) {
            JLabel bateauLabel = new JLabel();
            bateauLabel.setBounds(0, 0, 80, 80);
            gamePanel.add(bateauLabel);
            bateauLabels.add(bateauLabel);
        }

        add(mainPanel);
    }

    private void loadImages() {
        try {
            backgroundImage = ImageIO.read(new File("src/main/resources/images/ArrierPlan.jpg"));

            String avionFileName = avion.replace("/", "_").replace(" ", "_");
            File avionFile = new File("src/main/resources/icones/" + avionFileName + ".png");
            if (!avionFile.exists()) {
                throw new IOException("Fichier d'avion non trouvé: " + avionFile.getAbsolutePath());
            }
            avionImage = ImageIO.read(avionFile);

            bateauImages = new BufferedImage[BATEAU_COUNT];
            for (int i = 0; i < BATEAU_COUNT; i++) {
                File bateauFile = new File("src/main/resources/images/B" + (i+1) + ".png");
                if (!bateauFile.exists()) {
                    throw new IOException("Fichier de bateau non trouvé: " + bateauFile.getAbsolutePath());
                }
                bateauImages[i] = ImageIO.read(bateauFile);
            }

            File vieFile = new File("src/main/resources/images/UneVie.png");
            if (!vieFile.exists()) {
                throw new IOException("Fichier de vie non trouvé: " + vieFile.getAbsolutePath());
            }
            vieImage = ImageIO.read(vieFile);

            File tirFile = new File("src/main/resources/images/Projectile.png");
            if (!tirFile.exists()) {
                throw new IOException("Fichier de tir non trouvé: " + tirFile.getAbsolutePath());
            }
            tirImage = ImageIO.read(tirFile);

            File mortFile = new File("src/main/resources/images/Mort.png");
            if (!mortFile.exists()) {
                throw new IOException("Fichier de mort non trouvé: " + mortFile.getAbsolutePath());
            }
            mortImage = ImageIO.read(mortFile);

            File viePerdueFile = new File("src/main/resources/images/PerduVie.png");
            if (!viePerdueFile.exists()) {
                throw new IOException("Fichier de vie perdue non trouvé: " + viePerdueFile.getAbsolutePath());
            }
            viePerdueImage = ImageIO.read(viePerdueFile);

            avionLabel.setIcon(new ImageIcon(avionImage.getScaledInstance(100, 100, Image.SCALE_SMOOTH)));
            for (int i = 0; i < BATEAU_COUNT; i++) {
                bateauLabels.get(i).setIcon(new ImageIcon(bateauImages[i].getScaledInstance(80, 80, Image.SCALE_SMOOTH)));
            }
            
            for (int i = 0; i < VIE_MAX; i++) {
                vieLabels.get(i).setIcon(new ImageIcon(vieImage.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                viePerdueLabels.get(i).setIcon(new ImageIcon(viePerdueImage.getScaledInstance(30, 30, Image.SCALE_SMOOTH)));
                vieLabels.get(i).setVisible(i < vies);
                viePerdueLabels.get(i).setVisible(false);
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erreur lors du chargement des images:\n" + e.getMessage() + "\n\n" +
                            "Vérifiez que les fichiers suivants existent:\n" +
                            "- src/main/resources/images/ArrierPlan.jpg\n" +
                            "- src/main/resources/icones/" + avion.replace("/", "_").replace(" ", "_") + ".png\n" +
                            "- src/main/resources/images/B1.png\n" +
                            "- src/main/resources/images/B2.png\n" +
                            "- src/main/resources/images/B3.png\n" +
                            "- src/main/resources/images/B4.png\n" +
                            "- src/main/resources/images/UneVie.png\n" +
                            "- src/main/resources/images/Projectile.png\n" +
                            "- src/main/resources/images/Mort.png\n" +
                            "- src/main/resources/images/PerduVie.png",
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            dispose();
        }
    }

    private void setupGame() {
        projectiles = new CopyOnWriteArrayList<>();

        Random random = new Random();
        for (JLabel bateauLabel : bateauLabels) {
            int x = random.nextInt(GAME_WIDTH - 80);
            bateauLabel.setLocation(x, -50);
        }

        this.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                        movingLeft = true;
                        break;
                    case KeyEvent.VK_RIGHT:
                        movingRight = true;
                        break;
                    case KeyEvent.VK_UP:
                        movingUp = true;
                        break;
                    case KeyEvent.VK_DOWN:
                        movingDown = true;
                        break;
                    case KeyEvent.VK_SPACE:
                        shoot();
                        break;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                        movingLeft = false;
                        break;
                    case KeyEvent.VK_RIGHT:
                        movingRight = false;
                        break;
                    case KeyEvent.VK_UP:
                        movingUp = false;
                        break;
                    case KeyEvent.VK_DOWN:
                        movingDown = false;
                        break;
                }
            }
        });

        avionMovementThread = new Thread(() -> {
            while (true) {
                int x = avionLabel.getX();
                int y = avionLabel.getY();

                if (movingLeft && x > 20) x -= AVION_SPEED;
                if (movingRight && x < GAME_WIDTH - 120) x += AVION_SPEED;
                if (movingUp && y > 20) y -= AVION_SPEED;
                if (movingDown && y < GAME_HEIGHT - 150) y += AVION_SPEED;

                final int finalX = x;
                final int finalY = y;
                SwingUtilities.invokeLater(() -> {
                    avionLabel.setLocation(finalX, finalY);
                });

                try {
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        avionMovementThread.start();

        Timer startTimer = new Timer();
        startTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                gameStarted = true;
                startTimer.cancel();
            }
        }, 1000);

        collisionTimer = new Timer();
        collisionTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (gameStarted && !isPaused) {
                moveBoats();
                checkCollisions();
                updateProjectiles();
            }
            }
        }, 1000, 50);
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            requestFocus();
        }
    }

    private void shoot() {
        if (!gameStarted || isPaused) return;
        
        SwingUtilities.invokeLater(() -> {
            int projectileX = avionLabel.getX() + (avionLabel.getWidth() - 20) / 2;
            int projectileY = avionLabel.getY();
            
            Projectile projectile = new Projectile(projectileX, projectileY, tirImage, TIR_SPEED);
            projectiles.add(projectile);
            gamePanel.add(projectile.getLabel());
            gamePanel.revalidate();
            
            audioManager.playShootSound();
        });
    }

    private void updateProjectiles() {
        if (!gameStarted || isPaused) return;

        ArrayList<Projectile> projectilesToRemove = new ArrayList<>();
        
        for (Projectile projectile : projectiles) {
            if (projectile.isActive()) {
                projectile.move();
            } else {
                projectilesToRemove.add(projectile);
            }
        }
        
        if (!projectilesToRemove.isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                for (Projectile projectile : projectilesToRemove) {
                    projectiles.remove(projectile);
                }
                gamePanel.revalidate();
                gamePanel.repaint();
            });
        }
    }

    private void moveBoats() {
        if (!gameStarted || isPaused) return;
        
        for (JLabel bateauLabel : bateauLabels) {
            int x = bateauLabel.getX();
            int y = bateauLabel.getY();

            y += bateauSpeed;

            if (y > GAME_HEIGHT) {
                y = -50;
                x = new Random().nextInt(GAME_WIDTH - 80);
            }

            final int finalX = x;
            final int finalY = y;
            SwingUtilities.invokeLater(() -> {
                bateauLabel.setLocation(finalX, finalY);
            });
        }
    }

    private void checkCollisions() {
        if (!gameStarted || isPaused) return;
        
        if (!isInvincible) {
            Rectangle avionBounds = new Rectangle(
                avionLabel.getX() + 20,
                avionLabel.getY() + 20,
                avionLabel.getWidth() - 40,
                avionLabel.getHeight() - 40
            );

            for (JLabel bateauLabel : bateauLabels) {
                Rectangle bateauBounds = new Rectangle(
                    bateauLabel.getX() + 10,
                    bateauLabel.getY() + 10,
                    bateauLabel.getWidth() - 20,
                    bateauLabel.getHeight() - 20
                );

                if (avionBounds.intersects(bateauBounds)) {
                    handleCollision(bateauLabel);
                    break;
                }
            }
        }

        for (JLabel bateauLabel : bateauLabels) {
            Rectangle bateauBounds = new Rectangle(
                bateauLabel.getX() + 10,
                bateauLabel.getY() + 10,
                bateauLabel.getWidth() - 20,
                bateauLabel.getHeight() - 20
            );

            for (Projectile projectile : projectiles) {
                if (projectile.isActive()) {
                    Rectangle projectileBounds = new Rectangle(
                        projectile.getX() + 2,
                        projectile.getY() + 2,
                        projectile.getWidth() - 4,
                        projectile.getHeight() - 4
                    );

                    if (projectileBounds.intersects(bateauBounds)) {
                        score += 5;
                        SwingUtilities.invokeLater(() -> scoreLabel.setText("Score: " + score));
                        projectile.deactivate();
                        resetBateau(bateauLabel);
                        break;
                    }
                }
            }
        }
    }

    private void resetBateau(JLabel bateauLabel) {
        SwingUtilities.invokeLater(() -> {
            int newX = new Random().nextInt(GAME_WIDTH - 80);
            bateauLabel.setLocation(newX, -50);
        });
    }

    private class Projectile {
        private JLabel label;
        private int x, y;
        private int speed;
        private boolean active;
        private final int width = 20;
        private final int height = 20;

        public Projectile(int startX, int startY, BufferedImage image, int speed) {
            this.x = startX;
            this.y = startY;
            this.speed = speed;
            this.active = true;
            
            label = new JLabel(new ImageIcon(image.getScaledInstance(width, height, Image.SCALE_SMOOTH)));
            label.setBounds(x, y, width, height);
        }

        public void move() {
            if (active) {
                y -= speed;
                if (y < -height) {
                    active = false;
                } else {
                    SwingUtilities.invokeLater(() -> label.setLocation(x, y));
                }
            }
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }

        public int getX() { return x; }
        public int getY() { return y; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
        public JLabel getLabel() { return label; }
        public boolean isActive() { return active; }
        public void deactivate() { 
            active = false; 
            SwingUtilities.invokeLater(() -> {
                if (label.getParent() != null) {
                    label.getParent().remove(label);
                }
            });
        }
    }

    private void handleCollision(JLabel bateauLabel) {
        if (isInvincible) return;
        
        vies--;
        updateLives();
        
        audioManager.playCollisionSound();
        
        resetBateau(bateauLabel);
        
        if (vies <= 0) {
            gameOver();
        } else {
            makeInvincible();
        }
    }

    private void makeInvincible() {
        isInvincible = true;
        
        Timer blinkTimer = new Timer();
        blinkTimer.scheduleAtFixedRate(new TimerTask() {
            boolean visible = true;
            int count = 0;
            @Override
            public void run() {
                if (count >= 8) {
                    blinkTimer.cancel();
                    SwingUtilities.invokeLater(() -> avionLabel.setVisible(true));
                    return;
                }
                SwingUtilities.invokeLater(() -> avionLabel.setVisible(visible));
                visible = !visible;
                count++;
            }
        }, 0, 250);

        Timer invincibilityTimer = new Timer();
        invincibilityTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                isInvincible = false;
                invincibilityTimer.cancel();
            }
        }, INVINCIBILITY_DURATION);
    }

    private void updateLives() {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < VIE_MAX; i++) {
                if (i < vies) {
                    vieLabels.get(i).setVisible(true);
                    viePerdueLabels.get(i).setVisible(false);
                } else {
                    vieLabels.get(i).setVisible(false);
                    if (i == vies) {
                        viePerdueLabels.get(i).setVisible(true);
                        animateLostLife(i);
                    }
                }
            }
        });
    }

    private void animateLostLife(int index) {
        new Thread(() -> {
            JLabel viePerdueLabel = viePerdueLabels.get(index);
            int startY = viePerdueLabel.getY();
            int endY = -viePerdueLabel.getHeight();
            
            viePerdueLabel.setVisible(true);
            
            for (int y = startY; y >= endY; y -= 5) {
                final int finalY = y;
                SwingUtilities.invokeLater(() -> {
                    viePerdueLabel.setLocation(viePerdueLabel.getX(), finalY);
                });
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {break;}
            }

            SwingUtilities.invokeLater(() -> {
                viePerdueLabel.setVisible(false);
            });
        }).start();
    }

    private void gameOver() {
        SwingUtilities.invokeLater(() -> {
            if (scoreTimer != null) {
                scoreTimer.cancel();
                scoreTimer = null;
            }
            if (collisionTimer != null) {
                collisionTimer.cancel();
                collisionTimer = null;
            }
            if (avionMovementThread != null) {
                avionMovementThread.interrupt();
                avionMovementThread = null;
            }

            for (Projectile projectile : projectiles) {
                if (projectile.getLabel().getParent() != null) {
                    gamePanel.remove(projectile.getLabel());
                }
            }
            projectiles.clear();

            audioManager.stopBackgroundMusic();
            audioManager.playDeathSound();

            int mortSize = 150;
            JLabel mortLabel = new JLabel(new ImageIcon(mortImage.getScaledInstance(mortSize, mortSize, Image.SCALE_SMOOTH)));
            int mortX = avionLabel.getX() + (avionLabel.getWidth() - mortSize) / 2;
            int mortY = avionLabel.getY() + (avionLabel.getHeight() - mortSize) / 2;
            mortLabel.setBounds(mortX, mortY, mortSize, mortSize);
            gamePanel.add(mortLabel, 0);
            avionLabel.setVisible(true);
            gamePanel.revalidate();
            gamePanel.repaint();

            Timer fadeTimer = new Timer();
            final float[] alpha = {1.0f};
            fadeTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    alpha[0] -= 0.1f;
                    if (alpha[0] <= 0) {
                        fadeTimer.cancel();
                    SwingUtilities.invokeLater(() -> {

                        ScoreDatabase db = new ScoreDatabase();
                        db.saveOrUpdateScore(joueur, score);

                            gamePanel.removeAll();
                            gamePanel.revalidate();
                            gamePanel.repaint();

                        Communication communication = new Communication(joueur, niveau, avion, score);
                        communication.setVisible(true);
                        dispose();
                    });
                    } else {
                        mortLabel.setIcon(new ImageIcon(createTranslucentImage(mortImage, alpha[0]).getScaledInstance(mortSize, mortSize, Image.SCALE_SMOOTH)));
                    }
                }
            }, 0, 100);
        });
    }

    private BufferedImage createTranslucentImage(BufferedImage image, float alpha) {
        BufferedImage translucentImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = translucentImage.createGraphics();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        return translucentImage;
    }

    private void checkLevelUp() {
        int newLevel = calculateLevel(score);
        if (newLevel > currentLevel) {
            currentLevel = newLevel;
            levelLabel.setText("Niveau: " + getNiveauName(currentLevel));
            updateGameDifficulty();
            showLevelUpMessage();
        }
    }

    private int calculateLevel(int score) {
        if (score < 50) return 1;        // Débutant
        if (score < 100) return 2;       // Intermédiaire
        if (score < 200) return 3;       // Professionnel
        if (score < 400) return 4;       // Haut niveau
        if (score < 600) return 5;       // Haut niveau 1
        if (score < 800) return 6;       // Haut niveau 2
        if (score < 1000) return 7;      // Haut niveau 3
        return 8;                        // Haut niveau 4
    }

    private String getNiveauName(int level) {
        switch (level) {
            case 1: return "Débutant";
            case 2: return "Intermédiaire";
            case 3: return "Professionnel";
            case 4: return "Haut niveau";
            case 5: return "Haut niveau I";
            case 6: return "Haut niveau II";
            case 7: return "Haut niveau III";
            default: return "Haut niveau MASTER";
        }
    }

    private void updateGameDifficulty() {
        switch (currentLevel) {
            case 2:
                bateauSpeed += 1;
                break;
            case 3:
                bateauSpeed += 2;
                break;
            case 4:
                bateauSpeed += 3;
                break;
            case 5:
                bateauSpeed += 4;
                break;
            case 6:
                bateauSpeed += 5;
                break;
            case 7:
                bateauSpeed += 6;
                break;
            case 8:
                bateauSpeed += 7;
                break;
        }
    }

    private void showLevelUpMessage() {
        isPaused = true;

        audioManager.playLevelUpSound();
        
        final String message = "FÉLICITATIONS " + joueur.toUpperCase() + "\n" +
                             "Niveau " + getNiveauName(currentLevel) + " !";

        SwingUtilities.invokeLater(() -> {
            JDialog dialog = new JDialog(this, "", false);
            dialog.setUndecorated(true);
            
            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new BorderLayout());
            mainPanel.setBackground(new Color(70, 130, 180));
            mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

            JLabel messageLabel = new JLabel(message);
            messageLabel.setFont(new Font("Arial", Font.BOLD, 18));
            messageLabel.setForeground(Color.WHITE);
            messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
            mainPanel.add(messageLabel, BorderLayout.CENTER);

            dialog.add(mainPanel);
            dialog.pack();
            dialog.setLocationRelativeTo(this);

            Timer closeTimer = new Timer();
            closeTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    SwingUtilities.invokeLater(() -> {
                        dialog.dispose();
                        isPaused = false;
                        levelLabel.setText("Niveau: " + getNiveauName(currentLevel));
                    });
                }
            }, 1000);

            dialog.setVisible(true);
        });
    }

    private int getNextLevelScore() {
        switch (currentLevel) {
            case 1: return 50;
            case 2: return 100;
            case 3: return 200;
            case 4: return 400;
            case 5: return 600;
            case 6: return 800;
            case 7: return 1000;
            default: return 0;
        }
    }

    @Override
    public void dispose() {
        try {
            if (chatOut != null) chatOut.close();
            if (chatIn != null) chatIn.close();
            if (chatClientSocket != null) chatClientSocket.close();
            if (chatServerSocket != null) chatServerSocket.close();
            if (chatThread != null) chatThread.interrupt();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (scoreTimer != null) {
            scoreTimer.cancel();
            scoreTimer = null;
        }
        if (collisionTimer != null) {
            collisionTimer.cancel();
            collisionTimer = null;
        }
        if (avionMovementThread != null) {
            avionMovementThread.interrupt();
            avionMovementThread = null;
        }

        if (avionImage != null) avionImage.flush();
        if (backgroundImage != null) backgroundImage.flush();
        if (tirImage != null) tirImage.flush();
        if (mortImage != null) mortImage.flush();
        if (vieImage != null) vieImage.flush();
        if (viePerdueImage != null) viePerdueImage.flush();
        if (bateauImages != null) {
            for (BufferedImage img : bateauImages) {
                if (img != null) img.flush();
            }
        }

        for (Projectile projectile : projectiles) {
            if (projectile.getLabel().getParent() != null) {
                gamePanel.remove(projectile.getLabel());
            }
        }
        projectiles.clear();

        audioManager.cleanup();

        gamePanel.removeAll();
        gamePanel.revalidate();
        gamePanel.repaint();

        super.dispose();
    }

    private void initializeChat() {
        Timer timer = new Timer();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    appendChatMessage("Bienvenue sur le serveur de chat!", "Serveur");
                });
            }
        }, 2000);
    }

    private void appendChatMessage(String msg, String sender) {
        String color;
        if (sender.equals("Serveur")) {
            color = "#008000";
        } else {
            color = "#C00";
        }
        
        chatHistoryHtml.append("<span style='color:")
                .append(color)
                .append(";font-weight:bold;font-size:12px;'>")
                .append(sender)
                .append(":</span> <span style='color:#222;font-size:12px;'>")
                .append(msg)
                .append("</span><br>");
        chatArea.setText(chatHistoryHtml.toString() + "</html>");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private void sendChatMessage() {
        String msg = chatInput.getText().trim();
        if (!msg.isEmpty()) {
            appendChatMessage(msg, joueur);
            
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    SwingUtilities.invokeLater(() -> {
                        appendChatMessage("Message reçu.", "Serveur");
                    });
                }
            }, 500);
            
            chatInput.setText("");
            requestFocusInWindow();
        }
    }
}
