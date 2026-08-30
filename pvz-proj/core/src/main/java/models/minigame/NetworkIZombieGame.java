package models.minigame;

import models.core.plant.Plant;
import models.core.plant.PlantFactory;
import models.core.projectile.Damage;
import models.core.zombie.Zombie;
import models.core.zombie.ZombieFactory;
import models.engine.board.Board;
import models.engine.board.Lane;
import models.engine.board.Position;
import models.engine.board.Tile;
import models.engine.board.TileType;
import network.client.NetworkManager;
import network.client.NetworkMatchContext;
import network.client.NetworkOperationResult;
import network.game.AuthoritativeIZombieGame;
import network.protocol.EntityState;
import network.protocol.GameRole;
import network.protocol.GameSnapshot;
import network.protocol.MessageType;
import network.protocol.NetworkMessage;
import network.protocol.ReactionCategory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;

/**
 * Network-backed I, Zombie session that deliberately reuses the normal mini-game
 * board/rendering core. The server remains authoritative; this class only mirrors
 * snapshots into the same Board/Plant/Zombie model used by local gameplay.
 */
public final class NetworkIZombieGame extends IZombieGame {
    private final NetworkManager network;
    private final NetworkMatchContext context;
    private final PlantFactory plantFactory;
    private final ZombieFactory zombieFactory;
    private final Map<String, Plant> plantsById;
    private final Map<String, Zombie> zombiesById;
    private final Queue<NetworkMessage> reactionEvents;
    private final List<NetworkProjectileView> projectiles;
    private Board mirrorBoard;
    private GameSnapshot snapshot;
    private long lastSequence;
    private volatile boolean actionInFlight;
    private volatile String networkMessage;
    private boolean finishApplied;

    public NetworkIZombieGame(NetworkManager network, NetworkMatchContext context) {
        super(1); // Internal compatibility only. Online I, Zombie has no user-facing stages.
        if (network == null || context == null) {
            throw new IllegalArgumentException("Online I, Zombie requires a network manager and match context.");
        }
        this.network = network;
        this.context = context;
        this.plantFactory = new PlantFactory();
        this.zombieFactory = new ZombieFactory();
        this.plantsById = new LinkedHashMap<>();
        this.zombiesById = new LinkedHashMap<>();
        this.reactionEvents = new ArrayDeque<>();
        this.projectiles = new ArrayList<>();
        this.mirrorBoard = new Board();
        this.lastSequence = -1L;
        this.actionInFlight = false;
        this.networkMessage = "Waiting for the server...";
        this.finishApplied = false;
        prepareMirrorBoard();
        network.clearMatchEvents();
    }

    @Override
    protected void onTick() {
        // The server advances gameplay time. The local session only drives rendering animations.
    }

    @Override
    protected void evaluateStatus() {
        // Win/loss is applied from authoritative MATCH_FINISHED snapshots.
    }

    @Override
    public Board getBoard() {
        return mirrorBoard;
    }

    @Override
    public int getSunAmount() {
        if (snapshot == null) {
            return 350;
        }
        return context.role() == GameRole.PLANTS ? snapshot.getPlantSun() : snapshot.getZombieSun();
    }

