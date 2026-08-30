package controllers.core;

import controllers.features.TravelLogController;
import models.account.Collection;
import models.account.News;
import models.account.PlantData;
import models.account.Quest;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import controllers.auth.AuthController;
import models.account.User;
import models.core.plant.DefaultPlantRegistry;
import models.core.plant.Plant;
import models.core.plant.PlantRegistry;
import models.core.plant.PlantType;
import models.core.zombie.Armor;
import models.core.zombie.DefaultZombieRegistry;
import models.core.zombie.Zombie;
import models.core.zombie.ZombieFactory;
import models.core.zombie.ZombieRegistry;
import models.core.zombie.ZombieType;
import models.engine.board.Board;
import models.engine.board.Lane;
import models.engine.board.Position;
import models.engine.board.Tile;
import models.engine.board.TileType;
import models.engine.events.GameEvent;
import models.engine.session.GameSession;
import models.engine.session.GameState;
import models.engine.session.PlantRechargeStatus;
import models.engine.sun.Sun;
import models.level.core.AdventureContentCatalog;
import models.level.core.AdventureLevelCatalog;
import models.level.core.Level;
import models.level.core.LevelType;
import models.level.rules.LevelRule;
import models.level.rules.LevelRuntimeContext;
import models.level.rules.NoSpecialRule;
import models.level.rules.SpecialLevelType;
import models.level.rules.TimedWarObjective;
import models.level.rules.impl.ConveyorBeltRule;
import models.level.rules.impl.DeadLineRule;
import models.level.rules.impl.LockedPlantsRule;
import models.level.rules.impl.LoveYourPlantsRule;
import models.level.rules.impl.NightOpsRule;
import models.level.rules.impl.PlantWhatYouGetRule;
import models.level.rules.impl.SaveOurSeedsRule;
import models.level.rules.impl.TimedWarRule;
import models.level.wave.AttackPattern;
import models.level.wave.Wave;
import models.level.wave.WaveManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;


abstract class GameControllerState {
    protected static final double TICKS_PER_SECOND = 10.0;
    protected static final int PLANT_SELECTION_LIMIT = 8;
    protected static final int BOOST_GEM_COST = 2;
    protected static final int DEFAULT_PLANT_PURCHASE_PRICE = 2000;
    protected static final int FIRST_WAVE_TICK = 100;
    protected static final int MINIMUM_WAVE_INTERVAL_TICKS = 220;
    protected static final int BASE_WAVE_INTERVAL_TICKS = 360;

    protected final AuthController authController;
    protected final TravelLogController travelLogController;
    protected final PlantRegistry plantRegistry;
    protected final Set<String> boostedPlantNames;

    protected final Set<Position> plantedPositionsThisLevel;
    protected final Set<String> plantedPlantNamesThisLevel;
    protected final Set<String> plantedPlantFamiliesThisLevel;
    protected final Set<String> killingPlantNamesThisLevel;
    protected final Set<String> killingPlantFamiliesThisLevel;
    protected final Map<String, Integer> killsByPlantName;
    protected final Map<String, Integer> killsByPlantFamily;

    protected GameSession gameSession;
    protected String currentChapterName;
    protected int currentLevelNumber;
    protected String lastMessage;

    protected boolean finalStatsRecorded;
    protected int firstWaveStartTick;
    protected int fastReactionKillsThisLevel;
    protected int cactusKillsThisLevel;
    protected int firstColumnNoMowerKillsThisLevel;
    protected int lawnMowerKillsThisLevel;
    protected int explosivePlantsUsedThisLevel;
    protected int sunProducerPlantsPlantedThisLevel;
    protected boolean anyNonCactusKillThisLevel;


    protected GameControllerState(AuthController authController) {
        this.authController = authController;
        this.travelLogController = new TravelLogController(authController);
        this.plantRegistry = DefaultPlantRegistry.getInstance();
        this.boostedPlantNames = new LinkedHashSet<>();

        this.plantedPositionsThisLevel = new HashSet<>();
        this.plantedPlantNamesThisLevel = new HashSet<>();
        this.plantedPlantFamiliesThisLevel = new HashSet<>();
        this.killingPlantNamesThisLevel = new HashSet<>();
        this.killingPlantFamiliesThisLevel = new HashSet<>();
        this.killsByPlantName = new HashMap<>();
        this.killsByPlantFamily = new HashMap<>();

        this.currentChapterName = "";
        this.currentLevelNumber = 1;
        this.lastMessage = "";

        resetRuntimeQuestTracking();
    }


