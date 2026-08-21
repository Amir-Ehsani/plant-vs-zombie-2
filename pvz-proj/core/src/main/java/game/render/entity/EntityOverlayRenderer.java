package game.render.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import game.render.BoardGeometry;
import models.core.plant.Plant;
import models.core.zombie.Zombie;
import models.engine.board.Board;

import java.util.List;
import java.util.Locale;

public final class EntityOverlayRenderer {
    private final BoardGeometry geometry;

    public EntityOverlayRenderer(BoardGeometry geometry) {
        this.geometry = geometry;
    }

    public void render(ShapeRenderer shapes, Board board) {
        if (shapes == null || board == null) {
            return;
        }
        shapes.begin(ShapeRenderer.ShapeType.Line);
        renderPlantStates(shapes, board);
        renderZombieStates(shapes, board);
        shapes.end();
    }

    private void renderPlantStates(ShapeRenderer shapes, Board board) {
        for (Plant plant : board.getAllPlants()) {
            Rectangle tile = geometry.getTileBounds(
                Math.max(1, Math.min(BoardGeometry.ROWS, (int) Math.round(plant.getY()))),
                Math.max(1, Math.min(BoardGeometry.COLUMNS, (int) Math.round(plant.getX())))
            );
            if (plant.isFrozenByZombie()) {
                shapes.setColor(new Color(0.45f, 0.75f, 1f, 0.85f));
                shapes.rect(tile.x + 8f, tile.y + 8f, tile.width - 16f, tile.height - 16f);
            }
            if (plant.isCoveredByOctopus()) {
                shapes.setColor(new Color(0.76f, 0.38f, 0.88f, 0.95f));
                shapes.rect(tile.x + 14f, tile.y + 14f, tile.width - 28f, tile.height - 28f);
            }
        }
    }

    private void renderZombieStates(ShapeRenderer shapes, Board board) {
        for (Zombie zombie : board.getAllZombies()) {
            List<String> effects = board.getZombieEffects(zombie);
            Color color = statusColor(effects);
            if (color == null) {
                continue;
            }
            Vector2 position = geometry.entityToScreen(zombie.getX(), zombie.getY());
            shapes.setColor(color);
            float width = geometry.getTileWidth() * 0.46f;
            float height = geometry.getTileHeight() * 0.72f;
            shapes.rect(position.x - width / 2f, position.y - height / 2f, width, height);
        }
    }

    private Color statusColor(List<String> effects) {
        if (hasEffect(effects, "frozen")) {
            return new Color(0.45f, 0.72f, 1f, 0.85f);
        }
        if (hasEffect(effects, "buttered")) {
            return new Color(1f, 0.84f, 0.25f, 0.90f);
        }
        if (hasEffect(effects, "chilled")) {
            return new Color(0.62f, 0.88f, 1f, 0.75f);
        }
        if (hasEffect(effects, "poisoned")) {
            return new Color(0.45f, 1f, 0.45f, 0.75f);
        }
        if (hasEffect(effects, "hypnotized")) {
            return new Color(0.80f, 0.52f, 1f, 0.80f);
        }
        return null;
    }

    private boolean hasEffect(List<String> effects, String prefix) {
        if (effects == null) {
            return false;
        }
        for (String effect : effects) {
            if (effect != null && effect.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
