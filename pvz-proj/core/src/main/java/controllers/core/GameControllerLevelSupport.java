package controllers.core;

import boss.core.BossCatalog;
import boss.core.BossRuntime;
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
import models.level.core.AdventureChapterConfigurator;
import models.level.core.Level;
import models.level.core.LevelType;
import models.level.core.SeasonType;
import models.level.rules.LevelRule;
import models.level.rules.LevelRuntimeContext;
import models.level.rules.NoSpecialRule;
import models.level.rules.SpecialLevelType;
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


abstract class GameControllerLevelSupport extends GameControllerStatusSupport {
    protected GameControllerLevelSupport(AuthController authController) {
        super(authController);
    }

    protected AdventureUnlockSummary synchronizeAdventureContent(
            User user, String chapterName, int levelNumber
    ) {
        AdventureUnlockSummary summary = new AdventureUnlockSummary();
        if (user == null) return summary;
        Collection collection = user.getCollection();
        ZombieRegistry zombieRegistry = DefaultZombieRegistry.getInstance();
        boolean changed = registerAdventureEntries(collection, zombieRegistry);
        changed |= unlockPlants(user, collection, summary, chapterName, levelNumber);
        changed |= unlockZombies(user, collection, summary, chapterName, levelNumber, zombieRegistry);
        if (changed) saveUsers();
        return summary;
    }

    private boolean registerAdventureEntries(Collection collection, ZombieRegistry zombieRegistry) {
        boolean changed = false;
        for (PlantType type : plantRegistry.getAllPlantTypes()) {
            if (type != null && !collection.hasPlant(type.getName())) {
                collection.addPlant(new PlantData(type.getName(), DEFAULT_PLANT_PURCHASE_PRICE, false));
                changed = true;
            }
        }
        for (ZombieType type : zombieRegistry.getAllZombieTypes()) {
            if (type != null && !collection.hasZombie(type.getName())) {
                collection.addZombie(type.getName(), false);
                changed = true;
            }
        }
        return changed;
    }

    private boolean unlockPlants(
            User user,
            Collection collection,
            AdventureUnlockSummary summary,
            String chapterName,
            int levelNumber
    ) {
        boolean changed = false;
        LinkedHashSet<String> names = new LinkedHashSet<>(
                AdventureContentCatalog.plantNamesUnlockedThrough(
                        chapterName, levelNumber, plantRegistry
                )
        );
        names.addAll(AdventureContentCatalog.plantNamesForLevel(
                chapterName, levelNumber, plantRegistry
        ));
        for (String name : names) {
            if (collection.hasOwnedPlant(name) || !collection.unlockPlant(name)) continue;
            summary.plantNames.add(name);
            user.addNews(News.plantUnlocked(name));
            changed = true;
        }
        return changed;
    }

    private boolean unlockZombies(
            User user,
            Collection collection,
            AdventureUnlockSummary summary,
            String chapterName,
            int levelNumber,
            ZombieRegistry registry
    ) {
        boolean changed = false;
        for (String name : AdventureContentCatalog.zombieNamesUnlockedThrough(
                chapterName, levelNumber, registry)) {
            if (collection.hasOwnedZombie(name) || !collection.unlockZombie(name)) continue;
            summary.zombieNames.add(name);
            user.addNews(News.zombieDiscovered(name));
            changed = true;
        }
        return changed;
    }


    protected static final class AdventureUnlockSummary {
        protected final List<String> plantNames = new ArrayList<>();
        protected final List<String> zombieNames = new ArrayList<>();

        protected String asMessage() {
            StringBuilder builder = new StringBuilder();
            if (!plantNames.isEmpty()) {
                builder.append("\nNew plants unlocked: ")
                        .append(String.join(", ", plantNames))
                        .append(".");
            }
            if (!zombieNames.isEmpty()) {
                builder.append("\nNew zombies discovered: ")
                        .append(String.join(", ", zombieNames))
                        .append(".");
            }
            return builder.toString();
        }
    }

