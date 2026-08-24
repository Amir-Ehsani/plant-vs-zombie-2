package ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;

import controllers.features.ShopController;

public class ShopItemCard extends Table implements Disposable {
    private final MenuButton buyButton;

    public ShopItemCard(
            Skin skin,
            PvzAnimationService animations,
            ShopController.ShopItem item,
            String remainingText,
            Runnable buyAction
    ) {
        pad(10f, 8f, 10f, 8f);
        setClip(true);
        TextureRegion background = animations == null ? null : animations.region("IMAGE_UI_STORE_GACHA_PINATA_GENERAL_CARD");
        if (background != null) {
            setBackground(new TextureRegionDrawable(background));
        }
        Label title = new Label(item == null ? "Item" : item.getName(), skin, "medium_outline");
        title.setAlignment(Align.center);
        title.setWrap(true);
        Label priceLabel = new Label(item == null ? "-" : item.getPrice() + " " + currencyName(item.getCurrency()), skin, "medium_outline");
        priceLabel.setAlignment(Align.center);
        Label amountLabel = new Label(receiveText(item), skin, "secondary");
        amountLabel.setAlignment(Align.center);
        amountLabel.setWrap(true);
        Label remainingLabel = new Label(remainingText == null ? "" : remainingText, skin, "secondary");
        remainingLabel.setAlignment(Align.center);
        remainingLabel.setWrap(true);
        buyButton = new MenuButton(item != null && item.isDaily() ? "Claim" : "Buy", skin, "green_small", buyAction);
        Actor icon = createIconActor(skin, animations, item);
        add(icon).size(116f, 114f).padTop(8f).padBottom(2f).row();
        add(title).width(176f).height(50f).padTop(4f).row();
        add(priceLabel).width(176f).padTop(3f).row();
        add(amountLabel).width(176f).height(46f).padTop(4f).row();
        add(remainingLabel).width(176f).height(40f).padTop(4f).row();
        add(buyButton).width(132f).height(36f).padTop(10f).padBottom(10f);
    }

    public void setBuyEnabled(boolean enabled) {
        buyButton.setDisabled(!enabled);
    }

    @Override
    public void dispose() {
    }

    private Actor createIconActor(Skin skin, PvzAnimationService animations, ShopController.ShopItem item) {
        TextureRegion region = resolveIconRegion(animations, item);
        if (region == null) {
            Label fallback = new Label(item == null ? "?" : shortType(item.getType()), skin, "big_outline");
            fallback.setColor(Color.valueOf("FFF5C9"));
            fallback.setAlignment(Align.center);
            return fallback;
        }
        Image icon = new Image(region);
        icon.setScaling(com.badlogic.gdx.utils.Scaling.fit);
        if (isPacketItem(item)) {
            TextureRegion boost = animations == null ? null : animations.region("IMAGE_UI_PACKETS_BOOST");
            if (boost != null) {
                Stack stack = new Stack();
                Image background = new Image(boost);
                background.setScaling(com.badlogic.gdx.utils.Scaling.fit);
                Table overlay = new Table();
                overlay.add(icon).size(76f, 96f).padTop(10f);
                stack.add(background);
                stack.add(overlay);
                return stack;
            }
        }
        return icon;
    }

    private TextureRegion resolveIconRegion(PvzAnimationService animations, ShopController.ShopItem item) {
        if (animations == null || item == null) {
            return null;
        }
        String type = item.getType();
        if ("pot".equals(type)) {
            return animations.region("IMAGE_ZEN_GARDEN_GROWING_PLANT_SLOT_GROWING_PLANT_SLOT_184X161_2");
        }
        if ("plant_food".equals(type)) {
            return animations.region("IMAGE_UI_ALMANAC_ALMANAC_STAT_ICON_PLANTFOOD_LARGE");
        }
        if ("random_seed_packet".equals(type)) {
            return animations.region("IMAGE_UI_STOREMULTI_SEEDPACKETICON");
        }
        if ("selected_seed_packet".equals(type)) {
            return animations.region("IMAGE_UI_PACKETS_READY");
        }
        if ("daily_seed_packet".equals(type)) {
            TextureRegion packet = packetRegion(animations, item.getTargetName());
            return packet != null ? packet : animations.region("IMAGE_UI_PACKETS_READY");
        }
        if ("currency_exchange".equals(type)) {
            return animations.region("IMAGE_UI_STOREMULTI_SEEDPACKETICON");
        }
        return null;
    }

    private TextureRegion packetRegion(PvzAnimationService animations, String plantName) {
        if (animations == null || plantName == null || plantName.isBlank()) {
            return null;
        }
        String token = plantName.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_");
        TextureRegion region = animations.region("IMAGE_UI_PACKETS_" + token);
        if (region != null) {
            return region;
        }
        return animations.region("IMAGE_UI_PACKETS_READY");
    }

    private boolean isPacketItem(ShopController.ShopItem item) {
        if (item == null) {
            return false;
        }
        String type = item.getType();
        return "random_seed_packet".equals(type)
                || "selected_seed_packet".equals(type)
                || "daily_seed_packet".equals(type);
    }

    private String shortType(String type) {
        if (type == null || type.isBlank()) {
            return "?";
        }
        if (type.length() <= 2) {
            return type.toUpperCase();
        }
        return type.substring(0, 2).toUpperCase();
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
            return item.getTargetName().isBlank()
                    ? "+" + item.getUnitAmount() + " daily seeds"
                    : "+" + item.getUnitAmount() + " seeds for " + item.getTargetName();
        }
        if ("selected_seed_packet".equals(type)) {
            return "+" + item.getUnitAmount() + " seeds for selected plant";
        }
        return "+" + item.getUnitAmount() + " random seeds";
    }

    private String currencyName(String currency) {
        return "gem".equals(currency) ? "Diamonds" : "Coins";
    }
}
