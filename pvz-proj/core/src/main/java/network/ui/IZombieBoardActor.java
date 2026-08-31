package network.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import network.game.AuthoritativeIZombieGame;
import network.protocol.EntityState;
import network.protocol.GameRole;
import network.protocol.GameSnapshot;
import ui.PamAnimationActor;
import ui.PvzAnimationService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * PvZ-styled synchronized lawn used by online and couch I, Zombie.
 *
 * The server remains authoritative. This actor only turns snapshots into the
 * Big Brainz lawn, real PAM plant/zombie actors, brains and projectiles and
 * interpolates moving entities between snapshots.
 */
public final class IZombieBoardActor extends Group {
    public interface CellListener { void selected(int row, int column); }

    private static final float INTERPOLATION_SPEED = 13f;
    private static final Color FALLBACK_LAWN = new Color(0.08f, 0.20f, 0.23f, 1f);
    private static final Color RED_LINE = new Color(0.92f, 0.08f, 0.10f, 0.90f);

    private final Texture pixel;
    private final CellListener listener;
    private final PvzAnimationService animations;
    private final TextureRegion background;
    private final TextureRegion brainImage;
    private final TextureRegion peaImage;
    private final Map<String, Float> renderedX = new HashMap<>();
    private final Map<String, Float> targetX = new HashMap<>();
    private final Map<String, EntityState> entitiesById = new HashMap<>();
    private final Map<String, PamAnimationActor> entityActors = new HashMap<>();
    private GameSnapshot snapshot;
    private GameRole interactionRole = GameRole.PLANTS;
    private boolean interactionEnabled = true;
    private int hoverRow = -1;
    private int hoverColumn = -1;
    private int keyboardLaneRow = -1;

    public IZombieBoardActor(CellListener listener) {
        this(null, listener);
    }

    public IZombieBoardActor(PvzAnimationService animations, CellListener listener) {
        this.listener = listener;
        this.animations = animations;
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        pixel = new Texture(pixmap);
        pixmap.dispose();
        background = region("IMAGE_BACKGROUNDS_BACKGROUND_LOD_BIGBRAINZ_TEXTURE");
        brainImage = region("IMAGE_UI_CALENDAR_TIMER_DECO_BIGBRAINZ");
        peaImage = region("IMAGE_EFFECTS_T_PEA_PROJECTILE_T_PEA_PROJECTILE_39X36");
        setTransform(false);
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
        entitiesById.clear();
        Set<String> visibleActors = new HashSet<>();
        Set<String> visiblePositions = new HashSet<>();
        if (snapshot != null) {
            for (EntityState entity : snapshot.getEntities()) {
                if (entity.getId() == null || entity.getId().isBlank()) continue;
                entitiesById.put(entity.getId(), entity);
                float x = (float) entity.getX();
                targetX.put(entity.getId(), x);
                renderedX.putIfAbsent(entity.getId(), x);
                visiblePositions.add(entity.getId());
                if (!"PROJECTILE".equals(entity.getCategory())) {
                    visibleActors.add(entity.getId());
                    ensureEntityActor(entity);
                }
            }
        }
        renderedX.keySet().removeIf(id -> !visiblePositions.contains(id));
        targetX.keySet().removeIf(id -> !visiblePositions.contains(id));
        for (String id : new HashSet<>(entityActors.keySet())) {
            if (visibleActors.contains(id)) continue;
            PamAnimationActor actor = entityActors.remove(id);
            if (actor != null) actor.remove();
        }
        layoutEntityActors();
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (delta > 0f && !renderedX.isEmpty()) {
            float alpha = 1f - (float) Math.exp(-INTERPOLATION_SPEED * Math.min(delta, 0.1f));
            for (Map.Entry<String, Float> entry : targetX.entrySet()) {
                String id = entry.getKey();
                float target = entry.getValue();
                float current = renderedX.getOrDefault(id, target);
                renderedX.put(id, current + (target - current) * alpha);
            }
        }
        layoutEntityActors();
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        Color previous = new Color(batch.getColor());
        drawLawn(batch, parentAlpha);
        drawBrainz(batch, parentAlpha);
        drawProjectiles(batch, parentAlpha);
        drawRedLine(batch, parentAlpha);
        drawKeyboardLane(batch, parentAlpha);
        drawHover(batch, parentAlpha);
        batch.setColor(previous);
        super.draw(batch, parentAlpha);
    }

