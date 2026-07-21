package models.core.zombie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ZombieType {
    private static final String DEFAULT_NAME = "normal zombie";
    private static final String DEFAULT_ID = "ZombieDefault";
    private static final int DEFAULT_BASE_HP = 190;
    private static final double DEFAULT_SPEED = 0.185;
    private static final int DEFAULT_DAMAGE_PER_TICK = 10;
    private static final int DEFAULT_WAVE_COST = 100;

    private final String name;
    private final String id;
    private final int baseHp;
    private final double speed;
    private final int damagePerTick;
    private final int waveCost;
    private final String defaultArmorName;
    private final List<String> tags;
    private final String ability;

    public ZombieType() {
        this(DEFAULT_NAME, DEFAULT_BASE_HP, DEFAULT_SPEED, DEFAULT_DAMAGE_PER_TICK,
                DEFAULT_WAVE_COST, DEFAULT_ID, null, List.of(), "");
    }

    public ZombieType(String name, int baseHp, double speed, int damagePerTick, int waveCost) {
        this(name, baseHp, speed, damagePerTick, waveCost, createDefaultId(name), null, List.of(), "");
    }

    public ZombieType(
            String name,
            int baseHp,
            double speed,
            int damagePerTick,
            int waveCost,
            String id,
            String defaultArmorName
    ) {
        this(name, baseHp, speed, damagePerTick, waveCost, id, defaultArmorName, List.of(), "");
    }

    public ZombieType(
            String name,
            int baseHp,
            double speed,
            int damagePerTick,
            int waveCost,
            String id,
            String defaultArmorName,
            List<String> tags,
            String ability
    ) {
        this.name = normalizeName(name);
        this.baseHp = Math.max(1, baseHp);
        this.speed = Math.max(0, speed);
        this.damagePerTick = Math.max(0, damagePerTick);
        this.waveCost = Math.max(0, waveCost);
        this.id = normalizeId(id);
        this.defaultArmorName = normalizeArmorName(defaultArmorName);
        this.tags = normalizeTags(tags);
        this.ability = ability == null ? "" : ability.trim();
    }

    private static String createDefaultId(String name) {
        if (name == null || name.isBlank()) {
            return DEFAULT_ID;
        }
        return "Zombie" + name.trim().replaceAll("[^a-zA-Z0-9]", "");
    }

    private String normalizeName(String value) {
        return value == null || value.isBlank() ? DEFAULT_NAME : value.trim();
    }

    private String normalizeId(String value) {
        return value == null || value.isBlank() ? createDefaultId(name) : value.trim();
    }

    private String normalizeArmorName(String value) {
        if (value == null || value.isBlank() || value.equals("-")) {
            return null;
        }
        return value.trim();
    }

    private List<String> normalizeTags(List<String> values) {
        List<String> result = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                String normalized = normalizeToken(value);
                if (!normalized.isEmpty() && !result.contains(normalized)) {
                    result.add(normalized);
                }
            }
        }
        return Collections.unmodifiableList(result);
    }

    private String normalizeToken(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replace('-', '_').replace(' ', '_');
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public int getBaseHp() {
        return baseHp;
    }

    public double getSpeed() {
        return speed;
    }

    public int getDamagePerTick() {
        return damagePerTick;
    }

    public int getWaveCost() {
        return waveCost;
    }

    public String getDefaultArmorName() {
        return defaultArmorName;
    }

    public boolean hasDefaultArmor() {
        return defaultArmorName != null;
    }

    public List<String> getTags() {
        return tags;
    }

    public boolean hasTag(String tag) {
        return tags.contains(normalizeToken(tag));
    }

    public String getAbility() {
        return ability;
    }
}
