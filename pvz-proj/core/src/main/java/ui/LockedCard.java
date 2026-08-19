package ui;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

public class LockedCard extends Table {
    private final Label priceLabel;
    private final MenuButton purchaseButton;

    public LockedCard(Skin skin, Runnable purchaseAction) {
        Label lockLabel = new Label("LOCKED", skin);
        priceLabel = new Label("Price: 0", skin);
        purchaseButton = new MenuButton("Purchase", skin, purchaseAction);
        pad(12f);
        add(lockLabel).row();
        add(priceLabel).padTop(6f).row();
        add(purchaseButton).width(150f).height(42f).padTop(8f);
    }

    public void setPrice(int price) {
        priceLabel.setText("Price: " + Math.max(0, price));
    }

    public void setPurchaseEnabled(boolean enabled) {
        purchaseButton.setDisabled(!enabled);
    }
}
