package models.engine.board;

/**
 * Shared limits for runtime-created graves.
 *
 * <p>Static level graves are configured before play starts, while runtime systems such as
 * Tomb Raiser and Dark Ages wave events must respect a shared active-grave ceiling so they
 * cannot gradually consume the entire lawn.</p>
 */
public final class GraveSpawnRules {
    /**
     * Keeps dynamic grave mechanics meaningful without letting them cover a 5x9 lawn.
     * Egypt currently starts with at most four configured graves, leaving room for the
     * Tomb Raiser's six-grave lifetime output.
     */
    public static final int MAX_ACTIVE_GRAVES = 10;

    private GraveSpawnRules() {
    }

    public static int countActiveGraves(Board board) {
        if (board == null) {
            return 0;
        }
        int count = 0;
        for (Lane lane : board.getLanes()) {
            for (Tile tile : lane.getTiles()) {
                if (tile != null && tile.isGraveTerrain()) {
                    count++;
                }
            }
        }
        return count;
    }

    public static int remainingCapacity(Board board) {
        return Math.max(0, MAX_ACTIVE_GRAVES - countActiveGraves(board));
    }
}
