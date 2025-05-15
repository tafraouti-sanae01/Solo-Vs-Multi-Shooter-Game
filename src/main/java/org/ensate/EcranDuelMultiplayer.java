package org.ensate;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class EcranDuelMultiplayer extends JFrame {
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

    private BufferedImage tirImage;
    private CopyOnWriteArrayList<Projectile> myProjectiles = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<Projectile> otherProjectiles = new CopyOnWriteArrayList<>();

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

    public EcranDuelMultiplayer(String joueur, String niveau, String avion, Socket socket, boolean isHost) {
        this.joueur = joueur;
        this.niveau = niveau;
        this.avion = avion;
        this.socket = socket;
        this.isHost = isHost;
        this.audioManager = AudioManager.getInstance();

        audioManager.stopBackgroundMusic();

        setTitle("Jeu de Tir - Mode Duel 1v1");
        setSize(GAME_WIDTH + CHAT_WIDTH, GAME_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        if (isHost) {
            myX = 150;
            myY = GAME_HEIGHT - 120;
            otherX = 600;
            otherY = 120;
        } else {
            myX = 600;
            myY = 120;
            otherX = 150;
            otherY = GAME_HEIGHT - 120;
        }

        BOAT_WIDTH = 80;
        BOAT_HEIGHT = 80;

        setupNetwork();
        setupUI();
        startGameLoop();
        setVisible(true);

        Timer timer = new Timer(20, e -> {
            moveProjectiles();
            checkCollisions();
            checkGameOver();
            gamePanel.repaint();
        });
        timer.start();

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
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            outputStream.flush();
            inputStream = new ObjectInputStream(socket.getInputStream());
            if (isHost) {
                outputStream.writeObject(avion);
                outputStream.flush();
                String avionAdverse = (String) inputStream.readObject();
                avionJoueur1 = avion;
                avionJoueur2 = avionAdverse;
                outputStream.writeObject(joueur);
                outputStream.flush();
                joueur2 = (String) inputStream.readObject();
            } else {
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
            String avionMe, avionOther;
            if ((isHost && myY > otherY) || (!isHost && myY > otherY)) {
                avionMe = avionJoueur1;
                avionOther = avionJoueur2;
            } else {
                avionMe = avionJoueur2;
                avionOther = avionJoueur1;
            }
            vaisseau1Image = ImageIO.read(new File("src/main/resources/icones/" + avionMe.replace("/", "").replace(" ", "") + ".png"));
            vaisseau2Image = ImageIO.read(new File("src/main/resources/icones/" + avionOther.replace("/", "").replace(" ", "") + ".png"));
            fondEtoile = ImageIO.read(new File("src/main/resources/images/ArrierPlan.jpg"));
            vieImage = ImageIO.read(new File("src/main/resources/images/UneVie.png"));
            viePerdueImage = ImageIO.read(new File("src/main/resources/images/PerduVie.png"));
            tirImage = ImageIO.read(new File("src/main/resources/images/Projectile.png"));
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

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();
                if (key == KeyEvent.VK_LEFT) {
                    myX = Math.max(0, myX - avionSpeed1);
                    sendGameData(new PlayerPosition(myX, myY));
                } else if (key == KeyEvent.VK_RIGHT) {
                    myX = Math.min(GAME_WIDTH - BOAT_WIDTH, myX + avionSpeed1);
                    sendGameData(new PlayerPosition(myX, myY));
                } else if (key == KeyEvent.VK_SPACE) {
                    shootProjectile(myX + BOAT_WIDTH / 2, myY, true);
                }
            }
        });

        sendButton.addActionListener(e -> sendChatMessage());
        chatInput.addActionListener(e -> sendChatMessage());
    }

    private void startGameLoop() {
        new Thread(() -> {
            while (isConnected && !gameOver) {
                try {
                    Object data = inputStream.readObject();
                    processReceivedData(data);
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                    isConnected = false;
                }
            }
        }).start();
    }

    private void moveProjectiles() {
        for (Projectile p : myProjectiles) {
            p.moveUp();
        }
        for (Projectile p : otherProjectiles) {
            p.moveUp();
        }
    }

    private void shootProjectile(int x, int y, boolean isMine) {
        int direction;
        if ((isHost && myY > otherY) || (!isHost && myY > otherY)) {
            direction = 1;
        } else {
            direction = -1;
        }
        Projectile p = new Projectile(x, y, tirImage, isMine ? tirSpeed1 : tirSpeed2, direction);
        if (isMine) {
            myProjectiles.add(p);
            sendGameData(new ProjectileData(x, y, tirSpeed1 * direction));
        } else {
            otherProjectiles.add(p);
        }
    }

    private void checkCollisions() {
        Rectangle myRect = new Rectangle(myX, myY, BOAT_WIDTH, BOAT_HEIGHT);
        Rectangle otherRect = new Rectangle(otherX, otherY, BOAT_WIDTH, BOAT_HEIGHT);

        for (Projectile p : otherProjectiles) {
            if (p.isActive() && myRect.intersects(p.getBounds())) {
                p.deactivate();
                vies1--;
                score2 += 10;
                if (vies1 <= 0 && !iAmDead) {
                    iAmDead = true;
                    gameOver = true;
                    winnerName = joueur2;
                }
            }
        }

        for (Projectile p : myProjectiles) {
            if (p.isActive() && otherRect.intersects(p.getBounds())) {
                p.deactivate();
                vies2--;
                score1 += 10;
                if (vies2 <= 0 && !otherIsDead) {
                    otherIsDead = true;
                    gameOver = true;
                    winnerName = joueur;
                }
            }
        }
    }

    private void checkGameOver() {
        if (gameOver) {
            finalWinnerName = winnerName;
            finalScore1 = score1;
            finalScore2 = score2;
            showGameOverDialog();
        }
    }

    private void showGameOverDialog() {
        JDialog gameOverDialog = new JDialog(this, "Fin de partie", true);
        gameOverDialog.setSize(350, 220);
        gameOverDialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new BorderLayout());
        String message = "<html><center>Partie terminée!<br>" +
                joueur + " : " + score1 + " points, " + vies1 + " vies restantes<br>" +
                joueur2 + " : " + score2 + " points, " + vies2 + " vies restantes<br><br>" +
                "Vainqueur: <b>" + winnerName + "</b></center></html>";
        JLabel messageLabel = new JLabel(message, SwingConstants.CENTER);
        panel.add(messageLabel, BorderLayout.CENTER);

        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            gameOverDialog.dispose();
            ScoreDatabase.saveSession(joueur, joueur2, score1, score2);
            dispose();
        });
        panel.add(okButton, BorderLayout.SOUTH);

        gameOverDialog.add(panel);
        gameOverDialog.setVisible(true);
    }

    private void processReceivedData(Object data) {
        if (data instanceof ProjectileData) {
            ProjectileData pd = (ProjectileData) data;
            int direction = (pd.speed > 0) ? 1 : -1;
            Projectile p = new Projectile(pd.x, pd.y, tirImage, Math.abs(pd.speed), direction);
            otherProjectiles.add(p);
        } else if (data instanceof PlayerPosition) {
            PlayerPosition pos = (PlayerPosition) data;
            otherX = pos.x;
            otherY = pos.y;
        } else if (data instanceof ChatMessage) {
            ChatMessage chat = (ChatMessage) data;
            appendChatMessage(chat.message, chat.joueur);
        }
    }

    public void sendGameData(Object data) {
        try {
            synchronized (outputStream) {
                outputStream.writeObject(data);
                outputStream.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cleanup() {
        isConnected = false;
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void dispose() {
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
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            if (fondEtoile != null) {
                g2d.drawImage(fondEtoile, 0, 0, getWidth(), getHeight(), null);
            }
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 15));
            if (isHost) {
                g2d.drawString(joueur + " (" + avionJoueur1 + ")", 20, 30);
                g2d.drawString("Score: " + score1, 20, 55);
                g2d.drawString(joueur2 + " (" + avionJoueur2 + ")", GAME_WIDTH - 320, 30);
                g2d.drawString("Score: " + score2, GAME_WIDTH - 320, 55);
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 16));
                g2d.drawString("Niveau: " + niveau, GAME_WIDTH/2 - 80, 35);
                for (int i = 0; i < vieMax1; i++) {
                    BufferedImage img = (i < vies1) ? vieImage : viePerdueImage;
                    g2d.drawImage(img, 20 + i*35, 80, 30, 30, null);
                }
                for (int i = 0; i < vieMax2; i++) {
                    BufferedImage img = (i < vies2) ? vieImage : viePerdueImage;
                    g2d.drawImage(img, GAME_WIDTH - 320 + i*35, 80, 30, 30, null);
                }
            } else {
                g2d.drawString(joueur2 + " (" + avionJoueur2 + ")", 20, 30);
                g2d.drawString("Score: " + score2, 20, 55);
                g2d.drawString(joueur + " (" + avionJoueur1 + ")", GAME_WIDTH - 320, 30);
                g2d.drawString("Score: " + score1, GAME_WIDTH - 320, 55);
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 16));
                g2d.drawString("Niveau: " + niveau, GAME_WIDTH/2 - 80, 35);
                for (int i = 0; i < vieMax2; i++) {
                    BufferedImage img = (i < vies2) ? vieImage : viePerdueImage;
                    g2d.drawImage(img, 20 + i*35, 80, 30, 30, null);
                }
                for (int i = 0; i < vieMax1; i++) {
                    BufferedImage img = (i < vies1) ? vieImage : viePerdueImage;
                    g2d.drawImage(img, GAME_WIDTH - 320 + i*35, 80, 30, 30, null);
                }
            }
            if (vaisseau1Image != null)
                g2d.drawImage(vaisseau1Image, myX, myY, BOAT_WIDTH, BOAT_HEIGHT, null);
            if (vaisseau2Image != null)
                g2d.drawImage(vaisseau2Image, otherX, otherY, BOAT_WIDTH, BOAT_HEIGHT, null);
            for (Projectile p : myProjectiles) {
                if (p.isActive()) {
                    p.draw(g2d);
                }
            }
            for (Projectile p : otherProjectiles) {
                if (p.isActive()) {
                    p.draw(g2d);
                }
            }
            if (gameOver) {
                g2d.setColor(Color.RED);
                g2d.setFont(new Font("Arial", Font.BOLD, 40));
                String msg = (winnerName == null || winnerName.equals("Match nul")) ? "Match nul !" : ("Le vainqueur est : " + winnerName);
                g2d.drawString(msg, GAME_WIDTH/2 - 250, GAME_HEIGHT/2);
            }
        }
    }

    public static class Projectile {
        private int x, y, speed, direction;
        private boolean active = true;
        private BufferedImage image;

        public Projectile(int x, int y, BufferedImage image, int speed, int direction) {
            this.x = x;
            this.y = y;
            this.image = image;
            this.speed = Math.abs(speed);
            this.direction = direction;
        }

        public void moveUp() {
            y -= speed * direction;
            if ((direction == 1 && y < 0) || (direction == -1 && y > 700)) {
                active = false;
            }
        }

        public boolean isActive() { return active; }
        public void deactivate() { active = false; }
        public Rectangle getBounds() { return new Rectangle(x, y, 10, 20); }

        public void draw(Graphics g) {
            g.drawImage(image, x, y, 10, 20, null);
        }
    }

    private void appendChatMessage(String msg, String sender) {
        chatHistoryHtml.append("<b>").append(sender).append(":</b> ").append(msg).append("<br>");
        chatArea.setText(chatHistoryHtml.toString() + "</html>");
    }

    private void appendChatMessage(String msg) {
        appendChatMessage(msg, joueur);
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

    private void initAvionCaracs() {
        // TODO: Initialiser les caractéristiques des avions pour le mode duel
        vieMax1 = 3;
        vieMax2 = 3;
        avionSpeed1 = 5;
        avionSpeed2 = 5;
        tirSpeed1 = 10;
        tirSpeed2 = 10;
    }

    public static class PlayerPosition implements Serializable {
        public int x, y;
        public PlayerPosition(int x, int y) { this.x = x; this.y = y; }
    }

    public static class ChatMessage implements Serializable {
        public String joueur, message;
        public ChatMessage(String joueur, String message) {
            this.joueur = joueur;
            this.message = message;
        }
    }

    public static class ProjectileData implements Serializable {
        public int x, y, speed;
        public ProjectileData(int x, int y, int speed) {
            this.x = x;
            this.y = y;
            this.speed = speed;
        }
    }
}