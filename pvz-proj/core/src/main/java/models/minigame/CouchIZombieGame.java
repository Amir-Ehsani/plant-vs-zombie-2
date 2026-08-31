package models.minigame;

import models.engine.board.Board;
import models.engine.board.Position;
import network.game.ActionResult;
import network.game.AuthoritativeIZombieGame;
import network.game.CouchIZombieController;
import network.protocol.EntityState;
import network.protocol.GameRole;
import network.protocol.GameSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * One-device two-player I, Zombie that reuses the same Ancient Egypt rules and board
 * as the online match. Plants pick a loadout first; then both seed banks share the lawn.
 */
public final class CouchIZombieGame extends IZombieGame implements EgyptIZombieChooser {
    private final AuthoritativeIZombieGame sim;
    private String lastActionMessage;
    private boolean finishApplied;

    public CouchIZombieGame() {
        super(1);
        this.sim = new AuthoritativeIZombieGame(
                CouchIZombieController.PLANT_PLAYER,
                CouchIZombieController.ZOMBIE_PLAYER
        );
        this.lastActionMessage = "Choose plants, then Let's Rock.";
        this.finishApplied = false;
        success(lastActionMessage);
    }

    @Override
    public Board getBoard() {
        return sim.getBoard();
    }

    @Override
    public int getSunAmount() {
        return sim.getPlantSun();
    }

    public int getZombieSunAmount() {
        return sim.getZombieSun();
    }

    public int getPlantSunAmount() {
        return sim.getPlantSun();
    }