    protected boolean hasRunningSession() {
        return gameSession != null && gameSession.isRunning();
    }

    protected boolean hasInitializedSession() {
        return gameSession != null
                && gameSession.getBoard() != null
                && gameSession.getTickManager() != null
                && gameSession.getSunManager() != null;
    }

    protected User getLoggedInUserOrFail() {
        if (authController == null || !authController.isLoggedIn()) {
            fail("You must login first.");
            return null;
        }

        User user = authController.getLoggedInUser();
        if (user == null) {
            fail("You must login first.");
            return null;
        }

        return user;
    }

    protected void saveUsers() {
        if (authController != null) {
            authController.saveUsers();
        }
    }

    protected List<Wave> createDifficultyWaves(
            int difficulty,
            List<String> allowedZombieNames,
            List<String> newlyUnlockedZombieNames,
            int stageOrdinal
    ) {
        List<Wave> waves = new ArrayList<>();
        int totalWaves = 2 + difficulty;
        int delayStep = Math.max(
                MINIMUM_WAVE_INTERVAL_TICKS,
                BASE_WAVE_INTERVAL_TICKS - difficulty * 20
        );
        int stageBonus = Math.max(0, stageOrdinal) / 3;

        for (int waveNumber = 1; waveNumber <= totalWaves; waveNumber++) {
            int zombieCount = difficulty + waveNumber + Math.min(3, stageBonus);
            int delay = FIRST_WAVE_TICK + (waveNumber - 1) * delayStep;

            waves.add(new Wave(
                    waveNumber,
                    delay,
                    zombiesForDifficultyWave(
                            waveNumber,
                            totalWaves,
                            difficulty,
                            zombieCount,
                            allowedZombieNames,
                            newlyUnlockedZombieNames,
                            stageOrdinal
                    )
            ));
        }

        return waves;
    }

    protected List<Zombie> zombiesForDifficultyWave(
            int waveNumber,
            int totalWaves,
            int difficulty,
            int requestedCount,
            List<String> allowedZombieNames,
            List<String> newlyUnlockedZombieNames,
            int stageOrdinal
    ) {
        ZombieRegistry registry = DefaultZombieRegistry.getInstance();
        ZombieFactory factory = new ZombieFactory(registry);
        List<ZombieType> available = availableZombieTypes(registry, allowedZombieNames);
        List<ZombieType> eligible = eligibleZombieTypes(available, waveNumber, difficulty, stageOrdinal);
        List<Zombie> zombies = finalWaveZombies(
                registry, factory, waveNumber, totalWaves,
                allowedZombieNames, newlyUnlockedZombieNames
        );
        addWaveZombies(zombies, eligible, factory, waveNumber, totalWaves,
                difficulty, requestedCount, stageOrdinal);
        return zombies;
    }

    private List<ZombieType> availableZombieTypes(
            ZombieRegistry registry, List<String> allowedZombieNames
    ) {
        List<ZombieType> types = new ArrayList<>();
        for (ZombieType type : registry.getAllZombieTypes()) {
            if (type != null && containsNormalizedName(allowedZombieNames, type.getName())) types.add(type);
        }
        types.sort((first, second) -> {
            int comparison = Integer.compare(first.getWaveCost(), second.getWaveCost());
            return comparison != 0 ? comparison : first.getName().compareToIgnoreCase(second.getName());
        });
        if (types.isEmpty()) {
            ZombieType fallback = registry.getZombieTypeByName("Default");
            if (fallback != null) types.add(fallback);
        }
        return types;
    }

