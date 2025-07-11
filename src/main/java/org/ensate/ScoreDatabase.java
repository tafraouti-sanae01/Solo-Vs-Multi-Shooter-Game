package org.ensate;

import java.sql.*;

public class ScoreDatabase {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/jeu_tir";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "";

    public ScoreDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void saveOrUpdateScore(String joueur, int score) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String select = "SELECT score FROM scores WHERE joueur = ?";
            PreparedStatement ps = conn.prepareStatement(select);
            ps.setString(1, joueur);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int oldScore = rs.getInt("score");
                if (score > oldScore) {
                    String update = "UPDATE scores SET score = ? WHERE joueur = ?";
                    PreparedStatement ups = conn.prepareStatement(update);
                    ups.setInt(1, score);
                    ups.setString(2, joueur);
                    ups.executeUpdate();
                }
            } else {
                String insert = "INSERT INTO scores (joueur, score) VALUES (?, ?)";
                PreparedStatement ins = conn.prepareStatement(insert);
                ins.setString(1, joueur);
                ins.setInt(2, score);
                ins.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getScore(String joueur) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            String select = "SELECT score FROM scores WHERE joueur = ?";
            PreparedStatement ps = conn.prepareStatement(select);
            ps.setString(1, joueur);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("score");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static void saveSession(String joueur1, String joueur2, int score1, int score2) {
        int scoreMax = Math.max(score1, score2);
        String sql = "INSERT INTO sessions (joueur1, joueur2, score_max) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, joueur1);
            stmt.setString(2, joueur2);
            stmt.setInt(3, scoreMax);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
} 