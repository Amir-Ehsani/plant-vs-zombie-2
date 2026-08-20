package ui;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public class AnimatedImageActor extends Image {
    private Animation<TextureRegion> animation;
    private float stateTime;
    private boolean looping;

    public AnimatedImageActor() {
        stateTime = 0f;
        looping = true;
    }

    public AnimatedImageActor(Animation<TextureRegion> animation, boolean looping) {
        this();
        setAnimation(animation, looping);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (animation == null) {
            return;
        }
        stateTime += delta;
        TextureRegion frame = animation.getKeyFrame(stateTime, looping);
        setDrawable(new TextureRegionDrawable(frame));
    }

    public void setAnimation(Animation<TextureRegion> animation, boolean looping) {
        this.animation = animation;
        this.looping = looping;
        stateTime = 0f;
    }

    public void restart() {
        stateTime = 0f;
    }
}
