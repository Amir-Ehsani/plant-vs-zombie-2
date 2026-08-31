package models.minigame;

import java.util.List;

/** Plant-picker contract shared by online I, Zombie and one-device Couch Play. */
public interface EgyptIZombieChooser {
    boolean lockSelectedPlants(List<String> plantNames);

    boolean isPlantsReady();

    boolean isRunning();

    boolean isChooserBusy();

    String getChooserMessage();

    List<String> getEgyptPlantCatalog();

    void pumpChooser();

    void abandonChooser();
}
