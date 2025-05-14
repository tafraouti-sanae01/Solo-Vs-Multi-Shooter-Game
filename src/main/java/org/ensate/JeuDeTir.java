package org.ensate;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.net.Socket;
import java.net.ServerSocket;

public class JeuDeTir extends javax.swing.JFrame {
    private JPanel JeuTir;
    private JComboBox comboBox1;
    private JTextField textField1;
    private JRadioButton debutantRadioButton;
    private JRadioButton intermediaireRadioButton;
    private JRadioButton professionnelRadioButton;
    private JRadioButton hautNiveauRadioButton;
    private JRadioButton miG51SRadioButton;
    private JRadioButton fA28ARadioButton;
    private JRadioButton su55RadioButton;
    private JRadioButton su51KRadioButton;
    private JProgressBar Vitesse;
    private JProgressBar Attack;
    private JProgressBar Vie;
    private JButton quiterButton;
    private JButton commencerButton;
    private JDialog modeSelectionDialog;
    private static final int SERVER_PORT = 5000;
    private AudioManager audioManager;

    private ButtonGroup niveauGroup;
    private ButtonGroup avionGroup;

    private Map<String, int[]> statsAvions;

    private static final String PLAYERS_FILE = "joueurs.txt";

    public JeuDeTir() {
        setTitle("Jeu de Tir");
        setContentPane(JeuTir);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(650, 450);
        this.audioManager = AudioManager.getInstance();

        debutantRadioButton.setOpaque(false);
        intermediaireRadioButton.setOpaque(false);
        professionnelRadioButton.setOpaque(false);
        hautNiveauRadioButton.setOpaque(false);
        miG51SRadioButton.setOpaque(false);
        fA28ARadioButton.setOpaque(false);
        su55RadioButton.setOpaque(false);
        su51KRadioButton.setOpaque(false);

        setupButtonGroups();
        initAvionStats();
        chargerJoueurs();
        setupEventListeners();
        setupProgressBars();
        audioManager.startBackgroundMusic();
        setVisible(true);
        setLocationRelativeTo(null);
    }

    @Override
    public void dispose() {
        audioManager.stopBackgroundMusic();
        super.dispose();
    }

    private void setupProgressBars() {
        Vitesse.setMinimum(0);
        Vitesse.setMaximum(100);
        Attack.setMinimum(0);
        Attack.setMaximum(100);
        Vie.setMinimum(0);
        Vie.setMaximum(100);

        Vitesse.setValue(0);
        Attack.setValue(0);
        Vie.setValue(0);
    }

    private void setupButtonGroups() {
        niveauGroup = new ButtonGroup();
        niveauGroup.add(debutantRadioButton);
        niveauGroup.add(intermediaireRadioButton);
        niveauGroup.add(professionnelRadioButton);
        niveauGroup.add(hautNiveauRadioButton);

        avionGroup = new ButtonGroup();
        avionGroup.add(miG51SRadioButton);
        avionGroup.add(fA28ARadioButton);
        avionGroup.add(su55RadioButton);
        avionGroup.add(su51KRadioButton);
    }

    private void initAvionStats() {
        statsAvions = new HashMap<>();
        statsAvions.put("MiG-51S", new int[]{80, 70, 60});
        statsAvions.put("F/A-28A", new int[]{70, 75, 65});
        statsAvions.put("Su-55", new int[]{85, 80, 70});
        statsAvions.put("Su-51K", new int[]{75, 85, 75});
    }

