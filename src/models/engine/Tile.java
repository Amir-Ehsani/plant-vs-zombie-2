package models.engine;



import models.core.plant.Plant;
import models.core.zombie.Zombie;

import java.util.ArrayList;
import java.util.List;

public class Tile {
    private final Position position;
    private final List<Zombie> zombies;
    private Plant currentPlant;
    private TileType tileType;

    public Tile(Position position) {
        this(position, TileType.NORMAL);
    }

    public Tile(Position position, TileType tileType ) {
        if (position == null) {
            throw new IllegalArgumentException("Tile position cannot be null.");
        }
        if (tileType == null) {
            throw new IllegalArgumentException("Tile type cannot be null.");
        }


        this.position = position;
        this.tileType = tileType;
        this.zombies = new ArrayList<>();
    }

    public boolean hasPlant() {
        return currentPlant != null;
    }

    public void addZombie(Zombie z) {
    }
}
