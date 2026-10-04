package studio.chef.lmsmaps.gen;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

// the Nature7 games this plugin can make maps for, the id is the game's maps folder name
public enum GameKind {
    LAST_STANDING("last_standing", ArenaGenerator::generate),
    SPLEEF("spleef", SpleefGenerator::generate),
    SKYWARS("skywars", SkyWarsGenerator::generate),
    CAPTURE_THE_FLAG("capture_the_flag", CaptureTheFlagGenerator::generate);

    @FunctionalInterface
    private interface Generator {
        BlockBuffer generate(long seed, int players, Theme theme);
    }

    private final String id;
    private final Generator generator;

    GameKind(String id, Generator generator) {
        this.id = id;
        this.generator = generator;
    }

    public String id() {
        return id;
    }

    public BlockBuffer generate(long seed, int players, Theme theme) {
        return generator.generate(seed, players, theme);
    }

    public static Optional<GameKind> byId(String id) {
        String wanted = id.toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(kind -> kind.id.equals(wanted)).findFirst();
    }
}
