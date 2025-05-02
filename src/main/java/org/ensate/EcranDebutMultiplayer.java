package org.ensate;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.net.Socket;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.sql.*;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.AudioInputStream;

public class EcranDebutMultiplayer extends JFrame {
    private Socket socket;
    private boolean isHost;
    private String joueur;
    private String niveau;
    private String avion;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;

    private BufferedImage vaisseau1Image, vaisseau2Image, fondEtoile;
    private String joueur2 = "Joueur 2";
    private int score1 = 0, score2 = 0;
    private int vies1 = 3, vies2 = 3;
    private boolean gameOver = false;
    private BufferedImage vieImage;
    private BufferedImage viePerdueImage;
    private ArrayList<JLabel> vieLabels1 = new ArrayList<>();
    private ArrayList<JLabel> vieLabels2 = new ArrayList<>();
    private JLabel scoreLabel1;
    private JLabel scoreLabel2;
    private JLabel gameOverLabel;
    private final int GAME_WIDTH = 900;
    private final int GAME_HEIGHT = 700;
    private final int CHAT_WIDTH = 350;
    int otherX, otherY;
    int myX, myY, BOAT_WIDTH, BOAT_HEIGHT;

    private String avionJoueur1;
    private String avionJoueur2;

    private GamePanel gamePanel;

    private BufferedImage[] bateauImages;
    private BufferedImage tirImage;
    private final int BATEAU_COUNT = 4;
    private int bateauSpeed = 5;
    private CopyOnWriteArrayList<Projectile> myProjectiles = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<Projectile> otherProjectiles = new CopyOnWriteArrayList<>();
    private List<Bateau> bateaux = new ArrayList<>();
    private Timer gameTimer;
    private Random random = new Random();

    class Bateau {
        int x, y, type;
        public Bateau(int x, int y, int type) {
            this.x = x;
            this.y = y;
            this.type = type;
        }
        public Rectangle getBounds() {
            return new Rectangle(x, y, BOAT_WIDTH, BOAT_HEIGHT);
        }
    }

    private boolean iAmDead = false;
    private boolean otherIsDead = false;
    private String winnerName = "";

    private JTextPane chatArea;
    private JTextField chatInput;
    private JButton sendButton;
    private StringBuilder chatHistoryHtml = new StringBuilder("<html>");

    private int vieMax1, vieMax2;
    private int avionSpeed1, avionSpeed2;
    private int tirSpeed1, tirSpeed2;

    private String finalWinnerName = null;
    private int finalScore1 = 0, finalScore2 = 0;

    private volatile boolean isConnected = true;

    private AudioManager audioManager;

