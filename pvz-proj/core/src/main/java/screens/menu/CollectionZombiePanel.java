package screens.menu;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
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
    private final ScrollPane detailsScroll;
    private final ScrollPane cardsScroll;
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
        cardsTable.top();
        detailsTable = new Table();
        detailsTable.top();
        detailsScroll = new ScrollPane(detailsTable, skin);
        detailsScroll.setFadeScrollBars(false);
        detailsScroll.setOverscroll(false, false);
        cardsScroll = new ScrollPane(cardsTable, skin);
        selectedZombieName = "";
        setClip(true);
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
        defaults().pad(3f);
        cardsScroll.setFadeScrollBars(false);
        cardsScroll.setScrollingDisabled(true, false);
        cardsScroll.setOverscroll(false, false);
        add(cardsScroll).width(708f).height(394f).top().left().padRight(8f);
        add(detailsScroll).width(346f).height(394f).top().left();
    }

    private void ensureSelection(List<ZombieType> types) {
        if (findType(types, selectedZombieName) != null) {
            return;
        }
        selectedZombieName = types.isEmpty() ? "" : types.get(0).getName();
    }

    private void refreshCards(List<ZombieType> types) {
        cardsTable.clearChildren();
        cardsTable.defaults().pad(5f).top();
        int column = 0;
        for (ZombieType type : types) {
            boolean discovered = isZombieDiscovered(type.getName());
            ZombieCard card = createCard(type, discovered);
            cardsTable.add(card).width(210f).top();
            column++;
            if (column % 3 == 0) {
                cardsTable.row();
            }
        }
        cardsScroll.layout();
    }

    private ZombieCard createCard(ZombieType type, boolean discovered) {
        ZombieCard card = new ZombieCard(skin);
        card.setDiscovered(discovered);
        card.setName(type.getName());
        card.setSelected(type.getName().equalsIgnoreCase(selectedZombieName));
        if (discovered) {
            card.setZombieActor(animations.createZombieActor(type.getName()));
        }
        card.setOnClick(() -> selectZombie(type.getName()));
        return card;
    }

    private void selectZombie(String zombieName) {
        selectedZombieName = zombieName;
        refreshCards(zombieRegistry.getAllZombieTypes());
        refreshDetails();
    }

    private void refreshDetails() {
        detailsTable.clearChildren();
        detailsTable.top();
        ZombieType type = zombieRegistry.getZombieTypeByName(selectedZombieName);
        if (type == null) {
            detailsTable.add(panelLabel("Select a zombie to view details.")).padTop(16f).center();
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
        panel.pad(16f);
        panel.add(new Label("?", skin, "big_outline")).padBottom(10f).row();
        panel.add(panelLabel("Unknown Zombie")).padBottom(4f).row();
        Label hint = panelLabel("Discover this zombie during gameplay to reveal its information.");
        hint.setWrap(true);
        hint.setAlignment(Align.center);
        panel.add(hint).width(280f).padTop(4f).row();
        detailsTable.add(panel).width(330f).top().center();
    }

    private void buildDiscoveredDetails(ZombieType type) {
        BorderedTable panel = new BorderedTable();
        panel.pad(14f);
        panel.defaults().pad(1f);
        Label title = new Label(type.getName(), skin, "medium_outline");
        title.setAlignment(Align.center);
        title.setWrap(true);
        panel.add(title).width(280f).padBottom(4f).row();
        panel.add(animations.createZombieActor(type.getName())).size(84f).padBottom(4f).row();
        addPair(panel, "Health", String.valueOf(type.getBaseHp()), "Speed", formatSpeed(type.getSpeed()));
        addPair(panel, "Wave Cost", String.valueOf(type.getWaveCost()), "Damage", String.valueOf(type.getDamagePerTick()));
        addWide(panel, "Armor", safeText(type.getDefaultArmorName()));
        addWide(panel, "Tags", type.getTags().isEmpty() ? "-" : String.join(", ", type.getTags()));
        addWide(panel, "Ability", safeText(type.getAbility()));
        detailsTable.add(panel).width(330f).top().center();
    }

    private void addPair(Table panel, String leftTitle, String leftValue, String rightTitle, String rightValue) {
        Table row = new Table();
        row.defaults().pad(1f);
        row.add(detailTitle(leftTitle)).width(78f).right();
        row.add(detailValue(leftValue)).width(68f).left().padRight(4f);
        row.add(detailTitle(rightTitle)).width(78f).right();
        row.add(detailValue(rightValue)).width(74f).left();
        panel.add(row).width(305f).left().row();
    }

    private void addWide(Table panel, String title, String value) {
        Table row = new Table();
        row.defaults().pad(1f);
        row.add(detailTitle(title)).width(84f).top().right().padRight(3f);
        row.add(detailValue(value)).width(214f).left();
        panel.add(row).width(305f).left().row();
    }

    private Label detailTitle(String text) {
        Label label = new Label(text + ":", skin, "secondary");
        label.setColor(PANEL_TEXT_COLOR);
        label.setAlignment(Align.right);
        return label;
    }

    private Label detailValue(String text) {
        Label label = new Label(safeText(text), skin, "secondary");
        label.setColor(PANEL_TEXT_COLOR);
        label.setWrap(true);
        label.setAlignment(Align.left);
        return label;
    }

    private Label panelLabel(String text) {
        Label label = new Label(text, skin, "secondary");
        label.setColor(PANEL_TEXT_COLOR);
        label.setWrap(true);
        label.setAlignment(Align.center);
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
