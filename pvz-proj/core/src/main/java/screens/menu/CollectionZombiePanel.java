package screens.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import controllers.auth.AuthController;
import models.account.User;
import models.core.zombie.DefaultZombieRegistry;
import models.core.zombie.ZombieRegistry;
import models.core.zombie.ZombieType;
import pvz.skin.BorderedTable;
import ui.PvzAnimationService;
import ui.ZombieCard;

import java.util.List;

public class CollectionZombiePanel extends Table {
    private static final Color PANEL_TEXT_COLOR = Color.valueOf("4A3A1F");
    private final Skin skin;
    private final AuthController authController;
    private final ZombieRegistry zombieRegistry;
    private final PvzAnimationService animations;
    private final Table cardsTable;
    private final Table detailsTable;
    private String selectedZombieName;

    public CollectionZombiePanel(
            Skin skin,
            AuthController authController,
            PvzAnimationService animations
    ) {
        this.skin = skin;
        this.authController = authController;
        zombieRegistry = DefaultZombieRegistry.getInstance();
        this.animations = animations;
        cardsTable = new Table();
        detailsTable = new Table();
        selectedZombieName = "";
        buildUi();
        refresh();
    }

    public void refresh() {
        List<ZombieType> types = zombieRegistry.getAllZombieTypes();
        ensureSelection(types);
        refreshCards(types);
        refreshDetails();
    }

    private void buildUi() {
        defaults().pad(5f);
        ScrollPane cardsScroll = new ScrollPane(cardsTable, skin);
        cardsScroll.setFadeScrollBars(false);
        ScrollPane detailsScroll = new ScrollPane(detailsTable, skin);
        detailsScroll.setFadeScrollBars(false);
        add(cardsScroll).width(700f).height(475f).top();
        add(detailsScroll).width(405f).height(475f).top();
    }

    private void ensureSelection(List<ZombieType> types) {
        if (findType(types, selectedZombieName) != null) {
            return;
        }
        selectedZombieName = types.isEmpty() ? "" : types.get(0).getName();
    }

    private void refreshCards(List<ZombieType> types) {
        cardsTable.clearChildren();
        int column = 0;
        for (ZombieType type : types) {
            boolean discovered = isZombieDiscovered(type.getName());
            ZombieCard card = createCard(type, discovered);
            cardsTable.add(card).width(210f).pad(6f).top();
            column++;
            if (column % 3 == 0) {
                cardsTable.row();
            }
        }
    }

    private ZombieCard createCard(ZombieType type, boolean discovered) {
        ZombieCard card = new ZombieCard(skin);
        card.setDiscovered(discovered);
        card.setName(type.getName());
        if (discovered) {
            card.setZombieActor(animations.createZombieActor(type.getName()));
        }
        card.setOnClick(() -> selectZombie(type.getName()));
        return card;
    }

    private void selectZombie(String zombieName) {
        selectedZombieName = zombieName;
        refreshDetails();
    }

    private void refreshDetails() {
        detailsTable.clearChildren();
        ZombieType type = zombieRegistry.getZombieTypeByName(selectedZombieName);
        if (type == null) {
            detailsTable.add(panelLabel("Select a zombie to view details."));
            return;
        }
        boolean discovered = isZombieDiscovered(type.getName());
        if (!discovered) {
            buildUnknownDetails();
            return;
        }
        buildDiscoveredDetails(type);
    }

    private void buildUnknownDetails() {
        BorderedTable panel = new BorderedTable();
        panel.pad(20f);
        panel.add(new Label("?", skin, "big_outline")).padBottom(14f).row();
        panel.add(panelLabel("Unknown Zombie")).row();
        panel.add(panelLabel("Discover this zombie during gameplay to reveal its information."))
                .width(320f).padTop(8f).row();
        detailsTable.add(panel).width(380f).top();
    }

    private void buildDiscoveredDetails(ZombieType type) {
        BorderedTable panel = new BorderedTable();
        panel.pad(18f);
        panel.add(new Label(type.getName(), skin, "medium_outline")).padBottom(8f).row();
        panel.add(animations.createZombieActor(type.getName())).size(190f).padBottom(8f).row();
        addDetail(panel, "Health", String.valueOf(type.getBaseHp()));
        addDetail(panel, "Speed", formatSpeed(type.getSpeed()));
        addDetail(panel, "Armor", safeText(type.getDefaultArmorName()));
        addDetail(panel, "Damage / Tick", String.valueOf(type.getDamagePerTick()));
        addDetail(panel, "Wave Cost", String.valueOf(type.getWaveCost()));
        addDetail(panel, "Tags", type.getTags().isEmpty() ? "-" : String.join(", ", type.getTags()));
        addDetail(panel, "Ability", safeText(type.getAbility()));
        detailsTable.add(panel).width(380f).top();
    }

    private void addDetail(Table panel, String title, String value) {
        Label label = panelLabel(title + ": " + safeText(value));
        label.setWrap(true);
        label.setAlignment(Align.left);
        panel.add(label).width(330f).left().padTop(4f).row();
    }

    private Label panelLabel(String text) {
        Label label = new Label(text, skin, "secondary");
        label.setColor(PANEL_TEXT_COLOR);
        label.setWrap(true);
        return label;
    }

    private boolean isZombieDiscovered(String zombieName) {
        User user = authController.getLoggedInUser();
        return user != null && user.getCollection().hasOwnedZombie(zombieName);
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private String formatSpeed(double speed) {
        return String.format("%.3f", speed);
    }

    private ZombieType findType(List<ZombieType> types, String name) {
        for (ZombieType type : types) {
            if (type.getName().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
