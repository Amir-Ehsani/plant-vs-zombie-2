package ui;

import boss.core.Boss;
import boss.core.BossRuntime;
import boss.core.BossState;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import game.animation.core.PvzAnimationService;

/** Presentation-only UI for boss identity, phases, intro and stunned feedback. */
public final class BossPresentationOverlay {
    private static final String PORTRAIT_ID = "IMAGE_UI_PENNY_PURSUITS_ZPS_ZOMBOSS_METER_ICON";
    private static final Color TITLE_COLOR = Color.valueOf("FFE45A");
    private static final Color STUN_COLOR = Color.valueOf("FFB329");

    private final Stage stage;
    private final Skin skin;
    private final BossRuntime runtime;
    private final TextureRegion portraitRegion;
    private final Table identityRoot;
    private final Label phaseLabel;
    private final Label stateLabel;
    private BossState lastState;
    private int lastSectionBreakSerial;

    public BossPresentationOverlay(
            Stage stage,
            Skin skin,
            PvzAnimationService animations,
            BossRuntime runtime
    ) {
        if (stage == null || skin == null || animations == null || runtime == null) {
            throw new IllegalArgumentException("Boss presentation requires stage, skin, animations and runtime.");
        }
        this.stage = stage;
        this.skin = skin;
        this.runtime = runtime;
        portraitRegion = animations.region(PORTRAIT_ID);

        Boss boss = runtime.getBoss();
        identityRoot = new Table();
        identityRoot.setFillParent(true);
        identityRoot.top();
        identityRoot.padTop(62f);
        identityRoot.setTouchable(Touchable.disabled);

        Table identity = new Table();
        identity.setTouchable(Touchable.disabled);
        if (portraitRegion != null) {
            Image portrait = new Image(portraitRegion);
            portrait.setScaling(Scaling.fit);
            identity.add(portrait).size(48f).padRight(8f);
        }

        Table text = new Table();
        Label nameLabel = new Label(boss.getDisplayName(), skin, "medium_outline");
        nameLabel.setColor(Color.WHITE);
        nameLabel.setFontScale(0.62f);
        nameLabel.setAlignment(Align.center);
        phaseLabel = new Label("", skin, "medium_outline");
        phaseLabel.setColor(TITLE_COLOR);
        phaseLabel.setFontScale(0.52f);
        phaseLabel.setAlignment(Align.center);
        text.add(nameLabel).center().row();
        text.add(phaseLabel).center().padTop(-3f);
        identity.add(text);

        stateLabel = new Label("STUNNED!", skin, "big_outline");
        stateLabel.setColor(STUN_COLOR);
        stateLabel.setAlignment(Align.center);
        stateLabel.setFontScale(0.68f);
        stateLabel.setVisible(false);

        identityRoot.add(identity).center().row();
        identityRoot.add(stateLabel).center().padTop(-8f);
        stage.addActor(identityRoot);

        lastState = boss.getState();
        lastSectionBreakSerial = boss.getHealth().getSectionBreakSerial();
        updatePhase();
    }

    public void update() {
        Boss boss = runtime.getBoss();
        updatePhase();
        BossState state = boss.getState();
        stateLabel.setVisible(state == BossState.STUNNED);

        int sectionSerial = boss.getHealth().getSectionBreakSerial();
        if (state == BossState.STUNNED
                && (lastState != BossState.STUNNED || sectionSerial != lastSectionBreakSerial)) {
            showStunnedCallout();
        }
        lastState = state;
        lastSectionBreakSerial = sectionSerial;
    }

