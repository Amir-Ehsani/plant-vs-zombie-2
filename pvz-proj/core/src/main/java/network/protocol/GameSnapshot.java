package network.protocol;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable snapshot sent by the authoritative server to both I, Zombie clients.
 * Sequence is monotonic so clients can discard stale network updates.
 *
 * Stage seven also carries server-owned placement cooldowns. Clients use these values
 * only for presentation/affordances; the authoritative server still validates every action.
 */
public final class GameSnapshot implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    private final long sequence;
    private final String status;
    private final int stage;
    private final long elapsedMillis;
    private final long remainingMillis;
    private final int plantSun;
    private final int zombieSun;
    private final boolean[] brains;
    private final List<EntityState> entities;
    private final Map<String, Long> plantCooldownMillis;
    private final Map<String, Long> zombieCooldownMillis;
    private final GameRole winner;
    private final String finishReason;
    private final boolean plantsReady;
    private final List<String> plantLoadout;

    /** Backward-compatible constructor retained for older tests/callers. */
    public GameSnapshot(long sequence,
                        String status,
                        long remainingMillis,
                        int plantSun,
                        int zombieSun,
                        boolean[] brains,
                        List<EntityState> entities,
                        GameRole winner,
                        String finishReason) {
        this(sequence, status, 1, 0L, remainingMillis, plantSun, zombieSun, brains, entities,
                Collections.emptyMap(), Collections.emptyMap(), winner, finishReason, true, List.of());
    }

    public GameSnapshot(long sequence,
                        String status,
                        int stage,
                        long elapsedMillis,
                        long remainingMillis,
                        int plantSun,
                        int zombieSun,
                        boolean[] brains,
                        List<EntityState> entities,
                        Map<String, Long> plantCooldownMillis,
                        Map<String, Long> zombieCooldownMillis,
                        GameRole winner,
                        String finishReason) {
        this(sequence, status, stage, elapsedMillis, remainingMillis, plantSun, zombieSun, brains, entities,
                plantCooldownMillis, zombieCooldownMillis, winner, finishReason, true, List.of());
    }

    public GameSnapshot(long sequence,
                        String status,
                        int stage,
                        long elapsedMillis,
                        long remainingMillis,
                        int plantSun,
                        int zombieSun,
                        boolean[] brains,
                        List<EntityState> entities,
                        Map<String, Long> plantCooldownMillis,
                        Map<String, Long> zombieCooldownMillis,
                        GameRole winner,
                        String finishReason,
                        boolean plantsReady,
                        List<String> plantLoadout) {
        this.sequence = Math.max(0L, sequence);
        this.status = Objects.requireNonNullElse(status, "RUNNING");
        this.stage = Math.max(1, Math.min(3, stage));
        this.elapsedMillis = Math.max(0L, elapsedMillis);
        this.remainingMillis = Math.max(0L, remainingMillis);
        this.plantSun = Math.max(0, plantSun);
        this.zombieSun = Math.max(0, zombieSun);
        this.brains = brains == null ? new boolean[0] : brains.clone();
        this.entities = entities == null ? new ArrayList<>() : new ArrayList<>(entities);
        this.plantCooldownMillis = normalizedCooldowns(plantCooldownMillis);
        this.zombieCooldownMillis = normalizedCooldowns(zombieCooldownMillis);
        this.winner = winner;
        this.finishReason = Objects.requireNonNullElse(finishReason, "");
        this.plantsReady = plantsReady;
        this.plantLoadout = plantLoadout == null ? new ArrayList<>() : new ArrayList<>(plantLoadout);
    }

    public long getSequence() { return sequence; }
    public String getStatus() { return status; }
    public int getStage() { return stage <= 0 ? 1 : stage; }
    public long getElapsedMillis() { return elapsedMillis; }
    public long getRemainingMillis() { return remainingMillis; }
    public int getPlantSun() { return plantSun; }
    public int getZombieSun() { return zombieSun; }
    public boolean[] getBrains() { return brains == null ? new boolean[0] : brains.clone(); }
    public List<EntityState> getEntities() { return entities == null ? Collections.emptyList() : Collections.unmodifiableList(entities); }
    public Map<String, Long> getPlantCooldowns() {
        return plantCooldownMillis == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(plantCooldownMillis);
    }

    public Map<String, Long> getZombieCooldowns() {
        return zombieCooldownMillis == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(zombieCooldownMillis);
    }
    public GameRole getWinner() { return winner; }
    public String getFinishReason() { return finishReason; }
    public boolean isPlantsReady() { return plantsReady; }
    public List<String> getPlantLoadout() {
        return plantLoadout == null ? Collections.emptyList() : Collections.unmodifiableList(plantLoadout);
    }

    public long getPlantCooldownMillis(String type) {
        return cooldownFor(plantCooldownMillis, type);
    }

    public long getZombieCooldownMillis(String type) {
        return cooldownFor(zombieCooldownMillis, type);
    }

    public int getBrainsRemaining() {
        int remaining = 0;
        if (brains != null) {
            for (boolean brain : brains) {
                if (brain) remaining++;
            }
        }
        return remaining;
    }

    public boolean isFinished() {
        return winner != null || "FINISHED".equalsIgnoreCase(status);
    }

    private static Map<String, Long> normalizedCooldowns(Map<String, Long> source) {
        LinkedHashMap<String, Long> result = new LinkedHashMap<>();
        if (source == null) {
            return result;
        }
        for (Map.Entry<String, Long> entry : source.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                continue;
            }
            String key = normalizeType(entry.getKey());
            long value = entry.getValue() == null ? 0L : Math.max(0L, entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    private static long cooldownFor(Map<String, Long> cooldowns, String type) {
        if (type == null || cooldowns == null) {
            return 0L;
        }
        return Math.max(0L, cooldowns.getOrDefault(normalizeType(type), 0L));
    }

    private static String normalizeType(String value) {
        return value.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }
}
