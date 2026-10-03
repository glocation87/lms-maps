package studio.chef.lmsmaps.gen;

/** Small deterministic math helpers: distance, hashing, value noise, facings. */
public final class Geo {
    private Geo() {}

    public static double dist(double x, double z) { return Math.sqrt(x * x + z * z); }

    /** Deterministic pseudo-random value in [0,1) for a grid cell. */
    public static double hash(long seed, int x, int z) {
        long h = seed * 0x9E3779B97F4A7C15L + x * 0xC2B2AE3D27D4EB4FL + z * 0x165667B19E3779F9L;
        h ^= h >>> 29; h *= 0xBF58476D1CE4E5B9L; h ^= h >>> 32; h *= 0x94D049BB133111EBL; h ^= h >>> 29;
        return (h >>> 11) * 0x1.0p-53;
    }
    public static double hash(long seed, int x, int y, int z) { return hash(seed + y * 7919L, x, z); }

    /** Smooth value noise in [0,1). */
    public static double noise(long seed, double x, double z) {
        int x0 = (int) Math.floor(x), z0 = (int) Math.floor(z);
        double fx = x - x0, fz = z - z0;
        double sx = fx * fx * (3 - 2 * fx), sz = fz * fz * (3 - 2 * fz);
        double a = hash(seed, x0, z0), b = hash(seed, x0 + 1, z0);
        double c = hash(seed, x0, z0 + 1), d = hash(seed, x0 + 1, z0 + 1);
        return lerp(lerp(a, b, sx), lerp(c, d, sx), sz);
    }
    /** Two-octave noise, roughly [0,1). */
    public static double fbm(long seed, double x, double z) {
        return (noise(seed, x, z) * 2 + noise(seed + 101, x * 2.1, z * 2.1)) / 3.0;
    }

    public static double lerp(double a, double b, double t) { return a + (b - a) * t; }
    /** 0 at or below lo, 1 at or above hi, smooth in between. */
    public static double smooth(double v, double lo, double hi) {
        double t = Math.max(0, Math.min(1, (v - lo) / (hi - lo)));
        return t * t * (3 - 2 * t);
    }

    public static String pick(double r, String... options) {
        return options[Math.min(options.length - 1, (int) (r * options.length))];
    }

    /** The horizontal facing that points along (dx,dz). North is -Z. */
    public static String facing(double dx, double dz) {
        if (Math.abs(dx) > Math.abs(dz)) return dx > 0 ? "east" : "west";
        return dz > 0 ? "south" : "north";
    }

    /** Minecraft yaw for looking along (dx,dz). 0 = south, 180 = north. */
    public static float yaw(double dx, double dz) {
        return (float) Math.toDegrees(Math.atan2(-dx, dz));
    }

    /**
     * A local frame for building the same structure on each side of a circle.
     * u runs sideways, v runs outward from the centre. dir: 0=N, 1=E, 2=S, 3=W.
     */
    public record Frame(int dir) {
        public int x(int u, int v) { return switch (dir) { case 0 -> u; case 1 -> v; case 2 -> -u; default -> -v; }; }
        public int z(int u, int v) { return switch (dir) { case 0 -> -v; case 1 -> u; case 2 -> v; default -> -u; }; }
        public String out() { return new String[]{"north", "east", "south", "west"}[dir]; }
        public String in() { return new String[]{"south", "west", "north", "east"}[dir]; }
        public String left() { return new String[]{"west", "north", "east", "south"}[dir]; }
        public String right() { return new String[]{"east", "south", "west", "north"}[dir]; }
    }
}
