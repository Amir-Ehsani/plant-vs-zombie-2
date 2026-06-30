package models.engine;



import models.core.plant.Plant;
import models.core.zombie.Zombie;

import java.util.ArrayList;
import java.util.Collections;
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

    //getters

    public Position getPosition() {
        return position;
    }

    public TileType getTileType() {
        return tileType;
    }

    public Plant getCurrentPlant() {
        return currentPlant;
    }

    public boolean hasPlant() {
        return currentPlant != null;
    }

    public List<Zombie> getZombies() {
        return Collections.unmodifiableList(zombies);
    }

    public boolean hasZombies() {
        return !zombies.isEmpty();
    }

    public boolean isPlantable() {
        return tileType == TileType.NORMAL;
    }



    public void addZombie(Zombie z) {
    }
}
