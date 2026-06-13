package models.account;

public class PlantData implements  IPurchasable{
    private String name;
    private int price;
    private int level;

    private boolean isUnlocked;

    @Override
    public int getPrice() {
        return this.price;
    }

    @Override
    public boolean isUnlocked() {
        return this.isUnlocked;
    }
}
