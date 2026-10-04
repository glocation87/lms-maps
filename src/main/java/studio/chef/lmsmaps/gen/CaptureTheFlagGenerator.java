package studio.chef.lmsmaps.gen;

import static studio.chef.lmsmaps.gen.Geo.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import studio.chef.lmsmaps.gen.Theme.Part;

/**
 * A walled field, red base at +z and blue base at -z. The flag sits on a pedestal in each base and the game
 * places the banner itself. Cover is placed point symmetric, every piece on one half is mirrored on the other.
 */
public final class CaptureTheFlagGenerator {
    public static final int HALF_WIDTH = 16;    // x
    public static final int HALF_LENGTH = 36;   // z
    public static final int FLAG_Z = 28;
    private static final int WALL = 5;

    private CaptureTheFlagGenerator() {}

    public static BlockBuffer generate(long seed, int players, Theme t) {
        BlockBuffer b = new BlockBuffer();
        int perTeam = Math.max(2, Math.min(8, (players + 1) / 2));
        int outX = HALF_WIDTH + 2, outZ = HALF_LENGTH + 2;
        b.clearCylinder((int) Math.ceil(dist(outX, outZ)) + 2, -10, WALL + 12);
        Random rng = new Random(seed);

        for (int x = -outX; x <= outX; x++)
            for (int z = -outZ; z <= outZ; z++) {
                boolean inside = Math.abs(x) <= HALF_WIDTH && Math.abs(z) <= HALF_LENGTH;
                if (inside) {
                    b.set(x, 0, z, t.surface(seed, x, z));
                    for (int y = -1; y >= -3; y--) b.set(x, y, z, t.get(Part.SOIL));
                } else {
                    for (int y = -3; y <= WALL; y++) b.set(x, y, z, t.brick(seed, x, y, z));
                    b.set(x, WALL + 1, z, t.get(Part.TRIM));
                }
                int bottom = -4 - (int) (fbm(seed + 5, x / 6.0, z / 6.0) * 4);
                for (int y = -4; y >= bottom; y--) b.set(x, y, z, pick(hash(seed, x, y, z), t.underside()));
            }

        // midfield line so you can tell which half you're in
        for (int x = -HALF_WIDTH; x <= HALF_WIDTH; x++) b.set(x, 0, 0, t.get(Part.PATH));

        base(b, t, "red", FLAG_Z, perTeam);
        base(b, t, "blue", -FLAG_Z, perTeam);

        List<int[]> placed = new ArrayList<>();
        for (int attempt = 0; attempt < 200 && placed.size() < 7; attempt++) {
            int cx = rng.nextInt(-HALF_WIDTH + 3, HALF_WIDTH - 2), cz = rng.nextInt(5, FLAG_Z - 7);
            boolean crowded = false;
            for (int[] p : placed) if (dist(cx - p[0], cz - p[1]) < 6) { crowded = true; break; }
            if (crowded) continue;
            placed.add(new int[]{cx, cz});
            double kind = rng.nextDouble();
            int length = 2 + rng.nextInt(3), height = 1 + rng.nextInt(2);
            boolean alongX = rng.nextBoolean();
            // the same piece at (x, z) and (-x, -z)
            for (int side = 1; side >= -1; side -= 2) {
                int x = cx * side, z = cz * side;
                if (kind < 0.5) wall(b, t, seed, x, z, length, height, alongX);
                else if (kind < 0.8) crates(b, t, x, z);
                else boulder(b, t, seed, x, z);
            }
        }

        // lights along the long walls
        for (int z = -HALF_LENGTH + 4; z <= HALF_LENGTH - 4; z += 8)
            for (int x : new int[]{-HALF_WIDTH - 1, HALF_WIDTH + 1}) b.set(x, WALL + 2, z, t.get(Part.LIGHT));

        b.bounds(new BlockBuffer.Region(-outX, -6, -outZ, outX, WALL + 16, outZ));
        b.meta("players", players);
        b.connectAll();
        return b;
    }

    // team coloured floor, a pedestal for the flag and a row of spawns behind it facing the enemy base
    private static void base(BlockBuffer b, Theme t, String team, int flagZ, int spawns) {
        int dir = Integer.signum(flagZ);
        for (int x = -5; x <= 5; x++)
            for (int z = flagZ - 4; z <= flagZ + 4; z++)
                if (Math.abs(x) + Math.abs(z - flagZ) <= 7) b.set(x, 0, z, team + "_concrete");
        b.set(0, 0, flagZ, t.get(Part.TRIM));
        b.point(team + "-flag", 0, 1, flagZ, 0);
        for (int x : new int[]{-3, 3})
            for (int z : new int[]{flagZ - 3, flagZ + 3}) {
                b.set(x, 1, z, t.get(Part.POST));
                b.set(x, 2, z, t.get(Part.LIGHT));
            }
        for (int i = 0; i < spawns; i++) {
            double x = (i - (spawns - 1) / 2.0) * 2 + 0.5;
            b.marker(team + "-spawns", x, 1, flagZ + dir * 5 + 0.5, dir > 0 ? 180 : 0);
        }
    }

    private static void wall(BlockBuffer b, Theme t, long seed, int cx, int cz, int length, int height, boolean alongX) {
        for (int i = -length / 2; i <= length / 2; i++) {
            int x = alongX ? cx + i : cx, z = alongX ? cz : cz + i;
            for (int y = 1; y <= height; y++) b.set(x, y, z, t.brick(seed, x, y, z));
        }
    }

    private static void crates(BlockBuffer b, Theme t, int cx, int cz) {
        b.set(cx, 1, cz, t.get(Part.CRATE));
        b.set(cx + 1, 1, cz, t.get(Part.CRATE_ALT));
        b.set(cx, 1, cz + 1, t.get(Part.CRATE_ALT));
        b.set(cx, 2, cz, t.get(Part.CRATE));
    }

    private static void boulder(BlockBuffer b, Theme t, long seed, int cx, int cz) {
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                for (int y = 1; y <= 2; y++)
                    if (y == 1 || dx * dz == 0) b.set(cx + dx, y, cz + dz, pick(hash(seed + 31, cx + dx, y, cz + dz), t.rocks()));
    }
}
