package game.chapter;

import audio.AudioCue;
import audio.AudioManager;
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
    private static final float FROST_WIND_DURATION = 2.5667f;
    private static final float FROST_THAW_STEP_SECONDS = 0.22f;
    private static final float TIDE_LINE_SHORE_ANCHOR = 0.28f;
    private static final float NECROMANCY_DURATION = 1.0f;
    private static final float ZOMBIE_ICE_HEALTH = 600f;
    private static final float WORLD_WIDTH = 1280f;
    private static final String FROST_WIND_PAM =
            "768/FULL/EFFECTS/FROSTBITE_CHILL_WIND/FROSTBITE_CHILL_WIND.PAM";
    private static final String NECROMANCY_DIRT_PAM =
            "768/INITIAL/EFFECTS/GRAVEBUSTER_DIRT/GRAVEBUSTER_DIRT.PAM";
    private static final String GRAVE_RISE_DIRT_PAM =
            "768/INITIAL/EFFECTS/DIRT_SPAWN_DIRT/DIRT_SPAWN_DIRT.PAM";
    private static final String LOW_BEACH_RIPPLE_PAM =
            "768/FULL/BACKGROUNDS/WATER_GARGANTUAR_RIPPLE/WATER_GARGANTUAR_RIPPLE.PAM";
    private static final String WAVE_UPPERLAYER_PAM =
            "768/FULL/BACKGROUNDS/WAVE_UPPERLAYER/WAVE_UPPERLAYER.PAM";
    private static final String WATER_UNDERLAYER_PAM =
            "768/FULL/BACKGROUNDS/WATER_UNDERLAYER/WATER_UNDERLAYER.PAM";
    private static final int MAX_SANDSTORM_BURSTS = 3;

    private static final Color WATER_COLOR = new Color(0.12f, 0.48f, 0.72f, 0.34f);
    private static final Color WATER_SHINE_COLOR = new Color(0.60f, 0.92f, 1f, 0.24f);
    private static final Color LOW_TIDE_COLOR = new Color(0.52f, 0.42f, 0.25f, 0.18f);
    private static final Color SLIPPERY_COLOR = new Color(0.54f, 0.88f, 1f, 0.26f);
    private static final Color ARROW_COLOR = new Color(0.88f, 0.98f, 1f, 0.82f);

    private final Level level;
    private final Board board;
    private final BoardGeometry geometry;
    private final PvzAnimationService animations;
    private final Camera camera;
    private final SeasonType season;
    private final Map<Position, Float> necromancySpawnTimes = new LinkedHashMap<>();
    private final Map<Position, Float> graveRiseTimes = new LinkedHashMap<>();
    private final Map<Position, Float> lowBeachSpawnTimes = new LinkedHashMap<>();
    private final Map<Zombie, Boolean> seenSandstormZombies = new IdentityHashMap<>();
    private final Map<Plant, Integer> displayedPlantIceLevels = new IdentityHashMap<>();
    private final Map<Plant, Float> plantIceStepTimes = new IdentityHashMap<>();
    private final List<SandBurstVisual> sandBursts = new ArrayList<>();

    private final TextureRegion waterTile;
    private final TextureRegion waveBigLeft;
    private final TextureRegion waveUpperTall;
    private final TextureRegion waveBigThin;
    private final TextureRegion waveUpperThin;
    private final TextureRegion waveUpperWide;
    private final TextureRegion waveUpperStrip;
    private final TextureRegion tideLine;
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
        this.waterTile = animations.region("IMAGE_UI_CARDS_BACKGROUNDS_CARD_PLANT_BG_BEACH_WATER");
        this.waveBigLeft = animations.region("IMAGE_BACKGROUNDS_WAVE_BIG_WAVE_BIG_870X1262");
        this.waveUpperTall = animations.region(
                "IMAGE_BACKGROUNDS_WAVE_UPPERLAYER_WAVE_UPPERLAYER_142X1335"
        );
        this.waveBigThin = animations.region("IMAGE_BACKGROUNDS_WAVE_BIG_WAVE_BIG_124X1335");
        this.waveUpperThin = animations.region(
                "IMAGE_BACKGROUNDS_WAVE_UPPERLAYER_WAVE_UPPERLAYER_124X1335"
        );
        this.waveUpperWide = animations.region(
                "IMAGE_BACKGROUNDS_WAVE_UPPERLAYER_WAVE_UPPERLAYER_484X1390"
        );
        this.waveUpperStrip = animations.region(
                "IMAGE_BACKGROUNDS_WAVE_UPPERLAYER_WAVE_UPPERLAYER_2852X86"
        );
        this.tideLine = animations.region(
                "IMAGE_BACKGROUNDS_WATER_TIDE_LINE_WATER_TIDE_LINE_161X397"
        );
        this.scorchedTile = animations.region("IMAGE_EFFECTS_SCORCHED_EARTH_SCORCHED_EARTH_128X152");
        this.scorchedEdge = animations.region("IMAGE_EFFECTS_SCORCHED_EARTH_EDGE_SCORCHED_EARTH_EDGE_128X152");
        this.lowBeachMarker = animations.region(
                "IMAGE_EFFECTS_ZOMBIE_OCTOPUS_PROJECTILE_ZOMBIE_OCTOPUS_PROJECTILE_87X61_2"
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
            animations.preload(LOW_BEACH_RIPPLE_PAM);
            animations.preload(WAVE_UPPERLAYER_PAM);
            animations.preload(WATER_UNDERLAYER_PAM);
            displayedTideX = targetTideX();
        } else if (season == SeasonType.FROSTBITE_CAVES) {
            animations.preload(FROST_WIND_PAM);
        } else if (season == SeasonType.DARK_AGES) {
            animations.preload(NECROMANCY_DIRT_PAM);
            animations.preload(GRAVE_RISE_DIRT_PAM);
        }
    }

    public void update(float delta) {
        float safeDelta = Math.max(0f, delta);
        elapsed += safeDelta;
        frostWindTime = Math.max(0f, frostWindTime - safeDelta);
        necromancyTime = Math.max(0f, necromancyTime - safeDelta);
        detectWaveChange();
        updateNecromancySpawns(safeDelta);
        updateGraveRiseEffects(safeDelta);
        updateLowBeachSpawnEffects(safeDelta);
        updateSandstormBursts(safeDelta);
        updateDisplayedTideLine(safeDelta);
        updatePlantIceVisuals(safeDelta);
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
        if (season == SeasonType.BIG_WAVE_BEACH) {
            drawLowBeachSpawnRipples(batch);
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
            drawGraveRiseDirt(batch);
            drawNecromancyDirt(batch);
        }
    }

    private void detectWaveChange() {
        int currentWave = level.getWaveManager().getCurrentWaveNumber();
        if (currentWave <= 0 || currentWave == observedWave) {
            return;
        }
        observedWave = currentWave;
        if (season == SeasonType.FROSTBITE_CAVES
                && !level.getLastFrostWindLanes().isEmpty()) {
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
        boolean spawned = false;
        for (Zombie zombie : board.getAllZombies()) {
            if (sandBursts.size() >= MAX_SANDSTORM_BURSTS) {
                break;
            }
            if (zombie == null || !zombie.isAlive() || seenSandstormZombies.containsKey(zombie)) {
                continue;
            }
            int tileX = Math.max(1, Math.min(board.getWidth(), (int) Math.ceil(zombie.getX())));
            if (tileX < board.getWidth() - 4) {
                continue;
            }
            seenSandstormZombies.put(zombie, Boolean.TRUE);
            sandBursts.add(new SandBurstVisual((float) zombie.getX(), (float) zombie.getY()));
            spawned = true;
        }
        if (spawned) {
            AudioManager.playGlobal(AudioCue.SANDSTORM);
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

    private void updateGraveRiseEffects(float delta) {
        if (season != SeasonType.DARK_AGES) {
            return;
        }
        Set<Position> pending = level.getPendingGraveRisePositions();
        for (Position position : pending) {
            graveRiseTimes.put(position, graveRiseTimes.getOrDefault(position, 0f) + delta);
        }
        graveRiseTimes.keySet().removeIf(position -> !pending.contains(position));
    }

    private void updateLowBeachSpawnEffects(float delta) {
        if (season != SeasonType.BIG_WAVE_BEACH) {
            return;
        }
        Set<Position> pending = level.getPendingLowBeachPositions();
        for (Position position : pending) {
            lowBeachSpawnTimes.put(position, lowBeachSpawnTimes.getOrDefault(position, 0f) + delta);
        }
        lowBeachSpawnTimes.keySet().removeIf(position -> !pending.contains(position));
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
        if (season == SeasonType.BIG_WAVE_BEACH) {
            drawClippedWater(batch);
            for (Position position : level.getLowTidePositions()) {
                drawLowBeachMarker(batch, position);
            }
            drawTideLine(batch);
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
        float width = WORLD_WIDTH - displayedTideX;
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

    private void drawClippedWater(Batch batch) {
        if (Float.isNaN(displayedTideX) || camera == null) {
            return;
        }
        Rectangle boardBounds = geometry.getBoardBounds();
        Rectangle clipBounds = new Rectangle(
                displayedTideX,
                boardBounds.y,
                Math.max(1f, WORLD_WIDTH - displayedTideX),
                boardBounds.height
        );
        Rectangle scissors = new Rectangle();
        batch.flush();
        ScissorStack.calculateScissors(camera, batch.getTransformMatrix(), clipBounds, scissors);
        if (!ScissorStack.pushScissors(scissors)) {
            return;
        }
        try {
            drawContinuousWaterTiles(batch);
            batch.flush();
        } finally {
            ScissorStack.popScissors();
        }
    }

    private void drawContinuousWaterTiles(Batch batch) {
        if (Float.isNaN(displayedTideX)) {
            return;
        }
        Rectangle boardBounds = geometry.getBoardBounds();
        drawAnimatedWaterBody(batch, boardBounds);
        drawMovingWaveStrip(batch, boardBounds);
        batch.setColor(Color.WHITE);
        drawWaveLayer(batch, waveBigLeft, displayedTideX, boardBounds, 0.90f, 0.08f);
        drawWaveLayer(batch, waveUpperTall, displayedTideX, boardBounds, 0.96f, 0.05f);
        drawWaveLayer(batch, waveBigThin, displayedTideX + geometry.getTileWidth() * 0.18f, boardBounds, 0.88f, 0.06f);
        drawWaveLayer(batch, waveUpperThin, displayedTideX + geometry.getTileWidth() * 0.34f, boardBounds, 0.94f, 0.07f);
        drawWaveLayer(batch, waveUpperWide, displayedTideX + geometry.getTileWidth() * 0.55f, boardBounds, 0.82f, 0.09f);
        batch.setColor(Color.WHITE);
    }

    private void drawAnimatedWaterBody(Batch batch, Rectangle boardBounds) {
        float centerX = (displayedTideX + WORLD_WIDTH) * 0.50f;
        float centerY = boardBounds.y + boardBounds.height * 0.50f;
        batch.setColor(1f, 1f, 1f, 0.72f);
        animations.draw(
                batch, WATER_UNDERLAYER_PAM, "Water", elapsed,
                centerX, centerY, 1.35f, true
        );
        batch.setColor(Color.WHITE);
    }

    private void drawMovingWaveStrip(Batch batch, Rectangle boardBounds) {
        TextureRegion strip = waveUpperStrip != null ? waveUpperStrip : waveUpperWide;
        if (strip == null) {
            animations.draw(
                    batch, WAVE_UPPERLAYER_PAM, "water", elapsed,
                    displayedTideX + geometry.getTileWidth(),
                    boardBounds.y + boardBounds.height * 0.55f,
                    0.92f, true
            );
            return;
        }
        float waterLeft = displayedTideX;
        float waterRight = WORLD_WIDTH;
        float nativeRatio = strip.getRegionWidth() / Math.max(1f, (float) strip.getRegionHeight());
        float stripHeight = Math.max(28f, geometry.getTileHeight() * 0.42f);
        float stripWidth = stripHeight * nativeRatio;
        float surge = Math.max(0f, MathUtils.sin(elapsed * 1.05f)) * geometry.getTileWidth() * 0.85f;
        batch.setColor(1f, 1f, 1f, 0.94f);
        int row = 0;
        for (float y = boardBounds.y; y < boardBounds.y + boardBounds.height; y += stripHeight * 0.78f) {
            float phase = surge + Math.max(0f, MathUtils.sin(elapsed * 1.35f + row * 0.85f))
                    * geometry.getTileWidth() * 0.28f;
            float startX = waterLeft + phase;
            for (float x = startX; x < waterRight + stripWidth * 0.20f; x += stripWidth * 0.96f) {
                batch.draw(strip, x, y, stripWidth, stripHeight);
            }
            row++;
        }
        batch.setColor(Color.WHITE);
    }

    private void drawWaveLayer(
            Batch batch, TextureRegion region, float boundaryX, Rectangle boardBounds,
            float alpha, float horizontalScale
    ) {
        if (region == null) {
            return;
        }
        float height = boardBounds.height;
        float width = height * region.getRegionWidth() / Math.max(1f, region.getRegionHeight());
        float horizontal = Math.abs(MathUtils.sin(elapsed * 1.5f + boundaryX * 0.01f))
                * geometry.getTileWidth() * Math.abs(horizontalScale);
        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(
                region, boundaryX + horizontal,
                boardBounds.y,
                width, height
        );
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
        float follow = 1f - (float) Math.exp(-2.6f * Math.max(0f, delta));
        displayedTideX += (target - displayedTideX) * follow;
        if (Math.abs(target - displayedTideX) < 0.35f) {
            displayedTideX = target;
        }
    }

    private float targetTideX() {
        int waterColumns = level.getCurrentWaterColumns();
        if (waterColumns <= 0) {
            return Float.NaN;
        }
        int firstWaterColumn = board.getWidth() - waterColumns + 1;
        return geometry.getTileBounds(1, firstWaterColumn).x;
    }

    private void drawTideLine(Batch batch) {
        if (Float.isNaN(displayedTideX) || tideLine == null) {
            return;
        }
        Rectangle boardBounds = geometry.getBoardBounds();
        float height = boardBounds.height;
        // Preserve the source aspect ratio: scale by height only, never stretch horizontally.
        float width = height * tideLine.getRegionWidth() / Math.max(1f, tideLine.getRegionHeight());
        batch.setColor(Color.WHITE);
        batch.draw(
                tideLine, displayedTideX - width * TIDE_LINE_SHORE_ANCHOR,
                boardBounds.y, width, height
        );
    }

    private void updatePlantIceVisuals(float delta) {
        if (season != SeasonType.FROSTBITE_CAVES || board == null) {
            return;
        }
        Set<Plant> activePlants = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        for (Plant plant : board.getAllPlants()) {
            if (plant == null || !plant.isAlive()) {
                continue;
            }
            activePlants.add(plant);
            int targetLevel = Math.max(0, Math.min(3, plant.getIceVisualLevel()));
            Integer previous = displayedPlantIceLevels.get(plant);
            if (previous == null) {
                displayedPlantIceLevels.put(plant, targetLevel);
                plantIceStepTimes.put(plant, 0f);
                continue;
            }
            int shownLevel = previous;
            if (targetLevel >= shownLevel) {
                displayedPlantIceLevels.put(plant, targetLevel);
                plantIceStepTimes.put(plant, 0f);
                continue;
            }

            float timer = plantIceStepTimes.getOrDefault(plant, 0f) + Math.max(0f, delta);
            while (shownLevel > targetLevel && timer >= FROST_THAW_STEP_SECONDS) {
                shownLevel--;
                timer -= FROST_THAW_STEP_SECONDS;
            }
            displayedPlantIceLevels.put(plant, shownLevel);
            plantIceStepTimes.put(plant, timer);
        }
        displayedPlantIceLevels.keySet().removeIf(plant -> !activePlants.contains(plant));
        plantIceStepTimes.keySet().removeIf(plant -> !activePlants.contains(plant));
    }

    private void drawFrozenEntities(Batch batch) {
        batch.begin();
        for (Plant plant : board.getAllPlants()) {
            if (plant == null || !plant.isAlive()) {
                continue;
            }
            int visualLevel = displayedPlantIceLevels.getOrDefault(
                    plant, plant.getIceVisualLevel()
            );
            if (visualLevel <= 0) {
                continue;
            }
            int row = Math.max(1, Math.min(board.getHeight(), (int) Math.round(plant.getY())));
            int column = Math.max(1, Math.min(board.getWidth(), (int) Math.round(plant.getX())));
            int iceIndex = Math.max(
                    0, Math.min(plantIceLevels.length - 1, visualLevel - 1)
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
        Set<Integer> lanes = level.getLastFrostWindLanes();
        if (lanes.isEmpty()) {
            return;
        }
        float animationTime = Math.max(0f, FROST_WIND_DURATION - frostWindTime);
        float progress = MathUtils.clamp(animationTime / FROST_WIND_DURATION, 0f, 1f);
        float alpha = Math.max(0.30f, effectAlpha(progress));
        Rectangle boardBounds = geometry.getBoardBounds();
        batch.begin();
        batch.setColor(0.86f, 0.96f, 1f, alpha);
        for (int lane : lanes) {
            Rectangle laneTile = geometry.getTileBounds(lane, 1);
            float y = laneTile.y + laneTile.height * 0.50f;
            float x = boardBounds.x + boardBounds.width * 0.50f;
            animations.draw(
                    batch, FROST_WIND_PAM, "animation", animationTime,
                    x, y, 0.72f, false
            );
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

    private void drawGraveRiseDirt(Batch batch) {
        if (graveRiseTimes.isEmpty()) {
            return;
        }
        batch.begin();
        for (Map.Entry<Position, Float> entry : graveRiseTimes.entrySet()) {
            Rectangle tile = geometry.getTileBounds(entry.getKey().getY(), entry.getKey().getX());
            animations.draw(
                    batch,
                    GRAVE_RISE_DIRT_PAM,
                    "tomb_dirt_anim",
                    entry.getValue(),
                    tile.x + tile.width / 2f,
                    tile.y + tile.height * 0.38f,
                    0.52f,
                    false
            );
        }
        batch.end();
    }

    private void drawLowBeachSpawnRipples(Batch batch) {
        if (lowBeachSpawnTimes.isEmpty()) {
            return;
        }
        batch.begin();
        for (Map.Entry<Position, Float> entry : lowBeachSpawnTimes.entrySet()) {
            Rectangle tile = geometry.getTileBounds(entry.getKey().getY(), entry.getKey().getX());
            animations.draw(
                    batch,
                    LOW_BEACH_RIPPLE_PAM,
                    "ripple",
                    entry.getValue(),
                    tile.x + tile.width / 2f,
                    tile.y + tile.height * 0.42f,
                    0.46f,
                    false
            );
        }
        batch.end();
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