    private void chargerJoueurs() {
        try {
            File file = new File(PLAYERS_FILE);
            if (file.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        comboBox1.addItem(line.trim());
                    }

                }
                reader.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erreur lors du chargement des joueurs: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sauvegarderJoueur(String nomJoueur) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(PLAYERS_FILE, true));
            writer.write(nomJoueur);
            writer.newLine();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Erreur lors de l'enregistrement du joueur: " + e.getMessage(),
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setupEventListeners() {
        textField1.addActionListener(e -> {
            ajouterJoueur();
        });

        comboBox1.addActionListener(e -> {
            if ("Nouveau..".equals(comboBox1.getSelectedItem())) {
                textField1.setEnabled(true);
                textField1.requestFocus();
            } else {
                textField1.setEnabled(false);
            }
        });

        ActionListener avionListener = e -> {
            JRadioButton selectedAvion = (JRadioButton) e.getSource();
            if (selectedAvion.isSelected()) {
                updateStats(selectedAvion.getText());
            }
        };

        miG51SRadioButton.addActionListener(avionListener);
        fA28ARadioButton.addActionListener(avionListener);
        su55RadioButton.addActionListener(avionListener);
        su51KRadioButton.addActionListener(avionListener);

        quiterButton.addActionListener(e -> {
            dispose();
            System.exit(0);
        });

        commencerButton.addActionListener(e -> {
            if (validateSelections()) {
                showModeSelectionDialog();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Veuillez sélectionner un joueur, un niveau et un avion avant de commencer.",
                        "Sélections incomplètes",
                        JOptionPane.WARNING_MESSAGE);
            }
        });
    }

    private void ajouterJoueur() {
        String nomJoueur = textField1.getText().trim();
        if (!nomJoueur.isEmpty()) {
            boolean existe = false;
            for (int i = 0; i < comboBox1.getItemCount(); i++) {
                if (nomJoueur.equals(comboBox1.getItemAt(i))) {
                    existe = true;
                    break;
                }
            }

            if (!existe) {
                comboBox1.addItem(nomJoueur);
                sauvegarderJoueur(nomJoueur);
            }
            comboBox1.setSelectedItem(nomJoueur);
            textField1.setText("");
            textField1.setEnabled(false);
        }
    }

    private String getNiveauSelectionne() {
        if (debutantRadioButton.isSelected()) return "Debutant";
        if (intermediaireRadioButton.isSelected()) return "Intermediaire";
        if (professionnelRadioButton.isSelected()) return "Professionnel";
        if (hautNiveauRadioButton.isSelected()) return "Haut niveau";
        return "";
    }

    private String getAvionSelectionne() {
        if (miG51SRadioButton.isSelected()) return "MiG-51S";
        if (fA28ARadioButton.isSelected()) return "F/A-28A";
        if (su55RadioButton.isSelected()) return "Su-55";
        if (su51KRadioButton.isSelected()) return "Su-51K";
        return "";
    }

    private void updateStats(String avionName) {
        int[] stats = statsAvions.get(avionName);
        if (stats != null) {
            Vitesse.setValue(stats[0]);
            Attack.setValue(stats[1]);
            Vie.setValue(stats[2]);
        }
    }

    private boolean validateSelections() {
        if (comboBox1.getSelectedItem() == null || "Nouveau..".equals(comboBox1.getSelectedItem())) {
            return false;
        }

        if (niveauGroup.getSelection() == null) {
            return false;
        }

        if (avionGroup.getSelection() == null) {
            return false;
        }

        return true;
    }

    private void showModeSelectionDialog() {
        modeSelectionDialog = new JDialog(this, "Sélection du mode de jeu", true);
        modeSelectionDialog.setUndecorated(false);
        modeSelectionDialog.setSize(400, 300);
        modeSelectionDialog.setLocationRelativeTo(this);
        modeSelectionDialog.setResizable(false);
        modeSelectionDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(135, 206, 235); // Bleu ciel
                Color color2 = new Color(255, 255, 255); // Blanc
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        mainPanel.setLayout(new GridBagLayout());

        JLabel titleLabel = new JLabel("Sélection du mode de jeu");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(new Color(70, 130, 180));

        JButton singlePlayerButton = createStyledButton("Mode Monojoueur");
        JButton multiPlayerButton = createStyledButton("Mode Multijoueur");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(20, 50, 20, 50);

        mainPanel.add(titleLabel, gbc);
        gbc.insets = new Insets(10, 50, 10, 50);
        mainPanel.add(singlePlayerButton, gbc);
        gbc.insets = new Insets(10, 50, 20, 50);
        mainPanel.add(multiPlayerButton, gbc);

        singlePlayerButton.addActionListener(e -> {
            modeSelectionDialog.dispose();
            startSinglePlayerGame();
        });

        multiPlayerButton.addActionListener(e -> {
            modeSelectionDialog.dispose();
            showMultiplayerOptions();
        });

        modeSelectionDialog.add(mainPanel);
        modeSelectionDialog.setVisible(true);
    }

    private void showMultiplayerOptions() {
        JDialog optionsDialog = new JDialog(this, "", true);
        optionsDialog.setUndecorated(true);
        optionsDialog.setSize(400, 250);
        optionsDialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(240, 240, 240);
                Color color2 = new Color(255, 255, 255);
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createLineBorder(new Color(70, 130, 180), 2));

        ImageIcon questionIcon = new ImageIcon("src/main/resources/images/question_icon.png");
        JLabel iconLabel = new JLabel(questionIcon);

        JLabel titleLabel = new JLabel("Que souhaitez-vous faire ?");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(new Color(70, 130, 180));

        JButton createButton = createStyledButton("Créer une partie");
        JButton joinButton = createStyledButton("Rejoindre une partie");
        JButton createDuelButton = createStyledButton("Créer un duel 1v1");
        JButton joinDuelButton = createStyledButton("Rejoindre un duel 1v1");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 20, 5, 20);

        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setOpaque(false);
        headerPanel.add(iconLabel);
        headerPanel.add(titleLabel);

        mainPanel.add(headerPanel, gbc);
        gbc.insets = new Insets(20, 50, 10, 50);
        mainPanel.add(createButton, gbc);
        gbc.insets = new Insets(10, 50, 10, 50);
        mainPanel.add(joinButton, gbc);
        gbc.insets = new Insets(10, 50, 10, 50);
        mainPanel.add(createDuelButton, gbc);
        gbc.insets = new Insets(10, 50, 20, 50);
        mainPanel.add(joinDuelButton, gbc);

        createButton.addActionListener(e -> {
            optionsDialog.dispose();
            createMultiplayerGame();
        });

        joinButton.addActionListener(e -> {
            optionsDialog.dispose();
            joinMultiplayerGame();
        });

        createDuelButton.addActionListener(e -> {
            optionsDialog.dispose();
            createDuelMultiplayerGame();
        });

        joinDuelButton.addActionListener(e -> {
            optionsDialog.dispose();
            joinDuelMultiplayerGame();
        });

        optionsDialog.add(mainPanel);
        optionsDialog.setVisible(true);
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(70, 130, 180));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setPreferredSize(new Dimension(200, 40));

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(100, 149, 237));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(70, 130, 180));
            }
        });

        return button;
    }

    private void startSinglePlayerGame() {
        String joueur = (String) comboBox1.getSelectedItem();
        String niveau = getNiveauSelectionne();
        String avion = getAvionSelectionne();
        dispose();
        SwingUtilities.invokeLater(() -> {
            JeuInterface jeuInterface = new JeuInterface(joueur, niveau, avion);
            jeuInterface.setVisible(true);
            jeuInterface.requestFocus();
        });
    }

    private void createMultiplayerGame() {
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            JDialog waitDialog = new JDialog(this, "", true);
            waitDialog.setUndecorated(true);
            waitDialog.setSize(450, 180);
            waitDialog.setLocationRelativeTo(this);

            JPanel waitPanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    int w = getWidth();
                    int h = getHeight();
                    Color color1 = new Color(240, 240, 240);
                    Color color2 = new Color(255, 255, 255);
                    GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
                    g2d.setPaint(gp);
                    g2d.fillRect(0, 0, w, h);
                }
            };
            waitPanel.setLayout(new GridBagLayout());
            waitPanel.setBorder(BorderFactory.createLineBorder(new Color(70, 130, 180), 2));

            ImageIcon infoIcon = new ImageIcon("src/main/resources/images/info_icon.png");
            JLabel iconLabel = new JLabel(infoIcon);

            JLabel waitLabel = new JLabel("<html>En attente d'un autre joueur...<br>Adresse IP: " +
                    getLocalIpAddress() + "<br>Port: " + SERVER_PORT + "</html>");
            waitLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

            JButton okButton = createStyledButton("OK");
            okButton.setPreferredSize(new Dimension(100, 30));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(10, 20, 5, 20);

            JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            headerPanel.setOpaque(false);
            headerPanel.add(iconLabel);
            headerPanel.add(waitLabel);

            waitPanel.add(headerPanel, gbc);
            gbc.insets = new Insets(10, 20, 10, 20);
            waitPanel.add(okButton, gbc);

            okButton.addActionListener(e -> waitDialog.dispose());

            waitDialog.add(waitPanel);

            new Thread(() -> {
                try {
                    Socket clientSocket = serverSocket.accept();
                    SwingUtilities.invokeLater(() -> {
                        waitDialog.dispose();
                        startMultiplayerGame(clientSocket, true);
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        waitDialog.dispose();
                        showErrorDialog("Erreur de connexion: " + e.getMessage());
                    });
                }
            }).start();

            waitDialog.setVisible(true);
        } catch (IOException e) {
            e.printStackTrace();
            showErrorDialog("Erreur lors de la création de la partie: " + e.getMessage());
        }
    }

    private void joinMultiplayerGame() {
        JDialog joinDialog = new JDialog(this, "", true);
        joinDialog.setUndecorated(true);
        joinDialog.setSize(350, 150);
        joinDialog.setLocationRelativeTo(this);

        JPanel joinPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(240, 240, 240);
                Color color2 = new Color(255, 255, 255);
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        joinPanel.setLayout(new GridBagLayout());
        joinPanel.setBorder(BorderFactory.createLineBorder(new Color(70, 130, 180), 2));

        ImageIcon infoIcon = new ImageIcon("src/main/resources/images/info_icon.png");
        JLabel iconLabel = new JLabel(infoIcon);

        JLabel promptLabel = new JLabel("Entrez l'adresse IP de l'hôte:");
        promptLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JTextField ipField = new JTextField(20);
        ipField.setPreferredSize(new Dimension(200, 30));
        ipField.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setOpaque(false);
        JButton okButton = createStyledButton("OK");
        JButton cancelButton = createStyledButton("Cancel");
        okButton.setPreferredSize(new Dimension(100, 30));
        cancelButton.setPreferredSize(new Dimension(100, 30));
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 20, 5, 20);

        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setOpaque(false);
        headerPanel.add(iconLabel);
        headerPanel.add(promptLabel);

        joinPanel.add(headerPanel, gbc);
        gbc.insets = new Insets(5, 20, 10, 20);
        joinPanel.add(ipField, gbc);
        joinPanel.add(buttonPanel, gbc);

        okButton.addActionListener(e -> {
            String ipAddress = ipField.getText().trim();
            if (!ipAddress.isEmpty()) {
                joinDialog.dispose();
                try {
                    Socket socket = new Socket(ipAddress, SERVER_PORT);
                    startMultiplayerGame(socket, false);
                } catch (IOException ex) {
                    showErrorDialog("Erreur de connexion: " + ex.getMessage());
                }
            }
        });

        cancelButton.addActionListener(e -> joinDialog.dispose());

        joinDialog.add(joinPanel);
        joinDialog.setVisible(true);
    }

    private void showErrorDialog(String message) {
        JDialog errorDialog = new JDialog(this, "", true);
        errorDialog.setUndecorated(true);
        errorDialog.setSize(300, 150);
        errorDialog.setLocationRelativeTo(this);

        JPanel errorPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(255, 200, 200);
                Color color2 = new Color(255, 255, 255);
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        errorPanel.setLayout(new GridBagLayout());
        errorPanel.setBorder(BorderFactory.createLineBorder(Color.RED, 2));

        // Icône erreur
        ImageIcon errorIcon = new ImageIcon("src/main/resources/images/error_icon.png");
        JLabel iconLabel = new JLabel(errorIcon);

        JLabel messageLabel = new JLabel(message);
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messageLabel.setForeground(Color.RED);

        JButton okButton = createStyledButton("OK");
        okButton.setPreferredSize(new Dimension(100, 30));
        okButton.setBackground(Color.RED);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 20, 5, 20);

        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setOpaque(false);
        headerPanel.add(iconLabel);
        headerPanel.add(messageLabel);

        errorPanel.add(headerPanel, gbc);
        gbc.insets = new Insets(10, 20, 10, 20);
        errorPanel.add(okButton, gbc);

        okButton.addActionListener(e -> errorDialog.dispose());

        errorDialog.add(errorPanel);
        errorDialog.setVisible(true);
    }

    private void startMultiplayerGame(Socket socket, boolean isHost) {
        String joueur = (String) comboBox1.getSelectedItem();
        String niveau = getNiveauSelectionne();
        String avion = getAvionSelectionne();
        dispose();
        new EcranDebutMultiplayer(joueur, niveau, avion, socket, isHost);
    }

    private String getLocalIpAddress() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    private void createDuelMultiplayerGame() {
        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            JDialog waitDialog = new JDialog(this, "", true);
            waitDialog.setUndecorated(true);
            waitDialog.setSize(450, 180);
            waitDialog.setLocationRelativeTo(this);

            JPanel waitPanel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    int w = getWidth();
                    int h = getHeight();
                    Color color1 = new Color(240, 240, 240);
                    Color color2 = new Color(255, 255, 255);
                    GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
                    g2d.setPaint(gp);
                    g2d.fillRect(0, 0, w, h);
                }
            };
            waitPanel.setLayout(new GridBagLayout());
            waitPanel.setBorder(BorderFactory.createLineBorder(new Color(70, 130, 180), 2));

            ImageIcon infoIcon = new ImageIcon("src/main/resources/images/info_icon.png");
            JLabel iconLabel = new JLabel(infoIcon);

            JLabel waitLabel = new JLabel("<html>En attente d'un autre joueur pour le duel...<br>Adresse IP: " +
                    getLocalIpAddress() + "<br>Port: " + SERVER_PORT + "</html>");
            waitLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

            JButton okButton = createStyledButton("OK");
            okButton.setPreferredSize(new Dimension(100, 30));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(10, 20, 5, 20);

            JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            headerPanel.setOpaque(false);
            headerPanel.add(iconLabel);
            headerPanel.add(waitLabel);

            waitPanel.add(headerPanel, gbc);
            gbc.insets = new Insets(10, 20, 10, 20);
            waitPanel.add(okButton, gbc);

            okButton.addActionListener(e -> waitDialog.dispose());

            waitDialog.add(waitPanel);

            new Thread(() -> {
                try {
                    Socket clientSocket = serverSocket.accept();
                    SwingUtilities.invokeLater(() -> {
                        waitDialog.dispose();
                        String joueur = (String) comboBox1.getSelectedItem();
                        String niveau = getNiveauSelectionne();
                        String avion = getAvionSelectionne();
                        dispose();
                        new EcranDuelMultiplayer(joueur, niveau, avion, clientSocket, true);
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        waitDialog.dispose();
                        showErrorDialog("Erreur de connexion: " + e.getMessage());
                    });
                }
            }).start();

            waitDialog.setVisible(true);
        } catch (IOException e) {
            e.printStackTrace();
            showErrorDialog("Erreur lors de la création de la partie: " + e.getMessage());
        }
    }

    private void joinDuelMultiplayerGame() {
        JDialog joinDialog = new JDialog(this, "", true);
        joinDialog.setUndecorated(true);
        joinDialog.setSize(350, 150);
        joinDialog.setLocationRelativeTo(this);

        JPanel joinPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int w = getWidth();
                int h = getHeight();
                Color color1 = new Color(240, 240, 240);
                Color color2 = new Color(255, 255, 255);
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, w, h);
            }
        };
        joinPanel.setLayout(new GridBagLayout());
        joinPanel.setBorder(BorderFactory.createLineBorder(new Color(70, 130, 180), 2));

        ImageIcon infoIcon = new ImageIcon("src/main/resources/images/info_icon.png");
        JLabel iconLabel = new JLabel(infoIcon);

        JLabel ipLabel = new JLabel("Adresse IP:");
        JTextField ipField = new JTextField(20);
        ipField.setPreferredSize(new Dimension(200, 30));

        JButton connectButton = createStyledButton("Se connecter");
        connectButton.setPreferredSize(new Dimension(120, 30));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 20, 5, 20);

        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setOpaque(false);
        headerPanel.add(iconLabel);
        headerPanel.add(new JLabel("Rejoindre une partie en duel"));

        joinPanel.add(headerPanel, gbc);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridBagLayout());
        inputPanel.setOpaque(false);
        GridBagConstraints inputGbc = new GridBagConstraints();
        inputGbc.insets = new Insets(5, 5, 5, 5);
        inputGbc.gridx = 0;
        inputGbc.gridy = 0;
        inputGbc.anchor = GridBagConstraints.EAST;
        inputPanel.add(ipLabel, inputGbc);
        inputGbc.gridx = 1;
        inputGbc.anchor = GridBagConstraints.WEST;
        inputPanel.add(ipField, inputGbc);

        joinPanel.add(inputPanel, gbc);
        gbc.insets = new Insets(10, 20, 10, 20);
        joinPanel.add(connectButton, gbc);

        connectButton.addActionListener(e -> {
            String ip = ipField.getText().trim();
            try {
                Socket socket = new Socket(ip, SERVER_PORT);
                joinDialog.dispose();
                String joueur = (String) comboBox1.getSelectedItem();
                String niveau = getNiveauSelectionne();
                String avion = getAvionSelectionne();
                dispose();
                new EcranDuelMultiplayer(joueur, niveau, avion, socket, false);
            } catch (IOException ex) {
                showErrorDialog("Erreur de connexion: " + ex.getMessage());
            }
        });

        joinDialog.add(joinPanel);
        joinDialog.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JeuDeTir jeu = new JeuDeTir();
            }
        });
    }
}