    public void showIntro() {
        Boss boss = runtime.getBoss();
        RealtimeTable banner = createBanner();
        if (portraitRegion != null) {
            Image portrait = new Image(portraitRegion);
            portrait.setScaling(Scaling.fit);
            banner.add(portrait).size(138f).padRight(18f);
        }
        Table words = new Table();
        Label title = new Label("BOSS BATTLE", skin, "big_outline");
        title.setColor(TITLE_COLOR);
        title.setAlignment(Align.center);
        Label name = new Label(boss.getDisplayName(), skin, "medium_outline");
        name.setColor(Color.WHITE);
        name.setAlignment(Align.center);
        words.add(title).center().row();
        words.add(name).center().padTop(6f).row();
        Label line = new Label(introLine(boss), skin, "medium_outline");
        line.setColor(Color.WHITE);
        line.setFontScale(0.64f);
        line.setAlignment(Align.center);
        line.setWrap(true);
        words.add(line).width(430f).center().padTop(8f);
        banner.add(words);
        showTransient(banner, 2.1f);
    }

    private void updatePhase() {
        int phase = runtime.getBoss().getHealth().getCurrentSectionNumber();
        phaseLabel.setText("PHASE " + phase + "/3");
    }

    private void showStunnedCallout() {
        Boss boss = runtime.getBoss();
        RealtimeTable banner = createBanner();
        if (portraitRegion != null) {
            Image portrait = new Image(portraitRegion);
            portrait.setScaling(Scaling.fit);
            banner.add(portrait).size(96f).padRight(12f);
        }
        Table words = new Table();
        Label stunned = new Label("STUNNED!", skin, "big_outline");
        stunned.setColor(STUN_COLOR);
        stunned.setAlignment(Align.center);
        Label taunt = new Label(stunLine(boss), skin, "medium_outline");
        taunt.setColor(Color.WHITE);
        taunt.setFontScale(0.62f);
        taunt.setWrap(true);
        taunt.setAlignment(Align.center);
        words.add(stunned).center().row();
        words.add(taunt).width(390f).center().padTop(5f);
        banner.add(words);
        showTransient(banner, 1.55f);
    }

    private RealtimeTable createBanner() {
        RealtimeTable banner = new RealtimeTable();
        banner.setFillParent(true);
        banner.center();
        banner.setTouchable(Touchable.disabled);
        banner.setTransform(true);
        banner.setOrigin(Align.center);
        return banner;
    }

    private void showTransient(RealtimeTable banner, float holdSeconds) {
        banner.getColor().a = 0f;
        banner.setScale(0.88f);
        stage.addActor(banner);
        banner.toFront();
        banner.addAction(Actions.sequence(
                Actions.parallel(
                        Actions.fadeIn(0.16f, Interpolation.fade),
                        Actions.scaleTo(1f, 1f, 0.18f, Interpolation.swingOut)
                ),
                Actions.delay(holdSeconds),
                Actions.parallel(
                        Actions.fadeOut(0.20f, Interpolation.fade),
                        Actions.scaleTo(1.04f, 1.04f, 0.20f, Interpolation.sineIn)
                ),
                Actions.removeActor()
        ));
    }

    private String introLine(Boss boss) {
        return switch (boss.getId()) {
            case "zomboss-egypt" -> "Dr. Zomboss brings the Sphinx-inator into battle!";
            case "zomboss-frostbite" -> "The Tuskmaster is ready to freeze the lawn solid!";
            case "zomboss-dark" -> "The Dark Dragon descends over the battlefield!";
            case "zomboss-beach" -> "The Sharktronic Sub is surfacing for battle!";
            default -> "Dr. Zomboss has entered the battlefield!";
        };
    }

    private String stunLine(Boss boss) {
        return switch (boss.getId()) {
            case "zomboss-egypt" -> "My magnificent machine has merely encountered a tiny setback!";
            case "zomboss-frostbite" -> "A temporary malfunction! The ice age is not over!";
            case "zomboss-dark" -> "You dare dent the Dark Dragon?";
            case "zomboss-beach" -> "You have not sunk my plans yet!";
            default -> "You have only delayed the inevitable!";
        };
    }

    private static final class RealtimeTable extends Table {
        @Override
        public void act(float delta) {
            float uiDelta = delta > 0f
                    ? delta
                    : Math.min(Math.max(Gdx.graphics.getDeltaTime(), 0f), 1f / 15f);
            super.act(uiDelta);
        }
    }
}
