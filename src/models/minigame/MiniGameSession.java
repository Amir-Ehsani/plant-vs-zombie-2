package models.minigame;

import models.core.plant.Plant;
import models.core.zombie.Zombie;
import models.engine.board.Board;
import models.engine.board.Position;
import models.engine.board.Tile;

import java.util.Collections;
import java.util.Map;

public abstract class MiniGameSession {
    public enum Status {
        RUNNING,
        WON,
        LOST
    }

    private final MiniGameType type;
    private final int stage;
    private int currentTick;
    private Status status;
    private String lastMessage;
    private boolean completionSignal;

    protected MiniGameSession(MiniGameType type, int stage) {
        if (type == null) {
            throw new IllegalArgumentException("Mini-game type cannot be null.");
        }
        if (stage < 1 || stage > 3) {
            throw new IllegalArgumentException("Mini-game stage must be between 1 and 3.");
        }
        this.type = type;
        this.stage = stage;
        this.currentTick = 0;
        this.status = Status.RUNNING;
        this.lastMessage = "";
        this.completionSignal = false;
    }

    public final boolean advanceTicks(int count) {
        if (count <= 0) {
            fail("Tick count must be positive.");
            return false;
        }
        if (!isRunning()) {
            fail("The mini-game is already finished.");
            return false;
        }

        int advanced = 0;
        for (int index = 0; index < count && isRunning(); index++) {
            currentTick++;
            advanced++;
            onTick();
            evaluateStatus();
        }
        if (isRunning()) {
            success("Advanced " + advanced + " ticks. " + compactStatus());
        } else {
            lastMessage = lastMessage + "\nAdvanced " + advanced + " ticks.";
        }
        return true;
    }

    protected abstract void onTick();

    protected abstract void evaluateStatus();

    public abstract String renderMap();

    public abstract String renderStatus();

    public abstract String renderHelp();

    protected String renderBoard(Board board, Map<Position, String> overlays) {
        if (board == null) {
            return "board is not available";
        }
        Map<Position, String> safeOverlays = overlays == null
                ? Collections.emptyMap()
                : overlays;
        StringBuilder builder = new StringBuilder();
        builder.append("    1   2   3   4   5   6   7   8   9\n");
        for (int y = 1; y <= board.getHeight(); y++) {
            builder.append("row ").append(y).append(' ');
            for (int x = 1; x <= board.getWidth(); x++) {
                Position position = new Position(x, y);
                String overlay = safeOverlays.get(position);
                if (overlay != null && !overlay.isBlank()) {
                    builder.append('[').append(padCell(overlay)).append(']');
                    continue;
                }
                Tile tile = board.getTileAt(position);
                builder.append('[').append(padCell(tileSymbol(tile))).append(']');
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    protected String tileSymbol(Tile tile) {
        if (tile == null) {
            return " ";
        }
        if (tile.hasZombies()) {
            return "Z" + Math.min(9, livingZombieCount(tile));
        }
        if (tile.hasPlant()) {
            Plant plant = tile.getCurrentPlant();
            String name = plant == null ? "P" : plant.getName();
            return name.isBlank() ? "P" : "P" + Character.toUpperCase(name.charAt(0));
        }
        return ".";
    }

    protected int livingZombieCount(Tile tile) {
        int count = 0;
        for (Zombie zombie : tile.getZombies()) {
            if (zombie != null && zombie.isAlive()) {
                count++;
            }
        }
        return count;
    }

    private String padCell(String value) {
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.length() >= 3) {
            return cleaned.substring(0, 3);
        }
        return String.format("%-3s", cleaned);
    }

    protected final void markWon(String message) {
        if (!isRunning()) {
            return;
        }
        status = Status.WON;
        completionSignal = true;
        success(message);
    }

    protected final void markLost(String message) {
        if (!isRunning()) {
            return;
        }
        status = Status.LOST;
        fail(message);
    }

    protected final void success(String message) {
        lastMessage = "OK: " + message;
    }

    protected final void fail(String message) {
        lastMessage = "ERROR: " + message;
    }

    public final MiniGameType getType() {
        return type;
    }

    public final int getStage() {
        return stage;
    }

    public final int getCurrentTick() {
        return currentTick;
    }

    public final Status getStatus() {
        return status;
    }

    public final boolean isRunning() {
        return status == Status.RUNNING;
    }

    public final boolean isWon() {
        return status == Status.WON;
    }

    public final boolean isLost() {
        return status == Status.LOST;
    }

    public final String getLastMessage() {
        return lastMessage;
    }

    public final boolean consumeCompletionSignal() {
        boolean value = completionSignal;
        completionSignal = false;
        return value;
    }

    protected final String compactStatus() {
        return type.getDisplayName() + " stage " + stage
                + " | tick=" + currentTick
                + " | status=" + status.name().toLowerCase();
    }
}
