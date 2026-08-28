package game.dialogue;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import game.animation.core.PvzAnimationService;
import models.level.core.AdventureLevelCatalog;
import models.minigame.MiniGameType;
import ui.MenuButton;
import ui.ModalWindow;

import java.util.ArrayList;
import java.util.List;

public final class LevelDialogueController {
    private static final String DAVE_IMAGE = "IMAGE_UI_MAINMENU_DAVE_WAIST_CROP";
    private static final String PENNY_IMAGE = "IMAGE_UI_PENNY_PURSUITS_ZOMBOSS_PENNY";
    private static final String ZOMBOSS_IMAGE = "IMAGE_UI_PENNY_PURSUITS_ZPS_ZOMBOSS_METER_ICON";

    private final Stage stage;
    private final Skin skin;
    private final PvzAnimationService animations;
    private DialogueWindow window;
    private List<DialogueLine> lines = List.of();
    private int index;
    private Runnable completion;

    public LevelDialogueController(Stage stage, Skin skin, PvzAnimationService animations) {
        if (stage == null || skin == null || animations == null) {
            throw new IllegalArgumentException("Dialogue controller requires stage, skin and animations.");
        }
        this.stage = stage;
        this.skin = skin;
        this.animations = animations;
    }

    public boolean showIntro(String chapterName, int levelNumber, Runnable onComplete) {
        List<DialogueLine> intro = introLines(chapterName, levelNumber);
        if (intro.isEmpty()) {
            return false;
        }
        show(intro, onComplete);
        return true;
    }


    public boolean showMiniGameIntro(MiniGameType type, int stageNumber, Runnable onComplete) {
        if (type == null) {
            return false;
        }
        List<DialogueLine> intro = miniGameIntroLines(type, stageNumber);
        if (intro.isEmpty()) {
            return false;
        }
        show(intro, onComplete);
        return true;
    }

    public boolean showBossOutro(boolean victory, Runnable onComplete) {
        List<DialogueLine> outro = new ArrayList<>();
        if (victory) {
            outro.add(new DialogueLine("Zomboss", "Impossible! My brilliant plan was flawless!", ZOMBOSS_IMAGE));
            outro.add(new DialogueLine(
                    "Zomboss",
                    "Enjoy this victory. I will return with something much worse.",
                    ZOMBOSS_IMAGE
            ));
            outro.add(new DialogueLine("Crazy Dave", "Wabby wabbo! We did it!", DAVE_IMAGE));
        } else {
            outro.add(new DialogueLine("Zomboss", "Another lawn falls before my superior intellect.", ZOMBOSS_IMAGE));
            outro.add(new DialogueLine(
                    "Penny",
                    "We can retry. I have already recalculated our strategy.",
                    PENNY_IMAGE
            ));
        }
        show(outro, onComplete);
        return true;
    }

    public boolean isShowing() {
        return window != null && window.getStage() != null;
    }

    private void show(List<DialogueLine> source, Runnable onComplete) {
        close();
        lines = new ArrayList<>(source);
        index = 0;
        completion = onComplete;
        window = new DialogueWindow(skin, this::next);
        window.show(stage);
        renderCurrentLine();
    }

    private void next() {
        index++;
        if (index >= lines.size()) {
            Runnable done = completion;
            close();
            if (done != null) {
                done.run();
            }
            return;
        }
        renderCurrentLine();
    }

    private void renderCurrentLine() {
        if (window == null || index < 0 || index >= lines.size()) {
            return;
        }
        DialogueLine line = lines.get(index);
        TextureRegion portrait = animations.region(line.imageId());
        window.setLine(line.speaker(), line.text(), portrait, index + 1 >= lines.size());
    }

    private void close() {
        if (window != null) {
            window.close();
            window = null;
        }
        lines = List.of();
        completion = null;
        index = 0;
    }

