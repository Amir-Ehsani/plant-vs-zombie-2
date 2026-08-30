package ui;

import audio.AudioCue;
import audio.AudioManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

/** Boss-specific result overlay with boss identity and portrait. */
public final class BossGameOverDialog extends ModalWindow {
    public BossGameOverDialog(
            Skin skin,
            boolean victory,
            String bossName,
            TextureRegion bossPortrait,
            Runnable primaryAction,
            Runnable exitAction
    ) {
        super(victory ? "Boss Defeated" : "Boss Victory", skin);
        AudioManager.playGlobal(victory ? AudioCue.WIN : AudioCue.LOSE);

        Table content = getContentTable();
        if (bossPortrait != null) {
            Image portrait = new Image(bossPortrait);
            portrait.setScaling(Scaling.fit);
            content.add(portrait).width(160f).height(160f).padRight(18f);
        }

        Table words = new Table();
        Label name = new Label(bossName == null ? "Dr. Zomboss" : bossName, skin, "big_outline");
        name.setColor(Color.valueOf("FFE45A"));
        name.setAlignment(Align.center);
        name.setWrap(true);
        words.add(name).width(430f).center().row();

        Label result = new Label(
                victory
                        ? "The boss machine is down. The lawn is safe!"
                        : "The boss broke through the defense. Retry the battle!",
                skin,
                "medium_outline"
        );
        result.setColor(Color.WHITE);
        result.setAlignment(Align.center);
        result.setWrap(true);
        words.add(result).width(430f).center().padTop(10f).row();

        Table actions = new Table();
        actions.add(new MenuButton("Exit", skin, "brown", exitAction))
                .width(190f).height(48f).padRight(8f);
        actions.add(new MenuButton(victory ? "Continue" : "Retry", skin, "green", primaryAction))
                .width(190f).height(48f);
        words.add(actions).center().padTop(18f);
        content.add(words).width(450f);
    }
}