    protected Level createAdventureLevel(String chapterName, int levelNumber) {
        int difficulty = currentDifficultyLevel();
        ZombieRegistry zombieRegistry = DefaultZombieRegistry.getInstance();
        List<String> allowedPlants = AdventureContentCatalog.plantNamesForLevel(
                chapterName, levelNumber, plantRegistry
        );
        List<String> allowedZombies = AdventureContentCatalog.zombieNamesForLevel(
                chapterName, levelNumber, zombieRegistry
        );

        if (levelNumber == AdventureLevelCatalog.BOSS_LEVEL) {
            return createBossLevel(chapterName, allowedPlants, allowedZombies);
        }

        List<String> newlyUnlockedZombies = AdventureContentCatalog.zombieNamesIntroducedAt(
                chapterName, levelNumber, zombieRegistry
        );
        List<Wave> waves = createDifficultyWaves(
                difficulty,
                allowedZombies,
                newlyUnlockedZombies,
                AdventureContentCatalog.stageOrdinal(chapterName, levelNumber)
        );
        WaveManager waveManager = new WaveManager(waves, null, AttackPattern.ROUND_ROBIN);
        int initialSun = Math.max(50, 200 - difficulty * 10);

        LevelRule rule = levelNumber == 1
                ? new NoSpecialRule()
                : createSpecialRule(chapterName, levelNumber, allowedPlants, difficulty);
        LevelType levelType = levelNumber == 1 ? LevelType.NORMAL : LevelType.SPECIAL;

        Level level = new Level(
                AdventureLevelCatalog.levelId(chapterName, levelNumber),
                waveManager,
                levelType,
                allowedPlants,
                allowedZombies,
                rule,
                initialSun
        );
        AdventureChapterConfigurator.configure(level, chapterName, levelNumber);
        return level;
    }

    private Level createBossLevel(
            String chapterName,
            List<String> allowedPlants,
            List<String> allowedZombies
    ) {
        if (!BossCatalog.supportsChapter(chapterName)) {
            throw new IllegalArgumentException(
                    "No boss implementation is available for this chapter."
            );
        }
        WaveManager waveManager = new WaveManager(
                new ArrayList<>(), null, AttackPattern.ROUND_ROBIN
        );
        List<String> bossPlants = new ArrayList<>(allowedPlants);
        ConveyorBeltRule conveyor = new ConveyorBeltRule(
                bossPlants,
                25,
                new java.util.Random(AdventureLevelCatalog.levelId(chapterName, 4) * 7919L)
        );
        Level level = new Level(
                AdventureLevelCatalog.levelId(chapterName, AdventureLevelCatalog.BOSS_LEVEL),
                waveManager,
                LevelType.BOSS,
                bossPlants,
                allowedZombies,
                conveyor,
                0
        );
        AdventureChapterConfigurator.configure(
                level, chapterName, AdventureLevelCatalog.BOSS_LEVEL
        );
        level.bindBossRuntime(new BossRuntime(
                BossCatalog.create(chapterName),
                AdventureLevelCatalog.levelId(
                        chapterName, AdventureLevelCatalog.BOSS_LEVEL
                ) * 104729L
        ));
        return level;
    }

    protected LevelRule createSpecialRule(
            String chapterName,
            int levelNumber,
            List<String> allowedPlants,
            int difficulty
    ) {
        SpecialLevelType type = AdventureLevelCatalog.specialTypeFor(chapterName, levelNumber);

        return switch (type) {
            case CONVEYOR_BELT -> new ConveyorBeltRule(
                    AdventureContentCatalog.conveyorPlantNamesForLevel(
                            chapterName, levelNumber, plantRegistry
                    ),
                    50,
                    new java.util.Random(
                            AdventureLevelCatalog.levelId(chapterName, levelNumber) * 7919L
                    )
            );
            case LOCKED_PLANTS -> new LockedPlantsRule(
                    8,
                    3,
                    Arrays.asList("Bonk Choy", "Repeater", "Twin Sunflower"),
                    lockedPlantFamilies()
            );
            case SAVE_OUR_SEEDS -> new SaveOurSeedsRule(protectedSeedPositions());
            case TIMED_WAR -> new TimedWarRule(
                    2600,
                    8 + difficulty,
                    350 + difficulty * 50
            );
            case NIGHT_OPS -> new NightOpsRule();
            case DEAD_LINE -> new DeadLineRule(3.0);
            case LOVE_YOUR_PLANTS -> new LoveYourPlantsRule(4);
            case PLANT_WHAT_YOU_GET -> new PlantWhatYouGetRule(2000);
            default -> new NoSpecialRule();
        };
    }

    protected List<String> ownedAllowedPlants(List<String> allowedPlants) {
        List<String> ownedPlants = new ArrayList<>();
        if (allowedPlants != null) {
            for (String plantName : allowedPlants) {
                PlantType type = plantRegistry.getByName(plantName);
                if (type != null
                        && isPlantUnlockedByUser(type.getName())
                        && !normalizeName(type.getCategory()).equals("sun producer")) {
                    ownedPlants.add(type.getName());
                }
            }
        }

        if (ownedPlants.isEmpty()) {
            ownedPlants.add("Peashooter");
        }
        return ownedPlants;
    }


    private boolean isDebugModeEnabled(User user) {
        return user != null
                && user.getSettings() != null
                && user.getSettings().isDebugMode();
    }

    protected abstract boolean isPlantUnlockedByUser(String plantName);
}
