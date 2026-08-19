package ui;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import models.account.User;

public class ResourceBar extends Table {
    private final Skin skin;
    private final CurrencyActor coinsActor;
    private final CurrencyActor diamondsActor;
    private MenuButton addCoinButton;
    private MenuButton addDiamondButton;

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
        add(coinsActor).padRight(16f);
        add(diamondsActor).padRight(debugVisible ? 16f : 0f);
        if (!debugVisible) {
            return;
        }
        addCoinButton = new MenuButton("+Coin", skin, addCoin);
        addDiamondButton = new MenuButton("+Diamond", skin, addDiamond);
        add(addCoinButton).width(100f).height(36f).padRight(8f);
        add(addDiamondButton).width(120f).height(36f);
    }
}
