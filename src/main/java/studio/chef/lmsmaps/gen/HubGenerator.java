package studio.chef.lmsmaps.gen;

import static studio.chef.lmsmaps.gen.Geo.*;

/**
 * The lobby: a floating circular plaza.
 *
 *  - Centre: raised quartz dais with a fountain
 *  - 4 quartz walkways lead to 4 gate alcoves:
 *      N = PLAY (gold pressure-plate pad = join queue), E = KITS, W = STATS, S = SPECTATE
 *  - Cherry trees in planters on the diagonals, lamp posts, a walled rim with lantern pillars
 *  - Players spawn on the south walkway looking north at the fountain and the PLAY gate
 *
 * Origin (0,0,0) = centre of the floor. The floor surface is y=0, players stand at y=1.
 */
public final class HubGenerator {
    public static final int R = 26;

    private HubGenerator() {}

    public static BlockBuffer generate(long seed) {
        BlockBuffer b = new BlockBuffer();
        b.clearCylinder(R + 3, -24, 14);

        // ---- floor + floating-island underside ----
        for (int x = -R - 1; x <= R + 1; x++) {
            for (int z = -R - 1; z <= R + 1; z++) {
                double d = dist(x, z);
                if (d > R + 0.5) continue;
                b.set(x, 0, z, floor(seed, x, z, d));
                int depth = 3 + (int) Math.round((R + 0.5 - d) * 0.5 + fbm(seed, x / 5.0, z / 5.0) * 4);
                for (int y = -1; y >= -depth; y--) {
                    String s = y == -1 ? "stone_bricks" : y > -4 ? "stone"
                            : pick(hash(seed, x, y, z), "stone", "andesite", "tuff", "cobblestone", "stone");
                    b.set(x, y, z, s);
                }
            }
        }

        // ---- outer wall with lantern pillars ----
        for (int x = -R - 1; x <= R + 1; x++) {
            for (int z = -R - 1; z <= R + 1; z++) {
                double d = dist(x, z);
                if (d <= R - 1 || d > R + 0.5) continue;
                for (int y = 1; y <= 3; y++) b.set(x, y, z, brick(seed, x, y, z));
                b.set(x, 4, z, "stone_brick_slab[type=bottom]");
            }
        }
        for (int k = 0; k < 12; k++) {
            if (k % 3 == 0) continue; // gates sit on the axes
            double a = Math.toRadians(k * 30);
            int px = (int) Math.round(Math.cos(a) * (R - 0.5)), pz = (int) Math.round(Math.sin(a) * (R - 0.5));
            for (int y = 1; y <= 4; y++) b.set(px, y, pz, "stone_bricks");
            b.set(px, 5, pz, "chiseled_stone_bricks");
            b.set(px, 6, pz, "lantern");
        }

        // ---- central dais + fountain ----
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                double d = dist(x, z);
                if (d > 4.5) continue;
                b.set(x, 0, z, "quartz_bricks");
                b.set(x, 1, z, d > 3.5 ? "quartz_stairs[facing=" + facing(-x, -z) + "]" : "quartz_bricks");
                if (d > 2.5 && d <= 3.5) b.set(x, 2, z, (x == 0 || z == 0) ? "chiseled_quartz_block" : "quartz_bricks");
                else if (d <= 2.5 && !(x == 0 && z == 0)) b.set(x, 2, z, "water");
            }
        }
        for (int y = 2; y <= 4; y++) b.set(0, y, 0, "quartz_pillar");
        b.set(0, 5, 0, "sea_lantern");
        b.set(0, 6, 0, "end_rod[facing=up]");

        // ---- lamp posts ----
        for (int k = 0; k < 8; k++) {
            double a = Math.toRadians(22.5 + k * 45);
            int px = (int) Math.round(Math.cos(a) * 15.5), pz = (int) Math.round(Math.sin(a) * 15.5);
            b.set(px, 1, pz, "polished_blackstone_wall");
            b.set(px, 2, pz, "dark_oak_fence");
            b.set(px, 3, pz, "dark_oak_fence");
            b.set(px, 4, pz, "lantern");
        }

        // ---- cherry trees in planters on the diagonals ----
        for (int k = 0; k < 4; k++) {
            double a = Math.toRadians(45 + k * 90);
            int cx = (int) Math.round(Math.cos(a) * 20.5), cz = (int) Math.round(Math.sin(a) * 20.5);
            planterTree(b, seed + k, cx, cz);
        }

        // ---- gates ----
        gate(b, new Frame(0), "crying_obsidian", "gold_block", true,
                "", "&l» PLAY «", "&8Last Man", "&8Standing");
        gate(b, new Frame(1), "waxed_cut_copper", "waxed_copper_block", false,
                "", "&lKITS", "&8choose your", "&8loadout");
        gate(b, new Frame(3), "bookshelf", "polished_blackstone", false,
                "", "&lSTATS", "&8leaderboard", "");
        gate(b, new Frame(2), "glass", "amethyst_block", false,
                "", "&lSPECTATE", "&8watch a", "&8live round");

        // gate decor
        Frame e = new Frame(1), w = new Frame(3), s = new Frame(2), n = new Frame(0);
        b.set(e.x(-2, R - 1), 1, e.z(-2, R - 1), "anvil[facing=" + e.left() + "]");
        b.set(e.x(0, R - 1), 1, e.z(0, R - 1), "enchanting_table");
        b.set(e.x(2, R - 1), 1, e.z(2, R - 1), "smithing_table");
        b.set(w.x(0, R - 2), 1, w.z(0, R - 2), "lectern[facing=" + w.in() + "]");
        for (int u : new int[]{-2, 2}) {
            b.set(s.x(u, R - 1), 1, s.z(u, R - 1), "amethyst_block");
            b.set(s.x(u, R - 1), 2, s.z(u, R - 1), "amethyst_cluster[facing=up]");
            b.set(n.x(u, R - 1), 1, n.z(u, R - 1), "respawn_anchor[charges=4]");
        }

        // ---- spawn: south walkway, looking north ----
        b.spawn(0.5, 1, 15.5, 180f);
        b.meta("radius", R);
        b.connectAll();
        return b;
    }

    private static String floor(long seed, int x, int z, double d) {
        if (Math.abs(x) <= 1 || Math.abs(z) <= 1) return "smooth_quartz";                 // walkways
        if ((d >= 11.5 && d < 12.5) || (d >= 19 && d < 20)) return "polished_blackstone_bricks"; // trim rings
        if (d < 11.5) return (((x >> 1) + (z >> 1)) & 1) == 0 ? "smooth_stone" : "polished_andesite";
        if (d < 19) return "polished_andesite";
        return brick(seed, x, 0, z);
    }

    private static String brick(long seed, int x, int y, int z) {
        double r = hash(seed + 7, x, y, z);
        return r < 0.12 ? "cracked_stone_bricks" : r < 0.22 ? "mossy_stone_bricks" : "stone_bricks";
    }

    private static void planterTree(BlockBuffer b, long seed, int cx, int cz) {
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++) {
                boolean rim = Math.max(Math.abs(dx), Math.abs(dz)) == 2;
                b.set(cx + dx, 1, cz + dz, rim ? "stone_bricks" : "grass_block");
                if (!rim && (dx != 0 || dz != 0) && hash(seed, dx, dz) < 0.5)
                    b.set(cx + dx, 2, cz + dz, pick(hash(seed + 3, dx, dz), "allium", "azure_bluet", "oxeye_daisy"));
            }
        for (int y = 2; y <= 7; y++) b.set(cx, y, cz, "cherry_log");
        for (int dx = -3; dx <= 3; dx++)
            for (int dy = -2; dy <= 2; dy++)
                for (int dz = -3; dz <= 3; dz++) {
                    double e = (dx * dx + dz * dz) / 10.0 + (dy * dy) / 4.0;
                    if (e > 1 || (e > 0.7 && hash(seed, dx * 13 + dy, dz) < 0.35)) continue;
                    int y = 8 + dy;
                    if (dx == 0 && dz == 0 && y <= 7) continue;
                    b.set(cx + dx, y, cz + dz, "cherry_leaves[persistent=true]");
                }
    }

    /** A recessed alcove in the rim wall, facing the plaza. */
    private static void gate(BlockBuffer b, Frame f, String panel, String accent, boolean joinPad, String... sign) {
        int v0 = R - 5, v1 = R;
        for (int u = -3; u <= 3; u++)
            for (int v = v0; v <= v1; v++) {
                boolean pad = Math.abs(u) <= 1 && v >= v0 + 1 && v <= v0 + 3;
                b.set(f.x(u, v), 0, f.z(u, v), pad ? accent : "polished_blackstone_bricks");
                for (int y = 1; y <= 7; y++) {
                    String s;
                    if (y == 7) s = v == v0 ? "chiseled_polished_blackstone" : "polished_blackstone_bricks";
                    else if (Math.abs(u) == 3) s = v == v0 ? "polished_blackstone" : "polished_blackstone_bricks";
                    else if (v == v1) s = panel;
                    else s = "air";
                    b.set(f.x(u, v), y, f.z(u, v), s);
                }
                if (pad && joinPad) b.set(f.x(u, v), 1, f.z(u, v), "light_weighted_pressure_plate");
            }
        b.set(f.x(0, v0), 8, f.z(0, v0), "gold_block");
        b.sign(f.x(0, v0 - 1), 7, f.z(0, v0 - 1), "birch_wall_sign[facing=" + f.in() + "]", sign);
        for (int u : new int[]{-2, 2}) b.set(f.x(u, v0 + 1), 6, f.z(u, v0 + 1), "lantern[hanging=true]");
        if (joinPad) {
            int ax = f.x(-1, v0 + 1), az = f.z(-1, v0 + 1), bx = f.x(1, v0 + 3), bz = f.z(1, v0 + 3);
            b.region("join_pad", new BlockBuffer.Region(Math.min(ax, bx), 1, Math.min(az, bz),
                    Math.max(ax, bx), 2, Math.max(az, bz)));
        }
    }
}
