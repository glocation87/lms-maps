package studio.chef.lmsmaps.gen;

import static studio.chef.lmsmaps.gen.Geo.*;

import java.util.Random;
import studio.chef.lmsmaps.gen.Theme.Part;

/**
 * One small island per player on a ring, a bigger centre island with the good chests, nothing but void between.
 * Islands are a few blocks apart so you have to bridge, the centre is a longer bridge away.
 */
public final class SkyWarsGenerator {
    public static final int RING = 26;
    private static final int ISLAND_R = 4;
    private static final int CENTER_R = 8;

    private SkyWarsGenerator() {}

    public static BlockBuffer generate(long seed, int players, Theme t) {
        BlockBuffer b = new BlockBuffer();
        players = Math.max(2, Math.min(12, players));
        int edge = RING + ISLAND_R + 4;
        b.clearCylinder(edge + 2, -20, 30);
        Random rng = new Random(seed);

        for (int k = 0; k < players; k++) {
            double a = 2 * Math.PI * (k + 0.5) / players;
            int cx = (int) Math.round(Math.cos(a) * RING), cz = (int) Math.round(Math.sin(a) * RING);
            island(b, t, seed + k, cx, cz, ISLAND_R);
            b.spawn(cx + 0.5, 1, cz + 0.5, yaw(-cx, -cz));

            // chest on the outer edge facing the middle, the tree goes on the side so it doesn't block the bridge
            int ox = (int) Math.round(Math.cos(a) * 2), oz = (int) Math.round(Math.sin(a) * 2);
            b.set(cx + ox, 1, cz + oz, "chest[facing=" + facing(-ox, -oz) + "]");
            b.marker("island-chests", cx + ox, 1, cz + oz, 0);
            if (rng.nextBoolean()) tree(b, t, rng, cx - oz, cz + ox);
        }

        island(b, t, seed + 99, 0, 0, CENTER_R);
        for (int[] c : new int[][]{{3, 3}, {-3, 3}, {3, -3}, {-3, -3}}) {
            b.set(c[0], 1, c[1], "chest[facing=" + facing(-c[0], -c[1]) + "]");
            b.marker("center-chests", c[0], 1, c[1], 0);
        }
        // a raised middle with lights, so the centre reads as the place to fight over
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++)
                b.set(x, 1, z, x == 0 && z == 0 ? t.get(Part.BRICK_CHISELED) : t.get(Part.TRIM));
        for (int[] c : new int[][]{{5, 0}, {-5, 0}, {0, 5}, {0, -5}}) {
            b.set(c[0], 1, c[1], t.get(Part.POST));
            b.set(c[0], 2, c[1], t.get(Part.LIGHT));
        }
        b.point("center", 0.5, 2, 0.5, 0);

        b.bounds(new BlockBuffer.Region(-edge, -24, -edge, edge, 40, edge));
        b.meta("players", players);
        b.connectAll();
        return b;
    }

    // flat top, then a rough cone underneath so it looks like it was torn out of the ground
    private static void island(BlockBuffer b, Theme t, long seed, int cx, int cz, int r) {
        for (int dx = -r - 1; dx <= r + 1; dx++)
            for (int dz = -r - 1; dz <= r + 1; dz++) {
                double d = dist(dx, dz) + (noise(seed, dx / 2.5, dz / 2.5) - 0.5) * 1.2;
                if (d > r + 0.5) continue;
                int x = cx + dx, z = cz + dz;
                b.set(x, 0, z, t.surface(seed, x, z));
                int depth = 2 + (int) ((r + 0.5 - d) * 1.1 + fbm(seed + 7, x / 3.0, z / 3.0) * 2);
                for (int y = -1; y >= -depth; y--)
                    b.set(x, y, z, y > -3 ? t.get(Part.SOIL) : pick(hash(seed, x, y, z), t.underside()));
            }
    }

    private static void tree(BlockBuffer b, Theme t, Random rng, int x, int z) {
        boolean alt = rng.nextBoolean();
        String log = t.get(alt ? Part.ALT_LOG : Part.LOG), leaf = t.get(alt ? Part.ALT_LEAVES : Part.LEAVES);
        int top = 4 + rng.nextInt(2);
        for (int dx = -2; dx <= 2; dx++)
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -2; dz <= 2; dz++)
                    if (dx * dx + dz * dz + dy * dy * 2 <= 5) b.set(x + dx, top + dy, z + dz, leaf);
        for (int y = 1; y < top; y++) b.set(x, y, z, log);
    }
}
