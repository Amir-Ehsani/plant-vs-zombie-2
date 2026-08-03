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


abstract class GameControllerStatusSupport extends GameControllerMapRenderSupport {
    protected GameControllerStatusSupport(AuthController authController) {
        super(authController);
    }

    protected Map<String, List<String>> lockedPlantFamilies() {
        Map<String, List<String>> families = new LinkedHashMap<>();
        families.put("pea-family", Arrays.asList(
                "Peashooter", "Repeater", "Fire Peashooter", "Split Pea", "Pea Pod"
        ));
        families.put("wall-family", Arrays.asList("Wall-nut", "Tall-nut"));
        families.put("catapult-family", Arrays.asList(
                "Cabbage-pult", "Kernel-pult", "Pepper-pult", "Melon-pult"
        ));
        return families;
    }

    protected Map<Position, String> protectedSeedPositions() {
        Map<Position, String> protectedPlants = new LinkedHashMap<>();
        protectedPlants.put(new Position(3, 2), "Peashooter");
        protectedPlants.put(new Position(3, 4), "Wall-nut");
        return protectedPlants;
    }

    protected void appendPlantDetails(StringBuilder builder, Plant plant, String indent) {
        PlantType type = plant.getType();
        String tags = type.getTags();

        builder.append("\n")
                .append(indent)
                .append("name: ")
                .append(plant.getName())
                .append("\n")
                .append(indent)
                .append("category: ")
                .append(type.getCategory())
                .append("\n")
                .append(indent)
                .append("tags: ")
                .append(tags == null || tags.isBlank() ? "none" : tags)
                .append("\n")
                .append(indent)
                .append("health: ")
                .append(plant.getHp())
                .append("/")
                .append(plant.getMaxHp())
                .append("\n")
                .append(indent)
                .append("sun cost: ")
                .append(plant.getCurrentSunCost())
                .append("\n")
                .append(indent)
                .append("damage: ")
                .append(plant.getAttackDamage())
                .append("\n")
                .append(indent)
                .append("action interval: ")
                .append(type.getActionInterval())
                .append(" ticks")
                .append("\n")
                .append(indent)
                .append("seed recharge: ")
                .append(type.getRecharge())
                .append(" ticks")
                .append("\n")
                .append(indent)
                .append("attack cooldown remaining: ")
                .append(plant.getCooldownRemaining())
                .append(" ticks")
                .append("\n")
                .append(indent)
                .append("boosted: ")
                .append(plant.isBoosted());
    }

    protected void appendZombieDetails(StringBuilder builder, Zombie zombie) {
        builder.append("\n  name: ")
                .append(zombie.getName())
                .append("\n    position: ")
                .append(formatCoordinate(zombie.getX(), zombie.getY()))
                .append("\n    health: ")
                .append(zombie.getHp())
                .append("/")
                .append(zombie.getMaxHp())
                .append("\n    speed: ")
                .append(String.format(Locale.ROOT, "%.3f", zombie.getCurrentSpeed()))
                .append("\n    damage/tick: ")
                .append(zombie.getType().getDamagePerTick())
                .append("\n    wave cost: ")
                .append(zombie.getType().getWaveCost())
                .append("\n    glowing: ")
                .append(zombie.isGlowing())
                .append("\n    armor: ");

        Armor armor = zombie.getArmor();

        if (armor == null || armor.isBroken()) {
            builder.append("none");
        } else {
            builder.append(armor.getName())
                    .append(" type=")
                    .append(armor.getArmorType())
                    .append(" hp=")
                    .append(armor.getHp());
        }
    }

    protected void appendGameHeader(StringBuilder builder, Level level, int tick, Board board) {
        builder.append("tick: ")
                .append(tick)
                .append(" | second: ")
                .append(formatSeconds(tick))
                .append(" | state: ")
                .append(gameSession.getState().getStatus())
                .append(" | paused: ")
                .append(gameSession.getTickManager().isPaused());

        if (level != null) {
            builder.append("\nadventure: ")
                    .append(AdventureLevelCatalog.displayChapterName(currentChapterName))
                    .append(" | stage: ")
                    .append(currentLevelNumber)
                    .append(" | title: ")
                    .append(AdventureLevelCatalog.levelTitle(currentChapterName, currentLevelNumber));

            builder.append("\nlevel: ")
                    .append(level.getLevelId())
                    .append(" | type: ")
                    .append(level.getLevelType())
                    .append(" | level-status: ")
                    .append(level.getStatus());

            if (level.getSpecialLevelType() != SpecialLevelType.NONE) {
                builder.append(" | special: ")
                        .append(level.getSpecialLevelType());
            }
        }

        builder.append("\nwave: ")
                .append(currentWaveNumber(level))
                .append("/")
                .append(totalWaves(level))
                .append(" | suns: ")
                .append(gameSession.getTotalSunAmount())
                .append(" | plant foods: ")
                .append(gameSession.getPlantFoodCount())
                .append(" | active zombies: ")
                .append(board.getActiveZombieCount());
    }

