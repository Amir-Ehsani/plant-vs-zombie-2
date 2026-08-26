package game.render.entity;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import game.animation.core.AnimationDefinition;
import game.animation.core.EntityAnimationProfile;
import game.animation.core.PvzAnimationService;
import game.render.BoardGeometry;

import java.util.ArrayList;
import java.util.List;

final class PlantActionVisual {
    private static final float MIN_DURATION = 0.05f;
    private static final float MINT_LOOP_SECONDS = 8f;

    private final EntityAnimationProfile profile;
    private final List<Segment> segments = new ArrayList<>();
    private final int column;
    private final int lane;
    private final float playbackRate;
    private final float duration;
    private float elapsed;

    PlantActionVisual(EntityAnimationProfile profile, String clip, int column, int lane) {
        this(profile, clip, column, lane, 1f);
    }

    PlantActionVisual(
        EntityAnimationProfile profile,
        String clip,
        int column,
        int lane,
        float playbackRate
    ) {
        this.profile = profile;
        this.column = column;
        this.lane = lane;
        this.playbackRate = Math.max(0.05f, playbackRate);
        buildSegments(clip);
        float total = 0f;
        for (Segment segment : segments) {
            total += segment.duration;
        }
        duration = Math.max(MIN_DURATION, total);
    }

    private void buildSegments(String requestedClip) {
        AnimationDefinition definition = profile.getDefinition();
        if (requestedClip != null && requestedClip.equalsIgnoreCase("intro")
                && definition.hasClip("intro") && definition.hasClip("loop")
                && definition.hasClip("outro")) {
            addSegment("intro", clipDuration("intro"), false);
            addSegment("loop", MINT_LOOP_SECONDS, true);
            addSegment("outro", clipDuration("outro"), false);
            return;
        }
        if (requestedClip != null && requestedClip.equalsIgnoreCase("attack")
                && definition.getName().equalsIgnoreCase("GRAVEBUSTER")
                && definition.hasClip("attack1")) {
            addSegment("attack", clipDuration("attack"), false);
            addSegment("attack1", clipDuration("attack1"), false);
            return;
        }
        if (requestedClip != null && definition.hasClip(requestedClip)) {
            addSegment(requestedClip, clipDuration(requestedClip), false);
        }
    }

    private float clipDuration(String clip) {
        return Math.max(MIN_DURATION,
            profile.getDefinition().getClipDuration(clip) / playbackRate);
    }

    private void addSegment(String clip, float segmentDuration, boolean loop) {
        segments.add(new Segment(clip, Math.max(MIN_DURATION, segmentDuration), loop));
    }

    void update(float delta) {
        if (delta > 0f) {
            elapsed += delta;
        }
    }

    boolean isFinished() {
        return elapsed >= duration;
    }

    int getLane() {
        return lane;
    }

    void render(Batch batch, BoardGeometry geometry, PvzAnimationService animations) {
        if (segments.isEmpty()) {
            return;
        }
        Vector2 position = geometry.entityToScreen(column, lane);
        float remaining = elapsed;
        Segment current = segments.get(segments.size() - 1);
        for (Segment segment : segments) {
            current = segment;
            if (remaining < segment.duration) {
                break;
            }
            remaining -= segment.duration;
        }
        animations.draw(
            batch,
            profile.getPath(),
            current.clip,
            remaining * playbackRate,
            position.x,
            position.y,
            profile.getScale(),
            current.loop
        );
    }

    private static final class Segment {
        private final String clip;
        private final float duration;
        private final boolean loop;

        private Segment(String clip, float duration, boolean loop) {
            this.clip = clip;
            this.duration = duration;
            this.loop = loop;
        }
    }
}
