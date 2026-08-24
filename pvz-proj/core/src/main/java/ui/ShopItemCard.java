package ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Scaling;
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
        pad(12f, 8f, 8f, 8f);
        setClip(true);
        TextureRegion background = animations == null
                ? null
                : animations.region("IMAGE_UI_STORE_GACHA_PINATA_GENERAL_CARD");
        if (background != null) {
            setBackground(new TextureRegionDrawable(background));
        }
        Label title = new Label(item == null ? "Item" : item.getName(), skin, "medium_outline");
        title.setAlignment(Align.center);
        title.setWrap(true);
        Label amountLabel = new Label(receiveText(item), skin, "secondary");
        amountLabel.setAlignment(Align.center);
        amountLabel.setWrap(true);
        Label remainingLabel = new Label(remainingText == null ? "" : remainingText, skin, "secondary");
        remainingLabel.setAlignment(Align.center);
        remainingLabel.setWrap(true);
        buyButton = new MenuButton("Buy", skin, "green_small", buyAction);
        buyButton.getLabel().setAlignment(Align.left);
        buyButton.padLeft(12f);
        Actor icon = createIconActor(skin, animations, item);
        add(createIconHolder(icon)).width(176f).height(104f).padTop(16f).padBottom(0f).row();
        add(title).width(176f).height(58f).padTop(2f).row();
        add(amountLabel).width(176f).height(46f).padTop(2f).row();
        add(remainingLabel).width(176f).height(30f).padTop(2f).row();
        add(createBuyArea(skin, animations, item)).width(144f).height(38f).padTop(2f).padBottom(8f);
    }

    public void setBuyEnabled(boolean enabled) {
        buyButton.setDisabled(!enabled);
    }

    @Override
    public void dispose() {
    }

    private Table createIconHolder(Actor icon) {
        Table holder = new Table();
        holder.add(icon).size(92f, 74f).padTop(22f).bottom();
        return holder;
    }

    private Stack createBuyArea(Skin skin, PvzAnimationService animations, ShopController.ShopItem item) {
        Stack stack = new Stack();
        stack.add(buyButton);
        Table overlay = new Table();
        overlay.setTouchable(Touchable.disabled);
        overlay.add().expandX().fillX();
        TextureRegion currencyRegion = currencyRegion(animations, item == null ? null : item.getCurrency());
        if (currencyRegion != null) {
            Image currencyIcon = new Image(currencyRegion);
            currencyIcon.setScaling(Scaling.fit);
            overlay.add(currencyIcon).size(18f, 18f).padRight(2f);
        }
        Label price = new Label(item == null ? "-" : String.valueOf(item.getPrice()), skin, "secondary");
        price.setAlignment(Align.center);
        overlay.add(price).padRight(10f);
        stack.add(overlay);
        return stack;
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
        icon.setScaling(Scaling.fit);
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
            return animations.region("IMAGE_GRAVESTONES_DARK_PLANTFOOD_DARK_PLANTFOOD_132X160");
        }
        if ("daily_seed_packet".equals(type)) {
            TextureRegion packet = packetRegion(animations, item.getTargetName());
            return packet != null ? packet : animations.region("IMAGE_UI_PACKETS_READY");
        }
        if ("currency_exchange".equals(type)) {
            return animations.region("IMAGE_EFFECTS_PRIZE_GEMS_LARGE_PRIZE_GEMS_LARGE_511X558");
        }
        return null;
    }

    private TextureRegion packetRegion(PvzAnimationService animations, String plantName) {
        return SeedPacketCatalog.region(animations, plantName);
    }

    private TextureRegion currencyRegion(PvzAnimationService animations, String currency) {
        if (animations == null) {
            return null;
        }
        if ("gem".equals(currency)) {
            return animations.region("IMAGE_EFFECTS_COIN_DIAMOND_COIN_DIAMOND_141X146");
        }
        return animations.region("IMAGE_UI_THYMED_EVENTS_ECS_CONVRT_COIN");
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
}