    private List<ZombieType> eligibleZombieTypes(
            List<ZombieType> available, int waveNumber, int difficulty, int stageOrdinal
    ) {
        int maximumCost = 250 + Math.max(0, stageOrdinal) * 80 + waveNumber * 120 + difficulty * 70;
        int minimumVariety = Math.min(available.size(), Math.max(4, available.size() - 2));
        List<ZombieType> eligible = new ArrayList<>();
        for (int index = 0; index < available.size(); index++) {
            ZombieType type = available.get(index);
            if (index < minimumVariety || type.getWaveCost() <= maximumCost) {
                eligible.add(type);
            }
        }
        if (eligible.isEmpty() && !available.isEmpty()) {
            eligible.add(available.get(0));
        }
        return eligible;
    }

    private List<Zombie> finalWaveZombies(
            ZombieRegistry registry,
            ZombieFactory factory,
            int waveNumber,
            int totalWaves,
            List<String> allowedNames,
            List<String> unlockedNames
    ) {
        List<Zombie> zombies = new ArrayList<>();
        if (waveNumber != totalWaves || unlockedNames == null) return zombies;
        for (String name : unlockedNames) {
            ZombieType type = registry.getZombieTypeByName(name);
            if (type != null && containsNormalizedName(allowedNames, type.getName())) {
                zombies.add(factory.createZombie(type, 9, 1));
            }
        }
        return zombies;
    }

    private void addWaveZombies(
            List<Zombie> zombies,
            List<ZombieType> eligible,
            ZombieFactory factory,
            int waveNumber,
            int totalWaves,
            int difficulty,
            int requestedCount,
            int stageOrdinal
    ) {
        if (eligible.isEmpty()) return;
        int count = Math.max(requestedCount, zombies.size());
        int accessible = eligible.size();
        for (int index = zombies.size(); index < count; index++) {
            int typeIndex = Math.floorMod(
                    index * 2 + waveNumber + difficulty + Math.max(0, stageOrdinal), accessible
            );
            zombies.add(factory.createZombie(eligible.get(typeIndex), 9, 1));
        }
    }


    protected boolean containsNormalizedName(List<String> names, String targetName) {
        if (names == null || targetName == null) {
            return false;
        }
        String target = normalizeName(targetName);
        for (String name : names) {
            if (normalizeName(name).equals(target)) {
                return true;
            }
        }
        return false;
    }

    protected int currentDifficultyLevel() {
        if (authController == null || authController.getLoggedInUser() == null) {
            return 3;
        }

        return authController.getLoggedInUser().getDifficultyLevel();
    }

    protected void resetRuntimeQuestTracking() {
        plantedPositionsThisLevel.clear();
        plantedPlantNamesThisLevel.clear();
        plantedPlantFamiliesThisLevel.clear();
        killingPlantNamesThisLevel.clear();
        killingPlantFamiliesThisLevel.clear();
        killsByPlantName.clear();
        killsByPlantFamily.clear();

        finalStatsRecorded = false;
        firstWaveStartTick = -1;
        fastReactionKillsThisLevel = 0;
        cactusKillsThisLevel = 0;
        firstColumnNoMowerKillsThisLevel = 0;
        lawnMowerKillsThisLevel = 0;
        explosivePlantsUsedThisLevel = 0;
        sunProducerPlantsPlantedThisLevel = 0;
        anyNonCactusKillThisLevel = false;
    }


    protected void ensureQuestList(User user) {
        if (user == null || travelLogController == null) {
            return;
        }

        travelLogController.showPage("all");
    }

    protected void resetQuestProgress(User user, String progressKey) {
        if (user == null || progressKey == null || progressKey.isBlank()) {
            return;
        }

        ensureQuestList(user);

        for (Quest quest : user.getQuests()) {
            if (quest != null && quest.matchesProgressKey(progressKey)) {
                quest.resetProgress();
            }
        }

        saveUsers();
    }

    protected String normalizeName(String value) {
        if (value == null) {
            return "";
        }

        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', ' ')
                .replace('_', ' ')
                .replaceAll("\\s+", " ");
    }

    protected void success(String message) {
        lastMessage = "OK: " + message;
    }

    protected void fail(String message) {
        lastMessage = "ERROR: " + (message == null ? "Unknown error." : message);
    }
}
