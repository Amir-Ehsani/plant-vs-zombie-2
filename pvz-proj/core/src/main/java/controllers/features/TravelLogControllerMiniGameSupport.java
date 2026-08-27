package controllers.features;

import controllers.auth.AuthController;
import models.account.PlantData;
import models.account.Quest;
import models.account.User;
import models.core.plant.DefaultPlantRegistry;
import models.core.plant.PlantRegistry;
import models.core.plant.PlantType;
import models.engine.board.Position;
import models.minigame.IZombieGame;
import models.minigame.MiniGameSession;
import models.minigame.MiniGameType;
import models.minigame.VasebreakerGame;
import models.minigame.WallNutBowlingGame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


abstract class TravelLogControllerMiniGameSupport extends TravelLogControllerQuestSupport {
    protected TravelLogControllerMiniGameSupport(AuthController authController) {
        super(authController);
    }

    public boolean enterMiniGame(String miniGameName) {
        User user = getLoggedInUserOrFail();

        if (user == null) {
            return false;
        }

        MiniGameType type = MiniGameType.fromText(miniGameName);

        if (type == null) {
            return fail("Mini-game " + miniGameName + " does not exist.");
        }

        return enterMiniGame(type, firstPlayableStage(user, type));
    }

    public boolean enterMiniGame(String miniGameName, int stage) {
        User user = getLoggedInUserOrFail();

        if (user == null) {
            return false;
        }

        MiniGameType type = MiniGameType.fromText(miniGameName);

        if (type == null) {
            return fail("Mini-game " + miniGameName + " does not exist.");
        }

        return enterMiniGame(type, stage);
    }

    public boolean advanceMiniGameTime(int ticks) {
        MiniGameSession session = requireActiveMiniGame();

        if (session == null) {
            return false;
        }

        boolean result = session.advanceTicks(ticks);
        synchronizeMiniGameMessage();
        return result;
    }

    public boolean breakVase(Position position) {
        MiniGameSession session = requireActiveMiniGame();

        if (!(session instanceof VasebreakerGame game)) {
            return fail("break vase is only available in Vasebreaker.");
        }

        boolean result = game.breakVase(position);
        synchronizeMiniGameMessage();
        return result;
    }

    public boolean plantVasebreakerPacket(int packetId, Position position) {
        MiniGameSession session = requireActiveMiniGame();

        if (!(session instanceof VasebreakerGame game)) {
            return fail("plant packet is only available in Vasebreaker.");
        }

        boolean result = game.plantPacket(packetId, position);
        synchronizeMiniGameMessage();
        return result;
    }

    public boolean launchNut(String nutType, Position position) {
        MiniGameSession session = requireActiveMiniGame();

        if (!(session instanceof WallNutBowlingGame game)) {
            return fail("launch nut is only available in Wall-nut Bowling.");
        }

        boolean result = game.launchNut(nutType, position);
        synchronizeMiniGameMessage();
        return result;
    }

    public boolean spawnIZombie(String zombieType, Position position) {
        MiniGameSession session = requireActiveMiniGame();

        if (!(session instanceof IZombieGame game)) {
            return fail("spawn zombie is only available in I, Zombie.");
        }

        boolean result = game.spawnZombie(zombieType, position);
        synchronizeMiniGameMessage();
        return result;
    }

    public boolean collectIZombieSun(int dropId) {
        MiniGameSession session = requireActiveMiniGame();

        if (!(session instanceof IZombieGame game)) {
            return fail("sun collection is only available in I, Zombie.");
        }

        boolean result = game.collectSunDrop(dropId);
        synchronizeMiniGameMessage();
        return result;
    }

    public String showActiveMiniGameMap() {
        MiniGameSession session = requireActiveMiniGame();

        if (session == null) {
            return lastMessage;
        }

        success(session.renderMap());
        return lastMessage;
    }

    public String showActiveMiniGameStatus() {
        MiniGameSession session = requireActiveMiniGame();

        if (session == null) {
            return lastMessage;
        }

        success(session.renderStatus());
        return lastMessage;
    }

    public String showActiveMiniGameHelp() {
        MiniGameSession session = requireActiveMiniGame();

        if (session == null) {
            return lastMessage;
        }

        success(session.renderHelp()
                + "\nabandon minigame"
                + "\nmenu show current"
                + "\nmenu exit");
        return lastMessage;
    }

    public String showVasebreakerPackets() {
        MiniGameSession session = requireActiveMiniGame();

        if (!(session instanceof VasebreakerGame game)) {
            fail("show packets is only available in Vasebreaker.");
            return lastMessage;
        }

        success(game.renderPackets());
        return lastMessage;
    }

    public String showBowlingNuts() {
        MiniGameSession session = requireActiveMiniGame();

        if (!(session instanceof WallNutBowlingGame game)) {
            fail("show nuts is only available in Wall-nut Bowling.");
            return lastMessage;
        }

        success(game.renderInventory());
        return lastMessage;
    }

