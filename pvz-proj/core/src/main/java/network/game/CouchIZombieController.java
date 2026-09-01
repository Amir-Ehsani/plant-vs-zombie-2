package network.game;

import network.protocol.GameSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Pure-Java controller for the optional one-device two-player I, Zombie mode.
 *
 * Plants are intended to be driven by mouse input while zombies are driven by keyboard input.
 * The controller contains no LibGDX dependencies so the exact same rules are easy to test.
 */
public final class CouchIZombieController {
    public static final String PLANT_PLAYER = "Couch Plants";
    public static final String ZOMBIE_PLAYER = "Couch Zombies";

    private final AuthoritativeIZombieGame game;
    private final List<String> plantTypes;
    private final List<String> zombieTypes;
    private String selectedPlant;
    private String selectedZombie;
    private int zombieRow;

    public CouchIZombieController() {
        this(1);
    }

    public CouchIZombieController(int stage) {
        game = new AuthoritativeIZombieGame(PLANT_PLAYER, ZOMBIE_PLAYER, stage);
        plantTypes = Collections.unmodifiableList(new ArrayList<>(
                AuthoritativeIZombieGame.plantCostsForStage(stage).keySet()
        ));
        zombieTypes = Collections.unmodifiableList(new ArrayList<>(
                AuthoritativeIZombieGame.zombieCostsForStage(stage).keySet()
        ));
        selectedPlant = plantTypes.isEmpty() ? "" : plantTypes.get(0);
        selectedZombie = zombieTypes.isEmpty() ? "" : zombieTypes.get(0);
        zombieRow = AuthoritativeIZombieGame.ROWS / 2;
        game.useFullCatalogLoadout();
    }

    public AuthoritativeIZombieGame getGame() { return game; }
    public List<String> getPlantTypes() { return plantTypes; }
    public List<String> getZombieTypes() { return zombieTypes; }
    public String getSelectedPlant() { return selectedPlant; }
    public String getSelectedZombie() { return selectedZombie; }
    public int getZombieRow() { return zombieRow; }
    public int getStage() { return game.getStage(); }
    public boolean isFinished() { return game.isFinished(); }
    public GameSnapshot snapshot() { return game.snapshot(); }

    public boolean selectPlant(String type) {
        String normalized = normalize(type);
        if (!plantTypes.contains(normalized)) {
            return false;
        }
        selectedPlant = normalized;
        return true;
    }

    public boolean selectPlantByIndex(int zeroBasedIndex) {
        if (zeroBasedIndex < 0 || zeroBasedIndex >= plantTypes.size()) {
            return false;
        }
        selectedPlant = plantTypes.get(zeroBasedIndex);
        return true;
    }

    public boolean selectZombie(String type) {
        String normalized = normalize(type);
        if (!zombieTypes.contains(normalized)) {
            return false;
        }
        selectedZombie = normalized;
        return true;
    }

    public boolean selectZombieByIndex(int zeroBasedIndex) {
        if (zeroBasedIndex < 0 || zeroBasedIndex >= zombieTypes.size()) {
            return false;
        }
        selectedZombie = zombieTypes.get(zeroBasedIndex);
        return true;
    }

    public int moveZombieRow(int delta) {
        if (delta == 0) {
            return zombieRow;
        }
        zombieRow = Math.max(0, Math.min(AuthoritativeIZombieGame.ROWS - 1, zombieRow + delta));
        return zombieRow;
    }

    public void setZombieRow(int row) {
        zombieRow = Math.max(0, Math.min(AuthoritativeIZombieGame.ROWS - 1, row));
    }

    public ActionResult placeSelectedPlant(int row, int column) {
        return game.placePlant(PLANT_PLAYER, selectedPlant, row, column);
    }

    public ActionResult releaseSelectedZombie() {
        return game.spawnZombie(ZOMBIE_PLAYER, selectedZombie, zombieRow);
    }

    public void advance(long deltaMillis) {
        game.advance(deltaMillis);
    }

    public int plantCost(String type) {
        return cost(AuthoritativeIZombieGame.plantCostsForStage(getStage()), type);
    }

    public int zombieCost(String type) {
        return cost(AuthoritativeIZombieGame.zombieCostsForStage(getStage()), type);
    }

    public boolean canAffordSelectedPlant() {
        GameSnapshot snapshot = game.snapshot();
        return snapshot.getPlantSun() >= plantCost(selectedPlant);
    }

    public boolean canAffordSelectedZombie() {
        GameSnapshot snapshot = game.snapshot();
        return snapshot.getZombieSun() >= zombieCost(selectedZombie);
    }

    public long selectedPlantCooldownMillis() {
        return game.snapshot().getPlantCooldownMillis(selectedPlant);
    }

    public long selectedZombieCooldownMillis() {
        return game.snapshot().getZombieCooldownMillis(selectedZombie);
    }

    public boolean selectedPlantReady() {
        return !game.isFinished() && canAffordSelectedPlant() && selectedPlantCooldownMillis() <= 0L;
    }

    public boolean selectedZombieReady() {
        return !game.isFinished() && canAffordSelectedZombie() && selectedZombieCooldownMillis() <= 0L;
    }

    private static int cost(Map<String, Integer> costs, String type) {
        if (costs == null) {
            return Integer.MAX_VALUE;
        }
        return costs.getOrDefault(normalize(type), Integer.MAX_VALUE);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }
}
