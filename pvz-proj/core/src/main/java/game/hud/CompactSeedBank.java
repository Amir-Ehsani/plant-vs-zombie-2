package game.hud;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import game.animation.core.EntityAnimationProfile;
import game.animation.core.EntityAnimationRegistry;
import game.animation.core.PvzAnimationService;
import models.core.plant.PlantType;
import models.engine.session.GameSession;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CompactSeedBank {
    private static final float BANK_X = 14f;
    private static final float BANK_TOP = 704f;
    private static final float SLOT_WIDTH = 136f;
    private static final float SLOT_HEIGHT = 76f;
    private static final float SLOT_GAP = 3f;
    private static final float SLOT_INSET = 2f;
    private static final float PLANT_X_OFFSET = SLOT_WIDTH * 0.50f;
    private static final float PLANT_Y_OFFSET = SLOT_HEIGHT * 0.52f;
    private static final float COMPACT_SCALE_MULTIPLIER = 0.82f;
    private static final int MAX_VISIBLE_SLOTS = 8;

    private static final Color SLOT_COLOR = new Color(0.88f, 0.84f, 0.68f, 1f);
    private static final Color READY_BORDER = new Color(0.18f, 0.34f, 0.15f, 1f);
    private static final Color COOLDOWN_BORDER = new Color(0.38f, 0.38f, 0.34f, 1f);
    private static final Color SELECTED_BORDER = new Color(0.95f, 0.68f, 0.12f, 1f);
    private static final Color COOLDOWN_SHADE = new Color(0.03f, 0.04f, 0.05f, 0.68f);
    private static final Color TEXT_COLOR = new Color(0.20f, 0.16f, 0.08f, 1f);

    private final PvzAnimationService animations;
    private final EntityAnimationRegistry registry;
    private final BitmapFont font;
    private final Map<String, EntityAnimationProfile> profiles = new LinkedHashMap<>();

    public CompactSeedBank(PvzAnimationService animations, Skin skin) {
        if (animations == null || animations.getCatalog() == null || skin == null) {
            throw new IllegalArgumentException("Seed bank requires animations and skin.");
        }
        this.animations = animations;
        this.registry = new EntityAnimationRegistry(animations.getCatalog());
        this.font = skin.get("secondary", Label.LabelStyle.class).font;
    }

    public void render(
        ShapeRenderer shapes,
        Batch batch,
        GameSession session,
        float stateTime,
        String selectedPlantName
    ) {
        if (shapes == null || batch == null || session == null) {
            return;
        }
        List<String> plants = visiblePlants(session);
        if (plants.isEmpty()) {
            return;
        }
        drawSlots(shapes, session, plants, selectedPlantName);
        drawPlants(batch, session, plants, stateTime);
        drawCooldownShade(shapes, session, plants);
        drawCosts(batch, session, plants);
    }

    public String findPlantAt(GameSession session, float x, float y) {
        if (session == null || x < BANK_X || x > BANK_X + SLOT_WIDTH) {
            return null;
        }
        List<String> plants = visiblePlants(session);
        for (int index = 0; index < plants.size(); index++) {
            float slotY = slotY(index);
            if (y >= slotY && y <= slotY + SLOT_HEIGHT) {
                return plants.get(index);
            }
        }
        return null;
    }

    private void drawSlots(
        ShapeRenderer shapes,
        GameSession session,
        List<String> plants,
        String selectedPlantName
    ) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int index = 0; index < plants.size(); index++) {
            float y = slotY(index);
            String name = plants.get(index);
            boolean ready = session.getPlantRechargeRemainingTicks(name) <= 0;
            boolean selected = isSelected(name, selectedPlantName);
            shapes.setColor(selected ? SELECTED_BORDER : ready ? READY_BORDER : COOLDOWN_BORDER);
            shapes.rect(BANK_X, y, SLOT_WIDTH, SLOT_HEIGHT);
            shapes.setColor(SLOT_COLOR);
            shapes.rect(
                BANK_X + SLOT_INSET,
                y + SLOT_INSET,
                SLOT_WIDTH - SLOT_INSET * 2f,
                SLOT_HEIGHT - SLOT_INSET * 2f
            );
        }
        shapes.end();
    }

    private void drawPlants(
        Batch batch,
        GameSession session,
        List<String> plants,
        float stateTime
    ) {
        batch.begin();
        for (int index = 0; index < plants.size(); index++) {
            drawPlant(batch, session, plants.get(index), slotY(index), stateTime);
        }
        batch.end();
    }

    private void drawCooldownShade(
        ShapeRenderer shapes,
        GameSession session,
        List<String> plants
    ) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(COOLDOWN_SHADE);
        for (int index = 0; index < plants.size(); index++) {
            String plantName = plants.get(index);
            float remainingRatio = cooldownRemainingRatio(session, plantName);
            if (remainingRatio <= 0f) {
                continue;
            }
            float innerHeight = SLOT_HEIGHT - SLOT_INSET * 2f;
            float darkHeight = innerHeight * remainingRatio;
            float brightHeight = innerHeight - darkHeight;
            shapes.rect(
                BANK_X + SLOT_INSET,
                slotY(index) + SLOT_INSET + brightHeight,
                SLOT_WIDTH - SLOT_INSET * 2f,
                darkHeight
            );
        }
        shapes.end();
    }

    private float cooldownRemainingRatio(GameSession session, String plantName) {
        int remaining = session.getPlantRechargeRemainingTicks(plantName);
        if (remaining <= 0) {
            return 0f;
        }
        PlantType type = session.getPlantType(plantName);
        int total = type == null ? remaining : Math.max(1, type.getRecharge());
        total = Math.max(total, remaining);
        return MathUtils.clamp(remaining / (float) total, 0f, 1f);
    }

    private void drawCosts(Batch batch, GameSession session, List<String> plants) {
        Color previousFontColor = new Color(font.getColor());
        batch.begin();
        font.setColor(TEXT_COLOR);
        for (int index = 0; index < plants.size(); index++) {
            int cost = session.getPlantCost(plants.get(index));
            font.draw(batch, String.valueOf(cost), BANK_X + 100f, slotY(index) + 21f);
        }
        font.setColor(previousFontColor);
        batch.end();
    }

    private void drawPlant(
        Batch batch,
        GameSession session,
        String plantName,
        float y,
        float stateTime
    ) {
        EntityAnimationProfile profile = profileFor(session, plantName);
        if (profile == null) {
            return;
        }
        String clip = profile.firstClip("idle", "play", "walk");
        animations.draw(
            batch,
            profile.getPath(),
            clip,
            stateTime,
            BANK_X + PLANT_X_OFFSET,
            y + PLANT_Y_OFFSET,
            profile.getScale() * COMPACT_SCALE_MULTIPLIER,
            true
        );
    }

    private EntityAnimationProfile profileFor(GameSession session, String plantName) {
        EntityAnimationProfile cached = profiles.get(plantName);
        if (cached != null) {
            return cached;
        }
        PlantType type = session.getPlantType(plantName);
        EntityAnimationProfile profile = registry.forPlantType(type);
        if (profile != null) {
            animations.preload(profile.getPath());
            profiles.put(plantName, profile);
        }
        return profile;
    }

    private List<String> visiblePlants(GameSession session) {
        List<String> result = new ArrayList<>();
        for (String plantName : session.getSelectedPlantNames()) {
            if (result.size() >= MAX_VISIBLE_SLOTS) {
                break;
            }
            result.add(plantName);
        }
        return result;
    }

    private boolean isSelected(String plantName, String selectedPlantName) {
        return plantName != null
            && selectedPlantName != null
            && plantName.equalsIgnoreCase(selectedPlantName);
    }

    private float slotY(int index) {
        return BANK_TOP - SLOT_HEIGHT - index * (SLOT_HEIGHT + SLOT_GAP);
    }
}
