package org.ensate;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Projectile {
    private JLabel label;
    private int x;
    private int y;
    private int speed;
    private boolean active;
    private Thread movementThread;

    public Projectile(int x, int y, BufferedImage image, int speed) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.active = true;

        this.label = new JLabel(new ImageIcon(image.getScaledInstance(10, 20, Image.SCALE_SMOOTH)));
        this.label.setBounds(x, y, 10, 20);

        startMovement();
    }

    private void startMovement() {
        movementThread = new Thread(() -> {
            while (active && y > 0) {
                y -= speed;
                SwingUtilities.invokeLater(() -> {
                    label.setLocation(x, y);
                });
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    break;
                }
            }
            active = false;
        });
        movementThread.start();
    }

    public JLabel getLabel() {
        return label;
    }
    public boolean isActive() {
        return active;
    }

    public void deactivate() {
        active = false;
        if (movementThread != null) {
            movementThread.interrupt();
        }
    }

    public Rectangle getBounds() {
        return label.getBounds();
    }
} 