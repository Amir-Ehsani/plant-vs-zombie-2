package game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import models.engine.board.Position;


public final class BoardRenderer {
    private static final Color FALLBACK_BACKGROUND = new Color(0.20f, 0.32f, 0.16f, 1f);
    private static final Color BOARD_FILL = new Color(0.36f, 0.53f, 0.24f, 0.45f);
    private static final Color GRID_COLOR = new Color(1f, 1f, 1f, 0.34f);
    private static final Color HOVER_COLOR = new Color(1f, 0.93f, 0.30f, 0.35f);

    private final BoardGeometry geometry;

    public BoardRenderer(BoardGeometry geometry) {
        this.geometry = geometry;
    }

    public void drawBackground(Batch batch, TextureRegion background, float worldWidth, float worldHeight) {
        if (background != null) {
            batch.draw(background, 0f, 0f, worldWidth, worldHeight);
        }
    }

    public void drawBoardFill(ShapeRenderer shapes, boolean hasBackground) {
        Rectangle board = geometry.getBoardBounds();
        shapes.set(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(hasBackground ? BOARD_FILL : FALLBACK_BACKGROUND);
        shapes.rect(board.x, board.y, board.width, board.height);
    }

    public void drawGrid(ShapeRenderer shapes) {
        shapes.set(ShapeRenderer.ShapeType.Line);
        shapes.setColor(GRID_COLOR);
        for (int row = 1; row <= BoardGeometry.ROWS; row++) {
            for (int column = 1; column <= BoardGeometry.COLUMNS; column++) {
                Rectangle tile = geometry.getTileBounds(row, column);
                shapes.rect(tile.x, tile.y, tile.width, tile.height);
            }
        }
    }

    public void drawHover(ShapeRenderer shapes, Position hoveredTile) {
        if (hoveredTile == null) {
            return;
        }
        Rectangle tile = geometry.getTileBounds(hoveredTile.getY(), hoveredTile.getX());
        shapes.set(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(HOVER_COLOR);
        shapes.rect(tile.x, tile.y, tile.width, tile.height);
    }
}