    @Override
    public List<ZombieOptionView> getAvailableZombieOptions() {
        List<ZombieOptionView> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : AuthoritativeIZombieGame.zombieCosts().entrySet()) {
            long cooldown = sim.zombieCooldownMillis(entry.getKey());
            int ticks = millisToTicks(cooldown);
            result.add(new ZombieOptionView(
                    NetworkIZombieGame.zombieDisplayName(entry.getKey()),
                    entry.getValue(),
                    ticks,
                    ticks
            ));
        }
        return List.copyOf(result);
    }

    public List<PlantOptionView> getAvailablePlantOptions() {
        if (!sim.isPlantsReady()) {
            return List.of();
        }
        List<PlantOptionView> result = new ArrayList<>();
        GameSnapshot snapshot = sim.snapshot();
        List<String> loadout = snapshot.getPlantLoadout();
        if (loadout.isEmpty()) {
            loadout = new ArrayList<>(AuthoritativeIZombieGame.plantCosts().keySet());
        }
        for (String name : loadout) {
            String type = normalizePlantType(name);
            int cost = AuthoritativeIZombieGame.plantSunCost(type);
            result.add(new PlantOptionView(
                    NetworkIZombieGame.plantDisplayName(type),
                    cost,
                    sim.plantCooldownMillis(type)
            ));
        }
        return List.copyOf(result);
    }

    @Override
    public List<SunDropView> getSunDrops() {
        List<SunDropView> result = new ArrayList<>();
        for (EntityState entity : sim.snapshot().getEntities()) {
            if (entity == null || !"SUN".equals(entity.getCategory())) {
                continue;
            }
            int dropId = parseDropId(entity);
            result.add(new SunDropView(
                    dropId,
                    Math.max(1, entity.getHealth()),
                    entity.getX() + 1.0,
                    entity.getRow() + 1.0,
                    40
            ));
        }
        return List.copyOf(result);
    }

    @Override
    public boolean collectSunDrop(int dropId) {
        GameSnapshot snapshot = sim.snapshot();
        String actor = actorForSun(snapshot, dropId);
        if (actor == null) {
            lastActionMessage = "That sun has already disappeared.";
            fail(lastActionMessage);
            return false;
        }
        ActionResult result = sim.collectSun(actor, dropId);
        lastActionMessage = result.getMessage();
        if (result.wasSuccessful()) {
            success(lastActionMessage);
        } else {
            fail(lastActionMessage);
        }
        return result.wasSuccessful();
    }

    @Override
    public boolean spawnZombie(String zombieName, Position position) {
        if (position == null || position.getX() <= RED_LINE_COLUMN) {
            lastActionMessage = "Release zombies on the right side of the red line.";
            fail(lastActionMessage);
            return false;
        }
        ActionResult result = sim.spawnZombie(
                CouchIZombieController.ZOMBIE_PLAYER,
                normalizeZombieType(zombieName),
                position.getY() - 1
        );
        lastActionMessage = result.getMessage();
        if (result.wasSuccessful()) {
            success(lastActionMessage);
        } else {
            fail(lastActionMessage);
        }
        return result.wasSuccessful();
    }

    public boolean placePlant(String plantName, Position position) {
        if (position == null || position.getX() < 1 || position.getX() > RED_LINE_COLUMN) {
            lastActionMessage = "Plants belong on the left side of the red line.";
            fail(lastActionMessage);
            return false;
        }
        ActionResult result = sim.placePlant(
                CouchIZombieController.PLANT_PLAYER,
                normalizePlantType(plantName),
                position.getY() - 1,
                position.getX() - 1
        );
        lastActionMessage = result.getMessage();
        if (result.wasSuccessful()) {
            success(lastActionMessage);
        } else {
            fail(lastActionMessage);
        }
        return result.wasSuccessful();
    }

    public long getRemainingMillis() {
        return sim.getRemainingMillis();
    }

    public long getCooldownMillis(String type, GameRole role) {
        if (role == GameRole.PLANTS) {
            return sim.plantCooldownMillis(normalizePlantType(type));
        }
        return sim.zombieCooldownMillis(normalizeZombieType(type));
    }

    public int getChoiceCost(String type, GameRole role) {
        String normalized = role == GameRole.PLANTS ? normalizePlantType(type) : normalizeZombieType(type);
        return role == GameRole.PLANTS
                ? AuthoritativeIZombieGame.plantSunCost(normalized)
                : AuthoritativeIZombieGame.zombieCosts().getOrDefault(normalized, Integer.MAX_VALUE);
    }

    public String getActionMessage() {
        return lastActionMessage == null ? "" : lastActionMessage;
    }

    @Override
    public boolean lockSelectedPlants(List<String> plantNames) {
        ActionResult result = sim.lockPlants(CouchIZombieController.PLANT_PLAYER, plantNames);
        lastActionMessage = result.getMessage();
        if (result.wasSuccessful()) {
            success(lastActionMessage);
        } else {
            fail(lastActionMessage);
        }
        return result.wasSuccessful();
    }

    @Override
    public boolean isPlantsReady() {
        return sim.isPlantsReady();
    }

    @Override
    public boolean isChooserBusy() {
        return false;
    }

    @Override
    public String getChooserMessage() {
        return getActionMessage();
    }

    @Override
    public List<String> getEgyptPlantCatalog() {
        List<String> names = new ArrayList<>();
        for (String key : AuthoritativeIZombieGame.plantCosts().keySet()) {
            names.add(NetworkIZombieGame.plantDisplayName(key));
        }
        return List.copyOf(names);
    }

    @Override
    public void pumpChooser() {
        applyFinishIfNeeded();
    }

    @Override
    public void abandonChooser() {
        if (!sim.isFinished()) {
            sim.forceFinish(GameRole.PLANTS, "Couch play cancelled");
        }
        applyFinishIfNeeded();
    }

    @Override
    protected void onTick() {
        sim.advance(100L);
        applyFinishIfNeeded();
    }

    @Override
    protected void evaluateStatus() {
        applyFinishIfNeeded();
    }

    @Override
    public String renderStatus() {
        return "couch plants=" + sim.getPlantSun()
                + " zombies=" + sim.getZombieSun()
                + " remaining=" + sim.getRemainingMillis();
    }

    private void applyFinishIfNeeded() {
        if (finishApplied || !sim.isFinished()) {
            return;
        }
        finishApplied = true;
        String reason = sim.getFinishReason() == null || sim.getFinishReason().isBlank()
                ? "Couch play finished."
                : sim.getFinishReason();
        lastActionMessage = reason;
        if (sim.getWinner() == GameRole.PLANTS) {
            markWon(reason);
        } else {
            markLost(reason);
        }
    }

    private String actorForSun(GameSnapshot snapshot, int dropId) {
        if (snapshot == null) {
            return null;
        }
        for (EntityState entity : snapshot.getEntities()) {
            if (entity == null || !"SUN".equals(entity.getCategory())) {
                continue;
            }
            if (parseDropId(entity) != dropId) {
                continue;
            }
            String owner = entity.getAttribute("owner");
            if (GameRole.ZOMBIES.name().equalsIgnoreCase(owner)) {
                return CouchIZombieController.ZOMBIE_PLAYER;
            }
            return CouchIZombieController.PLANT_PLAYER;
        }
        return null;
    }

    private static int parseDropId(EntityState entity) {
        if (entity == null) {
            return -1;
        }
        try {
            return Integer.parseInt(entity.getAttribute("dropId"));
        } catch (RuntimeException ignored) {
            String id = entity.getId();
            if (id != null && id.startsWith("sun-")) {
                try {
                    return Integer.parseInt(id.substring(4));
                } catch (RuntimeException ignoredAgain) {
                    return -1;
                }
            }
            return -1;
        }
    }

    private static int millisToTicks(long millis) {
        return (int) Math.max(0L, (millis + 99L) / 100L);
    }

    private static String normalizePlantType(String displayName) {
        String normalized = normalizeKey(displayName);
        return switch (normalized) {
            case "WALL_NUT", "WALLNUT" -> "WALL_NUT";
            case "SNOW_PEA" -> "SNOW_PEA";
            case "TALL_NUT", "TALLNUT" -> "TALL_NUT";
            default -> normalized;
        };
    }

    private static String normalizeZombieType(String displayName) {
        String normalized = normalizeKey(displayName);
        return switch (normalized) {
            case "CONE_HEAD", "CONEHEAD" -> "CONE_HEAD";
            case "BUCKET_HEAD", "BUCKETHEAD" -> "BUCKET_HEAD";
            default -> normalized;
        };
    }

    private static String normalizeKey(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }
}