    protected void appendSpecialLevelStatus(StringBuilder builder, Level level) {
        if (level == null) return;
        LevelRule rule = level.getLevelRule();
        LevelRuntimeContext context = createLevelContext();
        if (appendTimedOrConveyorStatus(builder, level, rule, context)) return;
        if (appendLockedOrProtectedStatus(builder, rule)) return;
        appendRemainingRuleStatus(builder, rule, context);
    }

    private boolean appendTimedOrConveyorStatus(
            StringBuilder builder, Level level, LevelRule rule, LevelRuntimeContext context
    ) {
        if (rule instanceof TimedWarRule timedRule) {
            int remainingTicks = timedRule.getRemainingTicks(context);
            builder.append("\ntimed-war: time-left=").append(remainingTicks).append(" ticks (")
                    .append(formatSeconds(remainingTicks)).append("s) | objective=")
                    .append(timedRule.getObjective()).append(" | progress=")
                    .append(timedRule.getProgress(context)).append("/")
                    .append(timedRule.getTargetAmount());
            return true;
        }
        if (rule instanceof ConveyorBeltRule) {
            builder.append("\nconveyor plants: ").append(level.getConveyorPlants().isEmpty()
                    ? "none" : String.join(", ", level.getConveyorPlants()));
            return true;
        }
        return false;
    }

    private boolean appendLockedOrProtectedStatus(StringBuilder builder, LevelRule rule) {
        if (rule instanceof LockedPlantsRule lockedRule) {
            builder.append("\nplant selection: selected=").append(lockedRule.getSelectedPlants().size())
                    .append("/").append(lockedRule.getAvailableSelectionSlotCount())
                    .append(" | locked-slots=").append(lockedRule.getLockedSelectionSlotCount())
                    .append(" | remaining=").append(lockedRule.getRemainingSelectionSlotCount())
                    .append(" | selection-closed=").append(lockedRule.isSelectionLocked());
            if (!lockedRule.getUnavailablePlants().isEmpty()) {
                builder.append(" | unavailable=")
                        .append(String.join(", ", lockedRule.getUnavailablePlants()));
            }
            return true;
        }
        if (rule instanceof SaveOurSeedsRule saveRule) {
            builder.append("\nprotected plants: ").append(saveRule.getProtectedPositions());
            return true;
        }
        return false;
    }

    private void appendRemainingRuleStatus(
            StringBuilder builder, LevelRule rule, LevelRuntimeContext context
    ) {
        if (rule instanceof DeadLineRule deadLineRule) {
            builder.append("\ndead line: x=").append(deadLineRule.getDeadlineX());
        } else if (rule instanceof LoveYourPlantsRule loveRule) {
            builder.append("\nplant losses: ").append(loveRule.getDestroyedPlantCount(context))
                    .append("/").append(loveRule.getMaximumPlantLosses());
        } else if (rule instanceof NightOpsRule) {
            builder.append("\nnight ops: sky sun is disabled");
        }
    }




    protected void appendEvents(StringBuilder builder, List<GameEvent> events) {
        if (events == null) {
            return;
        }

        for (GameEvent event : events) {
            String eventText = formatEvent(event);

            if (eventText != null && !eventText.isBlank()) {
                builder.append("\n").append(eventText);
            }
        }
    }

    protected String formatEvent(GameEvent event) {
        if (event == null || event.getType() == null) return "";
        return switch (event.getType()) {
            case WAVE_STARTED, ZOMBIE_SPAWNED, ZOMBIE_KILLED -> formatZombieEvent(event);
            case PLANT_DESTROYED, PLANT_SUN_PRODUCED -> formatPlantEvent(event);
            case LAWN_MOWER_TRIGGERED -> formatMowerEvent(event);
            case SKY_SUN_DROPPING, SKY_SUN_LANDED, RADIOACTIVE_SUN_EXPLODED -> formatSunEvent(event);
            case PLANT_FOOD_DROPPED -> "A zombie dropped a plant food; you have "
                    + event.getCurrentCount() + " plant foods now.";
            case REWARD_DROPPED -> formatRewardEvent(event);
            case CHAPTER_EFFECT -> event.getEntityName();
            default -> "";
        };
    }