    public String showIZombieOptions() {
        MiniGameSession session = requireActiveMiniGame();

        if (!(session instanceof IZombieGame game)) {
            fail("show zombies is only available in I, Zombie.");
            return lastMessage;
        }

        success(game.renderAvailableZombies());
        return lastMessage;
    }

    public String showIZombieSun() {
        MiniGameSession session = requireActiveMiniGame();

        if (!(session instanceof IZombieGame game)) {
            fail("show sun is only available in I, Zombie.");
            return lastMessage;
        }

        success("Current sun: " + game.getSunAmount());
        return lastMessage;
    }

    public boolean abandonMiniGame() {
        if (activeMiniGame == null) {
            return fail("No mini-game is active.");
        }

        String gameName = activeMiniGame.getType().getDisplayName();
        int stage = activeMiniGame.getStage();
        boolean finished = !activeMiniGame.isRunning();
        activeMiniGame = null;

        return success((finished ? "Left" : "Abandoned")
                + " " + gameName + " stage " + stage + ".");
    }

    public boolean hasActiveMiniGame() {
        return activeMiniGame != null;
    }

    public MiniGameSession getActiveMiniGameSession() {
        return activeMiniGame;
    }

    public boolean isActiveMiniGameRunning() {
        return activeMiniGame != null && activeMiniGame.isRunning();
    }

    public MiniGameType getActiveMiniGameType() {
        return activeMiniGame == null ? null : activeMiniGame.getType();
    }

    public int getActiveMiniGameStage() {
        return activeMiniGame == null ? 0 : activeMiniGame.getStage();
    }

    public String getActiveMiniGameTitle() {
        if (activeMiniGame == null) {
            return "No active mini-game";
        }

        return activeMiniGame.getType().getDisplayName()
                + " - Stage " + activeMiniGame.getStage();
    }

    public List<MiniGameInfo> getMiniGames() {
        User user = authController == null ? null : authController.getLoggedInUser();
        List<MiniGameInfo> miniGames = new ArrayList<>();

        for (MiniGameType type : MiniGameType.values()) {
            List<MiniGameStageInfo> stages = new ArrayList<>();

            for (int stage = 1; stage <= 3; stage++) {
                boolean completed = user != null && user.isMiniGameStageCompleted(type.name(), stage);
                boolean unlocked = user != null && user.isMiniGameStageUnlocked(type.name(), stage);
                stages.add(new MiniGameStageInfo(stage, unlocked, completed));
            }

            miniGames.add(new MiniGameInfo(
                    commandName(type),
                    type.getDisplayName(),
                    stages
            ));
        }

        return miniGames;
    }

    public boolean isLoggedIn() {
        return authController != null && authController.isLoggedIn();
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

    protected boolean enterMiniGame(MiniGameType type, int stage) {
        User user = getLoggedInUserOrFail();

        if (user == null) {
            return false;
        }

        if (stage < 1 || stage > 3) {
            return fail("Mini-game stage must be between 1 and 3.");
        }

        if (activeMiniGame != null && activeMiniGame.isRunning()) {
            return fail("Finish or abandon the current mini-game first.");
        }

        if (!user.isMiniGameStageUnlocked(type.name(), stage)) {
            return fail(type.getDisplayName() + " stage " + stage + " is locked.");
        }

        ensureDefaultQuests(user);

        try {
            activeMiniGame = switch (type) {
                case VASEBREAKER -> new VasebreakerGame(stage);
                case WALLNUT_BOWLING -> new WallNutBowlingGame(stage);
                case I_ZOMBIE -> new IZombieGame(stage);
            };
        } catch (IllegalArgumentException | IllegalStateException exception) {
            activeMiniGame = null;
            return fail(exception.getMessage());
        }

        user.setCurrentChapterName("minigame:" + commandName(type) + ":stage-" + stage);
        authController.saveUsers();

        success("Entered " + type.getDisplayName() + " stage " + stage
                + ".\n" + stripMessagePrefix(activeMiniGame.getLastMessage()));
        return true;
    }

    protected void synchronizeMiniGameMessage() {
        if (activeMiniGame == null) {
            fail("No mini-game is active.");
            return;
        }

        lastMessage = activeMiniGame.getLastMessage();

        if (!activeMiniGame.consumeCompletionSignal()) {
            return;
        }

        User user = getLoggedInUserOrFail();

        if (user == null) {
            return;
        }

        boolean newCompletion = user.completeMiniGameStage(
                activeMiniGame.getType().name(),
                activeMiniGame.getStage()
        );

        if (newCompletion) {
            recordQuestProgress(user, "minigame_stage_completed", 1);
            authController.saveUsers();
        }

        StringBuilder builder = new StringBuilder(lastMessage == null ? "" : lastMessage);
        builder.append("\nStage progress recorded.");

        if (activeMiniGame.getStage() < 3) {
            builder.append(" Stage ")
                    .append(activeMiniGame.getStage() + 1)
                    .append(" is now unlocked.");
        } else {
            builder.append(" All stages of ")
                    .append(activeMiniGame.getType().getDisplayName())
                    .append(" are complete.");
        }

        lastMessage = builder.toString();
    }


    public abstract void recordQuestProgress(User user, String progressKey, int amount);
}
