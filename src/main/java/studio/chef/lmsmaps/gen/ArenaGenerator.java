package studio.chef.lmsmaps.gen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static studio.chef.lmsmaps.gen.Geo.*;

import studio.chef.lmsmaps.gen.Theme.Part;

/**
 * The battle arena: a floating colosseum.
 *
 *  - Grass battlefield (radius 44) with gentle hills, a pond, dirt paths
 *  - Centre: raised ruin plateau with 4 staircases, broken columns and a loot chest
 *  - N spawn pads on a ring (radius 33), each facing the centre
 *  - Seeded cover: ruined walls, boulders, trees, crate stacks
 *  - 6-high inner wall + railing, 10 rows of stands, lantern rim
 *
 * Origin (0,0,0) = centre of the battlefield at ground level. Players stand at y=1 on flat ground.
 */
public final class ArenaGenerator {
    public static final int R = 44;          // battlefield radius
    public static final int WALL = 6;        // inner wall height
    public static final int ROWS = 10;       // rows of stands
    public static final int OUT = R + 2 + ROWS; // outer rim radius
    public static final double SPAWN_RING = 33;

    private ArenaGenerator() {}

    public static BlockBuffer generate(long seed, int players) {
        return generate(seed, players, Theme.COLOSSEUM);
    }

