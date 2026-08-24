package screens.menu;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.pvz.Main;
import controllers.features.GreenhouseController;
import models.account.Greenhouse;
import models.account.PlantData;
import models.account.User;
import pvz.skin.BorderedTable;
import ui.BackButton;
import ui.ConfirmDialog;
import ui.DialogActor;
import ui.MenuButton;
import ui.PlantSelectionDialog;
import ui.PvzAnimationService;

public class GreenhouseScreen extends BaseMenuScreen {
    private final GreenhouseController controller;
    private final PvzAnimationService animations;
    private final Table potGrid;
    private String lastSignature;
    private float refreshAccumulator;

    public GreenhouseScreen(Main game) {
        super(game);
        controller = game.getGreenhouseController();
        animations = game.getAnimationService();
        potGrid = new Table();
        potGrid.top();
        lastSignature = "";
        refreshAccumulator = 0f;
        buildUi();
    }

    @Override
    public void show() {
        super.show();
        if (!requireLoggedIn()) {
            return;
        }
        refreshAll();
    }

    @Override
    public void render(float delta) {
        refreshAccumulator += Math.max(0f, delta);
        if (refreshAccumulator >= 1f) {
            refreshAccumulator = 0f;
            refreshIfChanged();
        }
        super.render(delta);
    }

