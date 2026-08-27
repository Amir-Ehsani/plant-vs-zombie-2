package boss.core;

import models.core.projectile.Damage;
import models.core.zombie.Zombie;
import models.core.zombie.ZombieType;

import java.util.List;

public final class BossHitbox extends Zombie {
    private static final ZombieType HITBOX_TYPE = new ZombieType(
            "Zomboss", 1, 0, 0, 0,
            "BossCoreHitbox", null, List.of("boss", "boss_hitbox", "stationary"), "boss_core"
    );

    private final Boss boss;
    private boolean active;

    BossHitbox(Boss boss, double x, double y) {
        super(HITBOX_TYPE, x, y);
        if (boss == null) {
            throw new IllegalArgumentException("Boss hitbox requires a boss.");
        }
        this.boss = boss;
        this.active = true;
        setCurrentSpeed(0);
    }

    @Override
    public void takeDamage(Damage damage) {
        if (!active || damage == null || damage.getAmount() <= 0) {
            return;
        }
        boss.getHealth().damage(damage.getAmount());
    }

    @Override
    public void kill() {
        if (active) {
            boss.getHealth().damage(Math.max(1, boss.getHealth().getCurrentSectionMaximum() / 5));
        }
    }

    @Override
    public boolean isAlive() {
        return active;
    }

    @Override
    public int getHp() {
        return boss.getHealth().getCurrentHp();
    }

    @Override
    public int getMaxHp() {
        return boss.getHealth().getMaximumHp();
    }

    void deactivate() {
        active = false;
    }

    public Boss getBoss() {
        return boss;
    }
}
