package game.effects;

import models.core.plant.Plant;
import models.core.plant.PlantActionTiming;
import models.core.zombie.Armor;
import models.core.zombie.Zombie;
import models.engine.board.Board;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CombatFeedbackSystem {
    private static final int LARGE_IMPACT_DAMAGE = 450;
    private static final float LARGE_IMPACT_STRENGTH = 5.5f;
    private static final float LARGE_IMPACT_SECONDS = 0.16f;
    private static final float EXPLOSION_STRENGTH = 13f;
    private static final float EXPLOSION_SECONDS = 0.32f;
    private static final float GARGANTUAR_STRENGTH = 11f;
    private static final float GARGANTUAR_SECONDS = 0.28f;

    private final ScreenShakeController shake;
    private final Map<Plant, PlantSnapshot> previousPlants = new IdentityHashMap<>();
    private final Map<Zombie, ZombieSnapshot> previousZombies = new IdentityHashMap<>();
    private boolean initialized;

    public CombatFeedbackSystem(ScreenShakeController shake) {
        if (shake == null) {
            throw new IllegalArgumentException("Combat feedback requires a screen shake controller.");
        }
        this.shake = shake;
    }

    public void update(float delta, Board board) {
        shake.update(delta);
        if (board == null) {
            clearSnapshots();
            return;
        }
        if (!initialized) {
            snapshot(board);
            initialized = true;
            return;
        }
        detectPlantActions(board);
        detectGargantuarCrushes(board);
        detectZombieImpacts(board);
        snapshot(board);
    }

    public void onImmediatePlant(String plantName) {
        String name = normalize(plantName);
        if (!isImmediateExplosion(name)) {
            return;
        }
        float delay = PlantActionTiming.specialImpactTicks(plantName) / 10f;
        shake.schedule(delay, explosionStrength(name), EXPLOSION_SECONDS);
    }

    public void clear() {
        clearSnapshots();
        initialized = false;
        shake.clear();
    }

    private void detectPlantActions(Board board) {
        for (Plant plant : board.getAllPlants()) {
            PlantSnapshot previous = previousPlants.get(plant);
            if (previous == null || plant.getVisualSpecialSerial() <= previous.specialSerial) {
                continue;
            }
            String name = normalize(plant.getName());
            if (name.equals("potato mine") || name.equals("primal potato mine")) {
                float delay = PlantActionTiming.meleeImpactTicks(plant.getName(), "attack") / 10f;
                shake.schedule(delay, EXPLOSION_STRENGTH, EXPLOSION_SECONDS);
            } else if (name.equals("explode o nut")) {
                shake.trigger(EXPLOSION_STRENGTH, EXPLOSION_SECONDS);
            } else if (name.equals("squash")) {
                float delay = PlantActionTiming.meleeImpactTicks(
                    plant.getName(), plant.getVisualSpecialClip()
                ) / 10f;
                shake.schedule(delay, LARGE_IMPACT_STRENGTH, LARGE_IMPACT_SECONDS);
            }
        }
    }

    private void detectGargantuarCrushes(Board board) {
        Set<Plant> active = Collections.newSetFromMap(new IdentityHashMap<>());
        active.addAll(board.getAllPlants());
        for (Map.Entry<Plant, PlantSnapshot> entry : previousPlants.entrySet()) {
            if (active.contains(entry.getKey())) {
                continue;
            }
            PlantSnapshot removed = entry.getValue();
            if (removed.health > 0 && hasNearbyGargantuar(board, removed)) {
                shake.trigger(GARGANTUAR_STRENGTH, GARGANTUAR_SECONDS);
            }
        }
    }

    private boolean hasNearbyGargantuar(Board board, PlantSnapshot plant) {
        for (Zombie zombie : board.getAllZombies()) {
            if (!zombie.isAlive() || !isGargantuar(zombie)) {
                continue;
            }
            if ((int) Math.round(zombie.getY()) != plant.lane) {
                continue;
            }
            if (Math.abs(zombie.getX() - plant.x) <= 0.90) {
                return true;
            }
        }
        return false;
    }

    private void detectZombieImpacts(Board board) {
        for (Zombie zombie : board.getAllZombies()) {
            ZombieSnapshot previous = previousZombies.get(zombie);
            if (previous == null) {
                continue;
            }
            int durability = zombieDurability(zombie);
            int damage = Math.max(0, previous.durability - durability);
            if (damage >= LARGE_IMPACT_DAMAGE) {
                shake.trigger(LARGE_IMPACT_STRENGTH, LARGE_IMPACT_SECONDS);
            }
            if (isGargantuar(zombie) && previous.hp > previous.maxHp / 2
                    && zombie.getHp() <= zombie.getMaxHp() / 2) {
                shake.trigger(GARGANTUAR_STRENGTH, GARGANTUAR_SECONDS);
            }
        }
    }

    private void snapshot(Board board) {
        previousPlants.clear();
        for (Plant plant : board.getAllPlants()) {
            previousPlants.put(plant, new PlantSnapshot(plant));
        }
        previousZombies.clear();
        for (Zombie zombie : board.getAllZombies()) {
            previousZombies.put(zombie, new ZombieSnapshot(zombie));
        }
    }

    private void clearSnapshots() {
        previousPlants.clear();
        previousZombies.clear();
    }

    private int zombieDurability(Zombie zombie) {
        Armor armor = zombie.getArmor();
        int armorHp = armor == null ? 0 : Math.max(0, armor.getHp());
        return Math.max(0, zombie.getHp()) + armorHp;
    }

    private boolean isGargantuar(Zombie zombie) {
        return zombie != null && normalize(zombie.getName()).contains("gargantuar");
    }

    private boolean isImmediateExplosion(String name) {
        return name.equals("cherry bomb")
            || name.equals("grapeshot")
            || name.equals("jalapeno")
            || name.equals("doom shroom");
    }

    private float explosionStrength(String name) {
        return name.equals("doom shroom") ? EXPLOSION_STRENGTH * 1.25f : EXPLOSION_STRENGTH;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT)
            .replace('-', ' ')
            .replace('_', ' ')
            .replaceAll("\\s+", " ");
    }

    private static final class PlantSnapshot {
        private final double x;
        private final int lane;
        private final int health;
        private final int specialSerial;

        private PlantSnapshot(Plant plant) {
            x = plant.getX();
            lane = (int) Math.round(plant.getY());
            health = Math.max(0, plant.getHp()) + Math.max(0, plant.getArmorHp());
            specialSerial = plant.getVisualSpecialSerial();
        }
    }

    private static final class ZombieSnapshot {
        private final int durability;
        private final int hp;
        private final int maxHp;

        private ZombieSnapshot(Zombie zombie) {
            Armor armor = zombie.getArmor();
            int armorHp = armor == null ? 0 : Math.max(0, armor.getHp());
            durability = Math.max(0, zombie.getHp()) + armorHp;
            hp = zombie.getHp();
            maxHp = zombie.getMaxHp();
        }
    }
}
