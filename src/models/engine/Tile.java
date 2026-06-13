package Model;

import plant.Plant;
import zombie.Zombie;

import java.util.List;

public class Tile {
    private Position position;
    private Plant currentPlant;
    private List<Zombie> zombies;

    public boolean hasPlant() {
        return currentPlant != null;
    }

    public void addZombie(Zombie z) {
    }
}
