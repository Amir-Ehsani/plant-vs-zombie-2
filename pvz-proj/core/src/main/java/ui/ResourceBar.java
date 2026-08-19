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
        pad(12f, 18f, 12f, 18f);
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
        content.add(coinsActor).padRight(14f);
        content.add(diamondsActor);
        if (debugVisible) {
            content.add(new MenuButton("+Coin", skin, "green_small", addCoin))
                    .width(92f).height(34f).padLeft(10f);
            content.add(new MenuButton("+Diamond", skin, "green_small", addDiamond))
                    .width(110f).height(34f).padLeft(6f);
        }
        add(content);
    }
}
