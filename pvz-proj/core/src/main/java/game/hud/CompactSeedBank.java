package game.hud;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import game.animation.core.EntityAnimationProfile;
import game.animation.core.EntityAnimationRegistry;
import game.animation.core.PvzAnimationService;
import models.core.plant.PlantType;
import models.engine.session.GameSession;
import models.level.core.Level;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

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
    private static final float CONVEYOR_ENTRY_Y = -SLOT_HEIGHT - 24f;
    private static final float CONVEYOR_ANIMATION_SPEED = 8f;

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
    private final Map<String, Float> conveyorCardY = new LinkedHashMap<>();
    private final TextureRegion conveyorBelt;
    private final TextureRegion conveyorTop;
    private final TextureRegion conveyorSide;

    public CompactSeedBank(PvzAnimationService animations, Skin skin) {
        if (animations == null || animations.getCatalog() == null || skin == null) {
            throw new IllegalArgumentException("Seed bank requires animations and skin.");
        }
        this.animations = animations;
        this.registry = new EntityAnimationRegistry(animations.getCatalog());
        this.font = skin.get("secondary", Label.LabelStyle.class).font;
        this.conveyorBelt = animations.region("IMAGE_UI_CONVEYOR_CONVEYOR_BELT");
        this.conveyorTop = animations.region("IMAGE_UI_CONVEYOR_CONVEYOR_TOP");
        this.conveyorSide = animations.region("IMAGE_UI_CONVEYOR_CONVEYOR_SIDE");
    }

    public void render(
        ShapeRenderer shapes,
        Batch batch,
        GameSession session,
        float stateTime,
        String selectedPlantName,
        Predicate<String> boostedPlant
    ) {
        if (shapes == null || batch == null || session == null) {
            return;
        }
        List<VisiblePlant> plants = visiblePlants(session);
        if (plants.isEmpty()) {
            return;
        }
        syncConveyorState(session, plants);
        drawConveyor(batch, session);
        drawSlots(shapes, session, plants, selectedPlantName);
        drawPlants(batch, session, plants, stateTime);
        drawCooldownShade(shapes, session, plants);
        drawCosts(batch, session, plants);
        drawBoostState(batch, session, plants, boostedPlant);
    }

    public String findPlantAt(GameSession session, float x, float y) {
        if (session == null || x < BANK_X || x > BANK_X + SLOT_WIDTH) {
            return null;
        }
        List<VisiblePlant> plants = visiblePlants(session);
        for (VisiblePlant plant : plants) {
            float slotY = slotY(session, plant);
            if (y >= slotY && y <= slotY + SLOT_HEIGHT) {
                return plant.name();
            }
        }
        return null;
    }

    private void drawConveyor(Batch batch, GameSession session) {
        if (!isConveyor(session) || conveyorBelt == null) {
            return;
        }
        float beltWidth = SLOT_WIDTH;
        float beltSegmentHeight = scaledHeight(conveyorBelt, beltWidth);
        float topHeight = conveyorTop == null ? 0f : scaledHeight(conveyorTop, beltWidth);
        float totalHeight = BANK_TOP + topHeight;

        batch.begin();
        for (float y = 0f; y < totalHeight; y += beltSegmentHeight) {
            batch.draw(conveyorBelt, BANK_X, y, beltWidth, beltSegmentHeight);
        }
        if (conveyorTop != null) {
            batch.draw(conveyorTop, BANK_X, BANK_TOP, beltWidth, topHeight);
        }
        if (conveyorSide != null) {
            float sideHeight = totalHeight;
            float sideWidth = conveyorSide.getRegionWidth() * sideHeight / conveyorSide.getRegionHeight();
            batch.draw(conveyorSide, BANK_X + beltWidth - sideWidth, 0f, sideWidth, sideHeight);
        }
        batch.end();
    }

    private void drawSlots(
        ShapeRenderer shapes,
        GameSession session,
        List<VisiblePlant> plants,
        String selectedPlantName
    ) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (VisiblePlant plant : plants) {
            float y = slotY(session, plant);
            String name = plant.name();
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
        List<VisiblePlant> plants,
        float stateTime
    ) {
        batch.begin();
        for (VisiblePlant plant : plants) {
            drawPlant(batch, session, plant, stateTime);
        }
        batch.end();
    }

    private void drawCooldownShade(
        ShapeRenderer shapes,
        GameSession session,
        List<VisiblePlant> plants
    ) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(COOLDOWN_SHADE);
        for (VisiblePlant plant : plants) {
            float remainingRatio = cooldownRemainingRatio(session, plant.name());
            if (remainingRatio <= 0f) {
                continue;
            }
            float innerHeight = SLOT_HEIGHT - SLOT_INSET * 2f;
            float darkHeight = innerHeight * remainingRatio;
            float brightHeight = innerHeight - darkHeight;
            shapes.rect(
                BANK_X + SLOT_INSET,
                slotY(session, plant) + SLOT_INSET + brightHeight,
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

    private void drawCosts(Batch batch, GameSession session, List<VisiblePlant> plants) {
        Color previousFontColor = new Color(font.getColor());
        batch.begin();
        font.setColor(TEXT_COLOR);
        for (VisiblePlant plant : plants) {
            int cost = session.getPlantCost(plant.name());
            font.draw(batch, String.valueOf(cost), BANK_X + 100f, slotY(session, plant) + 21f);
        }
        font.setColor(previousFontColor);
        batch.end();
    }

    private void drawBoostState(
        Batch batch,
        GameSession session,
        List<VisiblePlant> plants,
        Predicate<String> boostedPlant
    ) {
        if (boostedPlant == null) {
            return;
        }
        Color previousFontColor = new Color(font.getColor());
        batch.begin();
        font.setColor(TEXT_COLOR);
        for (VisiblePlant plant : plants) {
            if (boostedPlant.test(plant.name())) {
                font.draw(batch, "BOOST", BANK_X + 78f, slotY(session, plant) + 66f);
            }
        }
        font.setColor(previousFontColor);
        batch.end();
    }

    private void drawPlant(
        Batch batch,
        GameSession session,
        VisiblePlant plant,
        float stateTime
    ) {
        EntityAnimationProfile profile = profileFor(session, plant.name());
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
            slotY(session, plant) + PLANT_Y_OFFSET,
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

    private void syncConveyorState(GameSession session, List<VisiblePlant> plants) {
        if (!isConveyor(session)) {
            conveyorCardY.clear();
            return;
        }

        float alpha = Math.min(1f, Gdx.graphics.getDeltaTime() * CONVEYOR_ANIMATION_SPEED);
        Map<String, Boolean> activeKeys = new LinkedHashMap<>();
        for (VisiblePlant plant : plants) {
            activeKeys.put(plant.key(), Boolean.TRUE);
            float currentY = conveyorCardY.getOrDefault(plant.key(), CONVEYOR_ENTRY_Y);
            float targetY = staticSlotY(plant.index());
            float nextY = MathUtils.lerp(currentY, targetY, alpha);
            if (Math.abs(nextY - targetY) < 0.5f) {
                nextY = targetY;
            }
            conveyorCardY.put(plant.key(), nextY);
        }

        Iterator<String> iterator = conveyorCardY.keySet().iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            if (!activeKeys.containsKey(key)) {
                iterator.remove();
            }
        }
    }

    private boolean isConveyor(GameSession session) {
        Level level = session == null ? null : session.getCurrentLevel();
        return level != null && level.usesConveyorBelt();
    }

    private List<VisiblePlant> visiblePlants(GameSession session) {
        List<VisiblePlant> result = new ArrayList<>();
        Level level = session.getCurrentLevel();
        Iterable<String> source = level != null && level.usesConveyorBelt()
            ? level.getConveyorPlants()
            : session.getSelectedPlantNames();
        Map<String, Integer> occurrences = new LinkedHashMap<>();
        for (String plantName : source) {
            if (result.size() >= MAX_VISIBLE_SLOTS) {
                break;
            }
            String normalizedName = normalizeKey(plantName);
            int occurrence = occurrences.getOrDefault(normalizedName, 0) + 1;
            occurrences.put(normalizedName, occurrence);
            result.add(new VisiblePlant(normalizedName + "#" + occurrence, plantName, result.size()));
        }
        return result;
    }

    private boolean isSelected(String plantName, String selectedPlantName) {
        return plantName != null
            && selectedPlantName != null
            && plantName.equalsIgnoreCase(selectedPlantName);
    }

    private float slotY(GameSession session, VisiblePlant plant) {
        if (isConveyor(session)) {
            return conveyorCardY.getOrDefault(plant.key(), CONVEYOR_ENTRY_Y);
        }
        return staticSlotY(plant.index());
    }

    private float staticSlotY(int index) {
        return BANK_TOP - SLOT_HEIGHT - index * (SLOT_HEIGHT + SLOT_GAP);
    }

    private float scaledHeight(TextureRegion region, float targetWidth) {
        return region.getRegionHeight() * targetWidth / region.getRegionWidth();
    }

    private String normalizeKey(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace('-', ' ').replace('_', ' ');
    }

    private record VisiblePlant(String key, String name, int index) {
    }
}
