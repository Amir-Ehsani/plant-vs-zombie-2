package screens.menu;

import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.pvz.Main;
import controllers.features.SettingsController;
import models.account.Settings;
import ui.BackButton;
import ui.MenuButton;

public class SettingsScreen extends BaseMenuScreen {
    private final SettingsController controller;
    private final Label difficultyValue;
    private final Label gameSpeedValue;
    private final Label musicVolumeValue;
    private final Label soundVolumeValue;
    private final CheckBox gridBox;
    private final CheckBox debugBox;
    private final CheckBox musicEnabledBox;
    private final Slider musicVolumeSlider;
    private final Slider soundVolumeSlider;
    private boolean refreshing;

    public SettingsScreen(Main game) {
        super(game);
        controller = game.getSettingsController();
        difficultyValue = createLabel("");
        gameSpeedValue = createLabel("");
        musicVolumeValue = createLabel("");
        soundVolumeValue = createLabel("");
        gridBox = new CheckBox(" Show grid", skin, "default");
        debugBox = new CheckBox(" Debug mode", skin, "default");
        musicEnabledBox = new CheckBox(" Music enabled", skin, "default");
        musicVolumeSlider = new Slider(0f, 1f, 0.1f, false, skin, "default-horizontal");
        soundVolumeSlider = new Slider(0f, 1f, 0.1f, false, skin, "default-horizontal");
        refreshing = false;
        buildUi();
        bindListeners();
        refreshValues();
    }

    @Override
    public void show() {
        super.show();
        if (!requireLoggedIn()) {
            return;
        }
        refreshValues();
    }

    private void buildUi() {
        addMenuBackground();
        Table root = createRoot();
        addResourceBar(root);
        Table panel = createPanel();
        panel.defaults().padBottom(12f);
        panel.add(createTitle("Settings")).colspan(3).padBottom(16f).row();
        addDifficultyControls(panel);
        addGameSpeedControls(panel);
        addToggleControls(panel);
        addVolumeControls(panel);
        panel.add(new BackButton(skin, game.getScreenManager()::showMainMenu))
                .colspan(3).width(220f).height(48f).padTop(12f);
        ScrollPane scrollPane = new ScrollPane(panel, skin);
        scrollPane.setFadeScrollBars(false);
        root.add(scrollPane).width(900f).height(540f);
    }

    private void addDifficultyControls(Table panel) {
        panel.add(createLabel("Difficulty")).left().width(220f);
        panel.add(difficultyValue).width(90f);
        Table buttons = new Table();
        for (int value = Settings.MIN_DIFFICULTY; value <= Settings.MAX_DIFFICULTY; value++) {
            int difficulty = value;
            buttons.add(new MenuButton(String.valueOf(value), skin, "green_small",
                    () -> changeDifficulty(difficulty))).size(62f, 40f).padRight(6f);
        }
        panel.add(buttons).left().row();
    }

    private void addGameSpeedControls(Table panel) {
        panel.add(createLabel("Game Speed")).left().width(220f);
        panel.add(gameSpeedValue).width(90f);
        Table buttons = new Table();
        for (int value = Settings.MIN_GAME_SPEED; value <= Settings.MAX_GAME_SPEED; value++) {
            int gameSpeed = value;
            buttons.add(new MenuButton(String.valueOf(value) + "x", skin, "purple",
                    () -> changeGameSpeed(gameSpeed))).size(76f, 40f).padRight(6f);
        }
        panel.add(buttons).left().row();
    }

    private void addToggleControls(Table panel) {
        panel.add(createLabel("Grid")).left().width(220f);
        panel.add(gridBox).colspan(2).left().row();
        panel.add(createLabel("Debug")).left().width(220f);
        panel.add(debugBox).colspan(2).left().row();
        panel.add(createLabel("Music")).left().width(220f);
        panel.add(musicEnabledBox).colspan(2).left().row();
    }

    private void addVolumeControls(Table panel) {
        panel.add(createLabel("Music Volume")).left().width(220f);
        panel.add(musicVolumeValue).width(90f);
        panel.add(musicVolumeSlider).width(360f).row();
        panel.add(createLabel("Sound Volume")).left().width(220f);
        panel.add(soundVolumeValue).width(90f);
        panel.add(soundVolumeSlider).width(360f).row();
    }

    private void bindListeners() {
        gridBox.addListener(changeListener(() -> controller.setGridVisible(gridBox.isChecked())));
        debugBox.addListener(changeListener(() -> changeDebugMode(debugBox.isChecked())));
        musicEnabledBox.addListener(changeListener(() -> controller.setMusicEnabled(musicEnabledBox.isChecked())));
        musicVolumeSlider.addListener(changeListener(this::changeMusicVolume));
        soundVolumeSlider.addListener(changeListener(this::changeSoundVolume));
    }

    private ChangeListener changeListener(Runnable action) {
        return new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                if (!refreshing) {
                    action.run();
                }
            }
        };
    }

    private void changeDifficulty(int value) {
        controller.changeDifficulty(value);
        showControllerMessage(controller.getLastMessage());
        refreshValues();
    }

    private void changeGameSpeed(int value) {
        controller.changeGameSpeed(value);
        showControllerMessage(controller.getLastMessage());
        refreshValues();
    }

    private void changeDebugMode(boolean enabled) {
        controller.setDebugMode(enabled);
        showControllerMessage(controller.getLastMessage());
        refreshResourceBar();
    }

    private void changeMusicVolume() {
        float value = musicVolumeSlider.getValue();
        controller.setMusicVolume(value);
        musicVolumeValue.setText(volumeText(value));
    }

    private void changeSoundVolume() {
        float value = soundVolumeSlider.getValue();
        controller.setSoundVolume(value);
        soundVolumeValue.setText(volumeText(value));
    }

    private void refreshValues() {
        Settings settings = controller.getSettings();
        if (settings == null) {
            return;
        }
        refreshing = true;
        difficultyValue.setText(String.valueOf(settings.getDifficulty()));
        gameSpeedValue.setText(settings.getGameSpeed() + "x");
        gridBox.setChecked(settings.isGridVisible());
        debugBox.setChecked(settings.isDebugMode());
        musicEnabledBox.setChecked(settings.isMusicEnabled());
        musicVolumeSlider.setValue(settings.getMusicVolume());
        soundVolumeSlider.setValue(settings.getSoundVolume());
        musicVolumeValue.setText(volumeText(settings.getMusicVolume()));
        soundVolumeValue.setText(volumeText(settings.getSoundVolume()));
        refreshing = false;
        refreshResourceBar();
    }

    private String volumeText(float volume) {
        return Math.round(volume * 100f) + "%";
    }
}
