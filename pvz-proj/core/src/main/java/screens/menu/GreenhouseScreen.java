package screens.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.pvz.Main;
import controllers.features.GreenhouseController;
import models.account.Greenhouse;
import models.account.PlantData;
import models.account.User;
import ui.BackButton;
import ui.ConfirmDialog;
import ui.DialogActor;
import ui.MenuButton;
import ui.PlantSelectionDialog;
import ui.PvzAnimationService;

public class GreenhouseScreen extends BaseMenuScreen {
    private static final Color POT_TEXT_COLOR = Color.valueOf("4A3A1F");
    private static final float POT_WIDTH = 158f;
    private static final float POT_HEIGHT = 132f;

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

        Table topLeft = createRoot();
        topLeft.top().left();
        topLeft.add(createTitle("Greenhouse")).left().padBottom(2f).row();
        Label subtitle = new Label("Grow Your Plant Heroes", skin, "secondary");
        subtitle.setColor(POT_TEXT_COLOR);
        topLeft.add(subtitle).left().padBottom(8f).row();
        Table navigation = new Table();
        navigation.add(new MenuButton("Shop", skin, "green", game.getScreenManager()::showShopFromGreenhouse))
                .width(150f).height(42f).padRight(8f);
        navigation.add(new BackButton(skin, game.getScreenManager()::showMainMenu))
                .width(150f).height(42f);
        topLeft.add(navigation).left();

        Table topRight = createRoot();
        topRight.top().right();
        addResourceBar(topRight);

        Table board = createRoot();
        board.center();
        board.padTop(68f);
        board.add(potGrid).center();
    }

    private void addGreenhouseBackground() {
        TextureRegion region = animations.region("IMAGE_BACKGROUNDS_ZEN_GARDEN");
        if (region == null) {
            region = animations.region("IMAGE_BACKGROUNDS_ZEN_TEXTURE");
        }
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
            refreshResourceBar();
            lastSignature = signature;
        }
    }

    private void rebuildGrid(Greenhouse greenhouse) {
        potGrid.clearChildren();
        potGrid.defaults().pad(4f);
        for (int y = 1; y <= Greenhouse.HEIGHT; y++) {
            for (int x = 1; x <= Greenhouse.WIDTH; x++) {
                potGrid.add(createPotCard(greenhouse.getPot(x, y))).width(POT_WIDTH).height(POT_HEIGHT);
            }
            potGrid.row();
        }
    }

    private Table createPotCard(Greenhouse.Pot pot) {
        Stack stack = new Stack();
        Image potImage = createPotImage();
        if (potImage != null) {
            if (pot.isLocked()) {
                potImage.setColor(0.62f, 0.62f, 0.62f, 1f);
            }
            stack.add(potImage);
        }

        Table content = new Table();
        content.pad(5f, 7f, 5f, 7f);
        if (pot.isLocked()) {
            buildLockedPot(content, pot);
        } else if (pot.isEmpty()) {
            buildEmptyPot(content, pot);
        } else if (pot.isReady()) {
            buildReadyPot(content, pot);
        } else {
            buildGrowingPot(content, pot);
        }
        stack.add(content);

        Table card = new Table();
        card.add(stack).grow();
        return card;
    }

    private Image createPotImage() {
        TextureRegion region = animations.region(
                "IMAGE_ZEN_GARDEN_GROWING_PLANT_SLOT_GROWING_PLANT_SLOT_184X161"
        );
        if (region == null) {
            region = animations.region(
                    "IMAGE_ZEN_GARDEN_GROWING_PLANT_SLOT_GROWING_PLANT_SLOT_184X161_2"
            );
        }
        if (region == null) {
            return null;
        }
        Image image = new Image(region);
        image.setScaling(Scaling.fill);
        return image;
    }

    private void buildLockedPot(Table content, Greenhouse.Pot pot) {
        content.add().expandY().row();
        Label state = new Label("LOCKED", skin, "medium_outline");
        state.setAlignment(Align.center);
        content.add(state).width(136f).padBottom(1f).row();
        Label price = potLabel(GreenhouseController.POT_PURCHASE_PRICE + " Coins");
        price.setAlignment(Align.center);
        content.add(price).width(136f).padBottom(3f).row();
        content.add(new MenuButton("Buy Pot", skin, "green_small", () -> confirmPotPurchase(pot)))
                .width(108f).height(28f).padBottom(3f);
    }

    private void buildEmptyPot(Table content, Greenhouse.Pot pot) {
        content.add().expandY().row();
        Label state = potLabel("EMPTY");
        state.setAlignment(Align.center);
        content.add(state).width(136f).padBottom(3f).row();
        content.add(new MenuButton("Plant", skin, "green_small", () -> showPlantSelection(pot)))
                .width(108f).height(28f).padBottom(3f);
    }

    private void buildGrowingPot(Table content, Greenhouse.Pot pot) {
        Actor actor = animations.createPlantActor(pot.getPlantName());
        actor.setSize(76f, 66f);
        content.add(actor).size(82f, 68f).expandY().bottom().padTop(5f).row();
        Label name = potLabel(pot.getPlantName());
        name.setAlignment(Align.center);
        name.setWrap(true);
        content.add(name).width(136f).height(24f).row();
        Label remaining = potLabel(remainingTime(pot));
        remaining.setAlignment(Align.center);
        content.add(remaining).width(136f).row();
        int cost = Math.max(1, pot.getRemainingHoursRoundedUp());
        content.add(new MenuButton("Speed Up " + cost, skin, "purple", () -> confirmSpeedUp(pot, cost)))
                .width(114f).height(26f).padTop(2f).padBottom(2f);
    }

    private void buildReadyPot(Table content, Greenhouse.Pot pot) {
        Actor actor = animations.createPlantActor(pot.getPlantName());
        actor.setSize(76f, 66f);
        content.add(actor).size(82f, 68f).expandY().bottom().padTop(5f).row();
        Label state = new Label("READY", skin, "medium_outline");
        state.setAlignment(Align.center);
        content.add(state).width(136f).row();
        Label name = potLabel(pot.getPlantName());
        name.setAlignment(Align.center);
        content.add(name).width(136f).row();
        content.add(new MenuButton("Collect", skin, "green_small", () -> collectReward(pot)))
                .width(108f).height(26f).padTop(2f).padBottom(2f);
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
        return hours + "h " + remainder + "m";
    }

    private Label potLabel(String text) {
        Label label = new Label(text == null ? "" : text, skin, "secondary");
        label.setColor(POT_TEXT_COLOR);
        return label;
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
