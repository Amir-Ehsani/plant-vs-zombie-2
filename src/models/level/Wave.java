package Model;

import java.util.List;

public class Wave {
    private int waveNumber;
    private int delay;
    private List<Zombie> zombiesList;

    public boolean isReadyToSpawn(int currentTick) {
        return false;
    }
}
