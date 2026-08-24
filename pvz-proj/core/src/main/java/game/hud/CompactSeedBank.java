package game.hud;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
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
    private static final float PLANT_X_OFFSET = 42f;
    private static final float PLANT_Y_OFFSET = 39f;
    private static final float COMPACT_SCALE_MULTIPLIER = 0.70f;
    private static final int MAX_VISIBLE_SLOTS = 8;

    private static final Color SLOT_COLOR = new Color(0.88f, 0.84f, 0.68f, 1f);
    private static final Color READY_BORDER = new Color(0.18f, 0.34f, 0.15f, 1f);
    private static final Color COOLDOWN_BORDER = new Color(0.38f, 0.38f, 0.34f, 1f);
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

    public void render(ShapeRenderer shapes, Batch batch, GameSession session, float stateTime) {
        if (shapes == null || batch == null || session == null) {
            return;
        }
        List<String> plants = visiblePlants(session);
        if (plants.isEmpty()) {
            return;
        }
        drawSlots(shapes, session, plants);
        drawSlotContents(batch, session, plants, stateTime);
    }

    private void drawSlots(ShapeRenderer shapes, GameSession session, List<String> plants) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int index = 0; index < plants.size(); index++) {
            float y = slotY(index);
            String name = plants.get(index);
            boolean ready = session.getPlantRechargeRemainingTicks(name) <= 0;
            shapes.setColor(ready ? READY_BORDER : COOLDOWN_BORDER);
            shapes.rect(BANK_X, y, SLOT_WIDTH, SLOT_HEIGHT);
            shapes.setColor(SLOT_COLOR);
            shapes.rect(BANK_X + 2f, y + 2f, SLOT_WIDTH - 4f, SLOT_HEIGHT - 4f);
        }
        shapes.end();
    }

    private void drawSlotContents(
        Batch batch,
        GameSession session,
        List<String> plants,
        float stateTime
    ) {
        Color previousFontColor = new Color(font.getColor());
        batch.begin();
        font.setColor(TEXT_COLOR);
        for (int index = 0; index < plants.size(); index++) {
            String plantName = plants.get(index);
            float y = slotY(index);
            drawPlant(batch, session, plantName, y, stateTime);
            drawCost(batch, session, plantName, y);
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

    private void drawCost(Batch batch, GameSession session, String plantName, float y) {
        int cost = session.getPlantCost(plantName);
        int cooldown = session.getPlantRechargeRemainingTicks(plantName);
        font.draw(batch, String.valueOf(cost), BANK_X + 94f, y + 29f);
        if (cooldown > 0) {
            font.draw(
                batch,
                Math.max(1, (int) Math.ceil(cooldown / 10f)) + "s",
                BANK_X + 94f,
                y + 54f
            );
        }
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

    private float slotY(int index) {
        return BANK_TOP - SLOT_HEIGHT - index * (SLOT_HEIGHT + SLOT_GAP);
    }
}
