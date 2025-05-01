package org.ensate;

public class GameLevel {
    private final int levelNumber;
    private final int scoreThreshold;
    private final int boatSpeed;
    private final int boatCount;
    private final String backgroundImage;

    public GameLevel(int levelNumber, int scoreThreshold, int boatSpeed, int boatCount, String backgroundImage) {
        this.levelNumber = levelNumber;
        this.scoreThreshold = scoreThreshold;
        this.boatSpeed = boatSpeed;
        this.boatCount = boatCount;
        this.backgroundImage = backgroundImage;
    }

    public int getLevelNumber() {
        return levelNumber;
    }

    public int getScoreThreshold() {
        return scoreThreshold;
    }

    public int getBoatSpeed() {
        return boatSpeed;
    }

    public int getBoatCount() {
        return boatCount;
    }

    public String getBackgroundImage() {
        return backgroundImage;
    }
} 