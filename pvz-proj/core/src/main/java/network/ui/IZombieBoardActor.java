package network.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import network.game.AuthoritativeIZombieGame;
import network.protocol.EntityState;
import network.protocol.GameRole;
import network.protocol.GameSnapshot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Lightweight synchronized lawn renderer shared by network and Couch Play screens.
 *
 * Network snapshots arrive less frequently than render frames, so moving entities are
 * interpolated toward their newest server position. This affects rendering only; hit
 * validation and all gameplay still use the authoritative snapshot on the server.
 */
public final class IZombieBoardActor extends Actor {
    public interface CellListener { void selected(int row, int column); }

    private static final float INTERPOLATION_SPEED = 13f;

    private final Texture pixel;
    private final BitmapFont font = new BitmapFont();
    private final CellListener listener;
    private final Map<String, Float> renderedX = new HashMap<>();
    private final Map<String, Float> targetX = new HashMap<>();
    private GameSnapshot snapshot;
    private GameRole interactionRole = GameRole.PLANTS;
    private boolean interactionEnabled = true;
    private int hoverRow = -1;
    private int hoverColumn = -1;
    private int keyboardLaneRow = -1;

    public IZombieBoardActor(CellListener listener) {
        this.listener = listener;
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        pixel = new Texture(pixmap);
        pixmap.dispose();
        font.getData().setScale(0.75f);
        addListener(new InputListener() {
            @Override
            public boolean mouseMoved(InputEvent event, float x, float y) {
                updateHover(x, y);
                return true;
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (pointer < 0) {
                    hoverRow = -1;
                    hoverColumn = -1;
                }
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                updateHover(x, y);
                if (!interactionEnabled || IZombieBoardActor.this.listener == null) return false;
                if (hoverRow < 0 || hoverColumn < 0) return false;
                if (interactionRole == GameRole.PLANTS
                        && hoverColumn > AuthoritativeIZombieGame.LAST_PLANT_COLUMN) return false;
                IZombieBoardActor.this.listener.selected(hoverRow, hoverColumn);
                return true;
            }
        });
    }

    public void setInteraction(GameRole role, boolean enabled) {
        interactionRole = role == null ? GameRole.PLANTS : role;
        interactionEnabled = enabled;
    }

    /** Optional second-player lane marker used by Couch Play. Pass -1 to hide it. */
    public void setKeyboardLaneRow(int row) {
        keyboardLaneRow = row < 0 ? -1 : Math.max(0, Math.min(AuthoritativeIZombieGame.ROWS - 1, row));
    }

