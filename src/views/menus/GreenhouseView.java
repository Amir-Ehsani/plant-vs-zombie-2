package views.menus;

import controllers.core.MenuManager;
import controllers.features.GreenhouseController;
import controllers.features.ShopController;
import models.account.Greenhouse;
import views.core.BaseView;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GreenhouseView extends BaseView {
    private static final Pattern PLANT_PATTERN = Pattern.compile(
            "^plant\\s+pot\\s+at\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)\\s*$"
    );
    private static final Pattern COLLECT_PATTERN = Pattern.compile(
            "^collect\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)\\s*$"
    );
    private static final Pattern GROW_PATTERN = Pattern.compile(
            "^grow\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)\\s*$"
    );

    private final MenuManager menuManager;
    private final GreenhouseController controller;
    private final ShopController shopController;

    public GreenhouseView(
            String viewName,
            MenuManager menuManager,
            GreenhouseController controller,
            ShopController shopController
    ) {
        super(viewName);
        this.menuManager = menuManager;
        this.controller = controller;
        this.shopController = shopController;
    }

    public GreenhouseView() {
        this("Greenhouse Menu", null, new GreenhouseController(), new ShopController());
    }

    @Override
    public void display() {
        System.out.print(menuText());
    }

    @Override
    public void handleInput(String input) {
        String command = cleanInput(input);
        if (!isConnected()) {
            printControllerMessage("ERROR: Greenhouse menu is not connected.");
            return;
        }
        if (!controller.isLoggedIn()) {
            printControllerMessage("ERROR: No user is logged in.");
            menuManager.enterLoginMenu();
            return;
        }

        if (handleNavigation(command) || handleShow(command) || handlePositionCommand(command)) {
            return;
        }
        controller.invalidCommand("greenhouse menu");
        printControllerMessage(controller.getLastMessage());
    }

    public String menuText() {
        return """
                Greenhouse Menu
                show greenhouse
                plant pot at (<x>, <y>)
                collect (<x>, <y>)
                grow (<x>, <y>)
                enter shop
                menu show current
                menu exit
                """;
    }

    public void show(Greenhouse greenhouse) {
        System.out.print(render(greenhouse));
    }

    public void showMessage(String message) {
        printControllerMessage(message);
    }

    public String render(Greenhouse greenhouse) {
        if (greenhouse == null) {
            return "Greenhouse is not available.\n";
        }

        greenhouse.updateGrowth();
        StringBuilder builder = new StringBuilder("Greenhouse\n==========\n");
        builder.append("Size: ").append(Greenhouse.WIDTH).append("x").append(Greenhouse.HEIGHT).append("\n");
        builder.append("Unlocked pots: ").append(greenhouse.getUnlockedPotCount()).append("/20\n");
        builder.append("Produced coins: ").append(greenhouse.getProductionAmount()).append("\n");
        if (!greenhouse.getLastHarvestTime().isBlank()) {
            builder.append("Last harvest: ").append(greenhouse.getLastHarvestTime()).append("\n");
        }
        builder.append("\n").append(renderGrid(greenhouse));
        builder.append("\nLegend: [L] locked, [E] empty, [G] growing, [R] ready\n");
        return builder.toString();
    }

    public String renderGrid(Greenhouse greenhouse) {
        if (greenhouse == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder("       ");
        for (int x = 1; x <= Greenhouse.WIDTH; x++) {
            builder.append(String.format("%-16s", "x" + x));
        }
        builder.append("\n");

        for (int y = 1; y <= Greenhouse.HEIGHT; y++) {
            builder.append("y").append(y).append("     ");
            for (int x = 1; x <= Greenhouse.WIDTH; x++) {
                builder.append(String.format("%-16s", renderPot(greenhouse.getPot(x, y))));
            }
            builder.append("\n");
        }
        return builder.toString();
    }

    public String renderPot(Greenhouse.Pot pot) {
        if (pot == null) {
            return "[?]";
        }
        if (pot.isLocked()) {
            return "[L]";
        }
        if (pot.isEmpty()) {
            return "[E]";
        }
        if (pot.isReady()) {
            return "[R:" + shortName(pot.getPlantName()) + "]";
        }
        return "[G:" + shortName(pot.getPlantName()) + " " + pot.getRemainingMinutes() + "m]";
    }

    public String renderPotDetails(Greenhouse.Pot pot) {
        if (pot == null) {
            return "Pot is not available.\n";
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Pot (").append(pot.getX()).append(", ").append(pot.getY()).append(")\n");
        builder.append("Status: ").append(pot.getStatus()).append("\n");
        if (!pot.getPlantName().isBlank()) {
            builder.append("Plant: ").append(pot.getPlantName()).append("\n");
        }
        if (pot.isGrowing()) {
            builder.append("Remaining: ").append(pot.getRemainingMinutes()).append(" minutes\n");
            builder.append("Grow cost: ").append(pot.getRemainingHoursRoundedUp()).append(" gems\n");
        }
        if (pot.isReady()) {
            builder.append("Ready: yes\n");
        }
        return builder.toString();
    }

    private boolean handleNavigation(String command) {
        if ("menu show current".equals(command)) {
            menuManager.showCurrentMenu();
            printControllerMessage(menuManager.getLastMessage());
            return true;
        }
        if ("menu exit".equals(command)) {
            menuManager.enterGameMenu();
            printControllerMessage(menuManager.getLastMessage());
            return true;
        }
        if ("enter shop".equals(command)) {
            shopController.enterShop();
            menuManager.changeView(new ShopView("Shop Menu", menuManager, shopController, controller));
            printControllerMessage(shopController.getLastMessage());
            return true;
        }
        return false;
    }

    private boolean handleShow(String command) {
        if (!"show greenhouse".equals(command)) {
            return false;
        }
        Greenhouse greenhouse = controller.getCurrentGreenhouse();
        show(greenhouse);
        if (greenhouse == null) {
            printControllerMessage(controller.getLastMessage());
        }
        return true;
    }

    private boolean handlePositionCommand(String command) {
        Matcher matcher = PLANT_PATTERN.matcher(command);
        if (matcher.matches()) {
            controller.plantRandomAt(number(matcher, 1), number(matcher, 2));
            printControllerMessage(controller.getLastMessage());
            return true;
        }

        matcher = COLLECT_PATTERN.matcher(command);
        if (matcher.matches()) {
            controller.collect(number(matcher, 1), number(matcher, 2));
            printControllerMessage(controller.getLastMessage());
            return true;
        }

        matcher = GROW_PATTERN.matcher(command);
        if (matcher.matches()) {
            controller.growNow(number(matcher, 1), number(matcher, 2));
            printControllerMessage(controller.getLastMessage());
            return true;
        }
        return false;
    }

    private int number(Matcher matcher, int group) {
        Integer value = parseInteger(matcher.group(group));
        return value == null ? -1 : value;
    }

    private String shortName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.length() <= 8) {
            return trimmed;
        }
        return trimmed.substring(0, 8);
    }

    private boolean isConnected() {
        return menuManager != null && controller != null && shopController != null;
    }
}