    public static BlockBuffer generate(long seed, int players, Theme t) {
        BlockBuffer b = new BlockBuffer();
        b.clearCylinder(OUT + 2, -34, 26);
        Random rng = new Random(seed);
        players = Math.max(2, Math.min(24, players));

        // ---- spawn pads & pond positions ----
        List<int[]> pads = new ArrayList<>();
        for (int k = 0; k < players; k++) {
            double a = 2 * Math.PI * (k + 0.5) / players;
            pads.add(new int[]{(int) Math.round(Math.cos(a) * SPAWN_RING), (int) Math.round(Math.sin(a) * SPAWN_RING)});
        }
        double pa = Math.toRadians(205 + rng.nextInt(50));
        int pondX = (int) Math.round(Math.cos(pa) * 21), pondZ = (int) Math.round(Math.sin(pa) * 21);

        // ---- heightmap (flat near centre, spawn ring, pond and wall) ----
        int[][] H = new int[2 * R + 1][2 * R + 1];
        for (int x = -R; x <= R; x++)
            for (int z = -R; z <= R; z++) {
                double d = dist(x, z);
                double m = smooth(d, 12, 16) * (1 - smooth(d, R - 7, R - 3))
                        * smooth(Math.abs(d - SPAWN_RING), 3, 6) * smooth(dist(x - pondX, z - pondZ), 6, 9);
                double n = fbm(seed, x / 13.0, z / 13.0);
                H[x + R][z + R] = (int) Math.min(4, Math.floor(Math.max(0, (n - 0.42) * 10) * m));
            }

        // ---- ground, walls, stands, underside ----
        for (int x = -OUT - 1; x <= OUT + 1; x++)
            for (int z = -OUT - 1; z <= OUT + 1; z++) {
                double d = dist(x, z);
                if (d > OUT + 0.5) continue;
                int bottom = -(4 + (int) ((OUT + 0.5 - d) * 0.42 + fbm(seed + 5, x / 6.0, z / 6.0) * 5));
                int top;
                if (d <= R + 0.5) {                                   // battlefield
                    int h = H[x + R][z + R];
                    b.set(x, h, z, surface(t, seed, x, z));
                    for (int y = h - 1; y >= -3; y--) b.set(x, y, z, t.get(Part.SOIL));
                    top = -4;
                } else if (d <= R + 1.5) {                            // inner wall
                    for (int y = 0; y < WALL; y++) b.set(x, y, z, brick(t, seed, x, y, z));
                    b.set(x, WALL, z, t.get(Part.TRIM));
                    b.set(x, WALL + 1, z, t.get(Part.RAILING));
                    top = -1;
                } else if (d <= R + 1.5 + ROWS) {                     // stands
                    int row = (int) (d - (R + 1.5));
                    int seat = WALL + row;
                    String mat = row % 2 == 0 ? t.get(Part.SEAT) : t.get(Part.SEAT_ALT);
                    b.set(x, seat, z, mat + "[facing=" + facing(x, z) + "]");
                    top = seat - 1;
                } else {                                              // outer rim
                    int rimTop = WALL + ROWS + 2;
                    for (int y = WALL + ROWS - 1; y <= rimTop; y++) b.set(x, y, z, brick(t, seed, x, y, z));
                    top = WALL + ROWS - 2;
                }
                for (int y = top; y >= bottom; y--)
                    b.set(x, y, z, y > bottom + 3 ? t.get(Part.UNDERSIDE) : pick(hash(seed, x, y, z), t.underside()));
            }

        // rim pillars with lanterns
        for (int k = 0; k < 24; k++) {
            double a = Math.toRadians(k * 15);
            int px = (int) Math.round(Math.cos(a) * OUT), pz = (int) Math.round(Math.sin(a) * OUT);
            for (int y = WALL + ROWS + 3; y <= WALL + ROWS + 5; y++) b.set(px, y, pz, t.get(Part.BRICK));
            b.set(px, WALL + ROWS + 6, pz, t.get(Part.BRICK_CHISELED));
            b.set(px, WALL + ROWS + 7, pz, t.get(Part.LIGHT));
        }

        // sealed portcullis gates in the inner wall on the 4 axes
        for (int dir = 0; dir < 4; dir++) {
            Frame f = new Frame(dir);
            for (int u = -2; u <= 2; u++) {
                for (int y = 0; y <= 4; y++) b.set(f.x(u, R + 1), y, f.z(u, R + 1), y == 0 ? t.get(Part.GATE_SILL) : t.get(Part.BARS));
                for (int y = 1; y <= 4; y++) b.set(f.x(u, R + 2), y, f.z(u, R + 2), t.get(Part.GATE_BACK));
                b.set(f.x(u, R + 1), 5, f.z(u, R + 1), t.get(Part.GATE_TOP));
            }
        }

        // ---- dirt paths: spawn ring + 4 spokes ----
        for (int x = -R; x <= R; x++)
            for (int z = -R; z <= R; z++) {
                double d = dist(x, z);
                boolean ring = Math.abs(d - SPAWN_RING) < 0.9;
                boolean spoke = (Math.abs(x) <= 1 || Math.abs(z) <= 1) && d > 12 && d < SPAWN_RING;
                if (ring || spoke) b.set(x, H[x + R][z + R], z, t.get(Part.PATH));
            }

        // ---- pond ----
        String floater = t.get(Part.FLOATER), reed = t.get(Part.REED);
        for (int x = pondX - 5; x <= pondX + 5; x++)
            for (int z = pondZ - 5; z <= pondZ + 5; z++) {
                double dp = dist(x - pondX, z - pondZ);
                if (dp <= 3.5) {
                    b.set(x, 0, z, t.get(Part.FLUID));
                    b.set(x, -1, z, dp < 2 ? t.get(Part.FLUID_DEEP) : t.get(Part.SHORE));
                    b.set(x, -2, z, t.get(Part.BED));
                    if (floater != null && hash(seed + 9, x, z) < 0.12) b.set(x, 1, z, floater);
                } else if (dp <= 4.6) {
                    b.set(x, 0, z, t.get(Part.SHORE));
                    if (reed != null && dp <= 4.1 && hash(seed + 11, x, z) < 0.35) { b.set(x, 1, z, reed); b.set(x, 2, z, reed); }
                }
            }

        // ---- central ruin plateau ----
        for (int x = -9; x <= 9; x++)
            for (int z = -9; z <= 9; z++) {
                double d = dist(x, z);
                if (d > 8.2) continue;
                for (int y = 1; y <= 3; y++) b.set(x, y, z, brick(t, seed, x, y, z));
                if (d < 1.5) b.set(x, 3, z, t.get(Part.BRICK_CHISELED));
            }
        for (int dir = 0; dir < 4; dir++) {
            Frame f = new Frame(dir);
            for (int u = -1; u <= 1; u++)
                for (int step = 0; step < 3; step++) {
                    int v = 9 + step, y = 3 - step;
                    b.set(f.x(u, v), y, f.z(u, v), t.get(Part.STAIRS) + "[facing=" + f.in() + "]");
                    for (int yy = 1; yy < y; yy++) b.set(f.x(u, v), yy, f.z(u, v), t.get(Part.BRICK));
                }
        }
        for (int k = 0; k < 8; k++) {
            double a = Math.toRadians(22.5 + k * 45);
            int px = (int) Math.round(Math.cos(a) * 6.3), pz = (int) Math.round(Math.sin(a) * 6.3);
            int h = 2 + rng.nextInt(4);
            for (int y = 4; y < 4 + h; y++) b.set(px, y, pz, brick(t, seed, px, y, pz));
            b.set(px, 4 + h, pz, h >= 4 ? t.get(Part.BRICK_CHISELED) : t.get(Part.SLAB));
        }
        b.set(0, 4, 0, "chest[facing=south]");
        for (int[] c : new int[][]{{2, 2}, {-2, 2}, {2, -2}, {-2, -2}}) {
            b.set(c[0], 4, c[1], t.get(Part.CRATE));
            b.set(c[0], 5, c[1], t.get(Part.LIGHT));
        }
        b.region("center_chest", new BlockBuffer.Region(0, 4, 0, 0, 4, 0));

        // ---- spawn pads ----
        for (int[] p : pads) {
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++)
                    b.set(p[0] + dx, 0, p[1] + dz, dx == 0 && dz == 0 ? t.get(Part.PAD) : t.get(Part.PAD_RIM));
            double len = dist(p[0], p[1]);
            int qx = (int) Math.round(p[0] / len * (SPAWN_RING + 3)), qz = (int) Math.round(p[1] / len * (SPAWN_RING + 3));
            b.set(qx, 1, qz, t.get(Part.POST));
            b.set(qx, 2, qz, t.get(Part.POST));
            b.set(qx, 3, qz, t.get(Part.LIGHT));
            b.spawn(p[0] + 0.5, 1, p[1] + 0.5, yaw(-p[0], -p[1]));
        }

