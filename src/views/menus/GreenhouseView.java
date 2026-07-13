package views.menus;

import models.account.Greenhouse;

public class GreenhouseView {
    public void show(Greenhouse greenhouse) {
        System.out.print(render(greenhouse));
    }

    public void showMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }

        System.out.println(message);
    }

    public String render(Greenhouse greenhouse) {
        if (greenhouse == null) {
            return "Greenhouse is not available.\n";
        }

        greenhouse.updateGrowth();

        StringBuilder builder = new StringBuilder();

        builder.append("Greenhouse\n");
        builder.append("==========\n");
        builder.append("Size: ")
                .append(Greenhouse.WIDTH)
                .append("x")
                .append(Greenhouse.HEIGHT)
                .append("\n");
        builder.append("Production: ")
                .append(greenhouse.getProductionAmount())
                .append("\n");

        if (greenhouse.getLastHarvestTime() != null && !greenhouse.getLastHarvestTime().isBlank()) {
            builder.append("Last harvest: ")
                    .append(greenhouse.getLastHarvestTime())
                    .append("\n");
        }

        builder.append("\n");
        builder.append(renderGrid(greenhouse));
        builder.append("\n");
        builder.append("Legend:\n");
        builder.append("[L] locked\n");
        builder.append("[E] empty\n");
        builder.append("[G] growing\n");
        builder.append("[R] ready\n");

        return builder.toString();
    }

    public String renderGrid(Greenhouse greenhouse) {
        if (greenhouse == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        builder.append("     ");

        for (int x = 1; x <= Greenhouse.WIDTH; x++) {
            builder.append("x")
                    .append(x)
                    .append("        ");
        }

        builder.append("\n");

        for (int y = 1; y <= Greenhouse.HEIGHT; y++) {
            builder.append("y")
                    .append(y)
                    .append("   ");

            for (int x = 1; x <= Greenhouse.WIDTH; x++) {
                Greenhouse.Pot pot = greenhouse.getPot(x, y);
                builder.append(renderPot(pot));

                if (x < Greenhouse.WIDTH) {
                    builder.append("  ");
                }
            }

            builder.append("\n");
        }

        return builder.toString();
    }

    public String renderPot(Greenhouse.Pot pot) {
        if (pot == null) {
            return "[?]";
        }

        String status = pot.getStatus();

        if (status.equals("locked")) {
            return "[L]";
        }

        if (status.equals("empty")) {
            return "[E]";
        }

        if (status.equals("growing")) {
            return "[G:" + shortenName(pot.getPlantName()) + "]";
        }

        if (status.equals("ready")) {
            return "[R:" + shortenName(pot.getPlantName()) + "]";
        }

        return "[?]";
    }

    public String renderPotDetails(Greenhouse.Pot pot) {
        if (pot == null) {
            return "Pot is not available.\n";
        }

        StringBuilder builder = new StringBuilder();

        builder.append("Pot (")
                .append(pot.getX())
                .append(", ")
                .append(pot.getY())
                .append(")\n");
        builder.append("Status: ")
                .append(pot.getStatus())
                .append("\n");

        if (pot.getPlantName() != null && !pot.getPlantName().isBlank()) {
            builder.append("Plant: ")
                    .append(pot.getPlantName())
                    .append("\n");
        }

        if (pot.getPlantedAt() != null) {
            builder.append("Planted at: ")
                    .append(pot.getPlantedAt())
                    .append("\n");
        }

        if (pot.getReadyAt() != null) {
            builder.append("Ready at: ")
                    .append(pot.getReadyAt())
                    .append("\n");
        }

        if (pot.isReady()) {
            builder.append("Harvest reward: ")
                    .append(pot.getHarvestReward())
                    .append("\n");
        }

        return builder.toString();
    }

    private String shortenName(String name) {
        if (name == null || name.isBlank()) {
            return "-";
        }

        String trimmed = name.trim();

        if (trimmed.length() <= 3) {
            return trimmed.toUpperCase();
        }

        return trimmed.substring(0, 3).toUpperCase();
    }
}