package models.level;

import models.core.zombie.DefaultZombieRegistry;
import models.core.zombie.Zombie;
import models.core.zombie.ZombieFactory;
import models.core.zombie.ZombieRegistry;
import models.core.zombie.ZombieType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class WaveGenerator {
    private static final double NORMAL_WAVE_MULTIPLIER = 1.25;
    private static final int FINAL_WAVE_MULTIPLIER = 2;

    private final ZombieRegistry zombieRegistry;
    private final ZombieFactory zombieFactory;
    private final Random random;

    public WaveGenerator() {
        this(DefaultZombieRegistry.getInstance(), new Random());
    }

    public WaveGenerator(ZombieRegistry zombieRegistry, Random random) {
        if (zombieRegistry == null || random == null) {
            throw new IllegalArgumentException("Registry and random generator cannot be null.");
        }
        this.zombieRegistry = zombieRegistry;
        this.zombieFactory = new ZombieFactory(zombieRegistry);
        this.random = random;
    }

    public List<Wave> generateWaves(
            int waveCount,
            int firstWaveDifficulty,
            int firstWaveTick,
            int waveDelayTicks,
            List<String> allowedZombieNames
    ) {
        if (waveCount <= 0 || firstWaveDifficulty <= 0) {
            throw new IllegalArgumentException("Wave count and difficulty must be positive.");
        }
        if (firstWaveTick < 0 || waveDelayTicks < 0) {
            throw new IllegalArgumentException("Wave timing cannot be negative.");
        }

        List<ZombieType> allowedTypes = resolveAllowedTypes(allowedZombieNames);
        List<Wave> waves = new ArrayList<>();
        int difficulty = firstWaveDifficulty;

        for (int waveNumber = 1; waveNumber <= waveCount; waveNumber++) {
            if (waveNumber > 1) {
                difficulty = resolveNextDifficulty(difficulty, waveNumber == waveCount);
            }

            List<Zombie> zombies = createExactCostZombies(difficulty, allowedTypes);
            int delay = firstWaveTick + (waveNumber - 1) * waveDelayTicks;
            waves.add(new Wave(waveNumber, delay, zombies));
        }

        return Collections.unmodifiableList(waves);
    }

    private int resolveNextDifficulty(int previousDifficulty, boolean finalWave) {
        if (finalWave) {
            return previousDifficulty * FINAL_WAVE_MULTIPLIER;
        }
        return (int) Math.ceil(previousDifficulty * NORMAL_WAVE_MULTIPLIER);
    }

    private List<ZombieType> resolveAllowedTypes(List<String> allowedNames) {
        if (allowedNames == null || allowedNames.isEmpty()) {
            throw new IllegalArgumentException("Allowed zombie names cannot be empty.");
        }

        List<ZombieType> types = new ArrayList<>();
        for (String name : allowedNames) {
            ZombieType type = zombieRegistry.getZombieTypeByName(name);
            if (type == null) {
                throw new IllegalArgumentException("Unknown zombie type: " + name);
            }
            if (type.getWaveCost() > 0) {
                types.add(type);
            }
        }

        if (types.isEmpty()) {
            throw new IllegalArgumentException("No usable zombie type is available.");
        }
        return types;
    }

    private List<Zombie> createExactCostZombies(int targetCost, List<ZombieType> types) {
        List<ZombieType> shuffledTypes = new ArrayList<>(types);
        Collections.shuffle(shuffledTypes, random);

        Map<Integer, List<ZombieType>> solutions = new HashMap<>();
        solutions.put(0, new ArrayList<>());

        for (int cost = 1; cost <= targetCost; cost++) {
            for (ZombieType type : shuffledTypes) {
                int previousCost = cost - type.getWaveCost();
                List<ZombieType> previousSolution = solutions.get(previousCost);
                if (previousCost >= 0 && previousSolution != null) {
                    List<ZombieType> solution = new ArrayList<>(previousSolution);
                    solution.add(type);
                    solutions.put(cost, solution);
                    break;
                }
            }
        }

        List<ZombieType> selectedTypes = solutions.get(targetCost);
        if (selectedTypes == null) {
            throw new IllegalArgumentException(
                    "Wave difficulty " + targetCost + " cannot be made from the allowed zombie costs."
            );
        }

        List<Zombie> zombies = new ArrayList<>();
        for (ZombieType type : selectedTypes) {
            zombies.add(zombieFactory.createZombie(type, 1, 1));
        }
        return zombies;
    }
}