        // ---- seeded cover ----
        List<int[]> placed = new ArrayList<>();
        scatter(b, t, rng, seed, H, pads, pondX, pondZ, placed, 14, 29, 22, true);
        scatter(b, t, rng, seed, H, pads, pondX, pondZ, placed, 37, 41, 12, false);

        // ---- flowers & ferns ----
        for (int x = -R; x <= R; x++)
            for (int z = -R; z <= R; z++) {
                int h = H[x + R][z + R];
                if (!t.get(Part.GROUND).equals(b.get(x, h, z)) || b.get(x, h + 1, z) != null) continue;
                double r = hash(seed + 21, x, z);
                if (r < 0.07) b.set(x, h + 1, z, pick(r / 0.07, t.flora()));
            }

        b.meta("radius", R);
        b.meta("players", players);
        b.connectAll();
        return b;
    }

    private static void scatter(BlockBuffer b, Theme t, Random rng, long seed, int[][] H, List<int[]> pads,
                                int pondX, int pondZ, List<int[]> placed, double rMin, double rMax, int count, boolean inner) {
        int made = 0;
        for (int attempt = 0; attempt < 600 && made < count; attempt++) {
            double a = rng.nextDouble() * Math.PI * 2, r = rMin + rng.nextDouble() * (rMax - rMin);
            int cx = (int) Math.round(Math.cos(a) * r), cz = (int) Math.round(Math.sin(a) * r);
            if (Math.min(Math.abs(cx), Math.abs(cz)) < 4 && r < SPAWN_RING + 1) continue;  // keep spokes clear
            if (dist(cx - pondX, cz - pondZ) < 8) continue;
            boolean bad = false;
            for (int[] p : pads) if (dist(cx - p[0], cz - p[1]) < 6) { bad = true; break; }
            for (int[] p : placed) if (dist(cx - p[0], cz - p[1]) < 7) { bad = true; break; }
            if (bad) continue;
            placed.add(new int[]{cx, cz});
            made++;
            double kind = rng.nextDouble();
            if (inner) {
                if (kind < 0.4) ruinWall(b, t, rng, seed, H, cx, cz);
                else if (kind < 0.65) boulder(b, t, rng, seed, H, cx, cz);
                else if (kind < 0.9) tree(b, t, rng, seed, H, cx, cz);
                else crates(b, t, rng, H, cx, cz);
            } else {
                if (kind < 0.4) tree(b, t, rng, seed, H, cx, cz);
                else if (kind < 0.7) boulder(b, t, rng, seed, H, cx, cz);
                else crates(b, t, rng, H, cx, cz);
            }
        }
    }

    private static int h(int[][] H, int x, int z) {
        int i = x + R, j = z + R;
        if (i < 0 || j < 0 || i >= H.length || j >= H.length) return 0;
        return H[i][j];
    }

    /** A broken wall running tangent to the circle, so it gives cover from the centre. */
    private static void ruinWall(BlockBuffer b, Theme t, Random rng, long seed, int[][] H, int cx, int cz) {
        double a = Math.atan2(cz, cx);
        double tx = -Math.sin(a), tz = Math.cos(a);
        int len = 4 + rng.nextInt(4);
        for (int i = -len / 2; i <= len / 2; i++) {
            if (rng.nextDouble() < 0.1) continue;
            int x = (int) Math.round(cx + tx * i), z = (int) Math.round(cz + tz * i);
            int base = h(H, x, z);
            int height = 1 + rng.nextInt(3) + (Math.abs(i) < len / 3 ? 1 : 0);
            for (int y = base + 1; y <= base + height; y++) b.set(x, y, z, brick(t, seed, x, y, z));
            if (rng.nextDouble() < 0.3) b.set(x, base + height + 1, z, t.get(Part.SLAB));
        }
    }

    private static void boulder(BlockBuffer b, Theme t, Random rng, long seed, int[][] H, int cx, int cz) {
        double r = 1.6 + rng.nextDouble() * 1.1;
        int base = h(H, cx, cz), n = (int) Math.ceil(r);
        for (int dx = -n; dx <= n; dx++)
            for (int dy = -1; dy <= n; dy++)
                for (int dz = -n; dz <= n; dz++) {
                    if (dx * dx + dy * dy * 1.7 + dz * dz > r * r) continue;
                    int x = cx + dx, z = cz + dz;
                    b.set(x, base + 1 + dy, z, pick(hash(seed + 31, x, base + dy, z), t.rocks()));
                }
    }

    private static void tree(BlockBuffer b, Theme t, Random rng, long seed, int[][] H, int cx, int cz) {
        boolean alt = rng.nextBoolean();
        String log = t.get(alt ? Part.ALT_LOG : Part.LOG), leaf = t.get(alt ? Part.ALT_LEAVES : Part.LEAVES);
        int base = h(H, cx, cz), trunk = 4 + rng.nextInt(3), top = base + trunk;
        for (int dx = -3; dx <= 3; dx++)
            for (int dy = -2; dy <= 1; dy++)
                for (int dz = -3; dz <= 3; dz++) {
                    double e = (dx * dx + dz * dz) / (dy >= 0 ? 3.2 : 7.5) + (dy == 1 ? 0.5 : 0);
                    if (e > 1 || (e > 0.6 && hash(seed + 41, cx * 31 + dx, dy * 17 + cz * 7 + dz) < 0.3)) continue;
                    b.set(cx + dx, top + dy, cz + dz, leaf);
                }
        b.set(cx, top + 1, cz, leaf);
        for (int y = base + 1; y <= top - 1; y++) b.set(cx, y, cz, log);
    }

    private static void crates(BlockBuffer b, Theme t, Random rng, int[][] H, int cx, int cz) {
        for (int dx = 0; dx <= 1; dx++)
            for (int dz = 0; dz <= 1; dz++)
                b.set(cx + dx, h(H, cx + dx, cz + dz) + 1, cz + dz, rng.nextDouble() < 0.6 ? t.get(Part.CRATE) : t.get(Part.CRATE_ALT));
        b.set(cx, h(H, cx, cz) + 2, cz, t.get(Part.CRATE));
        if (rng.nextBoolean()) b.set(cx - 1, h(H, cx - 1, cz) + 1, cz, t.get(Part.CRATE_ALT));
    }

    private static String surface(Theme t, long seed, int x, int z) {
        double n = noise(seed + 77, x / 6.0, z / 6.0), r = hash(seed + 3, x, z);
        if (n > 0.78) return t.get(Part.GROUND_LUSH);
        if (n < 0.16) return r < 0.5 ? t.get(Part.GROUND_BARE) : t.get(Part.GROUND_LOOSE);
        if (n < 0.24) return t.get(Part.GROUND_BARE);
        return r < 0.04 ? t.get(Part.GROUND_SPECKLE) : t.get(Part.GROUND);
    }

    private static String brick(Theme t, long seed, int x, int y, int z) {
        double r = hash(seed + 13, x, y, z);
        if (r < 0.15) return t.get(Part.BRICK_CRACKED);
        return r < 0.35 ? t.get(Part.BRICK_MOSSY) : t.get(Part.BRICK);
    }
}
