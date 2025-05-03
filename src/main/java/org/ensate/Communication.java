package org.ensate;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class Communication extends JFrame {
    private BufferedImage backgroundImage;
    private JPanel mainPanel;
    private JButton rejouerButton;
    private JButton retourButton;
    private JButton quitterButton;
    private JLabel messageLabel;
    private JLabel scoreLabel;
    private int choix = 0;
    private String joueur;
    private String niveau;
    private String avion;
    private int score;
    private ScoreDatabase scoreDB;
    private static final int BUTTON_COOLDOWN = 1000;
    private boolean canClick = true;
    private AudioManager audioManager;

    public Communication(String joueur, String niveau, String avion, int score) {
        this.joueur = joueur;
        this.niveau = niveau;
        this.avion = avion;
        this.score = score;
        this.scoreDB = new ScoreDatabase();
        this.audioManager = AudioManager.getInstance();

        scoreDB.saveOrUpdateScore(joueur, score);

        setTitle("Game Over - " + joueur);
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLocationRelativeTo(null);

        initComponents();
        loadImages();
        setVisible(true);
    }

    private void initComponents() {
        mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                }
            }
        };
        mainPanel.setLayout(new GridBagLayout());
        add(mainPanel);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(10, 0, 10, 0);

        JLabel scoreLabel = new JLabel("Votre score: " + score);
        scoreLabel.setForeground(new Color(255, 255, 255));
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 32));
        mainPanel.add(scoreLabel, gbc);

        int bestScore = scoreDB.getScore(joueur);
        JLabel bestScoreLabel = new JLabel("Meilleur score: " + bestScore);
        bestScoreLabel.setForeground(new Color(255, 0, 0, 255));
        bestScoreLabel.setFont(new Font("Arial", Font.BOLD, 28));
        mainPanel.add(bestScoreLabel, gbc);

        if (score > bestScore) {
            JLabel newRecordLabel = new JLabel("NOUVEAU RECORD !");
            newRecordLabel.setForeground(new Color(0, 255, 0));
            newRecordLabel.setFont(new Font("Arial", Font.BOLD, 36));
            mainPanel.add(newRecordLabel, gbc);
        }

        JLabel questionLabel = new JLabel("Que voulez-vous faire ?");
        questionLabel.setForeground(new Color(135, 206, 250));
        questionLabel.setFont(new Font("Arial", Font.BOLD, 24));
        mainPanel.add(questionLabel, gbc);

        rejouerButton = new JButton("Rejouer");
        retourButton = new JButton("Retour au menu");
        quitterButton = new JButton("Quitter");

        Color buttonColor = new Color(70, 130, 180);
        Color buttonHoverColor = new Color(100, 149, 237);
        Color buttonDisabledColor = new Color(128, 128, 128);

        for (JButton button : new JButton[]{rejouerButton, retourButton, quitterButton}) {
            button.setPreferredSize(new Dimension(250, 50));
            button.setFont(new Font("Arial", Font.BOLD, 18));
            button.setForeground(Color.WHITE);
            button.setBackground(buttonColor);
            button.setFocusPainted(false);
            button.setBorderPainted(false);
            button.setOpaque(true);
            
            button.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    if (button.isEnabled()) {
                        button.setBackground(buttonHoverColor);
                    }
                }

                public void mouseExited(java.awt.event.MouseEvent evt) {
                    if (button.isEnabled()) {
                        button.setBackground(buttonColor);
                    }
                }
            });
        }

        mainPanel.add(Box.createVerticalStrut(40), gbc);
        mainPanel.add(rejouerButton, gbc);
        mainPanel.add(Box.createVerticalStrut(15), gbc);
        mainPanel.add(retourButton, gbc);
        mainPanel.add(Box.createVerticalStrut(15), gbc);
        mainPanel.add(quitterButton, gbc);

        rejouerButton.addActionListener(e -> {
            if (!canClick) return;
            disableButtons();
            choix = 0;
            SwingUtilities.invokeLater(() -> {
                try {
                    Thread.sleep(100);
                    JeuInterface newGame = new JeuInterface(joueur, niveau, avion);
                    newGame.setVisible(true);
                    dispose();
                } catch (InterruptedException ex) {
                    enableButtons();
                }
            });
        });

        retourButton.addActionListener(e -> {
            if (!canClick) return;
            disableButtons();
            choix = 1;
            SwingUtilities.invokeLater(() -> {
                try {
                    Thread.sleep(100);
                    JeuDeTir menu = new JeuDeTir();
                    menu.setVisible(true);
                    audioManager.startBackgroundMusic();
                    dispose();
                } catch (InterruptedException ex) {
                    enableButtons();
                }
            });
        });

        quitterButton.addActionListener(e -> {
            if (!canClick) return;
            disableButtons();
            choix = 2;
            dispose();
            System.exit(0);
        });
    }

    private void disableButtons() {
        canClick = false;
        rejouerButton.setEnabled(false);
        retourButton.setEnabled(false);
        quitterButton.setEnabled(false);
        
        Timer timer = new Timer(BUTTON_COOLDOWN, e -> {
            enableButtons();
            ((Timer)e.getSource()).stop();
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void enableButtons() {
        canClick = true;
        rejouerButton.setEnabled(true);
        retourButton.setEnabled(true);
        quitterButton.setEnabled(true);
    }

    private void loadImages() {
        try {
            backgroundImage = ImageIO.read(new File("src/main/resources/images/Communication.jpg"));
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erreur lors du chargement de l'image de fond",
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void dispose() {
        if (backgroundImage != null) {
            backgroundImage.flush();
        }
        super.dispose();
    }

    public int getChoix() {
        return choix;
    }

    public String getJoueur() {
        return joueur;
    }

    public String getNiveau() {
        return niveau;
    }

    public String getAvion() {
        return avion;
    }
}