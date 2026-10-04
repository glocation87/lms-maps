package studio.chef.lmsmaps.gen;

import static studio.chef.lmsmaps.gen.Geo.*;

import studio.chef.lmsmaps.gen.Theme.Part;

/**
 * A round snow floor over a pit, walled in with the theme's brick.
 * The floor is always snow because that's the only thing Spleef lets you break.
 * Bounds stop a few blocks under the floor, so dropping into the pit eliminates you before you land.
 */
public final class SpleefGenerator {
    public static final int R = 14;           // snow floor radius
    private static final int PIT = 8;         // pit floor depth
    private static final int WALL = 4;        // wall height above the floor
    private static final int OUT = R + 3;     // outside of the wall
    private static final double SPAWN_RING = R * 0.65;

    private SpleefGenerator() {}

    public static BlockBuffer generate(long seed, int players, Theme t) {
        BlockBuffer b = new BlockBuffer();
        players = Math.max(2, Math.min(16, players));
        b.clearCylinder(OUT + 3, -PIT - 6, WALL + 8);

        for (int x = -OUT; x <= OUT; x++)
            for (int z = -OUT; z <= OUT; z++) {
                double d = dist(x, z);
                if (d > OUT + 0.5) continue;
                if (d <= R + 0.5) {
                    b.set(x, 0, z, "snow_block");
                    b.set(x, -PIT, z, t.surface(seed, x, z));
                    // underside of the pit tapers off like a floating island
                    int depth = 2 + (int) ((R + 0.5 - d) * 0.35 + fbm(seed + 5, x / 5.0, z / 5.0) * 3);
                    for (int y = -PIT - 1; y >= -PIT - depth; y--)
                        b.set(x, y, z, y == -PIT - 1 ? t.get(Part.SOIL) : pick(hash(seed, x, y, z), t.underside()));
                } else {
                    for (int y = -PIT; y <= WALL; y++) b.set(x, y, z, t.brick(seed, x, y, z));
                    b.set(x, WALL + 1, z, t.get(Part.TRIM));
                }
            }

        // lanterns on short pillars around the rim
        for (int k = 0; k < 12; k++) {
            double a = Math.toRadians(k * 30);
            int px = (int) Math.round(Math.cos(a) * (R + 2)), pz = (int) Math.round(Math.sin(a) * (R + 2));
            b.set(px, WALL + 2, pz, t.get(Part.BRICK_CHISELED));
            b.set(px, WALL + 3, pz, t.get(Part.LIGHT));
        }

        for (int k = 0; k < players; k++) {
            double a = 2 * Math.PI * (k + 0.5) / players;
            double sx = Math.round(Math.cos(a) * SPAWN_RING) + 0.5, sz = Math.round(Math.sin(a) * SPAWN_RING) + 0.5;
            b.spawn(sx, 1, sz, yaw(-sx, -sz));
        }

        b.bounds(new BlockBuffer.Region(-OUT, -3, -OUT, OUT, WALL + 12, OUT));
        b.meta("players", players);
        return b;
    }
}
