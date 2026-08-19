package ui;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import models.account.User;
import pvz.skin.BorderedTable;

public class ResourceBar extends BorderedTable {
    private final Skin skin;
    private final CurrencyActor coinsActor;
    private final CurrencyActor diamondsActor;

    public ResourceBar(Skin skin) {
        this.skin = skin;
        coinsActor = new CurrencyActor(skin, "Coins");
        diamondsActor = new CurrencyActor(skin, "Diamonds");
        rebuild(false, null, null);
    }

    public void refresh(User user) {
        if (user == null) {
            coinsActor.setValue(0);
            diamondsActor.setValue(0);
            return;
        }
        coinsActor.setValue(user.getCoins());
        diamondsActor.setValue(user.getGems());
    }

    public void setDebugControls(boolean visible, Runnable addCoin, Runnable addDiamond) {
        rebuild(visible, addCoin, addDiamond);
    }

    private void rebuild(boolean debugVisible, Runnable addCoin, Runnable addDiamond) {
        clearChildren();
        Table content = new Table();
        content.defaults().padRight(16f);
        content.add(coinsActor);
        content.add(diamondsActor);
        if (debugVisible) {
            content.add(new MenuButton("+Coin", skin, "green_small", addCoin)).width(100f).height(36f).padLeft(8f);
            content.add(new MenuButton("+Diamond", skin, "green_small", addDiamond)).width(120f).height(36f);
        }
        add(content);
    }
}
