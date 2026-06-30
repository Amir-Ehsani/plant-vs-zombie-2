package models.engine;

import models.Result;
import models.core.plant.Plant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Board {
    private static final int DEFAULT_WIDTH = 9;
    private static final int DEFAULT_HEIGHT = 5;

    private final int width;
    private final int height;
    private final List<Lane> lanes;

    public Board() {
        this(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public Board(int width, int height) {
        if (width <= 0) {
            throw new IllegalArgumentException("Board width must be greater than 0.");
        }
        if (height <= 0) {
            throw new IllegalArgumentException("Board height must be greater than 0.");
        }

        this.width = width;
        this.height = height;
        this.lanes = new ArrayList<>();

        initializeLanes();
    }

    private void initializeLanes() {
        for (int y = 1; y <= height; y++) {
                lanes.add(new Lane(y, width));
        }
    }


    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public List<Lane> getLanes() {
        return Collections.unmodifiableList(lanes);
    }

    public Lane getLaneAt(int y) {
        if (y <= 0 || y > height) {
            return null;
        }

        return lanes.get(y - 1);
    }

    public Tile getTileAt(Position position) {
        if (position == null) {
            return null;
        }

        Lane lane = getLaneAt(position.getY());
        if (lane == null) {
            return null;
        }

        return lane.getTileAt(position.getX());
    }



    public Result placePlant(Plant plant, Position position) {
        if (plant == null) {
            return new Result(false, "Plant cannot be null.");
        }

        Tile tile = getTileAt(position);

        if (tile == null) {
            return new Result(false, "Invalid position.");
        }

        if(!tile.isPlantable()){
            return new Result(false, "tile is not plantable.");
        }

        if(tile.hasPlant()){
            return new Result(false, "There is already a plant at " + position + ".");
        }

        tile.placePlant(plant);
        return new Result(true, "Plant placed at " + position + ".");
    }

    public Result removePlant(Position position) {
        Tile tile = getTileAt(position);
        if (tile == null) {
            return new Result(false, "Invalid position.");
        }

        if (!tile.hasPlant()) {
            return new Result(false, "There is no plant at " + position + ".");
        }

        tile.removePlant();
        return new Result(true, "Plant removed from " + position + ".");
    }


}
