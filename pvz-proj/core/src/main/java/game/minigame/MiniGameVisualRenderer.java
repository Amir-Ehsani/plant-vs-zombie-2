package game.minigame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import game.animation.core.AnimationDefinition;
import game.animation.core.EntityAnimationProfile;
import game.animation.core.EntityAnimationRegistry;
import game.animation.core.PvzAnimationService;
import game.render.BoardGeometry;
import game.render.BoardRenderer;
import game.render.entity.EntityRenderSystem;
import game.render.mower.LawnMowerRenderSystem;
import game.render.projectile.ProjectileRenderSystem;
import models.core.plant.DefaultPlantRegistry;
import models.core.plant.Plant;
import models.core.plant.PlantType;
import models.engine.board.Board;
import models.engine.board.Lane;
import models.engine.board.Position;
import models.level.core.SeasonType;
import models.minigame.IZombieGame;
import models.minigame.MatchThreeGame;
import models.minigame.MiniGameSession;
import models.minigame.MiniGameType;
import models.minigame.VasebreakerGame;
import models.minigame.WallNutBowlingGame;
import models.minigame.ZombotanyGame;
import ui.SeedPacketCatalog;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class MiniGameVisualRenderer {
    private static final float BOARD_LEFT_RATIO = 315f / 1280f;
    private static final float BOARD_WIDTH_RATIO = 920f / 1280f;
    private static final Color HOVER_COLOR = new Color(1f, 0.93f, 0.30f, 0.20f);
    private static final Color LINE_COLOR = new Color(0.88f, 0.06f, 0.06f, 0.92f);
    private static final Color PACKET_OUTLINE = new Color(0.20f, 0.14f, 0.06f, 0.90f);
    private static final Color PACKET_FILL = new Color(0.94f, 0.88f, 0.52f, 0.92f);
    private static final Color FALLBACK_BRAIN = new Color(0.93f, 0.42f, 0.62f, 0.95f);
    private static final Color NUT_CARD = new Color(0.88f, 0.84f, 0.68f, 1f);
    private static final Color NUT_CARD_BORDER = new Color(0.18f, 0.34f, 0.15f, 1f);
    private static final Color NUT_SELECTED_BORDER = new Color(0.95f, 0.68f, 0.12f, 1f);
    private static final Color MATCH_SELECTED_COLOR = new Color(1f, 0.72f, 0.12f, 0.38f);
    private static final Color ZOMBOTANY_PEA_FALLBACK = new Color(0.35f, 0.75f, 0.18f, 1f);

    private static final String WALLNUT_PAM = "768/INITIAL/PLANT/WALLNUT/WALLNUT.PAM";
    private static final String EXPLODE_O_NUT_PAM = "768/INITIAL/PLANT/EXPLODEONUT/EXPLODEONUT.PAM";
    private static final String UPGRADE_EFFECT_PAM =
            "768/INITIAL/EFFECTS/COLLECTED_UPGRADE_EFFECT/COLLECTED_UPGRADE_EFFECT.PAM";
    private static final String JALAPENO_FIRE_PAM =
            "768/INITIAL/EFFECTS/JALAPENO_FIRE/JALAPENO_FIRE.PAM";
    private static final float MATCH_UPGRADE_EFFECT_DURATION = 1.15f;
    private static final float CONVEYOR_X = 18f;
    private static final float CONVEYOR_TOP = 710f;
    private static final float CONVEYOR_WIDTH = 122f;
    private static final float CONVEYOR_CARD_WIDTH = 104f;
    private static final float CONVEYOR_CARD_HEIGHT = 70f;
    private static final float CONVEYOR_CARD_GAP = 4f;
    private static final int MAX_CONVEYOR_CARDS = 8;
    private static final float NUT_SPAWN_INTERVAL = 5f;
    private static final float SUN_BASE_SIZE = 52.8f;
    private static final float SUN_FALL_DURATION = 0.55f;
    private static final float SUN_FULL_VISIBLE_TIME = 5f;
    private static final float SUN_FADE_DURATION = 0.5f;
    private static final float BRAIN_FADE_DURATION = 0.45f;

    private final MiniGameSession session;
    private final BoardGeometry geometry;
    private final BoardRenderer boardRenderer;
    private final PvzAnimationService animations;
    private final ui.PvzAnimationService uiAnimations;
    private final EntityRenderSystem entityRenderer;
    private final EntityAnimationRegistry plantAnimationRegistry;
    private final Map<String, EntityAnimationProfile> packetPlantProfiles;
    private final ProjectileRenderSystem projectileRenderer;
    private final LawnMowerRenderSystem mowerRenderer;
    private final float worldHeight;
    private final TextureRegion backgroundLeft;
    private final TextureRegion background;
    private final TextureRegion backgroundRight;
    private final TextureRegion brainImage;
    private final TextureRegion sunImage;
    private final TextureRegion conveyorBelt;
    private final TextureRegion conveyorTop;
    private final TextureRegion conveyorSide;
    private final TextureRegion brownVaseIntact;
    private final TextureRegion brownVaseBroken;
    private final TextureRegion plantVaseIntact;
    private final TextureRegion plantVaseBroken;
    private final TextureRegion gargVaseIntact;
    private final TextureRegion gargVaseBroken;
    private final TextureRegion vaseBreakFlashA;
    private final TextureRegion vaseBreakFlashB;
    private final TextureRegion craterImage;
    private final TextureRegion peaImage;
    private final String peaProjectilePath;
    private final String peaProjectileClip;
    private final Map<Integer, Rectangle> packetBounds;
    private final Map<Integer, Rectangle> sunDropBounds;
    private final List<BrokenVaseVisual> brokenVases;
    private final List<NutCardVisual> nutCards;
    private final Map<String, Integer> observedNutInventory;
    private final Map<Integer, SmoothNutVisual> smoothNuts;
    private final Map<Integer, SmoothPeaVisual> smoothPeas;
    private final Map<Integer, Float> sunVisualAges;
    private final Map<Integer, Float> brainFadeTimes;
    private final Random nutRandom;
    private float backgroundCenterX;
    private float nutSpawnTimer;
    private String selectedNutType;
    private Position selectedMatchTile;
    private String upgradedMatchPlantName;
    private float matchUpgradeEffectTime;

    public MiniGameVisualRenderer(
            MiniGameSession session,
            BoardGeometry geometry,
            PvzAnimationService animations,
            ui.PvzAnimationService uiAnimations,
            float worldHeight
    ) {
        this.session = session;
        this.geometry = geometry;
        this.boardRenderer = new BoardRenderer(geometry);
        this.animations = animations;
        this.uiAnimations = uiAnimations;
        this.entityRenderer = new EntityRenderSystem(
                geometry,
                animations,
                !(session instanceof IZombieGame),
                session instanceof MatchThreeGame
        );
        this.plantAnimationRegistry = new EntityAnimationRegistry(animations.getCatalog());
        this.packetPlantProfiles = new LinkedHashMap<>();
        this.projectileRenderer = new ProjectileRenderSystem(geometry, animations);
        this.mowerRenderer = new LawnMowerRenderSystem(geometry, animations, SeasonType.ANCIENT_EGYPT);
        this.worldHeight = worldHeight;
        String backgroundId = backgroundId(session.getType());
        background = animations.region(backgroundId);
        backgroundLeft = animations.region(backgroundId + "_LEFT");
        backgroundRight = animations.region(backgroundId + "_RIGHT");
        brainImage = animations.region("IMAGE_UI_CALENDAR_TIMER_DECO_BIGBRAINZ");
        sunImage = animations.region("IMAGE_EFFECTS_SUN_SUN_110X110");
        conveyorBelt = animations.region("IMAGE_UI_CONVEYOR_CONVEYOR_BELT");
        conveyorTop = animations.region("IMAGE_UI_CONVEYOR_CONVEYOR_TOP");
        conveyorSide = animations.region("IMAGE_UI_CONVEYOR_CONVEYOR_SIDE");
        brownVaseIntact = animations.region("IMAGE_VASEBREAKER_VASE_BROWN_VASE_BROWN_115X150");
        brownVaseBroken = animations.region("IMAGE_VASEBREAKER_VASE_BROWN_VASE_BROWN_76X59");
        plantVaseIntact = animations.region("IMAGE_UI_VASEBREAKER_ENDLESS_NODE_VASEBREAKER_ENDLESS_NODE_115X150_2");
        plantVaseBroken = animations.region("IMAGE_UI_VASEBREAKER_ENDLESS_NODE_VASEBREAKER_ENDLESS_NODE_138X151");
        gargVaseIntact = animations.region("IMAGE_VASEBREAKER_VASE_GARGANTUAR_VASE_GARGANTUAR_115X150");
        gargVaseBroken = animations.region("IMAGE_VASEBREAKER_VASE_GARGANTUAR_VASE_GARGANTUAR_76X59");
        vaseBreakFlashA = animations.region("IMAGE_VASEBREAKER_VASE_BROWN_VASE_BROWN_293X263");
        vaseBreakFlashB = animations.region("IMAGE_VASEBREAKER_VASE_BROWN_VASE_BROWN_68X51");
        craterImage = animations.region("IMAGE_EFFECTS_CRATER_CRATER_129X131");
        peaImage = animations.region("IMAGE_EFFECTS_T_PEA_PROJECTILE_T_PEA_PROJECTILE_39X36");
        AnimationDefinition peaProjectile = animations.getCatalog().findByName("T_PEA_PROJECTILE", null);
        if (peaProjectile == null) {
            peaProjectilePath = null;
            peaProjectileClip = null;
        } else {
            peaProjectilePath = peaProjectile.getPath();
            peaProjectileClip = peaProjectile.hasClip("animation")
                    ? "animation"
                    : peaProjectile.getClips().isEmpty()
                    ? null
                    : peaProjectile.getClips().iterator().next();
            animations.preload(peaProjectilePath);
        }
        packetBounds = new LinkedHashMap<>();
        sunDropBounds = new LinkedHashMap<>();
        brokenVases = new ArrayList<>();
        nutCards = new ArrayList<>();
        observedNutInventory = new LinkedHashMap<>();
        smoothNuts = new LinkedHashMap<>();
        smoothPeas = new LinkedHashMap<>();
        sunVisualAges = new LinkedHashMap<>();
        brainFadeTimes = new LinkedHashMap<>();
        nutRandom = new Random(13_700L + session.getStage());
        nutSpawnTimer = NUT_SPAWN_INTERVAL;
        selectedNutType = null;
        selectedMatchTile = null;
        upgradedMatchPlantName = null;
        matchUpgradeEffectTime = 0f;
        animations.preload(WALLNUT_PAM);
        animations.preload(EXPLODE_O_NUT_PAM);
        animations.preload(UPGRADE_EFFECT_PAM);
        animations.preload(JALAPENO_FIRE_PAM);
        updateBackgroundLayout();
    }

    public void update(float delta) {
        animations.update();
        Board board = board();
        entityRenderer.update(delta, board);
        projectileRenderer.observe(board, session.getCurrentTick());
        projectileRenderer.update(delta);
        if (matchUpgradeEffectTime > 0f) {
            matchUpgradeEffectTime = Math.max(0f, matchUpgradeEffectTime - Math.max(0f, delta));
            if (matchUpgradeEffectTime == 0f) {
                upgradedMatchPlantName = null;
            }
        }
        if (session instanceof WallNutBowlingGame game) {
            mowerRenderer.update(delta, board);
            updateNutConveyor(delta, game);
            updateSmoothNuts(delta, game);
        } else if (session instanceof IZombieGame game) {
            updateSunVisuals(delta, game);
            updateBrainTransitions(delta, game);
        } else if (session instanceof ZombotanyGame game) {
            mowerRenderer.update(delta, board);
            updateSmoothPeas(delta, game);
            updateZombotanySunVisuals(delta, game);
        }
        Iterator<BrokenVaseVisual> iterator = brokenVases.iterator();
        while (iterator.hasNext()) {
            BrokenVaseVisual visual = iterator.next();
            visual.time += delta;
            if (visual.time >= 0.34f) {
                iterator.remove();
            }
        }
    }

    public void render(Batch batch, ShapeRenderer shapes, float stateTime, Position hoveredTile) {
        drawBackground(batch, shapes);
        if (session instanceof WallNutBowlingGame) {
            drawConveyor(batch, stateTime);
        }
        drawBoardLines(shapes, hoveredTile);
        if (session instanceof MatchThreeGame game) {
            drawMatchThreeCraters(batch, shapes, game);
        }
        if (session instanceof WallNutBowlingGame || session instanceof ZombotanyGame) {
            mowerRenderer.render(batch, board());
        }
        entityRenderer.render(batch, board());
        if (session instanceof ZombotanyGame game) {
            drawZombotanyPlantHeads(batch, game, stateTime);
        }
        if (session instanceof MatchThreeGame game) {
            drawMatchUpgradeEffect(batch, game);
        }
        projectileRenderer.render(batch);
        drawSpecialEntities(batch, shapes, stateTime);
    }

    public void dispose() {
        animations.dispose();
    }

    public Integer findPacketAt(float x, float y) {
        for (Map.Entry<Integer, Rectangle> entry : packetBounds.entrySet()) {
            if (entry.getValue().contains(x, y)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public Integer findSunDropAt(float x, float y) {
        for (Map.Entry<Integer, Rectangle> entry : sunDropBounds.entrySet()) {
            if (entry.getValue().contains(x, y)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public String findWallNutAt(float x, float y) {
        for (NutCardVisual card : nutCards) {
            if (card.bounds().contains(x, y)) {
                return card.type;
            }
        }
        return null;
    }

    public void setSelectedNutType(String selectedNutType) {
        this.selectedNutType = selectedNutType;
    }

    public void setSelectedMatchTile(Position selectedMatchTile) {
        this.selectedMatchTile = selectedMatchTile;
    }

    public void startMatchUpgradeEffect(String plantName) {
        if (plantName == null || plantName.isBlank()) {
            return;
        }
        upgradedMatchPlantName = plantName;
        matchUpgradeEffectTime = MATCH_UPGRADE_EFFECT_DURATION;
    }

    public void renderDraggedPacket(
            Batch batch,
            VasebreakerGame game,
            Integer packetId,
            float x,
            float y,
            float stateTime
    ) {
        if (batch == null || game == null || packetId == null) {
            return;
        }
        for (VasebreakerGame.SeedPacketView packet : game.getSeedPackets()) {
            if (packet.id() != packetId) {
                continue;
            }
            batch.begin();
            batch.setColor(1f, 1f, 1f, 0.94f);
            if (!drawPacketPlant(batch, packet.plantName(), x, y, stateTime, 0.55f)) {
                TextureRegion region = SeedPacketCatalog.region(uiAnimations, packet.plantName());
                if (region != null) {
                    float height = 54f;
                    float width = height * region.getRegionWidth() / (float) region.getRegionHeight();
                    batch.draw(region, x - width / 2f, y - height / 2f, width, height);
                }
            }
            batch.setColor(Color.WHITE);
            batch.end();
            return;
        }
    }

    public void renderDraggedNut(Batch batch, String type, float x, float y, float stateTime) {
        if (batch == null || type == null || type.isBlank()) {
            return;
        }
        String path = type.equals("explosive") ? EXPLODE_O_NUT_PAM : WALLNUT_PAM;
        float scale = type.equals("giant") ? 0.58f : 0.42f;
        batch.begin();
        batch.setColor(1f, 1f, 1f, 0.94f);
        animations.draw(batch, path, "idle", stateTime, x, y, scale, true);
        batch.setColor(Color.WHITE);
        batch.end();
    }

    public void onVaseBroken(Position position, String kind) {
        if (position == null || kind == null) {
            return;
        }
        brokenVases.add(new BrokenVaseVisual(position, kind));
    }

    private void drawBackground(Batch batch, ShapeRenderer shapes) {
        batch.begin();
        boardRenderer.drawBackground(
                batch,
                backgroundLeft,
                background,
                backgroundRight,
                worldHeight,
                backgroundCenterX
        );
        batch.end();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        boardRenderer.drawBoardFill(shapes, background != null);
        shapes.end();
    }

    private void drawBoardLines(ShapeRenderer shapes, Position hoveredTile) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        if (selectedMatchTile != null) {
            Rectangle tile = geometry.getTileBounds(selectedMatchTile.getY(), selectedMatchTile.getX());
            shapes.setColor(MATCH_SELECTED_COLOR);
            shapes.rect(tile.x, tile.y, tile.width, tile.height);
        }
        if (hoveredTile != null) {
            Rectangle tile = geometry.getTileBounds(hoveredTile.getY(), hoveredTile.getX());
            shapes.setColor(HOVER_COLOR);
            shapes.rect(tile.x, tile.y, tile.width, tile.height);
        }
        drawModeLine(shapes);
        shapes.end();

    }

    private void drawModeLine(ShapeRenderer shapes) {
        int column = 0;
        if (session instanceof WallNutBowlingGame game) {
            column = game.getRedLineColumn();
        } else if (session instanceof IZombieGame game) {
            column = game.getRedLineColumn();
        }
        if (column <= 0) {
            return;
        }
        Rectangle tile = geometry.getTileBounds(1, column);
        Rectangle boardBounds = geometry.getBoardBounds();
        shapes.setColor(LINE_COLOR);
        shapes.rect(tile.x + tile.width - 2f, boardBounds.y, 4f, boardBounds.height);
    }

    private void drawSpecialEntities(Batch batch, ShapeRenderer shapes, float stateTime) {
        if (session instanceof VasebreakerGame game) {
            drawVases(batch, game);
            drawBrokenVases(batch);
            drawSeedPackets(batch, shapes, game, stateTime);
            return;
        }
        if (session instanceof WallNutBowlingGame game) {
            drawBowlingNuts(batch, shapes, game, stateTime);
            drawNutConveyorCards(batch, shapes, stateTime);
            return;
        }
        if (session instanceof IZombieGame game) {
            drawBrains(batch, shapes, game);
            drawSunDrops(batch, game, stateTime);
            return;
        }
        if (session instanceof ZombotanyGame game) {
            drawZombotanyLaneFire(batch, game, stateTime);
            drawZombotanyPeas(batch, shapes, game);
            drawZombotanySunDrops(batch, game, stateTime);
        }
    }

    private void drawMatchThreeCraters(Batch batch, ShapeRenderer shapes, MatchThreeGame game) {
        if (craterImage == null) {
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            shapes.setColor(new Color(0.28f, 0.16f, 0.08f, 0.88f));
            for (Position position : game.getCraters()) {
                Rectangle tile = geometry.getTileBounds(position.getY(), position.getX());
                shapes.ellipse(
                        tile.x + tile.width * 0.12f,
                        tile.y + tile.height * 0.20f,
                        tile.width * 0.76f,
                        tile.height * 0.50f
                );
            }
            shapes.end();
            return;
        }
        batch.begin();
        for (Position position : game.getCraters()) {
            Rectangle tile = geometry.getTileBounds(position.getY(), position.getX());
            float height = tile.height * 0.76f;
            float width = height * craterImage.getRegionWidth() / craterImage.getRegionHeight();
            batch.draw(
                    craterImage,
                    tile.x + (tile.width - width) / 2f,
                    tile.y + tile.height * 0.08f,
                    width,
                    height
            );
        }
        batch.end();
    }



    private void drawMatchUpgradeEffect(Batch batch, MatchThreeGame game) {
        if (matchUpgradeEffectTime <= 0f || upgradedMatchPlantName == null) {
            return;
        }
        float elapsed = MATCH_UPGRADE_EFFECT_DURATION - matchUpgradeEffectTime;
        float scale = 0.48f + 0.08f * (float) Math.sin(elapsed * 12f);
        batch.begin();
        for (Plant plant : game.getBoard().getAllPlants()) {
            if (plant == null || !plant.isAlive()
                    || !upgradedMatchPlantName.equalsIgnoreCase(plant.getName())) {
                continue;
            }
            Vector2 position = geometry.entityToScreen(plant.getX(), plant.getY());
            animations.draw(batch, UPGRADE_EFFECT_PAM, "idle", elapsed,
                    position.x, position.y, scale, false);
        }
        batch.end();
    }

    private void drawZombotanyPlantHeads(Batch batch, ZombotanyGame game, float stateTime) {
        batch.begin();
        for (ZombotanyGame.PlantZombieView view : game.getPlantZombies()) {
            String plantName = zombotanyPlantName(view.kind());
            PlantType type = DefaultPlantRegistry.getInstance().getByName(plantName);
            if (type == null) {
                continue;
            }
            EntityAnimationProfile profile = packetPlantProfiles.computeIfAbsent(
                    "zombotany:" + plantName, ignored -> plantAnimationRegistry.forPlantType(type));
            if (profile == null) {
                continue;
            }
            animations.preload(profile.getPath());
            Vector2 position = geometry.entityToScreen(view.zombie().getX(), view.zombie().getY());
            position.y += geometry.getTileHeight() * 0.43f;
            String clip = profile.firstClip("idle", "idle2");
            if (clip != null) {
                animations.draw(batch, profile.getPath(), clip, stateTime,
                        position.x, position.y, profile.getScale() * 0.60f, true);
            }
        }
        batch.end();
    }

    private String zombotanyPlantName(String kind) {
        String normalized = kind == null ? "" : kind.toLowerCase(Locale.ROOT);
        if (normalized.contains("wall")) {
            return "Wall-nut";
        }
        if (normalized.contains("jalapeno")) {
            return "Jalapeno";
        }
        if (normalized.contains("squash")) {
            return "Squash";
        }
        return "Peashooter";
    }

    private void drawZombotanyLaneFire(Batch batch, ZombotanyGame game, float stateTime) {
        if (game.getBurningLanes().isEmpty()) {
            return;
        }
        batch.begin();
        for (Integer lane : game.getBurningLanes()) {
            if (lane == null || lane < 1 || lane > BoardGeometry.ROWS) {
                continue;
            }
            for (int column = 1; column <= BoardGeometry.COLUMNS; column++) {
                Vector2 center = geometry.boardToScreen(lane, column);
                animations.draw(
                        batch,
                        JALAPENO_FIRE_PAM,
                        "idle2",
                        stateTime,
                        center.x,
                        center.y,
                        0.46f,
                        true
                );
            }
        }
        batch.end();
    }

    private void drawZombotanyPeas(Batch batch, ShapeRenderer shapes, ZombotanyGame game) {
        if (peaProjectilePath != null && peaProjectileClip != null) {
            batch.begin();
            for (SmoothPeaVisual pea : smoothPeas.values()) {
                Vector2 position = geometry.entityToScreen(pea.x, pea.y);
                position.y += geometry.getTileHeight() * 0.48f;
                animations.draw(
                        batch, peaProjectilePath, peaProjectileClip, pea.age,
                        position.x, position.y, 0.56f, true
                );
            }
            batch.end();
            return;
        }
        if (peaImage != null) {
            batch.begin();
            for (SmoothPeaVisual pea : smoothPeas.values()) {
                Vector2 position = geometry.entityToScreen(pea.x, pea.y);
                position.y += geometry.getTileHeight() * 0.48f;
                float height = 20f;
                float width = height * peaImage.getRegionWidth() / peaImage.getRegionHeight();
                batch.draw(peaImage, position.x - width / 2f, position.y - height / 2f, width, height);
            }
            batch.end();
            return;
        }
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(ZOMBOTANY_PEA_FALLBACK);
        for (SmoothPeaVisual pea : smoothPeas.values()) {
            Vector2 position = geometry.entityToScreen(pea.x, pea.y);
            position.y += geometry.getTileHeight() * 0.48f;
            shapes.circle(position.x, position.y, 9f);
        }
        shapes.end();
    }

    private void updateSmoothPeas(float delta, ZombotanyGame game) {
        Set<Integer> activeIds = new HashSet<>();
        for (ZombotanyGame.PeaShotView shot : game.getPeaShots()) {
            activeIds.add(shot.id());
            smoothPeas.computeIfAbsent(shot.id(), ignored -> new SmoothPeaVisual(shot));
        }

        float safeDelta = Math.max(0f, delta);
        Iterator<Map.Entry<Integer, SmoothPeaVisual>> iterator = smoothPeas.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, SmoothPeaVisual> entry = iterator.next();
            SmoothPeaVisual pea = entry.getValue();
            pea.update(safeDelta);
            if (!activeIds.contains(entry.getKey()) && pea.isFinished()) {
                iterator.remove();
            }
        }
    }

    private void drawVases(Batch batch, VasebreakerGame game) {
        batch.begin();
        for (VasebreakerGame.VaseView vase : game.getVases()) {
            TextureRegion region = intactVaseRegion(vase.kind());
            if (region == null) {
                continue;
            }
            Rectangle tile = geometry.getTileBounds(vase.position().getY(), vase.position().getX());
            float height = tile.height * 0.86f;
            float width = height * region.getRegionWidth() / region.getRegionHeight();
            batch.draw(region, tile.x + (tile.width - width) / 2f, tile.y + tile.height * 0.10f, width, height);
        }
        batch.end();
    }

    private void drawBrokenVases(Batch batch) {
        batch.begin();
        for (BrokenVaseVisual visual : brokenVases) {
            Rectangle tile = geometry.getTileBounds(visual.position.getY(), visual.position.getX());
            TextureRegion broken = brokenVaseRegion(visual.kind);
            if (broken != null && visual.time <= 0.24f) {
                float alpha = visual.time <= 0.16f ? 1f : 1f - (visual.time - 0.16f) / 0.08f;
                batch.setColor(1f, 1f, 1f, Math.max(0f, alpha));
                float height = tile.height * 0.42f;
                float width = height * broken.getRegionWidth() / broken.getRegionHeight();
                batch.draw(broken, tile.x + (tile.width - width) / 2f, tile.y + tile.height * 0.08f, width, height);
                batch.setColor(Color.WHITE);
            }
            drawVaseFlash(batch, tile, visual.time);
        }
        batch.end();
    }

    private void drawVaseFlash(Batch batch, Rectangle tile, float time) {
        TextureRegion region = time <= 0.10f ? vaseBreakFlashA : vaseBreakFlashB;
        if (region == null || time >= 0.20f) {
            return;
        }
        float progress = time <= 0.10f ? time / 0.10f : (time - 0.10f) / 0.10f;
        float scale = time <= 0.10f ? 0.28f + progress * 0.82f : 1.10f + progress * 0.18f;
        float alpha = time <= 0.10f ? 1f : 1f - progress;
        float base = tile.height * 0.92f * scale;
        float width = base * region.getRegionWidth() / region.getRegionHeight();
        batch.setColor(1f, 1f, 1f, Math.max(0f, alpha));
        batch.draw(region, tile.x + (tile.width - width) / 2f, tile.y + (tile.height - base) / 2f, width, base);
        batch.setColor(Color.WHITE);
    }

    private void drawSeedPackets(
            Batch batch,
            ShapeRenderer shapes,
            VasebreakerGame game,
            float stateTime
    ) {
        packetBounds.clear();
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (VasebreakerGame.SeedPacketView packet : game.getSeedPackets()) {
            Rectangle rect = packetRect(packet.dropPosition(), packet.id());
            packetBounds.put(packet.id(), rect);
            shapes.setColor(PACKET_OUTLINE);
            shapes.rect(rect.x, rect.y, rect.width, rect.height);
            shapes.setColor(PACKET_FILL);
            shapes.rect(rect.x + 2f, rect.y + 2f, rect.width - 4f, rect.height - 4f);
        }
        shapes.end();
        batch.begin();
        for (VasebreakerGame.SeedPacketView packet : game.getSeedPackets()) {
            Rectangle rect = packetBounds.get(packet.id());
            float centerX = rect.x + rect.width / 2f;
            float centerY = rect.y + rect.height / 2f + 2f;
            if (!drawPacketPlant(batch, packet.plantName(), centerX, centerY, stateTime, 0.36f)) {
                TextureRegion region = SeedPacketCatalog.region(uiAnimations, packet.plantName());
                if (region != null) {
                    float maxWidth = rect.width - 20f;
                    float maxHeight = rect.height - 20f;
                    float scale = Math.min(maxWidth / region.getRegionWidth(), maxHeight / region.getRegionHeight());
                    float width = region.getRegionWidth() * scale;
                    float height = region.getRegionHeight() * scale;
                    batch.draw(
                            region,
                            rect.x + (rect.width - width) / 2f,
                            rect.y + (rect.height - height) / 2f + 2f,
                            width,
                            height
                    );
                }
            }
        }
        batch.end();
    }

    private boolean drawPacketPlant(
            Batch batch,
            String plantName,
            float x,
            float y,
            float stateTime,
            float scaleMultiplier
    ) {
        EntityAnimationProfile profile = packetPlantProfile(plantName);
        if (profile == null) {
            return false;
        }
        String clip = profile.firstClip("idle", "idle2", "loop", "animation", "play");
        if (clip == null) {
            return false;
        }
        return animations.draw(
                batch,
                profile.getPath(),
                clip,
                stateTime,
                x,
                y,
                profile.getScale() * scaleMultiplier,
                true
        );
    }

    private EntityAnimationProfile packetPlantProfile(String plantName) {
        if (plantName == null || plantName.isBlank()) {
            return null;
        }
        EntityAnimationProfile cached = packetPlantProfiles.get(plantName);
        if (cached != null) {
            return cached;
        }
        PlantType type = DefaultPlantRegistry.getInstance().getByName(plantName);
        EntityAnimationProfile profile = plantAnimationRegistry.forPlantType(type);
        if (profile != null) {
            animations.preload(profile.getPath());
            packetPlantProfiles.put(plantName, profile);
        }
        return profile;
    }

    private Rectangle packetRect(Position position, int packetId) {
        Rectangle tile = geometry.getTileBounds(position.getY(), position.getX());
        int index = Math.max(0, (packetId - 1) % 3);
        float width = tile.width * 0.48f;
        float height = tile.height * 0.42f;
        float x = tile.x + tile.width * 0.48f + index * 6f - width * 0.5f;
        float y = tile.y + tile.height * 0.08f + index * 3f;
        return new Rectangle(x, y, width, height);
    }

    private void drawBowlingNuts(Batch batch, ShapeRenderer shapes, WallNutBowlingGame game, float stateTime) {
        batch.begin();
        boolean missingAny = false;
        for (WallNutBowlingGame.BowlingNutView nut : game.getActiveNuts()) {
            if (!drawNut(batch, nut, stateTime)) {
                missingAny = true;
            }
        }
        batch.end();
        if (missingAny) {
            drawMissingNutFallbacks(shapes, game);
        }
    }

    private boolean drawNut(Batch batch, WallNutBowlingGame.BowlingNutView nut, float stateTime) {
        SmoothNutVisual smooth = smoothNuts.get(nut.id());
        double visualX = smooth == null ? nut.x() : smooth.x;
        double visualY = smooth == null ? nut.y() : smooth.y;
        String path = nut.type().equals("explosive") ? EXPLODE_O_NUT_PAM : WALLNUT_PAM;
        Vector2 position = geometry.entityToScreen(visualX, visualY);
        float scale = nut.type().equals("giant") ? 0.72f : 0.48f;
        float rotation = (float) ((visualX - 1.0) * -180.0);
        Matrix4 original = new Matrix4(batch.getTransformMatrix());
        Matrix4 rotated = new Matrix4(original);
        rotated.translate(position.x, position.y, 0f)
                .rotate(0f, 0f, 1f, rotation)
                .translate(-position.x, -position.y, 0f);
        batch.setTransformMatrix(rotated);
        boolean drawn = animations.draw(batch, path, "idle", stateTime, position.x, position.y, scale, true);
        batch.setTransformMatrix(original);
        return drawn;
    }

    private void drawMissingNutFallbacks(ShapeRenderer shapes, WallNutBowlingGame game) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (WallNutBowlingGame.BowlingNutView nut : game.getActiveNuts()) {
            SmoothNutVisual smooth = smoothNuts.get(nut.id());
            double visualX = smooth == null ? nut.x() : smooth.x;
            double visualY = smooth == null ? nut.y() : smooth.y;
            Vector2 position = geometry.entityToScreen(visualX, visualY);
            shapes.setColor(nut.type().equals("explosive") ? Color.RED : new Color(0.58f, 0.36f, 0.12f, 1f));
            shapes.circle(position.x, position.y, nut.type().equals("giant") ? 34f : 24f);
        }
        shapes.end();
    }

    private void drawConveyor(Batch batch, float stateTime) {
        if (conveyorBelt == null) {
            return;
        }
        float segmentHeight = scaledHeight(conveyorBelt, CONVEYOR_WIDTH);
        float topHeight = conveyorTop == null ? 0f : scaledHeight(conveyorTop, CONVEYOR_WIDTH);
        float offset = (stateTime * 42f) % segmentHeight;
        float totalHeight = CONVEYOR_TOP + topHeight;
        batch.begin();
        for (float y = -segmentHeight + offset; y < totalHeight; y += segmentHeight) {
            batch.draw(conveyorBelt, CONVEYOR_X, y, CONVEYOR_WIDTH, segmentHeight);
        }
        if (conveyorTop != null) {
            batch.draw(conveyorTop, CONVEYOR_X, CONVEYOR_TOP + 4f, CONVEYOR_WIDTH, topHeight);
        }
        if (conveyorSide != null) {
            float sideHeight = totalHeight + 4f;
            float sideWidth = conveyorSide.getRegionWidth() * sideHeight / conveyorSide.getRegionHeight();
            batch.draw(conveyorSide, CONVEYOR_X + CONVEYOR_WIDTH - sideWidth + 6f, 0f, sideWidth, sideHeight);
        }
        batch.end();
    }

    private void drawNutConveyorCards(Batch batch, ShapeRenderer shapes, float stateTime) {
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (NutCardVisual card : nutCards) {
            Rectangle bounds = card.bounds();
            shapes.setColor(card.type.equals(selectedNutType) ? NUT_SELECTED_BORDER : NUT_CARD_BORDER);
            shapes.rect(bounds.x, bounds.y, bounds.width, bounds.height);
            shapes.setColor(NUT_CARD);
            shapes.rect(bounds.x + 2f, bounds.y + 2f, bounds.width - 4f, bounds.height - 4f);
        }
        shapes.end();
        batch.begin();
        for (NutCardVisual card : nutCards) {
            Rectangle bounds = card.bounds();
            String path = card.type.equals("explosive") ? EXPLODE_O_NUT_PAM : WALLNUT_PAM;
            float scale = card.type.equals("giant") ? 0.52f : 0.38f;
            animations.draw(
                    batch,
                    path,
                    "idle",
                    stateTime,
                    bounds.x + bounds.width * 0.50f,
                    bounds.y + bounds.height * 0.50f,
                    scale,
                    true
            );
        }
        batch.end();
    }

    private void updateNutConveyor(float delta, WallNutBowlingGame game) {
        Map<String, Integer> inventory = game.getInventory();
        if (observedNutInventory.isEmpty()) {
            observedNutInventory.putAll(inventory);
        } else {
            for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
                int previous = observedNutInventory.getOrDefault(entry.getKey(), entry.getValue());
                int removed = Math.max(0, previous - entry.getValue());
                while (removed-- > 0) {
                    removeFirstNutCard(entry.getKey());
                }
                observedNutInventory.put(entry.getKey(), entry.getValue());
            }
        }

        if (nutCards.size() < MAX_CONVEYOR_CARDS) {
            nutSpawnTimer -= delta;
            if (nutSpawnTimer <= 0f) {
                String type = chooseAvailableNutType(inventory);
                if (type != null) {
                    nutCards.add(new NutCardVisual(type, -CONVEYOR_CARD_HEIGHT - 20f));
                }
                nutSpawnTimer = NUT_SPAWN_INTERVAL;
            }
        }

        float alpha = 1f - (float) Math.exp(-10f * Math.max(0f, delta));
        for (int index = 0; index < nutCards.size(); index++) {
            NutCardVisual card = nutCards.get(index);
            float targetY = CONVEYOR_TOP - CONVEYOR_CARD_HEIGHT - 14f
                    - index * (CONVEYOR_CARD_HEIGHT + CONVEYOR_CARD_GAP);
            card.y = MathUtils.lerp(card.y, targetY, alpha);
            if (Math.abs(card.y - targetY) < 0.4f) {
                card.y = targetY;
            }
        }
    }

    private String chooseAvailableNutType(Map<String, Integer> inventory) {
        List<String> choices = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
            int displayed = displayedNutCount(entry.getKey());
            int remaining = Math.max(0, entry.getValue() - displayed);
            for (int index = 0; index < remaining; index++) {
                choices.add(entry.getKey());
            }
        }
        if (choices.isEmpty()) {
            return null;
        }
        return choices.get(nutRandom.nextInt(choices.size()));
    }

    private int displayedNutCount(String type) {
        int count = 0;
        for (NutCardVisual card : nutCards) {
            if (card.type.equals(type)) {
                count++;
            }
        }
        return count;
    }

    private void removeFirstNutCard(String type) {
        for (int index = 0; index < nutCards.size(); index++) {
            if (nutCards.get(index).type.equals(type)) {
                nutCards.remove(index);
                return;
            }
        }
    }

    private void updateSmoothNuts(float delta, WallNutBowlingGame game) {
        Set<Integer> activeIds = new HashSet<>();
        float alpha = 1f - (float) Math.exp(-15f * Math.max(0f, delta));
        for (WallNutBowlingGame.BowlingNutView nut : game.getActiveNuts()) {
            activeIds.add(nut.id());
            SmoothNutVisual visual = smoothNuts.get(nut.id());
            if (visual == null) {
                smoothNuts.put(nut.id(), new SmoothNutVisual(nut.x(), nut.y()));
                continue;
            }
            visual.x += (nut.x() - visual.x) * alpha;
            visual.y += (nut.y() - visual.y) * alpha;
        }
        smoothNuts.keySet().removeIf(id -> !activeIds.contains(id));
    }

    private void drawBrains(Batch batch, ShapeRenderer shapes, IZombieGame game) {
        if (brainImage == null) {
            shapes.begin(ShapeRenderer.ShapeType.Filled);
            for (Lane lane : game.getBoard().getLanes()) {
                float alpha = brainAlpha(lane);
                if (alpha <= 0f) {
                    continue;
                }
                Rectangle tile = geometry.getTileBounds(lane.getLaneId(), 1);
                float progress = brainFadeProgress(lane);
                float scale = 1f + progress * 0.14f;
                float width = 46f * scale;
                float height = 30f * scale;
                shapes.setColor(new Color(0.93f, 0.42f, 0.62f, alpha));
                shapes.ellipse(
                        tile.x - 34f - (width - 46f) / 2f,
                        tile.y + tile.height * 0.34f - (height - 30f) / 2f,
                        width,
                        height
                );
            }
            shapes.end();
            return;
        }
        batch.begin();
        for (Lane lane : game.getBoard().getLanes()) {
            float alpha = brainAlpha(lane);
            if (alpha <= 0f) {
                continue;
            }
            Rectangle tile = geometry.getTileBounds(lane.getLaneId(), 1);
            float progress = brainFadeProgress(lane);
            float height = tile.height * 0.64f * (1f + progress * 0.14f);
            float width = height * brainImage.getRegionWidth() / brainImage.getRegionHeight();
            batch.setColor(1f, 1f, 1f, alpha);
            batch.draw(
                    brainImage,
                    tile.x - width * 0.90f,
                    tile.y + (tile.height - height) / 2f,
                    width,
                    height
            );
        }
        batch.setColor(Color.WHITE);
        batch.end();
    }

    private float brainAlpha(Lane lane) {
        if (!lane.hasBrainBeenEaten()) {
            return 1f;
        }
        Float fadeTime = brainFadeTimes.get(lane.getLaneId());
        if (fadeTime == null) {
            return 0f;
        }
        return 1f - MathUtils.clamp(fadeTime / BRAIN_FADE_DURATION, 0f, 1f);
    }

    private float brainFadeProgress(Lane lane) {
        if (!lane.hasBrainBeenEaten()) {
            return 0f;
        }
        return MathUtils.clamp(
                brainFadeTimes.getOrDefault(lane.getLaneId(), BRAIN_FADE_DURATION) / BRAIN_FADE_DURATION,
                0f,
                1f
        );
    }

    private void drawSunDrops(Batch batch, IZombieGame game, float stateTime) {
        sunDropBounds.clear();
        if (sunImage == null) {
            return;
        }
        batch.begin();
        for (IZombieGame.SunDropView drop : game.getSunDrops()) {
            Vector2 basePosition = geometry.entityToScreen(drop.x(), drop.y());
            float age = sunVisualAges.getOrDefault(drop.id(), 0f);
            float fallProgress = MathUtils.clamp(age / SUN_FALL_DURATION, 0f, 1f);
            float fallOffset = 92f * (1f - fallProgress) * (1f - fallProgress);
            float bounce = fallProgress >= 0.78f && fallProgress < 1f
                    ? 5f * (float) Math.sin((fallProgress - 0.78f) / 0.22f * Math.PI)
                    : 0f;
            float alpha = age <= SUN_FULL_VISIBLE_TIME
                    ? 1f
                    : 1f - MathUtils.clamp(
                            (age - SUN_FULL_VISIBLE_TIME) / SUN_FADE_DURATION,
                            0f,
                            1f
                    );
            float pulse = 1f + 0.05f * (float) Math.sin(stateTime * 7f + drop.id());
            float size = SUN_BASE_SIZE * pulse;
            float x = basePosition.x - size / 2f;
            float y = basePosition.y + fallOffset - bounce - size / 2f;
            Rectangle bounds = new Rectangle(x, y, size, size);
            sunDropBounds.put(drop.id(), bounds);
            batch.setColor(1f, 1f, 1f, alpha);
            batch.draw(sunImage, bounds.x, bounds.y, bounds.width, bounds.height);
        }
        batch.setColor(Color.WHITE);
        batch.end();
    }

    private void drawZombotanySunDrops(Batch batch, ZombotanyGame game, float stateTime) {
        sunDropBounds.clear();
        if (sunImage == null) {
            return;
        }
        batch.begin();
        for (ZombotanyGame.SunDropView drop : game.getSunDrops()) {
            Vector2 basePosition = geometry.entityToScreen(drop.x(), drop.y());
            float age = sunVisualAges.getOrDefault(drop.id(), 0f);
            float fallProgress = MathUtils.clamp(age / SUN_FALL_DURATION, 0f, 1f);
            float fallOffset = 210f * (1f - fallProgress) * (1f - fallProgress);
            float bounce = fallProgress >= 0.78f && fallProgress < 1f
                    ? 5f * (float) Math.sin((fallProgress - 0.78f) / 0.22f * Math.PI)
                    : 0f;
            float pulse = 1f + 0.05f * (float) Math.sin(stateTime * 7f + drop.id());
            float size = SUN_BASE_SIZE * pulse;
            float x = basePosition.x - size / 2f;
            float y = basePosition.y + fallOffset - bounce - size / 2f;
            Rectangle bounds = new Rectangle(x, y, size, size);
            sunDropBounds.put(drop.id(), bounds);
            batch.setColor(Color.WHITE);
            batch.draw(sunImage, bounds.x, bounds.y, bounds.width, bounds.height);
        }
        batch.setColor(Color.WHITE);
        batch.end();
    }

    private void updateSunVisuals(float delta, IZombieGame game) {
        Set<Integer> activeIds = new HashSet<>();
        for (IZombieGame.SunDropView drop : game.getSunDrops()) {
            activeIds.add(drop.id());
            sunVisualAges.put(drop.id(), sunVisualAges.getOrDefault(drop.id(), 0f) + Math.max(0f, delta));
        }
        sunVisualAges.keySet().removeIf(id -> !activeIds.contains(id));
    }

    private void updateZombotanySunVisuals(float delta, ZombotanyGame game) {
        Set<Integer> activeIds = new HashSet<>();
        for (ZombotanyGame.SunDropView drop : game.getSunDrops()) {
            activeIds.add(drop.id());
            sunVisualAges.put(drop.id(), sunVisualAges.getOrDefault(drop.id(), 0f) + Math.max(0f, delta));
        }
        sunVisualAges.keySet().removeIf(id -> !activeIds.contains(id));
    }

    private void updateBrainTransitions(float delta, IZombieGame game) {
        for (Lane lane : game.getBoard().getLanes()) {
            if (!lane.hasBrainBeenEaten()) {
                continue;
            }
            float time = brainFadeTimes.getOrDefault(lane.getLaneId(), 0f);
            brainFadeTimes.put(
                    lane.getLaneId(),
                    Math.min(BRAIN_FADE_DURATION, time + Math.max(0f, delta))
            );
        }
    }

    private TextureRegion intactVaseRegion(String kind) {
        String normalized = normalize(kind);
        if (normalized.equals("PLANT")) {
            return plantVaseIntact;
        }
        if (normalized.equals("GARGANTUAR")) {
            return gargVaseIntact;
        }
        return brownVaseIntact;
    }

    private TextureRegion brokenVaseRegion(String kind) {
        String normalized = normalize(kind);
        if (normalized.equals("PLANT")) {
            return plantVaseBroken;
        }
        if (normalized.equals("GARGANTUAR")) {
            return gargVaseBroken;
        }
        return brownVaseBroken;
    }

    private void updateBackgroundLayout() {
        if (background == null) {
            backgroundCenterX = 0f;
            return;
        }
        float centerWidth = BoardRenderer.scaledWidth(background, worldHeight);
        backgroundCenterX = geometry.getBoardBounds().x - centerWidth * BOARD_LEFT_RATIO;
        Rectangle board = geometry.getBoardBounds();
        geometry.setBounds(board.x, board.y, centerWidth * BOARD_WIDTH_RATIO, board.height);
    }

    private Board board() {
        if (session instanceof VasebreakerGame game) {
            return game.getBoard();
        }
        if (session instanceof WallNutBowlingGame game) {
            return game.getBoard();
        }
        if (session instanceof IZombieGame game) {
            return game.getBoard();
        }
        if (session instanceof MatchThreeGame game) {
            return game.getBoard();
        }
        return ((ZombotanyGame) session).getBoard();
    }

    private String backgroundId(MiniGameType type) {
        if (type == MiniGameType.VASEBREAKER) {
            return "IMAGE_BACKGROUNDS_BACKGROUND_LOD_BIRTHDAY_TEXTURE";
        }
        if (type == MiniGameType.I_ZOMBIE) {
            return "IMAGE_BACKGROUNDS_BACKGROUND_LOD_BIGBRAINZ_TEXTURE";
        }
        if (type == MiniGameType.MATCH_THREE) {
            return "IMAGE_BACKGROUNDS_BACKGROUND_LOD_SUMMERNIGHTS_TEXTURE";
        }
        if (type == MiniGameType.PLANT_ZOMBIES) {
            return "IMAGE_BACKGROUNDS_EGYPT_TEXTURE";
        }
        return "IMAGE_BACKGROUNDS_FRONTLAWN_TEXTURE";
    }

    private float scaledHeight(TextureRegion region, float targetWidth) {
        return region.getRegionHeight() * targetWidth / region.getRegionWidth();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static final class BrokenVaseVisual {
        private final Position position;
        private final String kind;
        private float time;

        private BrokenVaseVisual(Position position, String kind) {
            this.position = position;
            this.kind = kind;
            this.time = 0f;
        }
    }

    private static final class NutCardVisual {
        private final String type;
        private float y;

        private NutCardVisual(String type, float y) {
            this.type = type;
            this.y = y;
        }

        private Rectangle bounds() {
            float x = CONVEYOR_X + (CONVEYOR_WIDTH - CONVEYOR_CARD_WIDTH) / 2f - 2f;
            return new Rectangle(x, y, CONVEYOR_CARD_WIDTH, CONVEYOR_CARD_HEIGHT);
        }
    }

    private static final class SmoothPeaVisual {
        private static final float TILES_PER_SECOND = 8.5f;
        private static final float MIN_TRAVEL_SECONDS = 0.20f;
        private static final float MAX_TRAVEL_SECONDS = 0.90f;

        private final double startX;
        private final double targetX;
        private final double y;
        private final float duration;
        private float age;
        private double x;

        private SmoothPeaVisual(ZombotanyGame.PeaShotView shot) {
            startX = shot.startX();
            targetX = shot.targetX();
            y = shot.y();
            float distance = Math.max(0.5f, Math.abs((float) (targetX - startX)));
            duration = MathUtils.clamp(
                    distance / TILES_PER_SECOND, MIN_TRAVEL_SECONDS, MAX_TRAVEL_SECONDS
            );
            float modelProgress = 1f - shot.remainingTicks() / (float) Math.max(1, shot.totalTicks());
            age = duration * MathUtils.clamp(modelProgress, 0f, 1f);
            x = startX;
            updatePosition();
        }

        private void update(float delta) {
            age = Math.min(duration, age + delta);
            updatePosition();
        }

        private void updatePosition() {
            float progress = MathUtils.clamp(age / duration, 0f, 1f);
            x = MathUtils.lerp((float) startX, (float) targetX, progress);
        }

        private boolean isFinished() {
            return age >= duration;
        }
    }

    private static final class SmoothNutVisual {
        private double x;
        private double y;

        private SmoothNutVisual(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}