    private List<DialogueLine> introLines(String chapterName, int levelNumber) {
        if (levelNumber < AdventureLevelCatalog.FIRST_PLAYABLE_LEVEL
                || levelNumber > AdventureLevelCatalog.BOSS_LEVEL) {
            return List.of();
        }
        String chapter = AdventureLevelCatalog.normalizeChapterName(chapterName);
        List<DialogueLine> result = new ArrayList<>();
        if (levelNumber == AdventureLevelCatalog.BOSS_LEVEL) {
            result.add(new DialogueLine("Penny", "Warning: Dr. Zomboss is directly ahead.", PENNY_IMAGE));
            result.add(new DialogueLine("Crazy Dave", "Then directly ahead is exactly where we plant!", DAVE_IMAGE));
            result.add(new DialogueLine("Zomboss", "How touching. Now prepare to lose your lawn.", ZOMBOSS_IMAGE));
            return result;
        }
        switch (chapter) {
            case "ancient-egypt" -> {
                result.add(new DialogueLine("Crazy Dave", ancientEgyptDaveLine(levelNumber), DAVE_IMAGE));
                result.add(new DialogueLine("Penny", ancientEgyptPennyLine(levelNumber), PENNY_IMAGE));
            }
            case "ice-cave" -> {
                result.add(new DialogueLine("Crazy Dave", frostbiteDaveLine(levelNumber), DAVE_IMAGE));
                result.add(new DialogueLine("Penny", frostbitePennyLine(levelNumber), PENNY_IMAGE));
            }
            case "wave-beach" -> {
                result.add(new DialogueLine("Crazy Dave", beachDaveLine(levelNumber), DAVE_IMAGE));
                result.add(new DialogueLine("Penny", beachPennyLine(levelNumber), PENNY_IMAGE));
            }
            case "wild-west" -> {
                result.add(new DialogueLine("Crazy Dave", darkAgesDaveLine(levelNumber), DAVE_IMAGE));
                result.add(new DialogueLine("Penny", darkAgesPennyLine(levelNumber), PENNY_IMAGE));
            }
            default -> {
                result.add(new DialogueLine("Crazy Dave", "New lawn, same problem: zombies!", DAVE_IMAGE));
                result.add(new DialogueLine("Penny", "Prepare the lawn before the first wave arrives.", PENNY_IMAGE));
            }
        }
        return result;
    }

    private List<DialogueLine> miniGameIntroLines(MiniGameType type, int stageNumber) {
        int stage = Math.max(1, Math.min(3, stageNumber));
        List<DialogueLine> result = new ArrayList<>();
        switch (type) {
            case VASEBREAKER -> {
                result.add(new DialogueLine(
                        "Crazy Dave",
                        "Vases! Smash 'em, see what pops out, then plant fast!",
                        DAVE_IMAGE
                ));
                result.add(new DialogueLine(
                        "Penny",
                        "Vasebreaker stage " + stage
                                + ". Plant packets can be dragged onto open lawn tiles.",
                        PENNY_IMAGE
                ));
            }
            case WALLNUT_BOWLING -> {
                result.add(new DialogueLine("Crazy Dave", "Time to bowl some wall-nuts!", DAVE_IMAGE));
                result.add(new DialogueLine(
                        "Penny",
                        "Wall-nut Bowling stage " + stage
                                + ". Drag a nut from the conveyor into a lane.",
                        PENNY_IMAGE
                ));
            }
            case I_ZOMBIE -> {
                result.add(new DialogueLine("Crazy Dave", "Whoa! This time WE'RE sending in the zombies!", DAVE_IMAGE));
                result.add(new DialogueLine(
                        "Penny",
                        "I, Zombie stage " + stage
                                + ". Spend sun carefully and eat every brain.",
                        PENNY_IMAGE
                ));
            }
            case MATCH_THREE -> {
                result.add(new DialogueLine(
                        "Crazy Dave",
                        "Match plants! Smash zombies! It's two games at once!",
                        DAVE_IMAGE
                ));
                result.add(new DialogueLine(
                        "Penny",
                        "Beghouled stage " + stage + ". Swap adjacent plants to make lines of three or more.",
                        PENNY_IMAGE
                ));
            }
            case PLANT_ZOMBIES -> {
                result.add(new DialogueLine(
                        "Crazy Dave",
                        "Those zombies have PLANTS for heads!",
                        DAVE_IMAGE
                ));
                result.add(new DialogueLine(
                        "Penny",
                        "Zombotany stage " + stage + ". Defend normally, but watch each plant-zombie ability.",
                        PENNY_IMAGE
                ));
            }
        }
        return result;
    }

    private String ancientEgyptDaveLine(int level) {
        return switch (level) {
            case 1 -> "Ancient Egypt! Sand, pyramids, and zombies. Mostly zombies.";
            case 2 -> "A conveyor full of plants? That's my kind of delivery service!";
            case 3 -> "Some plants are locked, but my tactical taco instincts are not!";
            default -> "More pyramids, more zombies, more planting!";
        };
    }

