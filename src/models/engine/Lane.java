package models.engine;

import models.entities.LawnMower;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Lane {
    private final int laneId;
    private final List<Tile> tiles;
    private final LawnMower lawnMower;

    public Lane(int laneId, int width) {
        if(laneId <= 0){
            throw new IllegalArgumentException("Lane id must be greater than 0.");
        }
        if(width <= 0){
            throw new IllegalArgumentException("Width must be greater than 0.");
        }

        this.laneId = laneId;
        this.tiles = new ArrayList<>();
        this.lawnMower = new LawnMower();

        initializeTiles(width);

    }

    private void initializeTiles(int width) {
        for(int i = 1; i <= width; i++) {
            Position position = new Position(i, laneId);
            tiles.add(new Tile(position));
        }
    }

    public int getLaneId() {
        return laneId;
    }

    public int getWidth() {
        return tiles.size();
    }

    public List<Tile> getTiles() {
        return Collections.unmodifiableList(tiles);
    }

    public Tile getTileAt(int x) {
        if (x <= 0 || x > tiles.size()) {
            return null;
        }

        return tiles.get(x - 1);
    }

    public LawnMower getLawnMower() {
        return lawnMower;
    }

    public void updateLaneTicks() {
    }


}