    public void setSnapshot(GameSnapshot snapshot) {
        this.snapshot = snapshot;
        targetX.clear();
        Set<String> visible = new HashSet<>();
        if (snapshot != null) {
            for (EntityState entity : snapshot.getEntities()) {
                if (entity.getId() == null || entity.getId().isBlank()) continue;
                float x = (float) entity.getX();
                targetX.put(entity.getId(), x);
                renderedX.putIfAbsent(entity.getId(), x);
                visible.add(entity.getId());
            }
        }
        renderedX.keySet().removeIf(id -> !visible.contains(id));
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (delta <= 0f || renderedX.isEmpty()) return;
        float alpha = 1f - (float) Math.exp(-INTERPOLATION_SPEED * Math.min(delta, 0.1f));
        for (Map.Entry<String, Float> entry : targetX.entrySet()) {
            String id = entry.getKey();
            float target = entry.getValue();
            float current = renderedX.getOrDefault(id, target);
            renderedX.put(id, current + (target - current) * alpha);
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        float cellWidth = getWidth() / AuthoritativeIZombieGame.COLUMNS;
        float cellHeight = getHeight() / AuthoritativeIZombieGame.ROWS;
        Color previous = new Color(batch.getColor());
        for (int row = 0; row < AuthoritativeIZombieGame.ROWS; row++) {
            float y = getY() + getHeight() - (row + 1) * cellHeight;
            for (int column = 0; column < AuthoritativeIZombieGame.COLUMNS; column++) {
                batch.setColor(((row + column) & 1) == 0
                        ? new Color(0.31f, 0.60f, 0.23f, 1f)
                        : new Color(0.38f, 0.68f, 0.27f, 1f));
                batch.draw(pixel, getX() + column * cellWidth, y, cellWidth - 1f, cellHeight - 1f);
            }
            batch.setColor(0.95f, 0.75f, 0.35f, 1f);
            batch.draw(pixel, getX() + (AuthoritativeIZombieGame.LAST_PLANT_COLUMN + 1) * cellWidth - 2f,
                    y, 4f, cellHeight);
        }

        drawKeyboardLane(batch, cellHeight);
        drawHover(batch, cellWidth, cellHeight);

        if (snapshot != null) {
            boolean[] brains = snapshot.getBrains();
            for (int row = 0; row < Math.min(brains.length, AuthoritativeIZombieGame.ROWS); row++) {
                if (!brains[row]) continue;
                float y = getY() + getHeight() - (row + 0.68f) * cellHeight;
                batch.setColor(0.82f, 0.42f, 0.88f, 1f);
                batch.draw(pixel, getX() + 4f, y, 20f, 20f);
            }
            for (EntityState entity : snapshot.getEntities()) drawEntity(batch, entity, cellWidth, cellHeight);
        }
        batch.setColor(previous);
    }

    private void drawKeyboardLane(Batch batch, float cellHeight) {
        if (keyboardLaneRow < 0) return;
        float y = getY() + getHeight() - (keyboardLaneRow + 1) * cellHeight;
        batch.setColor(0.78f, 0.18f, 0.18f, 0.16f);
        batch.draw(pixel, getX(), y, getWidth(), cellHeight - 1f);
        batch.setColor(1f, 0.35f, 0.22f, 0.95f);
        batch.draw(pixel, getX() + getWidth() - 7f, y + 2f, 5f, Math.max(1f, cellHeight - 5f));
    }

    private void drawHover(Batch batch, float cellWidth, float cellHeight) {
        if (!interactionEnabled || hoverRow < 0 || hoverColumn < 0) return;
        float y = getY() + getHeight() - (hoverRow + 1) * cellHeight;
        if (interactionRole == GameRole.ZOMBIES) {
            batch.setColor(1f, 1f, 1f, 0.16f);
            batch.draw(pixel, getX(), y, getWidth(), cellHeight - 1f);
            return;
        }
        boolean legal = hoverColumn <= AuthoritativeIZombieGame.LAST_PLANT_COLUMN;
        batch.setColor(legal ? new Color(1f, 1f, 1f, 0.22f) : new Color(1f, 0.2f, 0.2f, 0.18f));
        batch.draw(pixel, getX() + hoverColumn * cellWidth, y, cellWidth - 1f, cellHeight - 1f);
    }

    private void drawEntity(Batch batch, EntityState entity, float cellWidth, float cellHeight) {
        float xCells = renderedX.getOrDefault(entity.getId(), (float) entity.getX());
        float centerX = getX() + xCells * cellWidth;
        float centerY = getY() + getHeight() - (entity.getRow() + 0.5f) * cellHeight;
        if ("PROJECTILE".equals(entity.getCategory())) {
            batch.setColor("SNOW".equals(entity.getType()) ? Color.CYAN : Color.LIME);
            batch.draw(pixel, centerX - 5f, centerY - 5f, 10f, 10f);
            return;
        }
        float width = Math.min(54f, cellWidth * 0.68f);
        float height = Math.min(62f, cellHeight * 0.72f);
        if ("PLANT".equals(entity.getCategory())) {
            batch.setColor(plantColor(entity.getType()));
        } else if ("true".equalsIgnoreCase(entity.getAttribute("slowed"))) {
            batch.setColor(0.28f, 0.74f, 0.88f, 1f);
        } else {
            batch.setColor(zombieColor(entity.getType()));
        }
        batch.draw(pixel, centerX - width / 2f, centerY - height / 2f, width, height);

        float ratio = entity.getMaxHealth() <= 0 ? 0f
                : Math.max(0f, Math.min(1f, entity.getHealth() / (float) entity.getMaxHealth()));
        batch.setColor(Color.DARK_GRAY);
        batch.draw(pixel, centerX - width / 2f, centerY + height / 2f + 3f, width, 5f);
        batch.setColor(ratio > 0.5f ? Color.LIME : ratio > 0.25f ? Color.YELLOW : Color.RED);
        batch.draw(pixel, centerX - width / 2f, centerY + height / 2f + 3f, width * ratio, 5f);
        batch.setColor(Color.WHITE);
        font.draw(batch, abbreviation(entity.getType()), centerX - width / 2f + 3f, centerY + 4f);
    }

    private void updateHover(float x, float y) {
        if (getWidth() <= 0f || getHeight() <= 0f) return;
        hoverColumn = Math.max(0, Math.min(AuthoritativeIZombieGame.COLUMNS - 1,
                (int) (x / (getWidth() / AuthoritativeIZombieGame.COLUMNS))));
        hoverRow = Math.max(0, Math.min(AuthoritativeIZombieGame.ROWS - 1,
                (int) ((getHeight() - y) / (getHeight() / AuthoritativeIZombieGame.ROWS))));
    }

    private static Color plantColor(String type) {
        return switch (type) {
            case "SUNFLOWER" -> new Color(1f, 0.75f, 0.12f, 1f);
            case "WALL_NUT" -> new Color(0.58f, 0.31f, 0.13f, 1f);
            case "SNOW_PEA" -> new Color(0.22f, 0.75f, 0.92f, 1f);
            default -> new Color(0.12f, 0.75f, 0.23f, 1f);
        };
    }

    private static Color zombieColor(String type) {
        return switch (type) {
            case "CONEHEAD" -> new Color(0.93f, 0.42f, 0.10f, 1f);
            case "BUCKETHEAD" -> new Color(0.62f, 0.67f, 0.70f, 1f);
            case "FOOTBALL" -> new Color(0.72f, 0.10f, 0.12f, 1f);
            case "IMP" -> new Color(0.72f, 0.62f, 0.48f, 1f);
            default -> new Color(0.38f, 0.44f, 0.36f, 1f);
        };
    }

    private static String abbreviation(String type) {
        String normalized = type == null ? "?" : type.toUpperCase(Locale.ROOT);
        if (normalized.length() <= 5) return normalized;
        StringBuilder result = new StringBuilder();
        for (String part : normalized.split("_")) if (!part.isEmpty()) result.append(part.charAt(0));
        return result.length() > 1 ? result.toString() : normalized.substring(0, 5);
    }

    public void dispose() {
        pixel.dispose();
        font.dispose();
    }
}