    private void drawLawn(Batch batch, float parentAlpha) {
        batch.setColor(1f, 1f, 1f, parentAlpha);
        if (background != null) {
            batch.draw(background, 0f, 0f, 1280f, 720f);
        } else {
            batch.setColor(FALLBACK_LAWN.r, FALLBACK_LAWN.g, FALLBACK_LAWN.b, parentAlpha);
            batch.draw(pixel, 0f, 0f, 1280f, 720f);
        }
    }

    private void drawBrainz(Batch batch, float parentAlpha) {
        if (snapshot == null) return;
        float cellHeight = getHeight() / AuthoritativeIZombieGame.ROWS;
        boolean[] brains = snapshot.getBrains();
        for (int row = 0; row < Math.min(brains.length, AuthoritativeIZombieGame.ROWS); row++) {
            if (!brains[row]) continue;
            float centerY = getY() + getHeight() - (row + 0.5f) * cellHeight;
            if (brainImage != null) {
                float height = Math.min(58f, cellHeight * 0.62f);
                float width = height * brainImage.getRegionWidth() / (float) brainImage.getRegionHeight();
                batch.setColor(1f, 1f, 1f, parentAlpha);
                batch.draw(brainImage, getX() - width * 0.88f, centerY - height / 2f, width, height);
            } else {
                batch.setColor(0.94f, 0.35f, 0.66f, parentAlpha);
                batch.draw(pixel, getX() + 10f, centerY - 13f, 30f, 26f);
            }
        }
    }

    private void drawProjectiles(Batch batch, float parentAlpha) {
        if (snapshot == null) return;
        float cellWidth = getWidth() / AuthoritativeIZombieGame.COLUMNS;
        float cellHeight = getHeight() / AuthoritativeIZombieGame.ROWS;
        for (EntityState entity : snapshot.getEntities()) {
            if (!"PROJECTILE".equals(entity.getCategory())) continue;
            float xCells = renderedX.getOrDefault(entity.getId(), (float) entity.getX());
            float centerX = getX() + xCells * cellWidth;
            float centerY = getY() + getHeight() - (entity.getRow() + 0.5f) * cellHeight;
            if (peaImage != null) {
                float size = Math.min(30f, cellHeight * 0.28f);
                batch.setColor("SNOW".equals(entity.getType()) ? 0.68f : 1f,
                        "SNOW".equals(entity.getType()) ? 0.90f : 1f,
                        1f, parentAlpha);
                batch.draw(peaImage, centerX - size / 2f, centerY - size / 2f, size, size);
            } else {
                batch.setColor("SNOW".equals(entity.getType()) ? Color.CYAN : Color.LIME);
                batch.draw(pixel, centerX - 5f, centerY - 5f, 10f, 10f);
            }
        }
    }

    private void drawRedLine(Batch batch, float parentAlpha) {
        float cellWidth = getWidth() / AuthoritativeIZombieGame.COLUMNS;
        float x = getX() + (AuthoritativeIZombieGame.LAST_PLANT_COLUMN + 1) * cellWidth - 2f;
        batch.setColor(RED_LINE.r, RED_LINE.g, RED_LINE.b, RED_LINE.a * parentAlpha);
        batch.draw(pixel, x, getY() + 4f, 4f, Math.max(1f, getHeight() - 8f));
    }

    private void drawKeyboardLane(Batch batch, float parentAlpha) {
        if (keyboardLaneRow < 0) return;
        float cellHeight = getHeight() / AuthoritativeIZombieGame.ROWS;
        float y = getY() + getHeight() - (keyboardLaneRow + 1) * cellHeight;
        batch.setColor(1f, 0.86f, 0.30f, 0.14f * parentAlpha);
        batch.draw(pixel, getX(), y, getWidth(), cellHeight);
    }

