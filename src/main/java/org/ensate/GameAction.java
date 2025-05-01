package org.ensate;
import java.io.Serializable;

public class GameAction implements Serializable {
    public String action; // "move" ou "shoot"
    public int x, y;
    public String actionData;

    public GameAction(String action, int x, int y) {
        this.action = action;
        this.x = x;
        this.y = y;
    }

    public GameAction(String action, int x, int y, String actionData) {
        this.action = action;
        this.x = x;
        this.y = y;
        this.actionData = actionData;
    }
} 