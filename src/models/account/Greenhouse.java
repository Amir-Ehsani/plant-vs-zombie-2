package models.account;

public class Greenhouse {
    private int productionAmount;
    private String lastHarvestTime;

    public void addProduction(int productionAmount) {
        this.productionAmount += productionAmount;
    }
}
