package boss.core;

import models.level.core.AdventureLevelCatalog;

public final class BossCatalog {
    private static final String EGYPT_PATH =
            "768/INITIAL/ZOMBIE/ZOMBIE_EGYPT_ZOMBOSS/ZOMBIE_EGYPT_ZOMBOSS.PAM";
    private static final String FROSTBITE_PATH =
            "768/FULL/ZOMBIE/ZOMBIE_ICEAGE_ZOMBOSS/ZOMBIE_ICEAGE_ZOMBOSS.PAM";

    private BossCatalog() {
    }

    public static boolean supportsChapter(String chapterName) {
        String chapter = AdventureLevelCatalog.normalizeChapterName(chapterName);
        return chapter.equals("ancient-egypt") || chapter.equals("ice-cave");
    }

    public static Boss create(String chapterName) {
        String chapter = AdventureLevelCatalog.normalizeChapterName(chapterName);
        return switch (chapter) {
            case "ancient-egypt" -> new Boss(
                    "zomboss-egypt",
                    "Zombot Sphinx-inator",
                    chapter,
                    EGYPT_PATH,
                    0.82f,
                    new BossHealth(4000, 8000, 6500),
                    true,
                    true,
                    "intro", "idle", "walk_up", "walk_down", "zombie_portal_start",
                    "stun_loop", "die_idle",
                    26, 24, 30
            );
            case "ice-cave" -> new Boss(
                    "zomboss-frostbite",
                    "Zombot Tuskmaster 10,000 BC",
                    chapter,
                    FROSTBITE_PATH,
                    0.86f,
                    new BossHealth(5000, 4000, 4000),
                    false,
                    false,
                    "intro", "idle", "idle", "idle", "idle",
                    "stun", "die",
                    47, 24, 40
            );
            default -> throw new IllegalArgumentException(
                    "P2-09 boss core has no profile for chapter: " + chapterName
            );
        };
    }
}
