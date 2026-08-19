package ui;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import models.account.User;
import pvz.skin.BorderedTable;

public class ResourceBar extends BorderedTable {
    private final Skin skin;
    private final CurrencyActor coinsActor;
    private final CurrencyActor diamondsActor;
    private MenuButton addCoinButton;
    private MenuButton addDiamondButton;

    public ResourceBar(Skin skin) {
        this.skin = skin;
        pad(12f);
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
        add(coinsActor).padRight(16f);
        add(diamondsActor).padRight(debugVisible ? 16f : 0f);
        if (!debugVisible) {
            return;
        }
        addCoinButton = new MenuButton("+Coin", skin, "green_small", addCoin);
        addDiamondButton = new MenuButton("+Diamond", skin, "green_small", addDiamond);
        add(addCoinButton).width(100f).height(36f).padRight(8f);
        add(addDiamondButton).width(120f).height(36f);
    }
}
