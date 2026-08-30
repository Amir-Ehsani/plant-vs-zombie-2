package models.minigame;

import models.core.plant.DefaultPlantRegistry;
import models.core.plant.Plant;
import models.core.plant.PlantFactory;
import models.core.plant.PlantFood;
import models.core.plant.PlantFoodContext;
import models.core.plant.PlantType;
import models.core.projectile.Damage;
import models.core.zombie.Zombie;
import models.core.zombie.ZombieFactory;
import models.core.zombie.ZombieType;
import models.engine.board.Board;
import models.engine.board.Lane;
import models.engine.board.Position;
import models.engine.board.Tile;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public final class ZombotanyGame extends MiniGameSession {
    private static final int INITIAL_SUN = 500;
    private static final int SKY_SUN_INITIAL_INTERVAL = 120;
    private static final int SUN_DROP_LIFE_TICKS = 160;
    private static final int SUNFLOWER_INTERVAL = 100;
    private static final int JALAPENO_FUSE_TICKS = 100;
    private static final int JALAPENO_FIRE_VISUAL_TICKS = 18;
    private static final int PEA_ATTACK_INTERVAL = 15;
    private static final int PEA_DAMAGE = 20;
    private static final double PEA_TILES_PER_TICK = 0.85;
    private static final int SQUASH_WINDUP_TICKS = 5;
    private static final int SQUASH_JUMP_TICKS = 5;
    private static final double SQUASH_TRIGGER_DISTANCE = 0.78;
    private static final double DEFAULT_ZOMBIE_SPEED = 0.185;
    private static final double COMBAT_ZOMBIE_SPEED_SCALE = 0.055;
    private static final double SQUASH_MOVE_PER_TICK =
            DEFAULT_ZOMBIE_SPEED * COMBAT_ZOMBIE_SPEED_SCALE * 1.60;
    private static final double SQUASH_JUMP_HEIGHT = 0.62;
    private static final int TOTAL_WAVES = 4;
    private static final int BETWEEN_WAVE_DELAY = 70;

    private final Board board;
    private final PlantFactory plantFactory;
    private final ZombieFactory zombieFactory;
    private final Random random;
    private final Map<String, SeedOption> seedOptions;
    private final Map<String, Integer> rechargeRemaining;
    private final Map<Zombie, PlantZombieState> plantZombies;
    private final Map<Plant, PlantFood> activePlantFoods;
    private final List<PeaShot> peaShots;
    private final List<SunDrop> sunDrops;
    private final Map<Integer, Integer> burningLaneTicks;
    private final List<String> selectedPlantNames;
    private final ArrayDeque<String> announcements;
    private final int totalZombieCount;
    private final int spawnInterval;
    private final int zombiesPerWave;
    private int sunAmount;
    private int plantFoodAmount;
    private int spawnedZombies;
    private int defeatedZombies;
    private int nextSpawnTick;
    private int currentWave;
    private int spawnedInCurrentWave;
    private int currentWaveTarget;
    private int nextPeaShotId;
    private int nextSunDropId;
    private int nextSkySunTick;

    public ZombotanyGame(int stage) {
        this(stage, defaultPlantSelection(stage));
    }

    public ZombotanyGame(int stage, List<String> plantNames) {
        super(MiniGameType.PLANT_ZOMBIES, stage);
        board = new Board();
        plantFactory = new PlantFactory();
        zombieFactory = new ZombieFactory();
        random = new Random(79_000L + stage);
        seedOptions = new LinkedHashMap<>();
        rechargeRemaining = new LinkedHashMap<>();
        plantZombies = new IdentityHashMap<>();
        activePlantFoods = new IdentityHashMap<>();
        peaShots = new ArrayList<>();
        sunDrops = new ArrayList<>();
        burningLaneTicks = new LinkedHashMap<>();
        selectedPlantNames = new ArrayList<>();
        announcements = new ArrayDeque<>();
        totalZombieCount = 14 + stage * 6;
        spawnInterval = 34 - stage * 4;
        zombiesPerWave = (int) Math.ceil(totalZombieCount / (double) TOTAL_WAVES);
        sunAmount = INITIAL_SUN;
        plantFoodAmount = 0;
        spawnedZombies = 0;
        defeatedZombies = 0;
        nextSpawnTick = 25;
        currentWave = 0;
        spawnedInCurrentWave = 0;
        currentWaveTarget = 0;
        nextPeaShotId = 1;
        nextSunDropId = 1;
        nextSkySunTick = SKY_SUN_INITIAL_INTERVAL;
        initializeSeedOptions(plantNames);
        success("Zombotany stage " + stage + " started with " + INITIAL_SUN + " sun.");
    }

    public boolean plant(String plantName, Position position) {
        if (!isRunning()) {
            fail("The mini-game is already finished.");
            return false;
        }
        SeedOption option = seedOptions.get(normalize(plantName));
        if (option == null) {
            fail("That plant was not selected for this stage.");
            return false;
        }
        if (position == null || !board.isValidPosition(position) || !board.canPlacePlant(position)) {
            fail("That tile cannot be planted on.");
            return false;
        }
        if (rechargeRemaining.getOrDefault(normalize(option.name()), 0) > 0) {
            fail(option.name() + " is still recharging.");
            return false;
        }
        if (sunAmount < option.sunCost()) {
            fail("Not enough sun. " + option.sunCost() + " sun is required.");
            return false;
        }

        Plant plant = plantFactory.createPlant(option.name(), position.getX(), position.getY());
        if (!board.placePlant(plant, position)) {
            fail("That plant cannot be placed there.");
            return false;
        }
        sunAmount -= option.sunCost();
        rechargeRemaining.put(normalize(option.name()), option.rechargeTicks());
        success(option.name() + " planted at " + position + ".");
        return true;
    }

    @Override
    protected void onTick() {
        updateRecharge();
        updateLaneFireVisuals();
        updateActivePlantFoods();
        updateSunDrops();
        produceSun();
        spawnZombieIfReady();
        updatePeaShots();
        board.updateTicks();
        runPlantZombieAbilities();
        cleanupZombieStates();
    }

    @Override
    protected void evaluateStatus() {
        if (!isRunning()) {
            return;
        }
        if (board.hasBrainBeenEaten()) {
            markLost("A plant zombie reached the house. Zombotany was lost.");
            return;
        }
        if (spawnedZombies >= totalZombieCount && board.getActiveZombieCount() == 0) {
            markWon("All plant zombies were defeated. Zombotany was won.");
        }
    }

    @Override
    public String renderMap() {
        return renderBoard(board, null);
    }

    @Override
    public String renderStatus() {
        return compactStatus()
                + "\nsun=" + sunAmount
                + "\nactive zombies=" + board.getActiveZombieCount()
                + "\nremaining zombies=" + Math.max(0, totalZombieCount - spawnedZombies)
                + "\nplants=" + board.getPlantCount();
    }

    @Override
    public String renderHelp() {
        return """
                Zombotany commands
                show plants
                plant -t <type> -l <x, y>
                show map
                show status
                advance time -t <count> ticks
                """.trim();
    }

    public Board getBoard() {
        return board;
    }

    public int getSunAmount() {
        return sunAmount;
    }

    public int getPlantFoodAmount() {
        return plantFoodAmount;
    }

    public boolean addDebugPlantFood() {
        if (plantFoodAmount >= 3) {
            fail("Plant food storage is full.");
            return false;
        }
        plantFoodAmount++;
        success("1 debug plant food added.");
        return true;
    }

    public boolean feedPlant(Position position) {
        if (!isRunning() || plantFoodAmount <= 0 || position == null || !board.isValidPosition(position)) {
            fail("Plant food cannot be used there.");
            return false;
        }
        Tile tile = board.getTileAt(position);
        Plant plant = tile == null ? null : tile.getCurrentPlant();
        if (plant == null || !plant.isAlive()) {
            fail("There is no plant to feed on that tile.");
            return false;
        }
        PlantFood previous = activePlantFoods.remove(plant);
        if (previous != null) {
            previous.expire();
        }
        PlantFood food = new PlantFood();
        PlantFoodContext context = new PlantFoodContext(
                board, plantFactory, random, amount -> sunAmount += Math.max(0, amount),
                null, null, null
        );
        plant.usePlantFood(food, context);
        if (food.isActive()) {
            activePlantFoods.put(plant, food);
        }
        plantFoodAmount--;
        success("Plant food used on " + plant.getName() + ".");
        return true;
    }

    public void addDebugSun(int amount) {
        if (amount <= 0) {
            return;
        }
        sunAmount += amount;
        success(amount + " debug sun added.");
    }

    public boolean pluck(Position position) {
        if (!isRunning()) {
            fail("The mini-game is already finished.");
            return false;
        }
        if (position == null || !board.isValidPosition(position)) {
            fail("That tile is invalid.");
            return false;
        }
        Plant removed = board.removePlant(position);
        if (removed == null) {
            fail("There is no plant to remove on that tile.");
            return false;
        }
        success(removed.getName() + " removed from " + position + ".");
        return true;
    }

    public boolean collectSunDrop(int dropId) {
        Iterator<SunDrop> iterator = sunDrops.iterator();
        while (iterator.hasNext()) {
            SunDrop drop = iterator.next();
            if (drop.id != dropId) {
                continue;
            }
            sunAmount += drop.amount;
            iterator.remove();
            success("Collected " + drop.amount + " sun.");
            return true;
        }
        fail("That sun has already disappeared.");
        return false;
    }

    public List<String> getSelectedPlantNames() {
        return List.copyOf(selectedPlantNames);
    }

    public List<SeedOptionView> getSeedOptions() {
        List<SeedOptionView> result = new ArrayList<>();
        for (SeedOption option : seedOptions.values()) {
            int remaining = rechargeRemaining.getOrDefault(normalize(option.name()), 0);
            result.add(new SeedOptionView(option.name(), option.sunCost(), remaining, option.rechargeTicks()));
        }
        return List.copyOf(result);
    }

    public String renderPlantOptions() {
        StringBuilder builder = new StringBuilder("Selected plants:\n");
        for (SeedOptionView option : getSeedOptions()) {
            builder.append(option.plantName())
                    .append(" | cost=")
                    .append(option.sunCost())
                    .append(" | recharge=")
                    .append(option.remainingRechargeTicks())
                    .append(" ticks\n");
        }
        return builder.toString().trim();
    }

    public List<PlantZombieView> getPlantZombies() {
        List<PlantZombieView> result = new ArrayList<>();
        for (Map.Entry<Zombie, PlantZombieState> entry : plantZombies.entrySet()) {
            Zombie zombie = entry.getKey();
            if (zombie.isAlive()) {
                result.add(new PlantZombieView(zombie, entry.getValue().kind.displayName));
            }
        }
        return List.copyOf(result);
    }

    public List<PeaShotView> getPeaShots() {
        List<PeaShotView> result = new ArrayList<>();
        for (PeaShot shot : peaShots) {
            result.add(new PeaShotView(
                    shot.id, shot.startX, shot.targetX, shot.y, shot.remainingTicks, shot.totalTicks
            ));
        }
        return List.copyOf(result);
    }

    public List<SunDropView> getSunDrops() {
        List<SunDropView> result = new ArrayList<>();
        for (SunDrop drop : sunDrops) {
            result.add(new SunDropView(drop.id, drop.amount, drop.x, drop.y, drop.remainingTicks));
        }
        return List.copyOf(result);
    }

    public Set<Integer> getBurningLanes() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(burningLaneTicks.keySet()));
    }

    public int getBurningLaneRemainingTicks(int lane) {
        return Math.max(0, burningLaneTicks.getOrDefault(lane, 0));
    }

    public int getCurrentWaveNumber() {
        return currentWave;
    }

    public int getTotalWaves() {
        return TOTAL_WAVES;
    }

    public float getWaveProgress() {
        if (isWon()) {
            return 1f;
        }
        return Math.min(1f, defeatedZombies / (float) Math.max(1, totalZombieCount));
    }

    public int getDefeatedZombies() {
        return defeatedZombies;
    }

    public List<String> consumeAnnouncements() {
        List<String> result = new ArrayList<>(announcements);
        announcements.clear();
        return List.copyOf(result);
    }

    private void initializeSeedOptions(List<String> plantNames) {
        List<String> safeNames = plantNames == null || plantNames.isEmpty()
                ? defaultPlantSelection(getStage())
                : plantNames;
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String plantName : safeNames) {
            if (unique.size() >= 8) {
                break;
            }
            PlantType type = DefaultPlantRegistry.getInstance().getByName(plantName);
            if (type != null) {
                unique.add(type.getName());
            }
        }
        if (unique.isEmpty()) {
            throw new IllegalArgumentException("Select at least one plant for Zombotany.");
        }
        for (String plantName : unique) {
            addSeed(plantName);
            selectedPlantNames.add(plantName);
        }
    }

    private void addSeed(String plantName) {
        PlantType type = DefaultPlantRegistry.getInstance().getByName(plantName);
        if (type == null) {
            throw new IllegalStateException("Missing plant type: " + plantName);
        }
        SeedOption option = new SeedOption(type.getName(), type.getSunCost(), Math.max(1, type.getRecharge()));
        seedOptions.put(normalize(type.getName()), option);
        rechargeRemaining.put(normalize(type.getName()), 0);
    }

    private void updateRecharge() {
        for (Map.Entry<String, Integer> entry : rechargeRemaining.entrySet()) {
            if (entry.getValue() > 0) {
                entry.setValue(entry.getValue() - 1);
            }
        }
    }

    private void updateLaneFireVisuals() {
        List<Integer> expired = new ArrayList<>();
        for (Map.Entry<Integer, Integer> entry : burningLaneTicks.entrySet()) {
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                expired.add(entry.getKey());
            } else {
                entry.setValue(remaining);
            }
        }
        for (Integer lane : expired) {
            burningLaneTicks.remove(lane);
        }
    }

    private void produceSun() {
        if (getCurrentTick() >= nextSkySunTick) {
            spawnSunDrop(
                    25,
                    random.nextInt(board.getWidth()) + 1,
                    random.nextInt(board.getHeight()) + 1
            );
            scheduleNextSkySun();
        }
        if (getCurrentTick() % SUNFLOWER_INTERVAL != 0) {
            return;
        }
        for (Plant plant : board.getAllPlants()) {
            if (plant.isAlive() && plant.getName().equalsIgnoreCase("Sunflower")) {
                spawnSunDrop(25, plant.getX(), plant.getY());
            }
        }
    }

    private void updateActivePlantFoods() {
        Iterator<Map.Entry<Plant, PlantFood>> iterator = activePlantFoods.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Plant, PlantFood> entry = iterator.next();
            Plant plant = entry.getKey();
            PlantFood food = entry.getValue();
            if (plant == null || !plant.isAlive() || food == null) {
                if (food != null) {
                    food.expire();
                }
                iterator.remove();
                continue;
            }
            food.tick();
            if (!food.isActive()) {
                iterator.remove();
            }
        }
    }

    private void updateSunDrops() {
        Iterator<SunDrop> iterator = sunDrops.iterator();
        while (iterator.hasNext()) {
            SunDrop drop = iterator.next();
            drop.remainingTicks--;
            if (drop.remainingTicks <= 0) {
                iterator.remove();
            }
        }
    }

    private void spawnSunDrop(int amount, double x, double y) {
        sunDrops.add(new SunDrop(nextSunDropId++, amount, x, y, SUN_DROP_LIFE_TICKS));
    }

    private void scheduleNextSkySun() {
        double elapsedSeconds = getCurrentTick() / 10.0;
        double intervalSeconds = Math.max(6.0 + 0.05 * elapsedSeconds, 12.0);
        nextSkySunTick = getCurrentTick() + Math.max(1, (int) Math.ceil(intervalSeconds * 10.0));
    }

    private void spawnZombieIfReady() {
        if (spawnedZombies >= totalZombieCount || getCurrentTick() < nextSpawnTick) {
            return;
        }

        // A new wave may only begin after every zombie from the previous wave is gone.
        // This mirrors the normal adventure HUD semantics instead of filling progress on spawn.
        if (currentWave == 0) {
            startNextWave();
        } else if (spawnedInCurrentWave >= currentWaveTarget) {
            if (board.getActiveZombieCount() > 0) {
                return;
            }
            startNextWave();
        }

        PlantZombieKind kind = PlantZombieKind.values()[random.nextInt(PlantZombieKind.values().length)];
        int lane = random.nextInt(board.getHeight()) + 1;
        Zombie zombie = createPlantZombie(kind, lane);
        board.getTileAt(new Position(board.getWidth(), lane)).addZombie(zombie);
        plantZombies.put(zombie, new PlantZombieState(kind, getCurrentTick()));
        spawnedZombies++;
        spawnedInCurrentWave++;

        if (spawnedZombies >= totalZombieCount) {
            return;
        }
        if (spawnedInCurrentWave >= currentWaveTarget) {
            nextSpawnTick = getCurrentTick() + BETWEEN_WAVE_DELAY;
        } else {
            int jitter = random.nextInt(9) - 4;
            nextSpawnTick = getCurrentTick() + Math.max(14, spawnInterval + jitter);
        }
    }

    private void startNextWave() {
        currentWave++;
        spawnedInCurrentWave = 0;
        currentWaveTarget = Math.min(zombiesPerWave, totalZombieCount - spawnedZombies);
        announce("WAVE " + currentWave + " - ZOMBOTANY ZOMBIES APPROACHING!");
    }

    private Zombie createPlantZombie(PlantZombieKind kind, int lane) {
        int stageBonus = (getStage() - 1) * 55;
        int hp = kind.baseHp + stageBonus;
        ZombieType type = new ZombieType(
                kind.displayName,
                hp,
                kind.speed,
                kind.damage,
                100,
                kind.animationId,
                kind.defaultArmor,
                List.of(),
                ""
        );
        return zombieFactory.createZombie(type, board.getWidth() + 3.5, lane);
    }

    private void runPlantZombieAbilities() {
        for (Map.Entry<Zombie, PlantZombieState> entry : new ArrayList<>(plantZombies.entrySet())) {
            Zombie zombie = entry.getKey();
            PlantZombieState state = entry.getValue();
            if (!zombie.isAlive()) {
                continue;
            }
            state.markBoardEntry(zombie, getCurrentTick(), board.getWidth());
            switch (state.kind) {
                case PEASHOOTER -> runPeashooterAbility(zombie, state);
                case JALAPENO -> runJalapenoAbility(zombie, state);
                case SQUASH -> runSquashAbility(zombie, state);
                case WALL_NUT -> {
                }
            }
        }
    }

    private void runPeashooterAbility(Zombie zombie, PlantZombieState state) {
        if (state.boardEntryTick < 0 || getCurrentTick() - state.lastAbilityTick < PEA_ATTACK_INTERVAL) {
            return;
        }
        Plant target = nearestPlantToLeft(zombie);
        if (target == null) {
            return;
        }
        state.lastAbilityTick = getCurrentTick();
        double startX = zombie.getX() - 0.18;
        int travelTicks = Math.max(2, (int) Math.ceil(Math.abs(startX - target.getX()) / PEA_TILES_PER_TICK));
        peaShots.add(new PeaShot(
                nextPeaShotId++, startX, target.getX(), zombie.getY(), target, travelTicks, travelTicks
        ));
    }

    private void runJalapenoAbility(Zombie zombie, PlantZombieState state) {
        if (state.abilityTriggered || state.boardEntryTick < 0
                || getCurrentTick() - state.boardEntryTick < JALAPENO_FUSE_TICKS) {
            return;
        }
        state.abilityTriggered = true;
        int laneNumber = (int) Math.round(zombie.getY());
        Lane lane = board.getLaneAt(laneNumber);
        if (lane != null) {
            for (Tile tile : lane.getTiles()) {
                for (Plant plant : new ArrayList<>(tile.getPlants())) {
                    plant.kill();
                }
            }
            burningLaneTicks.put(laneNumber, JALAPENO_FIRE_VISUAL_TICKS);
        }
        zombie.kill();
        board.removeDeadEntities();
    }

    private void runSquashAbility(Zombie zombie, PlantZombieState state) {
        Plant target = state.squashTarget;
        if (target != null && !target.isAlive()) {
            resetSquashState(zombie, state);
            target = null;
        }
        if (state.squashPhase == SquashPhase.WINDUP) {
            updateSquashWindup(zombie, state);
            return;
        }
        if (state.squashPhase == SquashPhase.JUMPING) {
            updateSquashJump(zombie, state);
            return;
        }

        target = nearestPlantToLeft(zombie);
        if (target != null && zombie.getX() - target.getX() <= SQUASH_TRIGGER_DISTANCE) {
            state.squashTarget = target;
            state.squashPhase = SquashPhase.WINDUP;
            state.phaseTick = 0;
            state.jumpStartX = zombie.getX();
            zombie.setVisualVerticalOffset(0);
            return;
        }
        zombie.moveBy(-SQUASH_MOVE_PER_TICK, 0);
        zombie.setVisualVerticalOffset(0);
    }

    private void updateSquashWindup(Zombie zombie, PlantZombieState state) {
        state.phaseTick++;
        zombie.setVisualVerticalOffset(0);
        if (state.phaseTick < SQUASH_WINDUP_TICKS) {
            return;
        }
        Plant target = state.squashTarget;
        if (target == null || !target.isAlive()) {
            resetSquashState(zombie, state);
            return;
        }
        state.squashPhase = SquashPhase.JUMPING;
        state.phaseTick = 0;
        state.jumpStartX = zombie.getX();
    }

    private void updateSquashJump(Zombie zombie, PlantZombieState state) {
        Plant target = state.squashTarget;
        if (target == null || !target.isAlive()) {
            resetSquashState(zombie, state);
            return;
        }
        state.phaseTick++;
        float progress = Math.min(1f, state.phaseTick / (float) SQUASH_JUMP_TICKS);
        double targetX = target.getX();
        double x = state.jumpStartX + (targetX - state.jumpStartX) * progress;
        zombie.moveTo(x, zombie.getY());
        zombie.setVisualVerticalOffset(Math.sin(Math.PI * progress) * SQUASH_JUMP_HEIGHT);
        if (state.phaseTick < SQUASH_JUMP_TICKS) {
            return;
        }
        target.kill();
        zombie.setVisualVerticalOffset(0);
        zombie.kill();
        board.removeDeadEntities();
    }

    private void resetSquashState(Zombie zombie, PlantZombieState state) {
        state.squashTarget = null;
        state.squashPhase = SquashPhase.WALKING;
        state.phaseTick = 0;
        zombie.setVisualVerticalOffset(0);
    }

    private Plant nearestPlantToLeft(Zombie zombie) {
        Plant selected = null;
        double selectedDistance = Double.MAX_VALUE;
        int lane = (int) Math.round(zombie.getY());
        for (Plant plant : board.getAllPlants()) {
            if (!plant.isAlive() || (int) Math.round(plant.getY()) != lane || plant.getX() > zombie.getX()) {
                continue;
            }
            double distance = zombie.getX() - plant.getX();
            if (distance < selectedDistance) {
                selected = plant;
                selectedDistance = distance;
            }
        }
        return selected;
    }

    private void updatePeaShots() {
        boolean damagedPlant = false;
        for (PeaShot shot : new ArrayList<>(peaShots)) {
            shot.remainingTicks--;
            if (shot.remainingTicks > 0) {
                continue;
            }
            if (shot.target != null && shot.target.isAlive()) {
                shot.target.takeDamage(new Damage(PEA_DAMAGE, "zombotany pea"));
                damagedPlant = true;
            }
            peaShots.remove(shot);
        }
        if (damagedPlant) {
            board.removeDeadEntities();
        }
    }

    private void cleanupZombieStates() {
        Iterator<Map.Entry<Zombie, PlantZombieState>> iterator = plantZombies.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Zombie, PlantZombieState> entry = iterator.next();
            Zombie zombie = entry.getKey();
            if (zombie != null && zombie.isAlive()) {
                continue;
            }
            defeatedZombies++;
            iterator.remove();
        }
    }

    private void announce(String message) {
        if (message != null && !message.isBlank()) {
            announcements.add(message);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replace('-', ' ').replace('_', ' ').replaceAll("\\s+", " ");
    }

    private static List<String> defaultPlantSelection(int stage) {
        List<String> result = new ArrayList<>();
        result.add("Sunflower");
        result.add("Peashooter");
        result.add("Wall-nut");
        result.add("Cabbage-pult");
        result.add(stage == 1 ? "Potato Mine" : "Snow Pea");
        result.add(stage == 3 ? "Repeater" : "Bonk Choy");
        return result;
    }

    public record SeedOptionView(String plantName, int sunCost, int remainingRechargeTicks, int rechargeTicks) {
    }

    public record PlantZombieView(Zombie zombie, String kind) {
    }

    public record PeaShotView(
            int id, double startX, double targetX, double y, int remainingTicks, int totalTicks
    ) {
    }

    public record SunDropView(int id, int amount, double x, double y, int remainingTicks) {
    }

    private record SeedOption(String name, int sunCost, int rechargeTicks) {
    }

    private static final class SunDrop {
        private final int id;
        private final int amount;
        private final double x;
        private final double y;
        private int remainingTicks;

        private SunDrop(int id, int amount, double x, double y, int remainingTicks) {
            this.id = id;
            this.amount = amount;
            this.x = x;
            this.y = y;
            this.remainingTicks = remainingTicks;
        }
    }

    private static final class PeaShot {
        private final int id;
        private final double startX;
        private final double targetX;
        private final double y;
        private final Plant target;
        private final int totalTicks;
        private int remainingTicks;

        private PeaShot(
                int id, double startX, double targetX, double y, Plant target, int remainingTicks, int totalTicks
        ) {
            this.id = id;
            this.startX = startX;
            this.targetX = targetX;
            this.y = y;
            this.target = target;
            this.remainingTicks = remainingTicks;
            this.totalTicks = totalTicks;
        }
    }

    private static final class PlantZombieState {
        private final PlantZombieKind kind;
        private final int spawnTick;
        private int boardEntryTick;
        private int lastAbilityTick;
        private boolean abilityTriggered;
        private SquashPhase squashPhase;
        private Plant squashTarget;
        private int phaseTick;
        private double jumpStartX;

        private PlantZombieState(PlantZombieKind kind, int spawnTick) {
            this.kind = kind;
            this.spawnTick = spawnTick;
            boardEntryTick = -1;
            lastAbilityTick = spawnTick;
            abilityTriggered = false;
            squashPhase = SquashPhase.WALKING;
            squashTarget = null;
            phaseTick = 0;
            jumpStartX = 0;
        }

        private void markBoardEntry(Zombie zombie, int currentTick, int boardWidth) {
            if (boardEntryTick >= 0 || zombie == null || zombie.getX() > boardWidth) {
                return;
            }
            boardEntryTick = currentTick;
            if (kind == PlantZombieKind.PEASHOOTER) {
                lastAbilityTick = currentTick - PEA_ATTACK_INTERVAL;
            }
        }
    }

    private enum SquashPhase {
        WALKING,
        WINDUP,
        JUMPING
    }

    private enum PlantZombieKind {
        PEASHOOTER("Peashooter Zombie", "ZombieDefault", null, 260, 0.185, 10),
        WALL_NUT("Wall-nut Zombie", "ZombieArmor1", "Cone", 900, 0.155, 10),
        JALAPENO("Jalapeno Zombie", "ZombotanyJalapeno", null, 340, 0.185, 10),
        SQUASH("Squash Zombie", "ZombotanySquash", null, 300, 0, 0);

        private final String displayName;
        private final String animationId;
        private final String defaultArmor;
        private final int baseHp;
        private final double speed;
        private final int damage;

        PlantZombieKind(
                String displayName,
                String animationId,
                String defaultArmor,
                int baseHp,
                double speed,
                int damage
        ) {
            this.displayName = displayName;
            this.animationId = animationId;
            this.defaultArmor = defaultArmor;
            this.baseHp = baseHp;
            this.speed = speed;
            this.damage = damage;
        }
    }
}
