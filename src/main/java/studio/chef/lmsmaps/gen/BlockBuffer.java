package studio.chef.lmsmaps.gen;

import java.util.*;

/**
 * An in-memory "schematic". Generators write block states here (relative to an origin),
 * then BuildTask places them into the world a few thousand blocks per tick.
 *
 * Block states are plain Minecraft strings, e.g. "stone_bricks" or
 * "quartz_stairs[facing=north]". No Bukkit classes are used here, so the
 * generators can also run outside the server (for previews / tests).
 */
public final class BlockBuffer {

    public record SignText(int x, int y, int z, String[] lines) {}
    public record Spawn(double x, double y, double z, float yaw) {}
    public record Region(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {}

    private final Map<Long, String> blocks = new HashMap<>();
    private final List<SignText> signs = new ArrayList<>();
    private final List<Spawn> spawns = new ArrayList<>();
    private final Map<String, Region> regions = new LinkedHashMap<>();
    private final Map<String, String> meta = new LinkedHashMap<>();
    // named points for a game's map.yml section: lists (island-chests) and single points (center, red-flag)
    private final Map<String, List<Spawn>> markers = new LinkedHashMap<>();
    private final Map<String, Spawn> points = new LinkedHashMap<>();
    private Region bounds;

    // Optional clearing volume (cylinder), applied lazily by BuildTask.
    private int clearRadius = -1, clearMinY, clearMaxY;

    // ---------- coordinates ----------
    public static long key(int x, int y, int z) {
        return ((long) (x + 32768) << 32) | ((long) (y + 32768) << 16) | (long) (z + 32768);
    }
    public static int kx(long k) { return (int) ((k >>> 32) & 0xFFFF) - 32768; }
    public static int ky(long k) { return (int) ((k >>> 16) & 0xFFFF) - 32768; }
    public static int kz(long k) { return (int) (k & 0xFFFF) - 32768; }

    // ---------- writing ----------
    public void set(int x, int y, int z, String state) { blocks.put(key(x, y, z), state); }
    public void air(int x, int y, int z) { blocks.put(key(x, y, z), "air"); }
    public String get(int x, int y, int z) { return blocks.get(key(x, y, z)); }
    public boolean has(int x, int y, int z) {
        String s = get(x, y, z);
        return s != null && !s.equals("air");
    }
    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, String state) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
                    set(x, y, z, state);
    }

    public void sign(int x, int y, int z, String state, String... lines) {
        set(x, y, z, state);
        signs.add(new SignText(x, y, z, Arrays.copyOf(lines, 4)));
    }
    public void spawn(double x, double y, double z, float yaw) { spawns.add(new Spawn(x, y, z, yaw)); }
    public void region(String name, Region r) { regions.put(name, r); }
    public void meta(String k, Object v) { meta.put(k, String.valueOf(v)); }
    public void marker(String key, double x, double y, double z, float yaw) {
        markers.computeIfAbsent(key, k -> new ArrayList<>()).add(new Spawn(x, y, z, yaw));
    }
    public void point(String key, double x, double y, double z, float yaw) { points.put(key, new Spawn(x, y, z, yaw)); }
    /** Where the game counts you as in the map, when that isn't just the clearing cylinder. */
    public void bounds(Region r) { bounds = r; }
    public void clearCylinder(int radius, int minY, int maxY) {
        clearRadius = radius; clearMinY = minY; clearMaxY = maxY;
    }

    // ---------- reading ----------
    public Map<Long, String> blocks() { return blocks; }
    public List<SignText> signs() { return signs; }
    public List<Spawn> spawns() { return spawns; }
    public Map<String, Region> regions() { return regions; }
    public Map<String, String> meta() { return meta; }
    public Map<String, List<Spawn>> markers() { return markers; }
    public Map<String, Spawn> points() { return points; }
    public Region bounds() { return bounds; }
    public int clearRadius() { return clearRadius; }
    public int clearMinY() { return clearMinY; }
    public int clearMaxY() { return clearMaxY; }

    // ---------- block classification ----------
    public static String id(String state) {
        int i = state.indexOf('[');
        return i < 0 ? state : state.substring(0, i);
    }

    private static final String[] FRAGILE = {
            "lantern", "torch", "sign", "pressure_plate", "fern", "dandelion", "poppy", "cornflower",
            "oxeye_daisy", "allium", "lily_pad", "sugar_cane", "amethyst_cluster", "water", "carpet",
            "end_rod", "vine", "azure_bluet", "cactus", "dead_bush", "dry_grass", "roots", "sprouts", "fungus", "lava"
    };

    /** Blocks that need something under/behind them: placed in a second pass. */
    public static boolean isFragile(String state) {
        String id = id(state);
        // snow layers, but not snow_block
        if (id.equals("snow")) return true;
        for (String f : FRAGILE) if (id.contains(f)) return true;
        return false;
    }

    private static boolean isConnector(String id) {
        if (id.contains("_wall_")) return false; // wall_sign, wall_torch...
        return id.endsWith("_fence") || id.endsWith("_wall") || id.endsWith("_pane") || id.equals("iron_bars");
    }

    private static boolean isFullSolid(String state) {
        if (state == null || state.equals("air")) return false;
        String id = id(state);
        if (isFragile(state) || isConnector(id)) return false;
        return !(id.contains("stairs") || id.contains("slab") || id.contains("chest") || id.contains("anvil")
                || id.contains("lectern") || id.contains("enchanting") || id.contains("cluster"));
    }

    /**
     * Placing blocks without physics means fences / walls / bars don't auto-connect.
     * This pre-computes their connection states from their neighbours in the buffer.
     */
    public void connectAll() {
        int[][] dirs = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
        String[] names = {"north", "east", "south", "west"};
        Map<Long, String> updates = new HashMap<>();
        for (Map.Entry<Long, String> e : blocks.entrySet()) {
            String id = id(e.getValue());
            if (!isConnector(id)) continue;
            long k = e.getKey();
            int x = kx(k), y = ky(k), z = kz(k);
            boolean isWall = id.endsWith("_wall");
            boolean[] c = new boolean[4];
            StringBuilder sb = new StringBuilder(id).append('[');
            for (int i = 0; i < 4; i++) {
                String n = get(x + dirs[i][0], y, z + dirs[i][1]);
                boolean conn = n != null && (isConnector(id(n)) || isFullSolid(n));
                c[i] = conn;
                if (i > 0) sb.append(',');
                sb.append(names[i]).append('=').append(isWall ? (conn ? "low" : "none") : String.valueOf(conn));
            }
            if (isWall) {
                boolean straight = (c[0] && c[2] && !c[1] && !c[3]) || (c[1] && c[3] && !c[0] && !c[2]);
                boolean above = has(x, y + 1, z);
                sb.append(",up=").append(!straight || above);
            }
            sb.append(']');
            updates.put(k, sb.toString());
        }
        blocks.putAll(updates);
    }
}
