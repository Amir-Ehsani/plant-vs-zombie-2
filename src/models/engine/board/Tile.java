package models.engine.board;



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

    // state modifiers

    public void setTileType(TileType tileType) {
        if (tileType == null) {
            throw new IllegalArgumentException("Tile type cannot be null.");
        }

        this.tileType = tileType;
    }

    public void placePlant(Plant plant) {
        if (plant == null) {
            throw new IllegalArgumentException("Plant cannot be null.");
        }

        this.currentPlant = plant;
    }

    public Plant removePlant() {
        Plant removedPlant = currentPlant;
        currentPlant = null;
        return removedPlant;
    }

    public void removeZombie(Zombie zombie) {
        zombies.remove(zombie);
    }

    public void clearZombies() {
        zombies.clear();
    }

    public void addZombie(Zombie zombie) {
        if (zombie == null) {
            throw new IllegalArgumentException("Zombie cannot be null.");
        }
        zombies.add(zombie);
    }
}
