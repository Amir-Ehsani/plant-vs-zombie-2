package models.level;

import models.core.plant.Plant;
import models.core.plant.PlantFactory;
import models.engine.Position;
import models.engine.Tile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SaveOurSeedsRule extends AbstractLevelRule {
    private final Map<Position, String> protectedPlantNames;
    private final PlantFactory plantFactory;

    public SaveOurSeedsRule(Map<Position, String> protectedPlantNames) {
        this(protectedPlantNames, new PlantFactory());
    }

    public SaveOurSeedsRule(
            Map<Position, String> protectedPlantNames,
            PlantFactory plantFactory
    ) {
        if (protectedPlantNames == null || protectedPlantNames.isEmpty()) {
            throw new IllegalArgumentException("Protected plants cannot be empty.");
        }
        if (plantFactory == null) {
            throw new IllegalArgumentException("Plant factory cannot be null.");
        }

        this.protectedPlantNames = copyProtectedPlants(protectedPlantNames);
        this.plantFactory = plantFactory;
    }

    @Override
    public SpecialLevelType getType() {
        return SpecialLevelType.SAVE_OUR_SEEDS;
    }

    @Override
    public void onLevelStart(LevelRuntimeContext context) {
        resetResult();
        placeProtectedPlants(context);
        checkProtectedPlants(context);
    }

    @Override
    public void onTick(LevelRuntimeContext context) {
        checkProtectedPlants(context);
    }

    public List<Position> getProtectedPositions() {
        return Collections.unmodifiableList(new ArrayList<>(protectedPlantNames.keySet()));
    }

    public Map<Position, String> getProtectedPlantNames() {
        return Collections.unmodifiableMap(protectedPlantNames);
    }

    private void placeProtectedPlants(LevelRuntimeContext context) {
        for (Map.Entry<Position, String> entry : protectedPlantNames.entrySet()) {
            Position position = entry.getKey();
            Tile tile = context.getBoard().getTileAt(position);
            if (tile == null) {
                throw new IllegalStateException("Protected plant position is outside the board.");
            }
            if (tile.hasPlant()) {
                continue;
            }

            Plant plant = plantFactory.createPlant(
                    entry.getValue(),
                    position.getX(),
                    position.getY()
            );
            if (!context.getBoard().placePlant(plant, position)) {
                throw new IllegalStateException("Protected plant could not be placed at " + position + ".");
            }
        }
    }

    private void checkProtectedPlants(LevelRuntimeContext context) {
        for (Position position : protectedPlantNames.keySet()) {
            Tile tile = context.getBoard().getTileAt(position);
            Plant plant = tile == null ? null : tile.getCurrentPlant();
            if (plant == null || !plant.isAlive()) {
                markLost();
                return;
            }
        }
    }

    private Map<Position, String> copyProtectedPlants(Map<Position, String> source) {
        Map<Position, String> copy = new LinkedHashMap<>();
        for (Map.Entry<Position, String> entry : source.entrySet()) {
            Position position = entry.getKey();
            String plantName = entry.getValue();
            if (position == null || plantName == null || plantName.isBlank()) {
                throw new IllegalArgumentException("Protected plant entries cannot be empty.");
            }
            copy.put(position, plantName.trim());
        }
        return copy;
    }
}
