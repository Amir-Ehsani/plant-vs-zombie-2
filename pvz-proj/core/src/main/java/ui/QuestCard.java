package ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import models.account.Quest;
import pvz.skin.BorderedTable;

public class QuestCard extends BorderedTable {
    private static final Color TEXT_COLOR = Color.valueOf("4A3A1F");
    private final ProgressBarActor progressBar;
    private final Label progressLabel;
    private final MenuButton claimButton;

    public QuestCard(Skin skin, Quest quest, Runnable claimAction) {
        pad(16f);
        Label name = new Label(quest == null ? "Quest" : quest.getQuestDescription(), skin, "medium_outline");
        name.setAlignment(Align.left);
        name.setColor(TEXT_COLOR);
        name.setWrap(true);
        Label description = label(skin, quest == null ? "" : quest.getConditionDescription());
        description.setWrap(true);
        Label reward = label(skin, "Reward: " + (quest == null ? "-" : quest.rewardText()));
        Label priority = label(skin, "Priority: " + (quest == null ? "-" : quest.getPriority()));
        Label type = label(skin, "Type: " + (quest == null ? "-" : quest.getType()));
        progressBar = new ProgressBarActor(skin, 0f, quest == null ? 1f : Math.max(1, quest.getTargetAmount()));
        progressBar.setTextColor(TEXT_COLOR);
        progressBar.setValue(quest == null ? 0f : quest.getProgressAmount());
        progressLabel = label(skin, progressText(quest));
        progressLabel.setAlignment(Align.center);
        claimButton = new MenuButton("Collect Reward", skin, "green_small", claimAction);
        claimButton.setDisabled(quest == null || !quest.canClaimReward());
        add(name).width(500f).left().row();
        add(description).width(500f).left().padTop(5f).row();
        Table metadata = new Table();
        metadata.add(priority).left().padRight(12f);
        metadata.add(type).left();
        add(metadata).left().padTop(6f).row();
        add(reward).width(500f).left().padTop(5f).row();
        add(progressLabel).width(500f).padTop(7f).row();
        add(progressBar).width(360f).padTop(2f).row();
        add(claimButton).width(170f).height(38f).padTop(8f);
    }

    private Label label(Skin skin, String text) {
        Label label = new Label(text == null ? "" : text, skin, "secondary");
        label.setColor(TEXT_COLOR);
        return label;
    }

    private String progressText(Quest quest) {
        if (quest == null) {
            return "Progress: 0 / 1";
        }
        String suffix = quest.isRewardClaimed() ? " | Claimed" : quest.isCompleted() ? " | Complete" : "";
        return "Progress: " + quest.getProgressAmount() + " / " + quest.getTargetAmount() + suffix;
    }
}
