package network.protocol;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Serializable render-state for one authoritative match entity.
 * Gameplay logic stays on the server; clients only render this projection.
 */
public final class EntityState implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    private final String id;
    private final String category;
    private final String type;
    private final int row;
    private final double x;
    private final int health;
    private final int maxHealth;
    private final Map<String, String> attributes;

    public EntityState(String id,
                       String category,
                       String type,
                       int row,
                       double x,
                       int health,
                       int maxHealth) {
        this(id, category, type, row, x, health, maxHealth, Collections.emptyMap());
    }

    public EntityState(String id,
                       String category,
                       String type,
                       int row,
                       double x,
                       int health,
                       int maxHealth,
                       Map<String, String> attributes) {
        this.id = Objects.requireNonNullElse(id, "");
        this.category = Objects.requireNonNullElse(category, "");
        this.type = Objects.requireNonNullElse(type, "");
        this.row = row;
        this.x = x;
        this.health = Math.max(0, health);
        this.maxHealth = Math.max(0, maxHealth);
        this.attributes = attributes == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(attributes);
    }

    public String getId() { return id; }
    public String getCategory() { return category; }
    public String getType() { return type; }
    public int getRow() { return row; }
    public double getX() { return x; }
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public Map<String, String> getAttributes() { return Collections.unmodifiableMap(attributes); }
    public String getAttribute(String key) { return key == null ? null : attributes.get(key); }
}