    public EcranDebutMultiplayer(String joueur, String niveau, String avion, Socket socket, boolean isHost) {
        this.joueur = joueur;
        this.niveau = niveau;
        this.avion = avion;
        this.socket = socket;
        this.isHost = isHost;
        this.audioManager = AudioManager.getInstance();
        
        audioManager.stopBackgroundMusic();

        setTitle("Jeu de Tir - Mode Multijoueur");
        setSize(GAME_WIDTH + CHAT_WIDTH, GAME_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        BOAT_WIDTH = 80;
        BOAT_HEIGHT = 80;
        myX = 150;
        myY = GAME_HEIGHT - 120;
        otherX = 600;
        otherY = GAME_HEIGHT - 120;

        setupNetwork();
        setupUI();
        startGameLoop();
        startGameTimer();
        setVisible(true);

        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent e) {
                requestFocusInWindow();
            }
            
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                audioManager.cleanup();
            }
        });
    }

    private void setupNetwork() {
        try {
            if (isHost) {
                outputStream = new ObjectOutputStream(socket.getOutputStream());
                inputStream = new ObjectInputStream(socket.getInputStream());
                outputStream.writeObject(avion);
                outputStream.flush();
                String avionAdverse = (String) inputStream.readObject();
                avionJoueur1 = avion;
                avionJoueur2 = avionAdverse;
                outputStream.writeObject(joueur);
                outputStream.flush();
                joueur2 = (String) inputStream.readObject();
            } else {
                inputStream = new ObjectInputStream(socket.getInputStream());
                outputStream = new ObjectOutputStream(socket.getOutputStream());
                String avionAdverse = (String) inputStream.readObject();
                outputStream.writeObject(avion);
                outputStream.flush();
                avionJoueur1 = avionAdverse;
                avionJoueur2 = avion;
                joueur2 = (String) inputStream.readObject();
                outputStream.writeObject(joueur);
                outputStream.flush();
            }
            initAvionCaracs();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erreur de connexion réseau: " + e.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            dispose();
        }
        try {
            String avion1FileName = avionJoueur1.replace("/", "").replace(" ", "");
            String avion2FileName = avionJoueur2.replace("/", "").replace(" ", "");
            vaisseau1Image = ImageIO.read(new File("src/main/resources/icones/" + avion1FileName + ".png"));
            vaisseau2Image = ImageIO.read(new File("src/main/resources/icones/" + avion2FileName + ".png"));
            fondEtoile = ImageIO.read(new File("src/main/resources/images/ArrierPlan.jpg"));
            vieImage = ImageIO.read(new File("src/main/resources/images/UneVie.png"));
            viePerdueImage = ImageIO.read(new File("src/main/resources/images/PerduVie.png"));
            tirImage = ImageIO.read(new File("src/main/resources/images/Projectile.png"));
            bateauImages = new BufferedImage[BATEAU_COUNT];
            for (int i = 0; i < BATEAU_COUNT; i++) {
                bateauImages[i] = ImageIO.read(new File("src/main/resources/images/B" + (i+1) + ".png"));
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Erreur lors du chargement des images.", "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setupUI() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        gamePanel = new GamePanel();
        gamePanel.setPreferredSize(new Dimension(GAME_WIDTH, GAME_HEIGHT));
        mainPanel.add(gamePanel, BorderLayout.CENTER);

        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setPreferredSize(new Dimension(CHAT_WIDTH, GAME_HEIGHT));
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
        add(mainPanel);

        sendButton.addActionListener(e -> sendChatMessage());
        chatInput.addActionListener(e -> sendChatMessage());

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (gameOver || iAmDead) return;
                boolean moved = false;
                if (e.getKeyCode() == KeyEvent.VK_LEFT && myX > 0) {
                    myX -= avionSpeed1;
                    moved = true;
                } else if (e.getKeyCode() == KeyEvent.VK_RIGHT && myX < GAME_WIDTH - BOAT_WIDTH) {
                    myX += avionSpeed1;
                    moved = true;
                } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    shootProjectile(myX + BOAT_WIDTH/2 - 5, myY, true);
                    sendGameData(new GameAction("shoot", myX + BOAT_WIDTH/2 - 5, myY));
                }
                if (moved) {
                    gamePanel.repaint();
                    sendGameData(new GameAction("move", myX, myY));
                }
            }
        });
        setFocusable(true);
        requestFocusInWindow();
    }

    private void startGameLoop() {
        new Thread(() -> {
            try {
                while (isConnected) {
                    try {
                    Object receivedData = inputStream.readObject();
                    processReceivedData(receivedData);
                    } catch (EOFException e) {
                        isConnected = false;
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(this,
                                    "La connexion a été fermée par l'autre joueur",
                                    "Déconnexion",
                                    JOptionPane.INFORMATION_MESSAGE);
                            dispose();
                        });
                        break;
            } catch (IOException | ClassNotFoundException e) {
                        isConnected = false;
                e.printStackTrace();
                        SwingUtilities.invokeLater(() -> {
                dispose();
                        });
                        break;
                    }
                }
            } finally {
                cleanup();
            }
        }).start();
    }

    private void startGameTimer() {
        // Initialiser les bateaux
        bateaux.clear();
        for (int i = 0; i < BATEAU_COUNT; i++) {
            int x = random.nextInt(GAME_WIDTH - BOAT_WIDTH);
            int y = random.nextInt(200) - 200;
            bateaux.add(new Bateau(x, y, i % BATEAU_COUNT));
        }
        gameTimer = new Timer();
        gameTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                moveBoats();
                moveProjectiles();
                checkCollisions();
                gamePanel.repaint();
            }
        }, 0, 30);
    }

    private void moveBoats() {
        for (Bateau bateau : bateaux) {
            bateau.y += bateauSpeed;
            if (bateau.y > GAME_HEIGHT) {
                bateau.y = -BOAT_HEIGHT;
                bateau.x = random.nextInt(GAME_WIDTH - BOAT_WIDTH);
            }
        }
    }

    private void moveProjectiles() {
        for (Projectile p : myProjectiles) {
            if (p.isActive()) {
                p.moveUp();
            }
        }
        for (Projectile p : otherProjectiles) {
            if (p.isActive()) {
                p.moveUp();
            }
        }
        myProjectiles.removeIf(p -> !p.isActive());
        otherProjectiles.removeIf(p -> !p.isActive());
    }

    private void shootProjectile(int x, int y, boolean isMine) {
        int speed = isMine ? tirSpeed1 : tirSpeed2;
        Projectile p = new Projectile(x, y, tirImage, speed);
        if (isMine) {
            myProjectiles.add(p);
            audioManager.playShootSound();
        } else {
            otherProjectiles.add(p);
        }
    }

    private void checkCollisions() {
        if (gameOver || iAmDead) return;
        Rectangle avion1Bounds = new Rectangle(myX, myY, BOAT_WIDTH, BOAT_HEIGHT);
        Rectangle avion2Bounds = new Rectangle(otherX, otherY, BOAT_WIDTH, BOAT_HEIGHT);
        for (Bateau bateau : bateaux) {
            Rectangle bateauBounds = bateau.getBounds();
            if (avion1Bounds.intersects(bateauBounds) && vies1 > 0) {
                vies1--;
                audioManager.playCollisionSound();
                sendGameData(new GameAction("life", vies1, 0));
                bateau.y = -BOAT_HEIGHT;
                bateau.x = random.nextInt(GAME_WIDTH - BOAT_WIDTH);
                if (vies1 <= 0) {
                    iAmDead = true;
                    audioManager.playDeathSound();
                    sendGameData(new GameAction("gameover", 0, 0));
                    checkGameOver();
                }
            }
            if (avion2Bounds.intersects(bateauBounds) && vies2 > 0) {
                vies2--;
                if (vies2 <= 0) {
                    otherIsDead = true;
                    checkGameOver();
                }
                bateau.y = -BOAT_HEIGHT;
                bateau.x = random.nextInt(GAME_WIDTH - BOAT_WIDTH);
            }
        }
        for (Projectile p : myProjectiles) {
            Rectangle pb = p.getBounds();
            for (Bateau bateau : bateaux) {
                if (pb.intersects(bateau.getBounds()) && p.isActive()) {
                    p.deactivate();
                    bateau.y = -BOAT_HEIGHT;
                    bateau.x = random.nextInt(GAME_WIDTH - BOAT_WIDTH);
                    score1 += 5;
                    if (score1 % 50 == 0) {
                        audioManager.playLevelUpSound();
                    }
                }
            }
        }
        for (Projectile p : otherProjectiles) {
            Rectangle pb = p.getBounds();
            for (Bateau bateau : bateaux) {
                if (pb.intersects(bateau.getBounds()) && p.isActive()) {
                    p.deactivate();
                    bateau.y = -BOAT_HEIGHT;
                    bateau.x = random.nextInt(GAME_WIDTH - BOAT_WIDTH);
                    score2 += 5;
                }
            }
        }
    }

    private void checkGameOver() {
        if ((vies1 <= 0 || iAmDead) && !gameOver) {
            gameOver = true;
            String winner;
            if (isHost) {
            if (vies2 > 0 && !otherIsDead) {
                    winner = joueur2 + " (" + avionJoueur2 + ")";
            } else if (vies2 <= 0 || otherIsDead) {
                winner = "Match nul";
            } else {
                winner = joueur + " (" + avion + ")";
                }
            } else {
                if (vies2 > 0 && !otherIsDead) {
                    winner = joueur2 + " (" + avionJoueur1 + ")";
                } else if (vies2 <= 0 || otherIsDead) {
                    winner = "Match nul";
                } else {
                    winner = joueur + " (" + avionJoueur2 + ")";
                }
            }
            finalWinnerName = winner;
            sendGameData(new GameAction("gameover_sync", score1, score2, finalWinnerName));
            showGameOverDialog();
        } else if ((vies2 <= 0 || otherIsDead) && !gameOver) {
            gameOver = true;
            String winner;
            if (isHost) {
            if (vies1 > 0 && !iAmDead) {
                winner = joueur + " (" + avion + ")";
            } else if (vies1 <= 0 || iAmDead) {
                winner = "Match nul";
            } else {
                    winner = joueur2 + " (" + avionJoueur2 + ")";
                }
            } else {
                if (vies1 > 0 && !iAmDead) {
                    winner = joueur2 + " (" + avionJoueur1 + ")";
                } else if (vies1 <= 0 || iAmDead) {
                    winner = "Match nul";
                } else {
                    winner = joueur + " (" + avionJoueur2 + ")";
                }
            }
            finalWinnerName = winner;
            sendGameData(new GameAction("gameover_sync", score1, score2, finalWinnerName));
            showGameOverDialog();
        }
    }

    private void showGameOverDialog() {
        SwingUtilities.invokeLater(() -> {
            audioManager.stopBackgroundMusic();
            audioManager.playDeathSound();
            
            String msg = (finalWinnerName == null || finalWinnerName.equals("Match nul")) ? 
                        "Match nul !" : 
                        ("Le vainqueur est : " + finalWinnerName);

            if (isHost) {
                msg += "\n" + joueur + " (" + avion + ") : " + score1;
                msg += "\n" + joueur2 + " (" + avionJoueur2 + ") : " + score2;
            } else {
                msg += "\n" + joueur2 + " (" + avionJoueur1 + ") : " + score2;
                msg += "\n" + joueur + " (" + avionJoueur2 + ") : " + score1;
            }

            JOptionPane.showMessageDialog(this, msg, "Fin de partie", JOptionPane.INFORMATION_MESSAGE);
            ScoreDatabase db = new ScoreDatabase();
            db.saveOrUpdateScore(joueur, isHost ? score1 : score2);
            if (gameTimer != null) gameTimer.cancel();
            audioManager.cleanup();
            dispose();
        });
    }

    private void processReceivedData(Object data) {
        if (data instanceof GameAction) {
            GameAction action = (GameAction) data;
            if ("move".equals(action.action)) {
                otherX = action.x;
                otherY = action.y;
                SwingUtilities.invokeLater(() -> gamePanel.repaint());
            } else if ("shoot".equals(action.action)) {
                shootProjectile(action.x, action.y, false);
            } else if ("life".equals(action.action)) {
                vies2 = action.x;
                if (vies2 <= 0) {
                    otherIsDead = true;
                    checkGameOver();
                }
            } else if ("gameover".equals(action.action)) {
                otherIsDead = true;
                checkGameOver();
            } else if ("gameover_sync".equals(action.action)) {
                finalScore1 = score1;
                finalScore2 = score2;
                finalWinnerName = action.actionData;
                gameOver = true;
                showGameOverDialog();
            }
        } else if (data instanceof ChatMessage) {
            ChatMessage chat = (ChatMessage) data;
            appendChatMessage(chat.message, chat.joueur);
        }
    }

    public void sendGameData(Object data) {
        if (!isConnected) return;
        try {
            synchronized (outputStream) {
            outputStream.writeObject(data);
            outputStream.flush();
            }
        } catch (IOException e) {
            isConnected = false;
            e.printStackTrace();
            SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this,
                    "Erreur d'envoi des données: " + e.getMessage(),
                        "Erreur de connexion",
                    JOptionPane.ERROR_MESSAGE);
                dispose();
            });
        }
    }

    private void cleanup() {
        isConnected = false;
        if (gameTimer != null) {
            gameTimer.cancel();
        }
        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void dispose() {
        audioManager.cleanup();
        cleanup();
        super.dispose();
    }

    class GamePanel extends JPanel {
        public GamePanel() {
            setPreferredSize(new Dimension(GAME_WIDTH, GAME_HEIGHT));
            setFocusable(true);
            setBackground(Color.BLACK);
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (fondEtoile != null) {
                g.drawImage(fondEtoile, 0, 0, getWidth(), getHeight(), null);
            }
            g.setColor(Color.WHITE);
            g.setFont(new Font("Segoe UI", Font.BOLD, 15));
            if (isHost) {
                g.drawString(joueur + " (" + avionJoueur1 + ")", 20, 30);
                g.drawString("Score: " + score1, 20, 55);
                g.drawString(joueur2 + " (" + avionJoueur2 + ")", GAME_WIDTH - 320, 30);
                g.drawString("Score: " + score2, GAME_WIDTH - 320, 55);
                g.setFont(new Font("Segoe UI", Font.BOLD, 16));
                g.drawString("Niveau: " + niveau, GAME_WIDTH/2 - 80, 35);
                for (int i = 0; i < vieMax1; i++) {
                    BufferedImage img = (i < vies1) ? vieImage : viePerdueImage;
                    g.drawImage(img, 20 + i*35, 80, 30, 30, null);
                }
                for (int i = 0; i < vieMax2; i++) {
                    BufferedImage img = (i < vies2) ? vieImage : viePerdueImage;
                    g.drawImage(img, GAME_WIDTH - 320 + i*35, 80, 30, 30, null);
                }
            } else {
                g.drawString(joueur2 + " (" + avionJoueur2 + ")", 20, 30);
                g.drawString("Score: " + score2, 20, 55);
                g.drawString(joueur + " (" + avionJoueur1 + ")", GAME_WIDTH - 320, 30);
                g.drawString("Score: " + score1, GAME_WIDTH - 320, 55);
                g.setFont(new Font("Segoe UI", Font.BOLD, 16));
                g.drawString("Niveau: " + niveau, GAME_WIDTH/2 - 80, 35);
                for (int i = 0; i < vieMax2; i++) {
                    BufferedImage img = (i < vies2) ? vieImage : viePerdueImage;
                    g.drawImage(img, 20 + i*35, 80, 30, 30, null);
                }
                for (int i = 0; i < vieMax1; i++) {
                    BufferedImage img = (i < vies1) ? vieImage : viePerdueImage;
                    g.drawImage(img, GAME_WIDTH - 320 + i*35, 80, 30, 30, null);
                }
            }
            for (Bateau bateau : bateaux) {
                if (bateauImages != null && bateauImages.length > 0)
                    g.drawImage(bateauImages[bateau.type], bateau.x, bateau.y, BOAT_WIDTH, BOAT_HEIGHT, null);
            }
            if (vaisseau1Image != null)
                g.drawImage(vaisseau1Image, myX, myY, BOAT_WIDTH, BOAT_HEIGHT, null);
            if (vaisseau2Image != null)
                g.drawImage(vaisseau2Image, otherX, otherY, BOAT_WIDTH, BOAT_HEIGHT, null);
            for (Projectile p : myProjectiles) {
                p.draw(g);
            }
            for (Projectile p : otherProjectiles) {
                p.draw(g);
            }
            if (gameOver) {
                g.setColor(Color.RED);
                g.setFont(new Font("Arial", Font.BOLD, 40));
                String msg = (finalWinnerName == null || finalWinnerName.equals("Match nul")) ? "Match nul !" : ("Le vainqueur est : " + finalWinnerName);
                g.drawString(msg, GAME_WIDTH/2 - 250, GAME_HEIGHT/2);
            }
        }
    }

    public static class Projectile {
        private int x, y, speed;
        private boolean active = true;
        private BufferedImage image;
        public Projectile(int x, int y, BufferedImage image, int speed) {
            this.x = x;
            this.y = y;
            this.image = image;
            this.speed = speed;
        }
        public void moveUp() {
            y -= speed;
            if (y < -20) active = false;
        }
        public boolean isActive() { return active; }
        public void deactivate() { active = false; }
        public Rectangle getBounds() { return new Rectangle(x, y, 10, 20); }
        public void draw(Graphics g) {
            if (active && image != null) {
                g.drawImage(image, x, y, 10, 20, null);
            }
        }
    }

    private void appendChatMessage(String msg, String sender) {
        chatHistoryHtml.append("<span style='color:#C00;font-weight:bold;font-size:12px;'>")
                .append(sender)
                .append(":</span> <span style='color:#222;font-size:12px;'>")
                .append(msg)
                .append("</span><br>");
        chatArea.setText(chatHistoryHtml.toString() + "</html>");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private void appendChatMessage(String msg) {
        chatHistoryHtml.append(msg).append("<br>");
        chatArea.setText(chatHistoryHtml.toString() + "</html>");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private void sendChatMessage() {
        String msg = chatInput.getText().trim();
        if (!msg.isEmpty()) {
            appendChatMessage(msg, joueur);
            sendGameData(new ChatMessage(joueur, msg));
            chatInput.setText("");
            requestFocusInWindow();
        }
    }

    public static class ChatMessage implements Serializable {
        public String joueur;
        public String message;
        public ChatMessage(String joueur, String message) {
            this.joueur = joueur;
            this.message = message;
        }
    }

    private void initAvionCaracs() {
        switch (avion) {
            case "MiG-51S": vieMax1 = 4; avionSpeed1 = 8; tirSpeed1 = 8; break;
            case "F/A-28A": vieMax1 = 5; avionSpeed1 = 7; tirSpeed1 = 9; break;
            case "Su-55": vieMax1 = 4; avionSpeed1 = 9; tirSpeed1 = 7; break;
            case "Su-51K": vieMax1 = 6; avionSpeed1 = 6; tirSpeed1 = 10; break;
            default: vieMax1 = 4; avionSpeed1 = 7; tirSpeed1 = 8;
        }
        String avionAdv = isHost ? avionJoueur2 : avionJoueur1;
        switch (avionAdv) {
            case "MiG-51S": vieMax2 = 4; avionSpeed2 = 8; tirSpeed2 = 8; break;
            case "F/A-28A": vieMax2 = 5; avionSpeed2 = 7; tirSpeed2 = 9; break;
            case "Su-55": vieMax2 = 4; avionSpeed2 = 9; tirSpeed2 = 7; break;
            case "Su-51K": vieMax2 = 6; avionSpeed2 = 6; tirSpeed2 = 10; break;
            default: vieMax2 = 4; avionSpeed2 = 7; tirSpeed2 = 8;
        }
        vies1 = vieMax1;
        vies2 = vieMax2;
    }
}