    @Override
    public List<ZombieOptionView> getAvailableZombieOptions() {
        if (context.role() != GameRole.ZOMBIES) {
            return List.of();
        }
        List<ZombieOptionView> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : AuthoritativeIZombieGame.zombieCosts().entrySet()) {
            long cooldown = getCooldownMillis(entry.getKey());
            result.add(new ZombieOptionView(
                    zombieDisplayName(entry.getKey()),
                    entry.getValue(),
                    millisToTicks(cooldown),
                    millisToTicks(cooldown)
            ));
        }
        return List.copyOf(result);
    }

    @Override
    public List<SunDropView> getSunDrops() {
        return List.of();
    }

    @Override
    public boolean collectSunDrop(int dropId) {
        return false;
    }

    @Override
    public boolean spawnZombie(String zombieName, Position position) {
        if (context.role() != GameRole.ZOMBIES) {
            networkMessage = "Only the zombie player can release zombies.";
            return false;
        }
        if (position == null || position.getX() <= RED_LINE_COLUMN) {
            networkMessage = "Release zombies on the right side of the red line.";
            return false;
        }
        String type = normalizeZombieType(zombieName);
        if (!isChoiceUsable(type) || actionInFlight) {
            return false;
        }
        actionInFlight = true;
        networkMessage = "Releasing " + zombieDisplayName(type) + "...";
        network.spawnZombieAsync(context.matchId(), type, position.getY() - 1)
                .whenComplete(this::finishAction);
        return true;
    }

    public boolean placePlant(String plantName, Position position) {
        if (context.role() != GameRole.PLANTS) {
            networkMessage = "Only the plant player can plant.";
            return false;
        }
        if (position == null || position.getX() < 1 || position.getX() > RED_LINE_COLUMN) {
            networkMessage = "Plants belong on the left side of the red line.";
            return false;
        }
        String type = normalizePlantType(plantName);
        if (!isChoiceUsable(type) || actionInFlight) {
            return false;
        }
        actionInFlight = true;
        networkMessage = "Planting " + plantDisplayName(type) + "...";
        network.placePlantAsync(
                context.matchId(), type, position.getY() - 1, position.getX() - 1
        ).whenComplete(this::finishAction);
        return true;
    }

    public List<PlantOptionView> getAvailablePlantOptions() {
        if (context.role() != GameRole.PLANTS) {
            return List.of();
        }
        List<PlantOptionView> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : AuthoritativeIZombieGame.plantCosts().entrySet()) {
            result.add(new PlantOptionView(
                    plantDisplayName(entry.getKey()),
                    entry.getValue(),
                    getCooldownMillis(entry.getKey())
            ));
        }
        return List.copyOf(result);
    }

    public void pumpNetworkEvents() {
        NetworkMessage event;
        while ((event = network.pollMatchEvent()) != null) {
            if (event.getMatchId() != null && !context.matchId().equals(event.getMatchId())) {
                continue;
            }
            if (event.getType() == MessageType.MATCH_SNAPSHOT) {
                applySnapshot(event.getSnapshot());
            } else if (event.getType() == MessageType.REACTION_RECEIVED) {
                reactionEvents.add(event);
            } else if (event.getType() == MessageType.MATCH_FINISHED) {
                applySnapshot(event.getSnapshot());
                if (event.get("message") != null && !event.get("message").isBlank()) {
                    networkMessage = event.get("message");
                }
            }
        }
    }

    public NetworkMessage pollReactionEvent() {
        return reactionEvents.poll();
    }

    public CompletableFuture<NetworkOperationResult> sendReactionAsync(ReactionCategory category, String value) {
        return network.sendReactionAsync(context.matchId(), category, value);
    }

    public CompletableFuture<NetworkOperationResult> leaveMatchAsync() {
        return network.leaveMatchAsync(context.matchId());
    }

    public void clearNetworkMatch() {
        network.clearActiveMatch();
    }

    public GameRole getRole() {
        return context.role();
    }

    public String getOpponent() {
        return context.opponent();
    }

    public String getMatchId() {
        return context.matchId();
    }

    public long getRemainingMillis() {
        return snapshot == null ? AuthoritativeIZombieGame.DEFAULT_MATCH_DURATION_MILLIS : snapshot.getRemainingMillis();
    }

    public long getCooldownMillis(String type) {
        if (snapshot == null || type == null) {
            return 0L;
        }
        String normalized = normalizeKey(type);
        return context.role() == GameRole.PLANTS
                ? snapshot.getPlantCooldownMillis(normalized)
                : snapshot.getZombieCooldownMillis(normalized);
    }

    public int getChoiceCost(String type) {
        if (type == null) {
            return Integer.MAX_VALUE;
        }
        String normalized = normalizeKey(type);
        return context.role() == GameRole.PLANTS
                ? AuthoritativeIZombieGame.plantCosts().getOrDefault(normalized, Integer.MAX_VALUE)
                : AuthoritativeIZombieGame.zombieCosts().getOrDefault(normalized, Integer.MAX_VALUE);
    }

    public boolean isChoiceUsable(String type) {
        if (snapshot == null) {
            networkMessage = "Waiting for synchronized server state...";
            return false;
        }
        int cost = getChoiceCost(type);
        if (getSunAmount() < cost) {
            networkMessage = "Not enough sun.";
            return false;
        }
        long cooldown = getCooldownMillis(type);
        if (cooldown > 0L) {
            networkMessage = "That packet is recharging.";
            return false;
        }
        return !snapshot.isFinished();
    }

    public boolean isActionInFlight() {
        return actionInFlight;
    }

    public String getNetworkMessage() {
        return networkMessage == null ? "" : networkMessage;
    }

    public List<NetworkProjectileView> getNetworkProjectiles() {
        return List.copyOf(projectiles);
    }

    public boolean[] getNetworkBrains() {
        return snapshot == null ? new boolean[]{true, true, true, true, true} : snapshot.getBrains();
    }

    private void applySnapshot(GameSnapshot next) {
        if (next == null || next.getSequence() <= lastSequence) {
            return;
        }
        snapshot = next;
        lastSequence = next.getSequence();
        mirrorSnapshot(next);
        if (next.isFinished() && !finishApplied) {
            finishApplied = true;
            String reason = next.getFinishReason() == null || next.getFinishReason().isBlank()
                    ? "Online match finished."
                    : next.getFinishReason();
            if (next.getWinner() == context.role()) {
                markWon(reason);
            } else {
                markLost(reason);
            }
        }
    }

    private void mirrorSnapshot(GameSnapshot next) {
        clearMirrorMembership();
        projectiles.clear();
        Set<String> activePlants = new HashSet<>();
        Set<String> activeZombies = new HashSet<>();

        for (EntityState entity : next.getEntities()) {
            if (entity == null) {
                continue;
            }
            if ("PLANT".equals(entity.getCategory())) {
                mirrorPlant(entity, activePlants);
            } else if ("ZOMBIE".equals(entity.getCategory())) {
                mirrorZombie(entity, activeZombies);
            } else if ("PROJECTILE".equals(entity.getCategory())) {
                projectiles.add(new NetworkProjectileView(
                        entity.getId(), entity.getType(), entity.getX() + 1.0, entity.getRow() + 1
                ));
            }
        }

        plantsById.keySet().removeIf(id -> !activePlants.contains(id));
        zombiesById.keySet().removeIf(id -> !activeZombies.contains(id));
        boolean[] brains = next.getBrains();
        for (int row = 0; row < mirrorBoard.getHeight(); row++) {
            Lane lane = mirrorBoard.getLaneAt(row + 1);
            if (lane != null) {
                lane.setBrainEaten(row >= brains.length || !brains[row]);
                lane.setContinueAfterBrainEaten(true);
            }
        }
    }

    private void mirrorPlant(EntityState state, Set<String> active) {
        String id = state.getId();
        int column = parseColumn(state);
        int boardX = clamp(column + 1, 1, mirrorBoard.getWidth());
        int boardY = clamp(state.getRow() + 1, 1, mirrorBoard.getHeight());
        String name = plantDisplayName(state.getType());
        Plant plant = plantsById.get(id);
        if (plant == null || !plant.isAlive() || plant.getHp() < state.getHealth()) {
            try {
                plant = plantFactory.createPlant(name, boardX, boardY);
            } catch (RuntimeException ignored) {
                return;
            }
            plantsById.put(id, plant);
        }
        plant.moveTo(boardX, boardY);
        reduceHealth(plant, state.getHealth());
        Tile tile = mirrorBoard.getTileAt(new Position(boardX, boardY));
        if (tile != null && plant.isAlive()) {
            try {
                tile.placePlant(plant);
            } catch (RuntimeException ignored) {
                // Snapshot wins. If a layered tile is unsupported locally, keep the top-level visual stable.
            }
        }
        active.add(id);
    }

    private void mirrorZombie(EntityState state, Set<String> active) {
        String id = state.getId();
        double boardX = state.getX() + 1.0;
        int boardY = clamp(state.getRow() + 1, 1, mirrorBoard.getHeight());
        String name = zombieDisplayName(state.getType());
        Zombie zombie = zombiesById.get(id);
        if (zombie == null || !zombie.isAlive() || zombie.getHp() < state.getHealth()) {
            try {
                zombie = zombieFactory.createZombie(name, boardX, boardY);
            } catch (RuntimeException ignored) {
                return;
            }
            zombiesById.put(id, zombie);
        }
        zombie.moveTo(boardX, boardY);
        reduceHealth(zombie, state.getHealth());
        int tileX = clamp((int) Math.floor(state.getX()) + 1, 1, mirrorBoard.getWidth());
        Tile tile = mirrorBoard.getTileAt(new Position(tileX, boardY));
        if (tile != null && zombie.isAlive()) {
            tile.addZombie(zombie);
        }
        active.add(id);
    }

    private void clearMirrorMembership() {
        for (Lane lane : mirrorBoard.getLanes()) {
            for (Tile tile : lane.getTiles()) {
                while (tile.hasPlant()) {
                    tile.removePlant();
                }
                tile.clearZombies();
            }
        }
    }

    private void prepareMirrorBoard() {
        // Reuse the exact Ancient Egypt level-one terrain base used by ordinary gameplay.
        mirrorBoard.setTileType(new Position(5, 1), TileType.GRAVE);
        mirrorBoard.setTileType(new Position(6, 3), TileType.GRAVE);
        mirrorBoard.setTileType(new Position(4, 5), TileType.GRAVE);
        for (Lane lane : mirrorBoard.getLanes()) {
            lane.setContinueAfterBrainEaten(true);
            lane.getLawnMower().disable();
        }
    }

    private void finishAction(NetworkOperationResult result, Throwable error) {
        actionInFlight = false;
        if (error != null) {
            networkMessage = "Network action failed: " + readable(error);
        } else if (result != null) {
            networkMessage = result.message();
        }
    }

    private static void reduceHealth(Plant plant, int targetHealth) {
        if (plant == null || !plant.isAlive()) {
            return;
        }
        int damage = plant.getHp() - Math.max(0, targetHealth);
        if (damage > 0) {
            plant.takeDamage(new Damage(damage, "network sync"));
        }
    }

    private static void reduceHealth(Zombie zombie, int targetHealth) {
        if (zombie == null || !zombie.isAlive()) {
            return;
        }
        int damage = zombie.getHp() - Math.max(0, targetHealth);
        if (damage > 0) {
            zombie.takeDamage(new Damage(damage, "network sync"));
        }
    }

    private static int parseColumn(EntityState state) {
        try {
            return Integer.parseInt(state.getAttribute("column"));
        } catch (RuntimeException ignored) {
            return Math.max(0, (int) Math.floor(state.getX()));
        }
    }

    private static int millisToTicks(long millis) {
        return (int) Math.ceil(Math.max(0L, millis) / 100.0);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String normalizeKey(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private static String normalizeZombieType(String displayName) {
        String normalized = normalizeKey(displayName);
        return switch (normalized) {
            case "CONE_HEAD", "CONEHEAD" -> "CONE_HEAD";
            case "BUCKET_HEAD", "BUCKETHEAD" -> "BUCKET_HEAD";
            default -> normalized;
        };
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

    public static String plantDisplayName(String type) {
        return switch (normalizeKey(type)) {
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
        return switch (normalizeKey(type)) {
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

    private static String readable(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != null) {
            current = current.getCause();
        }
        if (current == null) {
            return "unknown error";
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }

    public record PlantOptionView(String plantName, int sunCost, long cooldownMillis) {
    }

    public record NetworkProjectileView(String id, String type, double x, int row) {
    }
}
