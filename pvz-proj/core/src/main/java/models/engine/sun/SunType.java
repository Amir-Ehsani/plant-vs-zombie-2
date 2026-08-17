package models.engine.sun;

public enum SunType {
    NORMAL(25),
    SPECIAL(100),
    RADIOACTIVE(0);

    private final int collectionAmount;

    SunType(int collectionAmount) {
        this.collectionAmount = collectionAmount;
    }

    public int getCollectionAmount() {
        return collectionAmount;
    }
}
