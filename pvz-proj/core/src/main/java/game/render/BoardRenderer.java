package game.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import models.engine.board.Position;

public final class BoardRenderer {
    private static final Color FALLBACK_BACKGROUND = new Color(0.20f, 0.32f, 0.16f, 1f);
    private static final Color GRID_COLOR = new Color(1f, 1f, 1f, 0.30f);
    private static final Color HOVER_COLOR = new Color(1f, 0.93f, 0.30f, 0.30f);

    private final BoardGeometry geometry;

    public BoardRenderer(BoardGeometry geometry) {
        this.geometry = geometry;
    }

    public void drawBackground(
        Batch batch,
        TextureRegion left,
        TextureRegion center,
        TextureRegion right,
        float worldHeight,
        float centerX,
        float centerYOffset
    ) {
        if (center == null) {
            return;
        }

        float centerWidth = scaledWidth(center, worldHeight);
        drawRegion(batch, left, centerX - scaledWidth(left, worldHeight), worldHeight);

        // Keep the unshifted center as a backing layer so a small chapter-specific
        // vertical correction never exposes the clear color at the top of the screen.
        // The visible center image itself is still drawn at its native aspect ratio.
        batch.draw(center, centerX, 0f, centerWidth, worldHeight);
        if (Math.abs(centerYOffset) > 0.001f) {
            batch.draw(center, centerX, centerYOffset, centerWidth, worldHeight);
        }

        drawRegion(batch, right, centerX + centerWidth, worldHeight);
    }

    public static float scaledWidth(TextureRegion region, float targetHeight) {
        if (region == null || region.getRegionHeight() <= 0) {
            return 0f;
        }
        return targetHeight * region.getRegionWidth() / region.getRegionHeight();
    }

    private void drawRegion(Batch batch, TextureRegion region, float x, float height) {
        if (region == null) {
            return;
        }
        batch.draw(region, x, 0f, scaledWidth(region, height), height);
    }

    public void drawBoardFill(ShapeRenderer shapes, boolean hasBackground) {
        if (hasBackground) {
            return;
        }

        Rectangle board = geometry.getBoardBounds();
        shapes.set(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(FALLBACK_BACKGROUND);
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
