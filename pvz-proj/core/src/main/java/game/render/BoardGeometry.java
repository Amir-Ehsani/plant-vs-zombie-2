package game.render;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import models.engine.board.Position;

public final class BoardGeometry {
    public static final int ROWS = 5;
    public static final int COLUMNS = 9;

    private final Rectangle boardBounds;
    private float tileWidth;
    private float tileHeight;

    public BoardGeometry(float x, float y, float width, float height) {
        boardBounds = new Rectangle();
        setBounds(x, y, width, height);
    }

    public void setBounds(float x, float y, float width, float height) {
        if (width <= 0f || height <= 0f) {
            throw new IllegalArgumentException("Board dimensions must be positive.");
        }
        boardBounds.set(x, y, width, height);
        tileWidth = width / COLUMNS;
        tileHeight = height / ROWS;
    }

    public Vector2 boardToScreen(int row, int column) {
        validateCell(row, column);
        Rectangle tile = getTileBounds(row, column);
        return new Vector2(tile.x + tile.width / 2f, tile.y + tile.height / 2f);
    }


    public Vector2 entityToScreen(double x, double y) {
        float screenX = boardBounds.x + ((float) x - 0.5f) * tileWidth;
        float screenY = boardBounds.y + (ROWS - (float) y + 0.5f) * tileHeight;
        return new Vector2(screenX, screenY);
    }

    public Position screenToBoard(float x, float y) {
        if (!boardBounds.contains(x, y)) {
            return null;
        }

        int column = (int) ((x - boardBounds.x) / tileWidth) + 1;
        int visualRowFromBottom = (int) ((y - boardBounds.y) / tileHeight);
        int row = ROWS - visualRowFromBottom;

        column = Math.max(1, Math.min(COLUMNS, column));
        row = Math.max(1, Math.min(ROWS, row));
        return new Position(column, row);
    }

    public Rectangle getTileBounds(int row, int column) {
        validateCell(row, column);
        float x = boardBounds.x + (column - 1) * tileWidth;
        float y = boardBounds.y + (ROWS - row) * tileHeight;
        return new Rectangle(x, y, tileWidth, tileHeight);
    }

    public Rectangle getBoardBounds() {
        return new Rectangle(boardBounds);
    }

    public float getTileWidth() {
        return tileWidth;
    }

    public float getTileHeight() {
        return tileHeight;
    }

    private void validateCell(int row, int column) {
        if (row < 1 || row > ROWS || column < 1 || column > COLUMNS) {
            throw new IllegalArgumentException("Board cell is outside the 5x9 board.");
        }
    }
}
