package game.render.projectile;

import com.badlogic.gdx.math.Vector2;
import game.render.BoardGeometry;
import models.core.zombie.Zombie;

final class ProjectileTarget {
    private final Zombie zombie;
    final double x;
    private final double y;
    private double lastX;
    private double lastY;

    private ProjectileTarget(Zombie zombie, double x, double y) {
        this.zombie = zombie;
        this.x = x;
        this.y = y;
        lastX = x;
        lastY = y;
    }

    static ProjectileTarget fromZombie(Zombie zombie, double x, double y) {
        return new ProjectileTarget(zombie, x, y);
    }

    static ProjectileTarget fixed(double x, double y) {
        return new ProjectileTarget(null, x, y);
    }

    Vector2 screenPosition(BoardGeometry geometry) {
        if (zombie != null && zombie.isAlive()) {
            lastX = zombie.getX();
            lastY = zombie.getY();
        }
        return geometry.entityToScreen(lastX, lastY);
    }
}