    private void drawHover(Batch batch, float parentAlpha) {
        if (!interactionEnabled || hoverRow < 0 || hoverColumn < 0) return;
        float cellWidth = getWidth() / AuthoritativeIZombieGame.COLUMNS;
        float cellHeight = getHeight() / AuthoritativeIZombieGame.ROWS;
        float y = getY() + getHeight() - (hoverRow + 1) * cellHeight;
        if (interactionRole == GameRole.ZOMBIES) {
            batch.setColor(1f, 1f, 1f, 0.13f * parentAlpha);
            batch.draw(pixel, getX(), y, getWidth(), cellHeight);
            return;
        }
        boolean legal = hoverColumn <= AuthoritativeIZombieGame.LAST_PLANT_COLUMN;
        batch.setColor(legal ? new Color(1f, 0.94f, 0.36f, 0.16f * parentAlpha)
                : new Color(1f, 0.18f, 0.18f, 0.12f * parentAlpha));
        batch.draw(pixel, getX() + hoverColumn * cellWidth, y, cellWidth, cellHeight);
    }

    private void ensureEntityActor(EntityState entity) {
        if (animations == null || entityActors.containsKey(entity.getId())) return;
        PamAnimationActor actor;
        if ("PLANT".equals(entity.getCategory())) {
            actor = animations.createPlantActor(plantDisplayName(entity.getType()));
        } else if ("ZOMBIE".equals(entity.getCategory())) {
            actor = animations.createZombieActor(zombieDisplayName(entity.getType()));
        } else {
            return;
        }
        actor.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.disabled);
        entityActors.put(entity.getId(), actor);
        addActor(actor);
    }

    private void layoutEntityActors() {
        if (getWidth() <= 0f || getHeight() <= 0f) return;
        float cellWidth = getWidth() / AuthoritativeIZombieGame.COLUMNS;
        float cellHeight = getHeight() / AuthoritativeIZombieGame.ROWS;
        for (Map.Entry<String, PamAnimationActor> entry : entityActors.entrySet()) {
            EntityState entity = entitiesById.get(entry.getKey());
            if (entity == null) continue;
            float xCells = renderedX.getOrDefault(entry.getKey(), (float) entity.getX());
            float centerX = xCells * cellWidth;
            float centerY = getHeight() - (entity.getRow() + 0.5f) * cellHeight;
            boolean zombie = "ZOMBIE".equals(entity.getCategory());
            float height = zombie ? cellHeight * 1.18f : cellHeight * 0.92f;
            if ("IMP".equals(entity.getType())) height *= 0.78f;
            if ("ALLSTAR".equals(entity.getType()) || "GARGANTUAR".equals(entity.getType())) height *= 1.10f;
            float width = zombie ? cellWidth * 0.92f : cellWidth * 0.78f;
            PamAnimationActor actor = entry.getValue();
            actor.setSize(width, height);
            actor.setPosition(centerX - width / 2f, centerY - height * (zombie ? 0.47f : 0.45f));
        }
    }

    private TextureRegion region(String id) {
        return animations == null ? null : animations.region(id);
    }

    private void updateHover(float x, float y) {
        if (getWidth() <= 0f || getHeight() <= 0f) return;
        hoverColumn = Math.max(0, Math.min(AuthoritativeIZombieGame.COLUMNS - 1,
                (int) (x / (getWidth() / AuthoritativeIZombieGame.COLUMNS))));
        hoverRow = Math.max(0, Math.min(AuthoritativeIZombieGame.ROWS - 1,
                (int) ((getHeight() - y) / (getHeight() / AuthoritativeIZombieGame.ROWS))));
    }

    public static String plantDisplayName(String type) {
        if (type == null) return "Peashooter";
        return switch (type) {
            case "SUNFLOWER" -> "Sunflower";
            case "WALL_NUT" -> "Wall-nut";
            case "SNOW_PEA" -> "Snow Pea";
            case "REPEATER" -> "Repeater";
            case "TALL_NUT" -> "Tall-nut";
            case "THREEPEATER" -> "Threepeater";
            default -> "Peashooter";
        };
    }

    public static String zombieDisplayName(String type) {
        if (type == null) return "Default";
        return switch (type) {
            case "CONE_HEAD" -> "cone head";
            case "BUCKET_HEAD" -> "bucket head";
            case "RA" -> "Ra";
            case "EXPLORER" -> "Explorer";
            case "ALLSTAR" -> "Allstar";
            case "WIZARD" -> "Wizard";
            case "PROSPECTOR" -> "Prospector";
            case "GARGANTUAR" -> "Gargantuar";
            case "IMP" -> "Imp";
            default -> "Default";
        };
    }

    public void dispose() {
        for (PamAnimationActor actor : entityActors.values()) actor.remove();
        entityActors.clear();
        pixel.dispose();
    }
}