    private String ancientEgyptPennyLine(int level) {
        return switch (level) {
            case 1 -> "The timeline is unstable. Build a defense before they arrive.";
            case 2 -> "Use plants as they arrive on the conveyor. There is no normal seed bank here.";
            case 3 -> "Plan around the locked plant choices and watch for the final-wave sandstorm.";
            default -> "Ancient Egypt defenses are ready.";
        };
    }

    private String frostbiteDaveLine(int level) {
        return switch (level) {
            case 1 -> "Brrr! Keep the plants warm and the zombies cold!";
            case 2 -> "Protect those important plants from the frozen horde!";
            case 3 -> "Cold wind plus a timer? I should have brought a sweater clock!";
            default -> "Ice everywhere! Plant carefully!";
        };
    }

    private String frostbitePennyLine(int level) {
        return switch (level) {
            case 1 -> "Frost winds can raise a plant's freeze level. "
                    + "Frozen plants stop working until their ice breaks.";
            case 2 -> "Protect the marked plants and account for slippery ice tiles.";
            case 3 -> "Complete the objective before time expires while managing frost winds.";
            default -> "Frostbite conditions detected.";
        };
    }

    private String beachDaveLine(int level) {
        return switch (level) {
            case 1 -> "Beach day! I remembered the plants. I forgot everything else!";
            case 2 -> "Night beach party! Zombies definitely were not invited.";
            case 3 -> "Don't let those zombies cross the line!";
            default -> "Surf's up! Zombies down!";
        };
    }

    private String beachPennyLine(int level) {
        return switch (level) {
            case 1 -> "Watch the water line. The tide can change whenever a wave arrives.";
            case 2 -> "Night conditions reduce normal sun income. Low beaches are dangerous when submerged.";
            case 3 -> "Keep every zombie to the right of the deadline while the tide changes.";
            default -> "Big Wave Beach conditions detected.";
        };
    }

    private String darkAgesDaveLine(int level) {
        return switch (level) {
            case 1 -> "Dark Ages! No sky sun? Good thing plants know how to make their own!";
            case 2 -> "Love your plants! Also protect them from everything trying to eat them!";
            case 3 -> "Plant what you get! It's like a surprise bag, but with zombies nearby!";
            default -> "Dark lawn, bright ideas!";
        };
    }

    private String darkAgesPennyLine(int level) {
        return switch (level) {
            case 1 -> "There is no falling sun here. Graves may rise at wave starts, and some can trigger necromancy.";
            case 2 -> "Limit plant losses while managing graves and Dark Ages zombies.";
            case 3 -> "Use the provided resources efficiently. Dark Ages grave mechanics remain active.";
            default -> "Dark Ages conditions detected.";
        };
    }

    private record DialogueLine(String speaker, String text, String imageId) {
    }

    private static final class DialogueWindow extends ModalWindow {
        private final Image portrait;
        private final Label speaker;
        private final Label text;
        private final MenuButton nextButton;

        private DialogueWindow(Skin skin, Runnable nextAction) {
            super("Dialogue", skin);
            Table content = getContentTable();
            portrait = new Image();
            portrait.setScaling(com.badlogic.gdx.utils.Scaling.fit);
            speaker = new Label("", skin, "medium");
            speaker.setColor(Color.valueOf("5B3A1E"));
            text = new Label("", skin, "medium");
            text.setColor(Color.valueOf("4A3A1F"));
            text.setWrap(true);
            text.setAlignment(Align.left);
            nextButton = new MenuButton("Next", skin, "green", nextAction);

            Table words = new Table();
            words.add(speaker).left().growX().row();
            words.add(text).width(500f).left().padTop(8f).row();
            words.add(nextButton).width(150f).height(44f).right().padTop(18f);

            content.add(portrait).width(180f).height(220f).padRight(20f);
            content.add(words).width(520f).top();
        }

        private void setLine(String speakerName, String message, TextureRegion region, boolean last) {
            speaker.setText(speakerName == null ? "" : speakerName);
            text.setText(message == null ? "" : message);
            portrait.setDrawable(region == null ? null : new TextureRegionDrawable(region));
            nextButton.setText(last ? "Continue" : "Next");
        }
    }
}
