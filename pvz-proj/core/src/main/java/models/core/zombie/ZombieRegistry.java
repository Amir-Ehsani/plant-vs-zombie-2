package models.core.zombie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ZombieRegistry {
    private final Map<String, ZombieType> zombieTypesByName;
    private final Map<String, Armor> armorsByName;

    public ZombieRegistry() {
        this.zombieTypesByName = new HashMap<>();
        this.armorsByName = new HashMap<>();
    }

    public void registerZombieType(ZombieType zombieType) {
        if (zombieType == null) {
            throw new IllegalArgumentException("Zombie type cannot be null.");
        }

        String key = normalizeName(zombieType.getName());

        if (key.isEmpty()) {
            throw new IllegalArgumentException("Zombie type name cannot be empty.");
        }

        if (zombieTypesByName.containsKey(key)) {
            throw new IllegalArgumentException("Duplicate zombie type: " + zombieType.getName());
        }

        zombieTypesByName.put(key, zombieType);
    }

    public void registerArmor(Armor armor) {
        if (armor == null) {
            throw new IllegalArgumentException("Armor cannot be null.");
        }

        String key = normalizeName(armor.getName());

        if (key.isEmpty()) {
            throw new IllegalArgumentException("Armor name cannot be empty.");
        }

        armorsByName.put(key, armor);
    }

    public ZombieType getZombieTypeByName(String zombieName) {
        return zombieTypesByName.get(normalizeName(zombieName));
    }

    public boolean containsZombie(String zombieName) {
        return zombieTypesByName.containsKey(normalizeName(zombieName));
    }

    public Armor createArmorByName(String armorName) {
        if (armorName == null || armorName.isBlank() || armorName.equals("-")) {
            return null;
        }

        if (armorName.contains("+")) {
            return createCombinedArmor(armorName);
        }

        Armor armor = armorsByName.get(normalizeName(armorName));

        if (armor == null) {
            return null;
        }

        return new Armor(armor.getName(), armor.getHp(), armor.getArmorType());
    }

    public List<ZombieType> getAllZombieTypes() {
        List<ZombieType> zombieTypes = new ArrayList<>(zombieTypesByName.values());

        zombieTypes.sort((first, second) -> first.getName().compareToIgnoreCase(second.getName()));

        return Collections.unmodifiableList(zombieTypes);
    }

    public List<String> getAllZombieNames() {
        List<String> names = new ArrayList<>();

        for (ZombieType zombieType : getAllZombieTypes()) {
            names.add(zombieType.getName());
        }

        return Collections.unmodifiableList(names);
    }

    private Armor createCombinedArmor(String armorName) {
        String[] parts = armorName.split("\\+");
        int totalHp = 0;
        boolean hasMetallicPart = false;

        for (String part : parts) {
            Armor armor = armorsByName.get(normalizeName(part));

            if (armor != null) {
                totalHp += armor.getHp();

                if (armor.getArmorType().equals("metallic")) {
                    hasMetallicPart = true;
                }
            }
        }

        if (totalHp == 0) {
            return null;
        }

        String armorType = hasMetallicPart ? "metallic" : "normal";
        return new Armor(armorName.trim(), totalHp, armorType);
    }

    private String normalizeName(String name) {
        if (name == null) {
            return "";
        }

        return name
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', ' ')
                .replace('_', ' ')
                .replaceAll("\\s+", " ");
    }
}