package game.chapter;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import game.animation.core.PvzAnimationService;
import game.render.BoardGeometry;
import models.core.plant.Plant;
import models.core.zombie.Zombie;
import models.engine.board.Board;
import models.engine.board.Position;
import models.engine.board.Tile;
import models.engine.board.TileType;
import models.level.core.Level;
import models.level.core.LevelType;
import models.level.core.SeasonType;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ChapterVisualRenderer {
    private static final float SAND_BURST_DURATION = 1.05f;
    private static final float FROST_WIND_DURATION = 1.45f;
    private static final float NECROMANCY_DURATION = 1.0f;
    private static final float ZOMBIE_ICE_HEALTH = 600f;
    private static final float WORLD_WIDTH = 1280f;
    private static final String WATER_TIDE_LINE_PAM =
            "768/FULL/BACKGROUNDS/WATER_TIDE_LINE/WATER_TIDE_LINE.PAM";
    private static final String NECROMANCY_DIRT_PAM =
            "768/INITIAL/EFFECTS/GRAVEBUSTER_DIRT/GRAVEBUSTER_DIRT.PAM";

    private static final Color WATER_COLOR = new Color(0.12f, 0.48f, 0.72f, 0.34f);
    private static final Color WATER_SHINE_COLOR = new Color(0.60f, 0.92f, 1f, 0.24f);
    private static final Color LOW_TIDE_COLOR = new Color(0.52f, 0.42f, 0.25f, 0.18f);
    private static final Color SLIPPERY_COLOR = new Color(0.54f, 0.88f, 1f, 0.26f);
    private static final Color ARROW_COLOR = new Color(0.88f, 0.98f, 1f, 0.82f);
    private static final Color NECROMANCY_PULSE_COLOR = new Color(0.74f, 0.32f, 0.88f, 0.42f);

    private final Level level;
    private final Board board;
    private final BoardGeometry geometry;
    private final PvzAnimationService animations;
    private final Camera camera;
    private final SeasonType season;
    private final Map<Position, Float> necromancySpawnTimes = new LinkedHashMap<>();
    private final Map<Zombie, Boolean> seenSandstormZombies = new IdentityHashMap<>();
    private final List<SandBurstVisual> sandBursts = new ArrayList<>();

    private final TextureRegion frostWind;
    private final TextureRegion waterTile;
    private final TextureRegion scorchedTile;
    private final TextureRegion scorchedEdge;
    private final TextureRegion lowBeachMarker;
    private final TextureRegion sandRearA;
    private final TextureRegion sandRearB;
    private final TextureRegion sandFrontA;
    private final TextureRegion sandFrontB;
    private final TextureRegion sandCloud;
    private final TextureRegion sandSpeedLine;
    private final TextureRegion[] plantIceLevels;
    private final TextureRegion[] zombieIceLevels;

    private int observedWave;
    private float elapsed;
    private float frostWindTime;
    private float necromancyTime;
    private float displayedTideX = Float.NaN;

    public ChapterVisualRenderer(
            Level level,
            BoardGeometry geometry,
            PvzAnimationService animations,
            Camera camera
    ) {
        if (level == null || geometry == null || animations == null || camera == null) {
            throw new IllegalArgumentException(
                    "Chapter visuals require a level, board geometry, animations, and camera."
            );
        }
        this.level = level;
        this.board = level.getBoard();
        this.geometry = geometry;
        this.animations = animations;
        this.camera = camera;
        this.season = level.getSeasonType();
        this.frostWind = animations.region("IMAGE_EFFECTS_FROSTBITE_CHILL_WIND_FROSTBITE_CHILL_WIND_290X163");
        this.waterTile = animations.region("IMAGE_UI_CARDS_BACKGROUNDS_CARD_PLANT_BG_BEACH_WATER");
        this.scorchedTile = animations.region("IMAGE_EFFECTS_SCORCHED_EARTH_SCORCHED_EARTH_128X152");
        this.scorchedEdge = animations.region("IMAGE_EFFECTS_SCORCHED_EARTH_EDGE_SCORCHED_EARTH_EDGE_128X152");
        this.lowBeachMarker = animations.region(
                "IMAGE_EFFECTS_ZOMBIE_OCTOPUS_PROJECTILE_ZOMBIE_OCTOPUS_PROJECTILE_87X61_3"
        );
        this.sandRearA = animations.region("IMAGE_EFFECTS_SANDSTORM_REAR_SANDSTORM_BACK1");
        this.sandRearB = animations.region("IMAGE_EFFECTS_SANDSTORM_REAR_SANDSTORM_BACK2");
        this.sandFrontA = animations.region("IMAGE_EFFECTS_SANDSTORM_TOP_SANDSTORM_FRONT1");
        this.sandFrontB = animations.region("IMAGE_EFFECTS_SANDSTORM_TOP_SANDSTORM_FRONT2");
        this.sandCloud = animations.region("IMAGE_EFFECTS_SANDSTORM_TOP_SANDSTORM_CLOUD");
        this.sandSpeedLine = animations.region("IMAGE_EFFECTS_SANDSTORM_TOP_SANDSTORM_SPEEDLINE_FILTERED");
        this.plantIceLevels = new TextureRegion[] {
                animations.region("IMAGE_EFFECTS_FROSTBITE_ICE_BLOCK_PLANT_FROSTBITE_ICE_BLOCK_PLANT_167X172"),
                animations.region("IMAGE_EFFECTS_FROSTBITE_ICE_BLOCK_PLANT_FROSTBITE_ICE_BLOCK_PLANT_167X172_4"),
                animations.region("IMAGE_EFFECTS_FROSTBITE_ICE_BLOCK_PLANT_FROSTBITE_ICE_BLOCK_PLANT_164X169")
        };
        this.zombieIceLevels = new TextureRegion[] {
                animations.region("IMAGE_EFFECTS_FROSTBITE_ICE_BLOCK_ZOMBIE_FROSTBITE_ICE_BLOCK_ZOMBIE_153X243"),
                animations.region("IMAGE_EFFECTS_FROSTBITE_ICE_BLOCK_ZOMBIE_FROSTBITE_ICE_BLOCK_ZOMBIE_153X243_2"),
                animations.region("IMAGE_EFFECTS_FROSTBITE_ICE_BLOCK_ZOMBIE_FROSTBITE_ICE_BLOCK_ZOMBIE_153X243_3"),
                animations.region("IMAGE_EFFECTS_FROSTBITE_ICE_BLOCK_ZOMBIE_FROSTBITE_ICE_BLOCK_ZOMBIE_153X243_4"),
                animations.region("IMAGE_EFFECTS_FROSTBITE_ICE_BLOCK_ZOMBIE_FROSTBITE_ICE_BLOCK_ZOMBIE_153X243_5"),
                animations.region("IMAGE_EFFECTS_FROSTBITE_ICE_BLOCK_ZOMBIE_FROSTBITE_ICE_BLOCK_ZOMBIE_153X243_6")
        };
        this.observedWave = 0;
        if (season == SeasonType.BIG_WAVE_BEACH) {
            animations.preload(WATER_TIDE_LINE_PAM);
            displayedTideX = targetTideX();
        } else if (season == SeasonType.DARK_AGES) {
            animations.preload(NECROMANCY_DIRT_PAM);
        }
    }

    public void update(float delta) {
        float safeDelta = Math.max(0f, delta);
        elapsed += safeDelta;
        frostWindTime = Math.max(0f, frostWindTime - safeDelta);
        necromancyTime = Math.max(0f, necromancyTime - safeDelta);
        detectWaveChange();
        updateNecromancySpawns(safeDelta);
        updateSandstormBursts(safeDelta);
        updateDisplayedTideLine(safeDelta);
    }

    public void renderBehindEntities(Batch batch, ShapeRenderer shapes) {
        if (board == null || season == null) {
            return;
        }
        drawTerrain(shapes);
        drawTerrainSprites(batch);
        if (season == SeasonType.ANCIENT_EGYPT) {
            drawSandstormRear(batch);
        }
    }

    public void renderAboveEntities(Batch batch, ShapeRenderer shapes) {
        if (board == null || season == null) {
            return;
        }
        if (season == SeasonType.FROSTBITE_CAVES) {
            drawFrozenEntities(batch);
            if (frostWindTime > 0f) {
                drawFrostWind(batch);
            }
        }
        if (season == SeasonType.ANCIENT_EGYPT) {
            drawSandstormFront(batch);
        }
        if (season == SeasonType.DARK_AGES) {
            drawNecromancyDirt(batch);
            drawNecromancyPulse(shapes);
        }
    }

    private void detectWaveChange() {
        int currentWave = level.getWaveManager().getCurrentWaveNumber();
        if (currentWave <= 0 || currentWave == observedWave) {
            return;
        }
        observedWave = currentWave;
        if (season == SeasonType.FROSTBITE_CAVES) {
            frostWindTime = FROST_WIND_DURATION;
        } else if (season == SeasonType.DARK_AGES) {
            necromancyTime = NECROMANCY_DURATION;
        }
    }

    private void updateSandstormBursts(float delta) {
        if (season != SeasonType.ANCIENT_EGYPT) {
            return;
        }
        Iterator<SandBurstVisual> iterator = sandBursts.iterator();
        while (iterator.hasNext()) {
            SandBurstVisual visual = iterator.next();
            visual.time += delta;
            if (visual.time >= SAND_BURST_DURATION) {
                iterator.remove();
            }
        }
        if (observedWave != level.getWaveManager().getTotalWaves()) {
            return;
        }
        for (Zombie zombie : board.getAllZombies()) {
            if (zombie == null || !zombie.isAlive() || seenSandstormZombies.containsKey(zombie)) {
                continue;
            }
            int tileX = Math.max(1, Math.min(board.getWidth(), (int) Math.ceil(zombie.getX())));
            if (tileX < board.getWidth() - 4) {
                continue;
            }
            seenSandstormZombies.put(zombie, Boolean.TRUE);
            sandBursts.add(new SandBurstVisual((float) zombie.getX(), (float) zombie.getY()));
        }
    }

    private void updateNecromancySpawns(float delta) {
        if (season != SeasonType.DARK_AGES) {
            return;
        }
        Set<Position> pending = level.getPendingNecromancyPositions();
        for (Position position : pending) {
            necromancySpawnTimes.put(
                    position, necromancySpawnTimes.getOrDefault(position, 0f) + delta
            );
        }
        necromancySpawnTimes.keySet().removeIf(position -> !pending.contains(position));
    }

    private void drawTerrain(ShapeRenderer shapes) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (int row = 1; row <= board.getHeight(); row++) {
            for (int column = 1; column <= board.getWidth(); column++) {
                Tile tile = board.getTileAt(new Position(column, row));
                if (tile == null) {
                    continue;
                }
                Rectangle bounds = geometry.getTileBounds(row, column);
                drawTileFill(shapes, tile.getTileType(), bounds);
            }
        }
        if (season == SeasonType.BIG_WAVE_BEACH) {
            drawContinuousWaterFill(shapes);
        }
        shapes.end();
    }

    private void drawTileFill(ShapeRenderer shapes, TileType type, Rectangle tile) {
        if (type == TileType.WATER) {
            if (season != SeasonType.BIG_WAVE_BEACH) {
                shapes.setColor(WATER_COLOR);
                shapes.rect(tile.x, tile.y, tile.width, tile.height);
            }
            return;
        }
        if (type == TileType.LOW_TIDE) {
            shapes.setColor(LOW_TIDE_COLOR);
            shapes.rect(tile.x, tile.y, tile.width, tile.height);
            return;
        }
        if (type == TileType.SLIPPERY_UP || type == TileType.SLIPPERY_DOWN) {
            shapes.setColor(SLIPPERY_COLOR);
            shapes.rect(tile.x + 2f, tile.y + 2f, tile.width - 4f, tile.height - 4f);
            drawSlipperyArrow(shapes, tile, type == TileType.SLIPPERY_UP);
        }
    }

    private void drawSlipperyArrow(ShapeRenderer shapes, Rectangle tile, boolean up) {
        float centerX = tile.x + tile.width / 2f;
        float centerY = tile.y + tile.height / 2f;
        float stemWidth = Math.max(4f, tile.width * 0.07f);
        float stemHeight = tile.height * 0.30f;
        float headWidth = tile.width * 0.20f;
        float headHeight = tile.height * 0.18f;
        shapes.setColor(ARROW_COLOR);
        if (up) {
            shapes.rect(centerX - stemWidth / 2f, centerY - stemHeight * 0.55f, stemWidth, stemHeight);
            shapes.triangle(
                    centerX - headWidth, centerY + stemHeight * 0.30f,
                    centerX + headWidth, centerY + stemHeight * 0.30f,
                    centerX, centerY + stemHeight * 0.30f + headHeight
            );
            return;
        }
        shapes.rect(centerX - stemWidth / 2f, centerY - stemHeight * 0.45f, stemWidth, stemHeight);
        shapes.triangle(
                centerX - headWidth, centerY - stemHeight * 0.20f,
                centerX + headWidth, centerY - stemHeight * 0.20f,
                centerX, centerY - stemHeight * 0.20f - headHeight
        );
    }

    private void drawTerrainSprites(Batch batch) {
        batch.begin();
        if (season == SeasonType.BIG_WAVE_BEACH && level.getLevelType() == LevelType.BOSS) {
            drawBossOceanExtension(batch);
        }
        if (season == SeasonType.BIG_WAVE_BEACH) {
            drawContinuousWaterTiles(batch);
        }
        for (int row = 1; row <= board.getHeight(); row++) {
            for (int column = 1; column <= board.getWidth(); column++) {
                Position position = new Position(column, row);
                Tile tile = board.getTileAt(position);
                if (tile == null) {
                    continue;
                }
                if (season == SeasonType.DARK_AGES && level.getBossRuntime() != null
                        && level.getBossRuntime().getBurningTiles().containsKey(position)) {
                    drawScorchedTile(batch, position);
                }
            }
        }
        if (season == SeasonType.BIG_WAVE_BEACH) {
            for (Position position : level.getLowTidePositions()) {
                drawLowBeachMarker(batch, position);
            }
            drawTideLine(batch);
        }
        batch.end();
    }

    private void drawScorchedTile(Batch batch, Position position) {
        TextureRegion region = scorchedTile;
        if (position.getX() == BoardGeometry.COLUMNS - 1
                && level.getBossRuntime().getCurrentTick() < level.getBossRuntime().getArrivalScorchUntilTick()) {
            region = scorchedEdge;
        }
        if (region == null) {
            return;
        }
        Rectangle tile = geometry.getTileBounds(position.getY(), position.getX());
        batch.setColor(Color.WHITE);
        batch.draw(region, tile.x, tile.y, tile.width, tile.height);
    }

    private void drawContinuousWaterFill(ShapeRenderer shapes) {
        if (Float.isNaN(displayedTideX)) {
            return;
        }
        Rectangle boardBounds = geometry.getBoardBounds();
        float width = boardBounds.x + boardBounds.width - displayedTideX;
        if (width <= 0f) {
            return;
        }
        shapes.setColor(WATER_COLOR);
        shapes.rect(displayedTideX, boardBounds.y, width, boardBounds.height);
        float waveY = boardBounds.y + boardBounds.height * 0.68f
                + MathUtils.sin(elapsed * 2.2f) * geometry.getTileHeight() * 0.05f;
        shapes.setColor(WATER_SHINE_COLOR);
        shapes.rect(displayedTideX, waveY, width, 3f);
    }

    private void drawContinuousWaterTiles(Batch batch) {
        if (waterTile == null || Float.isNaN(displayedTideX)) {
            return;
        }
        Rectangle boardBounds = geometry.getBoardBounds();
        float endX = boardBounds.x + boardBounds.width;
        float tileWidth = geometry.getTileWidth();
        float tileHeight = geometry.getTileHeight();
        for (float x = displayedTideX; x < endX; x += tileWidth) {
            float width = Math.min(tileWidth, endX - x);
            for (int row = 0; row < board.getHeight(); row++) {
                float y = boardBounds.y + row * tileHeight;
                float pulse = 0.80f + 0.06f * MathUtils.sin(
                        elapsed * 1.9f + x * 0.01f + row
                );
                batch.setColor(0.82f, 0.96f, 1f, pulse);
                batch.draw(waterTile, x, y, width, tileHeight);
            }
        }
        batch.setColor(Color.WHITE);
    }

    private void drawBossOceanExtension(Batch batch) {
        if (waterTile == null) {
            return;
        }
        Rectangle boardBounds = geometry.getBoardBounds();
        float startX = boardBounds.x + boardBounds.width;
        float endX = Math.max(startX, WORLD_WIDTH);
        float stripWidth = geometry.getTileWidth();
        for (float x = startX; x < endX; x += stripWidth) {
            float width = Math.min(stripWidth, endX - x);
            for (int row = 0; row < BoardGeometry.ROWS; row++) {
                float y = boardBounds.y + row * geometry.getTileHeight();
                batch.setColor(0.82f, 0.96f, 1f, 0.84f);
                batch.draw(waterTile, x, y, width, geometry.getTileHeight());
            }
        }
        batch.setColor(Color.WHITE);
    }

    private void drawLowBeachMarker(Batch batch, Position position) {
        if (lowBeachMarker == null || position == null) {
            return;
        }
        Rectangle tile = geometry.getTileBounds(position.getY(), position.getX());
        float width = tile.width * 0.68f;
        float height = width * lowBeachMarker.getRegionHeight() / lowBeachMarker.getRegionWidth();
        batch.setColor(1f, 1f, 1f, 0.92f);
        batch.draw(lowBeachMarker, tile.x + (tile.width - width) / 2f, tile.y + tile.height * 0.16f, width, height);
        batch.setColor(Color.WHITE);
    }

    private void updateDisplayedTideLine(float delta) {
        if (season != SeasonType.BIG_WAVE_BEACH || board == null) {
            return;
        }
        float target = targetTideX();
        if (Float.isNaN(target)) {
            return;
        }
        if (Float.isNaN(displayedTideX)) {
            displayedTideX = target;
            return;
        }
        float follow = 1f - (float) Math.exp(-4.0f * Math.max(0f, delta));
        displayedTideX += (target - displayedTideX) * follow;
        if (Math.abs(target - displayedTideX) < 0.35f) {
            displayedTideX = target;
        }
    }

    private float targetTideX() {
        int firstWaterColumn = Integer.MAX_VALUE;
        for (int row = 1; row <= board.getHeight(); row++) {
            for (int column = 1; column <= board.getWidth(); column++) {
                Tile tile = board.getTileAt(new Position(column, row));
                if (tile != null && tile.getTileType() == TileType.WATER) {
                    firstWaterColumn = Math.min(firstWaterColumn, column);
                }
            }
        }
        if (firstWaterColumn == Integer.MAX_VALUE) {
            return Float.NaN;
        }
        return geometry.getTileBounds(1, firstWaterColumn).x;
    }

    private void drawTideLine(Batch batch) {
        if (Float.isNaN(displayedTideX)) {
            return;
        }
        Rectangle boardBounds = geometry.getBoardBounds();
        Rectangle scissor = new Rectangle();
        batch.flush();
        ScissorStack.calculateScissors(
                camera, batch.getTransformMatrix(), boardBounds, scissor
        );
        if (!ScissorStack.pushScissors(scissor)) {
            return;
        }
        animations.draw(
                batch, WATER_TIDE_LINE_PAM, "idle", elapsed, displayedTideX,
                boardBounds.y + boardBounds.height / 2f, 1f, true
        );
        batch.flush();
        ScissorStack.popScissors();
    }

    private void drawFrozenEntities(Batch batch) {
        batch.begin();
        for (Plant plant : board.getAllPlants()) {
            if (plant == null || !plant.isAlive() || plant.getIceHits() <= 0) {
                continue;
            }
            int row = Math.max(1, Math.min(board.getHeight(), (int) Math.round(plant.getY())));
            int column = Math.max(1, Math.min(board.getWidth(), (int) Math.round(plant.getX())));
            int iceIndex = Math.max(
                    0, Math.min(plantIceLevels.length - 1, plant.getIceHits() - 1)
            );
            TextureRegion region = plantIceLevels[iceIndex];
            if (region != null) {
                Rectangle tile = geometry.getTileBounds(row, column);
                drawCentered(batch, region, tile, tile.height * 1.12f, 0.90f);
            }
        }
        for (Zombie zombie : board.getAllZombies()) {
            if (zombie == null || !zombie.isAlive()) {
                continue;
            }
            int iceHealth = board.getInitialZombieIceHealth(zombie);
            if (iceHealth <= 0) {
                continue;
            }
            float ratio = MathUtils.clamp(iceHealth / ZOMBIE_ICE_HEALTH, 0f, 1f);
            int index = Math.min(
                    zombieIceLevels.length - 1,
                    Math.max(0, (int) ((1f - ratio) * zombieIceLevels.length))
            );
            TextureRegion region = zombieIceLevels[index];
            if (region == null) {
                continue;
            }
            Vector2 center = geometry.entityToScreen(zombie.getX(), zombie.getY());
            float height = geometry.getTileHeight() * 1.48f;
            float width = height * region.getRegionWidth() / region.getRegionHeight();
            batch.setColor(1f, 1f, 1f, 0.92f);
            batch.draw(region, center.x - width / 2f, center.y - height * 0.50f, width, height);
            batch.setColor(Color.WHITE);
        }
        batch.end();
    }

    private void drawFrostWind(Batch batch) {
        if (frostWind == null) {
            return;
        }
        float progress = 1f - frostWindTime / FROST_WIND_DURATION;
        float alpha = effectAlpha(progress);
        Rectangle boardBounds = geometry.getBoardBounds();
        float height = boardBounds.height * 0.56f;
        float width = height * frostWind.getRegionWidth() / frostWind.getRegionHeight();
        float offset = progress * (boardBounds.width + width * 1.4f);
        batch.begin();
        batch.setColor(0.82f, 0.94f, 1f, alpha * 0.78f);
        for (int row = 0; row < 3; row++) {
            float x = boardBounds.x + boardBounds.width + width * 0.2f - offset + row * width * 0.48f;
            float y = boardBounds.y - height * 0.08f + row * height * 0.36f;
            batch.draw(frostWind, x, y, width, height);
        }
        batch.setColor(Color.WHITE);
        batch.end();
    }

    private void drawSandstormRear(Batch batch) {
        if (sandBursts.isEmpty()) {
            return;
        }
        batch.begin();
        for (SandBurstVisual visual : sandBursts) {
            float progress = MathUtils.clamp(visual.time / SAND_BURST_DURATION, 0f, 1f);
            float alpha = (1f - progress) * 0.55f;
            drawSandBurst(batch, visual, alpha, true);
        }
        batch.end();
    }

    private void drawSandstormFront(Batch batch) {
        if (sandBursts.isEmpty()) {
            return;
        }
        batch.begin();
        for (SandBurstVisual visual : sandBursts) {
            float progress = MathUtils.clamp(visual.time / SAND_BURST_DURATION, 0f, 1f);
            float alpha = effectAlpha(progress) * (1f - progress * 0.20f);
            drawSandBurst(batch, visual, alpha, false);
        }
        batch.end();
    }

    private void drawSandBurst(Batch batch, SandBurstVisual visual, float alpha, boolean rear) {
        Vector2 center = geometry.entityToScreen(visual.x, visual.y);
        float width = geometry.getTileWidth() * 1.8f;
        float height = geometry.getTileHeight() * 1.6f;
        TextureRegion first = rear ? sandRearA : sandFrontA;
        TextureRegion second = rear ? sandRearB : sandFrontB;
        if (first != null) {
            batch.setColor(1f, 0.88f, 0.58f, alpha);
            batch.draw(first, center.x - width * 0.70f, center.y - height * 0.48f, width, height);
        }
        if (second != null) {
            batch.setColor(1f, 0.86f, 0.54f, alpha * 0.92f);
            batch.draw(second, center.x - width * 0.10f, center.y - height * 0.42f, width, height);
        }
        if (!rear && sandCloud != null) {
            batch.setColor(1f, 0.88f, 0.58f, alpha * 0.88f);
            batch.draw(sandCloud, center.x - width * 0.26f, center.y - height * 0.10f, width * 0.92f, height * 0.82f);
        }
        if (!rear && sandSpeedLine != null) {
            batch.setColor(1f, 0.92f, 0.70f, alpha * 0.80f);
            for (int index = 0; index < 3; index++) {
                batch.draw(
                        sandSpeedLine,
                        center.x - width * 0.68f + index * width * 0.32f,
                        center.y - height * 0.05f + index * 12f,
                        width * 0.56f,
                        height * 0.20f
                );
            }
        }
        batch.setColor(Color.WHITE);
    }

    private void drawNecromancyDirt(Batch batch) {
        if (necromancySpawnTimes.isEmpty()) {
            return;
        }
        batch.begin();
        for (Map.Entry<Position, Float> entry : necromancySpawnTimes.entrySet()) {
            Rectangle tile = geometry.getTileBounds(entry.getKey().getY(), entry.getKey().getX());
            animations.draw(
                    batch,
                    NECROMANCY_DIRT_PAM,
                    "gravebuster_dirt_anim",
                    entry.getValue(),
                    tile.x + tile.width / 2f,
                    tile.y + tile.height * 0.43f,
                    0.50f,
                    false
            );
        }
        batch.end();
    }

    private void drawNecromancyPulse(ShapeRenderer shapes) {
        if (level.getActiveNecromancyGravePositions().isEmpty()) {
            return;
        }
        float localPulse = necromancyTime > 0f
                ? 1f - necromancyTime / NECROMANCY_DURATION
                : (MathUtils.sin(elapsed * 3f) + 1f) * 0.5f;
        float scale = necromancyTime > 0f ? 0.65f + localPulse * 0.55f : 0.78f + localPulse * 0.10f;
        float alpha = necromancyTime > 0f ? 0.62f * (1f - localPulse * 0.55f) : 0.20f + localPulse * 0.12f;
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (Position position : level.getActiveNecromancyGravePositions()) {
            Tile tile = board.getTileAt(position);
            if (tile == null || tile.getTileType() != TileType.GRAVE) {
                continue;
            }
            Rectangle bounds = geometry.getTileBounds(position.getY(), position.getX());
            float width = bounds.width * scale;
            float height = bounds.height * scale * 0.52f;
            shapes.setColor(NECROMANCY_PULSE_COLOR.r, NECROMANCY_PULSE_COLOR.g, NECROMANCY_PULSE_COLOR.b, alpha);
            shapes.ellipse(bounds.x + (bounds.width - width) / 2f, bounds.y + bounds.height * 0.20f, width, height);
        }
        shapes.end();
    }

    private void drawCentered(Batch batch, TextureRegion region, Rectangle tile, float height, float alpha) {
        float width = height * region.getRegionWidth() / region.getRegionHeight();
        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(region, tile.x + (tile.width - width) / 2f, tile.y + (tile.height - height) / 2f, width, height);
        batch.setColor(Color.WHITE);
    }

    private float effectAlpha(float progress) {
        return MathUtils.clamp(MathUtils.sin(MathUtils.PI * MathUtils.clamp(progress, 0f, 1f)), 0f, 1f);
    }

    private static final class SandBurstVisual {
        private final float x;
        private final float y;
        private float time;

        private SandBurstVisual(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

}
