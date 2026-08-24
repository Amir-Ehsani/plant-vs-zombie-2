package screens.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
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
    private static final float POT_WIDTH = 142f;
    private static final float POT_HEIGHT = 120f;

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
                .width(150f).height(42f);
        topLeft.add(navigation).left();

        Table topRight = createRoot();
        topRight.top().right();
        Table rightColumn = new Table();
        addResourceBar(rightColumn);
        rightColumn.row();
        rightColumn.add(new BackButton(skin, game.getScreenManager()::showMainMenu))
                .width(150f).height(42f).right().padTop(8f);
        topRight.add(rightColumn).top().right();

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
        potGrid.defaults().pad(3f);
        for (int y = 1; y <= Greenhouse.HEIGHT; y++) {
            for (int x = 1; x <= Greenhouse.WIDTH; x++) {
                potGrid.add(createPotCard(greenhouse.getPot(x, y))).width(POT_WIDTH).height(POT_HEIGHT);
            }
            potGrid.row();
        }
    }

    private Table createPotCard(Greenhouse.Pot pot) {
        Stack stack = new Stack();
        Image potImage = createPotImage(pot);
        if (potImage != null) {
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

    private Image createPotImage(Greenhouse.Pot pot) {
        String resourceId = pot != null && pot.isReady()
                ? "IMAGE_ZEN_GARDEN_GROWING_PLANT_SLOT_GROWING_PLANT_SLOT_184X161_2"
                : "IMAGE_ZEN_GARDEN_GROWING_PLANT_SLOT_GROWING_PLANT_SLOT_184X161";
        TextureRegion region = animations.region(resourceId);
        if (region == null && pot != null && pot.isReady()) {
            region = animations.region("IMAGE_ZEN_GARDEN_GROWING_PLANT_SLOT_GROWING_PLANT_SLOT_184X161");
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
        TextureRegion lockRegion = animations.region("IMAGE_ZEN_GARDEN_LOCKED_POT_ICON");
        if (lockRegion != null) {
            Image lockImage = new Image(lockRegion);
            lockImage.setScaling(Scaling.fit);
            content.add(lockImage).size(46f, 42f).padBottom(1f).row();
        } else {
            Label state = new Label("LOCKED", skin, "medium_outline");
            state.setAlignment(Align.center);
            content.add(state).width(124f).padBottom(1f).row();
        }
        Label price = new Label(String.valueOf(GreenhouseController.POT_PURCHASE_PRICE), skin, "medium_outline");
        price.setColor(Color.WHITE);
        price.setAlignment(Align.center);
        content.add(price).width(124f).padBottom(1f).row();
        Label currency = new Label("Coins", skin, "secondary");
        currency.setColor(Color.WHITE);
        currency.setAlignment(Align.center);
        content.add(currency).width(124f).padBottom(3f);
        content.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                confirmPotPurchase(pot);
            }
        });
    }

    private void buildEmptyPot(Table content, Greenhouse.Pot pot) {
        content.add().expandY().row();
        TextureRegion plantRegion = animations.region(
                "IMAGE_ZEN_GARDEN_GROWING_PLANT_SLOT_GROWING_PLANT_SLOT_122X161"
        );
        if (plantRegion != null) {
            Image plantButton = new Image(plantRegion);
            plantButton.setScaling(Scaling.fit);
            content.add(plantButton).size(72f, 72f).padBottom(3f).row();
        } else {
            Label state = potLabel("PLANT");
            state.setAlignment(Align.center);
            content.add(state).width(124f).padBottom(3f).row();
        }
        Label hint = potLabel("Tap to plant");
        hint.setAlignment(Align.center);
        content.add(hint).width(124f).padBottom(3f);
        content.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showPlantSelection(pot);
            }
        });
    }

    private void buildGrowingPot(Table content, Greenhouse.Pot pot) {
        Actor actor = animations.createPlantActor(pot.getPlantName());
        actor.setSize(70f, 58f);
        content.add(actor).size(76f, 60f).expandY().bottom().padTop(15f).padBottom(-5f).row();
        Label name = potLabel(pot.getPlantName());
        name.setAlignment(Align.center);
        name.setWrap(true);
        content.add(name).width(124f).height(22f).row();
        Label remaining = potLabel(remainingTime(pot));
        remaining.setAlignment(Align.center);
        content.add(remaining).width(124f).row();
        int cost = Math.max(1, pot.getRemainingHoursRoundedUp());
        content.add(createSpeedUpControl(pot, cost)).width(94f).height(24f).padTop(1f).padBottom(1f);
    }

    private void buildReadyPot(Table content, Greenhouse.Pot pot) {
        Actor actor = animations.createPlantActor(pot.getPlantName());
        actor.setSize(70f, 58f);
        content.add(actor).size(76f, 60f).expandY().bottom().padTop(15f).padBottom(-5f).row();
        Label state = new Label("READY", skin, "medium_outline");
        state.setAlignment(Align.center);
        content.add(state).width(124f).row();
        Label name = potLabel(pot.getPlantName());
        name.setAlignment(Align.center);
        content.add(name).width(124f).row();
        content.add(new MenuButton("Collect", skin, "green_small", () -> collectReward(pot)))
                .width(100f).height(24f).padTop(1f).padBottom(1f);
    }

    private Stack createSpeedUpControl(Greenhouse.Pot pot, int cost) {
        Stack control = new Stack();
        TextureRegion region = animations.region("IMAGE_ZEN_GARDEN_BUTTON_UNLOCK_INACTIVE");
        if (region != null) {
            Image image = new Image(region);
            image.setScaling(Scaling.fill);
            control.add(image);
        }
        Label label = new Label(cost + " Diamonds", skin, "secondary");
        label.setColor(Color.WHITE);
        label.setAlignment(Align.center);
        control.add(label);
        control.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                confirmSpeedUp(pot, cost);
            }
        });
        return control;
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