    private void buildUi() {
        addGreenhouseBackground();
        Table root = createRoot();
        root.pad(8f, 10f, 8f, 10f);
        addResourceBar(root);
        Table panel = createPanel();
        panel.pad(16f, 20f, 16f, 20f);
        panel.add(createTitle("Greenhouse")).colspan(3).padBottom(8f).row();
        Table navigation = new Table();
        navigation.add(new MenuButton("Shop", skin, "green", game.getScreenManager()::showShopFromGreenhouse))
                .width(160f).height(44f);
        navigation.add().expandX().fillX();
        navigation.add(new BackButton(skin, game.getScreenManager()::showMainMenu)).width(160f).height(44f);
        panel.add(navigation).colspan(3).width(1070f).fillX().padBottom(8f).row();
        ScrollPane scrollPane = new ScrollPane(potGrid, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setOverscroll(false, false);
        scrollPane.setScrollingDisabled(true, false);
        panel.add(scrollPane).colspan(3).width(1070f).height(446f).top();
        root.add(panel).width(1185f).height(610f);
    }

    private void addGreenhouseBackground() {
        TextureRegion region = animations.region("IMAGE_BACKGROUNDS_ZEN_TEXTURE");
        if (region == null) {
            addMenuBackground();
            return;
        }
        Image background = new Image(region);
        background.setBounds(0f, 0f, WORLD_WIDTH, WORLD_HEIGHT);
        background.setScaling(Scaling.fill);
        stage.addActor(background);
    }

    private void refreshAll() {
        Greenhouse greenhouse = controller.getCurrentGreenhouse();
        if (greenhouse == null) {
            return;
        }
        greenhouse.updateGrowth();
        rebuildGrid(greenhouse);
        refreshResourceBar();
        lastSignature = greenhouseSignature(greenhouse);
    }

    private void refreshIfChanged() {
        Greenhouse greenhouse = controller.getCurrentGreenhouse();
        if (greenhouse == null) {
            return;
        }
        greenhouse.updateGrowth();
        String signature = greenhouseSignature(greenhouse);
        if (!signature.equals(lastSignature)) {
            rebuildGrid(greenhouse);
            lastSignature = signature;
        }
    }

    private void rebuildGrid(Greenhouse greenhouse) {
        potGrid.clearChildren();
        potGrid.defaults().pad(6f).top();
        int column = 0;
        for (Greenhouse.Pot pot : greenhouse.getAllPots()) {
            potGrid.add(createPotCard(pot)).width(198f).height(224f).top();
            column++;
            if (column % Greenhouse.WIDTH == 0) {
                potGrid.row();
            }
        }
    }

    private Table createPotCard(Greenhouse.Pot pot) {
        BorderedTable card = new BorderedTable();
        card.pad(9f);
        card.setClip(true);
        Label coordinate = new Label("Pot " + pot.getX() + "," + pot.getY(), skin, "secondary");
        coordinate.setAlignment(Align.center);
        card.add(coordinate).width(164f).padBottom(3f).row();
        if (pot.isLocked()) {
            buildLockedPot(card, pot);
        } else if (pot.isEmpty()) {
            buildEmptyPot(card, pot);
        } else if (pot.isReady()) {
            buildReadyPot(card, pot);
        } else {
            buildGrowingPot(card, pot);
        }
        return card;
    }

    private void buildLockedPot(Table card, Greenhouse.Pot pot) {
        Label state = new Label("LOCKED", skin, "medium_outline");
        state.setAlignment(Align.center);
        card.add(state).width(164f).height(70f).padTop(18f).row();
        card.add(new Label(GreenhouseController.POT_PURCHASE_PRICE + " Coins", skin, "secondary"))
                .padTop(4f).row();
        card.add(new MenuButton("Buy Pot", skin, "green_small", () -> confirmPotPurchase(pot)))
                .width(132f).height(36f).padTop(10f).padBottom(8f);
    }

    private void buildEmptyPot(Table card, Greenhouse.Pot pot) {
        Label state = new Label("EMPTY", skin, "medium_outline");
        state.setAlignment(Align.center);
        card.add(state).width(164f).height(92f).padTop(18f).row();
        card.add(new Label("Ready for planting", skin, "secondary")).padTop(4f).row();
        card.add(new MenuButton("Plant", skin, "green_small", () -> showPlantSelection(pot)))
                .width(132f).height(36f).padTop(10f).padBottom(8f);
    }

    private void buildGrowingPot(Table card, Greenhouse.Pot pot) {
        Actor actor = animations.createPlantActor(pot.getPlantName());
        actor.setSize(92f, 92f);
        card.add(actor).size(96f).padTop(2f).row();
        Label name = new Label(pot.getPlantName(), skin, "secondary");
        name.setAlignment(Align.center);
        card.add(name).width(164f).row();
        card.add(new Label(remainingTime(pot), skin, "secondary")).padTop(2f).row();
        int cost = Math.max(1, pot.getRemainingHoursRoundedUp());
        card.add(new MenuButton("Speed Up " + cost, skin, "purple", () -> confirmSpeedUp(pot, cost)))
                .width(144f).height(34f).padTop(6f).padBottom(6f);
    }

    private void buildReadyPot(Table card, Greenhouse.Pot pot) {
        Actor actor = animations.createPlantActor(pot.getPlantName());
        actor.setSize(92f, 92f);
        card.add(actor).size(96f).padTop(2f).row();
        Label state = new Label("READY", skin, "medium_outline");
        state.setAlignment(Align.center);
        card.add(state).width(164f).row();
        card.add(new Label(pot.getPlantName(), skin, "secondary")).padTop(1f).row();
        card.add(new MenuButton("Collect", skin, "green_small", () -> collectReward(pot)))
                .width(132f).height(34f).padTop(6f).padBottom(6f);
    }

    private void confirmPotPurchase(Greenhouse.Pot pot) {
        ConfirmDialog dialog = new ConfirmDialog(
                "Purchase Pot",
                "Buy this pot for " + GreenhouseController.POT_PURCHASE_PRICE + " Coins?",
                skin,
                () -> buyPot(pot)
        );
        dialog.show(stage);
    }

    private void buyPot(Greenhouse.Pot pot) {
        controller.buyPot(pot.getX(), pot.getY());
        showControllerMessage(controller.getLastMessage());
        refreshAll();
    }

    private void showPlantSelection(Greenhouse.Pot pot) {
        PlantSelectionDialog dialog = new PlantSelectionDialog(
                "Choose Plant",
                skin,
                animations,
                controller.getAvailablePlants(),
                true,
                name -> plantInPot(pot, name)
        );
        dialog.show(stage);
    }

    private void plantInPot(Greenhouse.Pot pot, String plantName) {
        controller.plantAt(plantName, pot.getX(), pot.getY());
        showControllerMessage(controller.getLastMessage());
        refreshAll();
    }

    private void confirmSpeedUp(Greenhouse.Pot pot, int cost) {
        ConfirmDialog dialog = new ConfirmDialog(
                "Speed Up",
                "Finish this plant now for " + cost + " Diamonds?",
                skin,
                () -> speedUp(pot)
        );
        dialog.show(stage);
    }

    private void speedUp(Greenhouse.Pot pot) {
        controller.growNow(pot.getX(), pot.getY());
        showControllerMessage(controller.getLastMessage());
        refreshAll();
    }

    private void collectReward(Greenhouse.Pot pot) {
        String plantName = pot.getPlantName();
        boolean marigold = pot.isMarigold();
        int beforeBoost = boostCount(plantName);
        controller.collect(pot.getX(), pot.getY());
        if (!controller.wasSuccessful()) {
            showControllerMessage(controller.getLastMessage());
            refreshAll();
            return;
        }
        int afterBoost = boostCount(plantName);
        String rewardText;
        if (marigold) {
            rewardText = "You received: " + controller.getLastHarvestAmount() + " Coins";
        } else if (afterBoost > beforeBoost) {
            rewardText = "You received: 1 stored boost for " + plantName;
        } else {
            rewardText = "You received: Existing " + plantName + " boost was kept";
        }
        refreshAll();
        new DialogActor("Reward", rewardText, skin).show(stage);
    }

    private int boostCount(String plantName) {
        User user = game.getAuthController().getLoggedInUser();
        if (user == null) {
            return 0;
        }
        PlantData data = user.getCollection().findOwnedPlant(plantName);
        return data == null ? 0 : data.getBoostCount();
    }

    private String remainingTime(Greenhouse.Pot pot) {
        long minutes = Math.max(0L, pot.getRemainingMinutes());
        long hours = minutes / 60L;
        long remainder = minutes % 60L;
        return "Remaining: " + hours + "h " + remainder + "m";
    }

    private String greenhouseSignature(Greenhouse greenhouse) {
        StringBuilder builder = new StringBuilder();
        for (Greenhouse.Pot pot : greenhouse.getAllPots()) {
            builder.append(pot.getX()).append(':')
                    .append(pot.getY()).append(':')
                    .append(pot.getStatus()).append(':')
                    .append(pot.getPlantName()).append(':')
                    .append(pot.getRemainingMinutes()).append('|');
        }
        return builder.toString();
    }
}
