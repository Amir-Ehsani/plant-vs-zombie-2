package ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import controllers.features.ShopController;
import pvz.skin.BorderedTable;

public class ShopItemCard extends BorderedTable implements Disposable {
    private final Texture iconTexture;
    private final MenuButton buyButton;

    public ShopItemCard(
            Skin skin,
            ShopController.ShopItem item,
            String remainingText,
            Runnable buyAction
    ) {
        pad(12f);
        setClip(true);
        iconTexture = createIcon(item == null ? "" : item.getType());
        Image icon = new Image(iconTexture);
        Label title = new Label(item == null ? "Item" : item.getName(), skin, "medium_outline");
        title.setAlignment(Align.center);
        title.setWrap(true);
        String price = item == null ? "-" : item.getPrice() + " " + currencyName(item.getCurrency());
        Label priceLabel = new Label(price, skin, "secondary");
        priceLabel.setAlignment(Align.center);
        Label amountLabel = new Label(receiveText(item), skin, "secondary");
        amountLabel.setAlignment(Align.center);
        amountLabel.setWrap(true);
        Label remainingLabel = new Label(remainingText == null ? "" : remainingText, skin, "secondary");
        remainingLabel.setAlignment(Align.center);
        remainingLabel.setWrap(true);
        buyButton = new MenuButton("Buy", skin, "green_small", buyAction);
        add(icon).size(76f).padBottom(6f).row();
        add(title).width(210f).height(48f).row();
        add(priceLabel).width(210f).padTop(3f).row();
        add(amountLabel).width(210f).height(38f).padTop(2f).row();
        add(remainingLabel).width(210f).height(42f).padTop(2f).row();
        add(buyButton).width(150f).height(38f).padTop(8f).padBottom(6f);
    }

    public void setBuyEnabled(boolean enabled) {
        buyButton.setDisabled(!enabled);
    }

    @Override
    public void dispose() {
        iconTexture.dispose();
    }

    private Texture createIcon(String type) {
        Pixmap pixmap = new Pixmap(72, 72, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.valueOf("F4E8B8"));
        pixmap.fill();
        String normalized = type == null ? "" : type.trim().toLowerCase();
        if ("pot".equals(normalized)) {
            drawPot(pixmap);
        } else if ("plant_food".equals(normalized)) {
            drawPlantFood(pixmap);
        } else if ("currency_exchange".equals(normalized)) {
            drawCurrency(pixmap);
        } else {
            drawSeedPacket(pixmap, "daily_seed_packet".equals(normalized));
        }
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    private void drawPot(Pixmap pixmap) {
        pixmap.setColor(Color.valueOf("8B4F2B"));
        pixmap.fillRectangle(17, 27, 38, 10);
        pixmap.fillRectangle(22, 37, 28, 23);
        pixmap.setColor(Color.valueOf("4E8B3A"));
        pixmap.fillCircle(29, 22, 10);
        pixmap.fillCircle(43, 19, 11);
    }

    private void drawPlantFood(Pixmap pixmap) {
        pixmap.setColor(Color.valueOf("56A83E"));
        pixmap.fillCircle(36, 36, 24);
        pixmap.setColor(Color.valueOf("D9F28A"));
        pixmap.fillCircle(31, 32, 12);
        pixmap.fillRectangle(36, 24, 6, 26);
    }

    private void drawCurrency(Pixmap pixmap) {
        pixmap.setColor(Color.valueOf("E5B52D"));
        pixmap.fillCircle(27, 36, 18);
        pixmap.setColor(Color.valueOf("62BCE8"));
        pixmap.fillCircle(46, 36, 15);
    }

    private void drawSeedPacket(Pixmap pixmap, boolean daily) {
        pixmap.setColor(daily ? Color.valueOf("E8B83A") : Color.valueOf("5B9639"));
        pixmap.fillRectangle(15, 12, 42, 48);
        pixmap.setColor(Color.valueOf("F6E9A8"));
        pixmap.fillRectangle(20, 18, 32, 34);
        pixmap.setColor(Color.valueOf("5B9639"));
        pixmap.fillCircle(36, 35, 10);
    }

    private String receiveText(ShopController.ShopItem item) {
        if (item == null) {
            return "";
        }
        String type = item.getType();
        if ("pot".equals(type)) {
            return "Unlocks 1 greenhouse pot";
        }
        if ("plant_food".equals(type)) {
            return "+" + item.getUnitAmount() + " Plant Food";
        }
        if ("currency_exchange".equals(type)) {
            return "+" + item.getUnitAmount() + " Coins";
        }
        if ("daily_seed_packet".equals(type)) {
            return "+" + item.getUnitAmount() + " seeds for " + item.getTargetName();
        }
        if ("selected_seed_packet".equals(type)) {
            return "+" + item.getUnitAmount() + " seeds for selected plant";
        }
        return "+" + item.getUnitAmount() + " seeds";
    }

    private String currencyName(String currency) {
        return "gem".equals(currency) ? "Diamonds" : "Coins";
    }
}