    private String formatRewardEvent(GameEvent event) {
        String type = event.getEntityName();
        int amount = Math.max(1, event.getAmount());
        if ("coin".equalsIgnoreCase(type)) {
            return "A zombie dropped " + amount + " coins.";
        }
        if ("diamond".equalsIgnoreCase(type)) {
            return "A zombie dropped " + amount + " diamond(s).";
        }
        if ("sun".equalsIgnoreCase(type)) {
            return "A reward grave released " + amount + " sun.";
        }
        if ("plant_food".equalsIgnoreCase(type)) {
            return "A reward grave released one plant food.";
        }
        return "A zombie dropped a greenhouse pot reward.";
    }

    private String formatZombieEvent(GameEvent event) {
        return switch (event.getType()) {
            case WAVE_STARTED -> event.isFinalWave()
                    ? "The final wave has come." : "Wave " + event.getWaveNumber() + " started.";
            case ZOMBIE_SPAWNED -> "Zombie " + event.getEntityName()
                    + " spawned at wave " + event.getWaveNumber() + " in lane "
                    + event.getLaneNumber() + " which costed " + event.getWaveCost() + ".";
            case ZOMBIE_KILLED -> event.isGroupedByLawnMower() ? ""
                    : "Zombie of type " + event.getEntityName() + " is dead at "
                    + formatCoordinate(event.getX(), event.getY());
            default -> "";
        };
    }

    private String formatPlantEvent(GameEvent event) {
        if (event.getType() == models.engine.events.GameEventType.PLANT_DESTROYED) {
            return "Plant " + event.getEntityName() + " at "
                    + formatCoordinate(event.getX(), event.getY()) + " is destroyed.";
        }
        return "plant " + event.getEntityName() + " produced a sun at "
                + formatCoordinate(event.getX(), event.getY());
    }

    private String formatMowerEvent(GameEvent event) {
        String names = event.getEntityNames().isEmpty()
                ? "none" : String.join(", ", event.getEntityNames());
        return "The lawn mower in the row " + event.getLaneNumber()
                + " is triggered and killed these zombies: " + names;
    }

    private String formatSunEvent(GameEvent event) {
        return switch (event.getType()) {
            case SKY_SUN_DROPPING -> "New " + event.getSunType().name().toLowerCase(Locale.ROOT)
                    + " sun is dropping at position " + formatCoordinate(event.getX(), event.getY());
            case SKY_SUN_LANDED -> "Sun reached the ground at position "
                    + formatCoordinate(event.getX(), event.getY());
            case RADIOACTIVE_SUN_EXPLODED -> "Radioactive sun exploded at "
                    + formatCoordinate(event.getX(), event.getY()) + "; zombies killed="
                    + event.getAmount() + ", plants destroyed=" + event.getSecondaryAmount() + ".";
            default -> "";
        };
    }


    protected void appendFinishedState(StringBuilder builder) {
        GameState state = gameSession.getState();

        if (state == null) {
            return;
        }

        if (state.getStatus() == GameState.Status.WON) {
            builder.append("\nDear humanz, zis is not done yet; we will come back to eat your brainz, humanz.");
        } else if (state.getStatus() == GameState.Status.LOST) {
            builder.append("\nThe zombie ate your brain; LOSER!!!");
        }
    }

    protected int currentWaveNumber(Level level) {
        if (level == null || level.getWaveManager() == null) {
            return 0;
        }

        return level.getWaveManager().getCurrentWaveNumber();
    }

    protected int totalWaves(Level level) {
        if (level == null || level.getWaveManager() == null) {
            return 0;
        }

        return level.getWaveManager().getTotalWaves();
    }

    protected LevelRuntimeContext createLevelContext() {
        return new LevelRuntimeContext(
                gameSession.getBoard(),
                gameSession.getTickManager().getCurrentTick(),
                gameSession.getTotalSunAmount(),
                gameSession.getTotalSunProduced(),
                gameSession.getTotalZombiesKilled(),
                gameSession.getTotalPlantsDestroyed()
        );
    }

    protected String formatSeconds(int ticks) {
        return String.format(Locale.ROOT, "%.1f", ticks / TICKS_PER_SECOND);
    }

    protected String formatCoordinate(double x, double y) {
        if (Math.abs(x - Math.rint(x)) < 0.000001) {
            return "(" + (int) Math.rint(x) + ", " + (int) Math.rint(y) + ")";
        }

        return "(" + String.format(Locale.ROOT, "%.2f", x)
                + ", " + (int) Math.rint(y) + ")";
    }

}
