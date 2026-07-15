package controllers.core;


import models.engine.session.GameSession;
import models.engine.board.Position;

public class GameController {
    private GameSession gameSession;


    public void setGameSession(GameSession gameSession) {
        this.gameSession = gameSession;
    }

    public void handleUserClick(Position pos) {
    }

    public void handlePause() {
    }

    public void update() {
    }
}
