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


abstract class GameControllerDisplaySupport extends GameControllerSelectionSupport {
    protected GameControllerDisplaySupport(AuthController authController) {
        super(authController);
    }

    public void showSun() {
        if (!hasInitializedSession()) {
            fail("Game is not available.");
            return;
        }

        StringBuilder builder = new StringBuilder();

        builder.append("Current sun amount: ")
                .append(gameSession.getTotalSunAmount())
                .append("\nSuns:");

        List<Sun> suns = gameSession.getSunManager().getSuns();

        if (suns.isEmpty()) {
            builder.append("\n  none");
        } else {
            for (Sun sun : suns) {
                builder.append("\n  ")
                        .append(sun.getType())
                        .append(" amount=")
                        .append(sun.getSunAmount())
                        .append(" at ")
                        .append(sun.getPosition())
                        .append(" state=");

                if (sun.isFalling()) {
                    builder.append("falling, time-left=")
                            .append(sun.getFallingTicksRemaining())
                            .append(" ticks (")
                            .append(formatSeconds(sun.getFallingTicksRemaining()))
                            .append("s)");
                } else if (sun.isProducedByPlant()) {
                    builder.append("plant-produced, permanent");
                } else {
                    builder.append("ground, permanent");
                }
            }
        }

        success(builder.toString());
    }

    public void showMap() {
        if (!hasInitializedSession()) {
            fail("Game is not available.");
            return;
        }

        Board board = gameSession.getBoard();
        Level level = gameSession.getCurrentLevel();
        int tick = gameSession.getTickManager().getCurrentTick();

        StringBuilder builder = new StringBuilder();
        appendGameHeader(builder, level, tick, board);
        appendSpecialLevelStatus(builder, level);
        appendDetailedMap(builder, board);
        success(builder.toString());
    }

    public void showPlants() {
        if (!hasInitializedSession()) {
            fail("Game is not available.");
            return;
        }

        List<PlantRechargeStatus> statuses = gameSession.getPlantRechargeStatuses();
        StringBuilder builder = new StringBuilder("Plants Status:");

        if (statuses.isEmpty()) {
            builder.append("\n  none");
        } else {
            for (PlantRechargeStatus status : statuses) {
                builder.append("\n")
                        .append(status.getPlantName())
                        .append(" | cost: ")
                        .append(status.getSunCost())
                        .append(" | plantable: ")
                        .append(status.isPlantable())
                        .append(" | boosted: ")
                        .append(isPlantBoostedForThisGame(status.getPlantName()))
                        .append(" | cooldown remaining: ")
                        .append(formatSeconds(status.getRemainingTicks()))
                        .append("s");
            }
        }

        success(builder.toString());
    }

    public void showZombies() {
        if (!hasInitializedSession()) {
            fail("Game is not available.");
            return;
        }

        List<Zombie> zombies = gameSession.getBoard().getAllZombies();
        StringBuilder builder = new StringBuilder("Zombies Info\n============");

        if (zombies.isEmpty()) {
            builder.append("\nNo zombies are on the map.");
        } else {
            for (Zombie zombie : zombies) {
                builder.append("\n")
                        .append(zombie.getName())
                        .append(":")
                        .append("\n  position: ")
                        .append(formatCoordinate(zombie.getX(), zombie.getY()))
                        .append("\n  health: ")
                        .append(zombie.getHp())
                        .append("/")
                        .append(zombie.getMaxHp())
                        .append("\n  speed: ")
                        .append(String.format(Locale.ROOT, "%.3f", zombie.getCurrentSpeed()))
                        .append("\n  damage/tick: ")
                        .append(zombie.getType().getDamagePerTick())
                        .append("\n  glowing: ")
                        .append(zombie.isGlowing())
                        .append("\n  armor: ");

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
        }

        success(builder.toString());
    }

    public void showTile(Position position) {
        Tile tile = validatedTile(position);
        if (tile == null) return;
        Lane lane = gameSession.getBoard().getLaneAt(position.getY());
        StringBuilder builder = new StringBuilder();
        appendTileHeader(builder, tile, position, lane);
        appendTilePlants(builder, tile);
        appendTileZombies(builder, position);
        appendTileSuns(builder, position);
        success(builder.toString());
    }

    private Tile validatedTile(Position position) {
        if (!hasInitializedSession()) {
            fail("Game is not available.");
            return null;
        }
        if (position == null) {
            fail("Tile position is invalid.");
            return null;
        }
        Tile tile = gameSession.getBoard().getTileAt(position);
        if (tile == null) fail("Tile does not exist.");
        return tile;
    }

    private void appendTileHeader(StringBuilder builder, Tile tile, Position position, Lane lane) {
        builder.append("Tile ").append(position).append("\ntype: ").append(tile.getTileType())
                .append("\nplantable land: ").append(tile.isPlantable())
                .append("\nlawn mower: ")
                .append(lane != null && lane.getLawnMower().isReady() ? "ready" : "used");
        if (tile.hasDamageableTerrain()) {
            builder.append("\nterrain health: ").append(tile.getTerrainHealth())
                    .append("/").append(tile.getMaximumTerrainHealth());
        }
    }

    private void appendTilePlants(StringBuilder builder, Tile tile) {
        builder.append("\nplants:");
        if (tile.getPlants().isEmpty()) {
            builder.append(" none");
            return;
        }
        int layer = 1;
        for (Plant plant : tile.getPlants()) {
            builder.append("\n  layer ").append(layer++).append(":");
            appendPlantDetails(builder, plant, "    ");
        }
    }

    private void appendTileZombies(StringBuilder builder, Position position) {
        builder.append("\nzombies:");
        List<Zombie> zombies = zombiesDisplayedAt(position);
        if (zombies.isEmpty()) builder.append(" none");
        else for (Zombie zombie : zombies) appendZombieDetails(builder, zombie);
    }

    private void appendTileSuns(StringBuilder builder, Position position) {
        builder.append("\nsuns:");
        List<Sun> suns = sunsAt(position);
        if (suns.isEmpty()) {
            builder.append(" none");
            return;
        }
        for (Sun sun : suns) {
            builder.append("\n  type=").append(sun.getType())
                    .append(" amount=").append(sun.getSunAmount())
                    .append(" state=").append(sun.isFalling() ? "falling" : "collectible");
            if (sun.isFalling()) {
                builder.append(" time-left=").append(sun.getFallingTicksRemaining()).append(" ticks");
            }
        }
    }


    public void invalidCommand(String menuName) {
        fail("Invalid command in " + menuName + ".");
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public boolean wasSuccessful() {
        return lastMessage != null && lastMessage.startsWith("OK:");
    }

}
