package studio.chef.lmsmaps.gen;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

// The blocks an arena is built from. The layout never changes between themes, only the materials do
public final class Theme {

    public enum Part {
        GROUND, GROUND_SPECKLE, GROUND_LUSH, GROUND_BARE, GROUND_LOOSE, SOIL, PATH,
        BRICK, BRICK_MOSSY, BRICK_CRACKED, BRICK_CHISELED, TRIM, RAILING,
        SEAT, SEAT_ALT, STAIRS, SLAB, POST,
        GATE_SILL, GATE_BACK, GATE_TOP, BARS, LIGHT, PAD, PAD_RIM,
        FLUID, FLUID_DEEP, SHORE, BED, FLOATER, REED,
        LOG, LEAVES, ALT_LOG, ALT_LEAVES, CRATE, CRATE_ALT, UNDERSIDE
    }

    public static final Theme COLOSSEUM = new Builder("colosseum", "Colosseum")
        .set(Part.GROUND, "grass_block")
        .set(Part.GROUND_SPECKLE, "podzol")
        .set(Part.GROUND_LUSH, "moss_block")
        .set(Part.GROUND_BARE, "coarse_dirt")
        .set(Part.GROUND_LOOSE, "gravel")
        .set(Part.SOIL, "dirt")
        .set(Part.PATH, "dirt_path")
        .set(Part.BRICK, "stone_bricks")
        .set(Part.BRICK_MOSSY, "mossy_stone_bricks")
        .set(Part.BRICK_CRACKED, "cracked_stone_bricks")
        .set(Part.BRICK_CHISELED, "chiseled_stone_bricks")
        .set(Part.TRIM, "polished_andesite")
        .set(Part.RAILING, "polished_blackstone_wall")
        .set(Part.SEAT, "stone_brick_stairs")
        .set(Part.SEAT_ALT, "polished_andesite_stairs")
        .set(Part.STAIRS, "stone_brick_stairs")
        .set(Part.SLAB, "stone_brick_slab[type=bottom]")
        .set(Part.POST, "stone_brick_wall")
        .set(Part.GATE_SILL, "polished_blackstone_bricks")
        .set(Part.GATE_BACK, "polished_blackstone")
        .set(Part.GATE_TOP, "chiseled_polished_blackstone")
        .set(Part.BARS, "iron_bars")
        .set(Part.LIGHT, "lantern")
        .set(Part.PAD, "gold_block")
        .set(Part.PAD_RIM, "polished_andesite")
        .set(Part.FLUID, "water")
        .set(Part.FLUID_DEEP, "water")
        .set(Part.SHORE, "sand")
        .set(Part.BED, "clay")
        .set(Part.FLOATER, "lily_pad")
        .set(Part.REED, "sugar_cane")
        .set(Part.LOG, "oak_log")
        .set(Part.LEAVES, "oak_leaves[persistent=true]")
        .set(Part.ALT_LOG, "birch_log")
        .set(Part.ALT_LEAVES, "birch_leaves[persistent=true]")
        .set(Part.CRATE, "barrel[facing=up]")
        .set(Part.CRATE_ALT, "hay_block")
        .set(Part.UNDERSIDE, "stone")
        .rocks("cobblestone", "mossy_cobblestone", "andesite", "stone")
        .underside("stone", "andesite", "tuff", "cobblestone")
        .flora("fern", "fern", "dandelion", "poppy", "cornflower", "oxeye_daisy")
        .build();

    public static final Theme DESERT = COLOSSEUM.toBuilder("desert", "Desert Ruins")
        .set(Part.GROUND, "sand")
        .set(Part.GROUND_SPECKLE, "red_sand")
        .set(Part.GROUND_LUSH, "red_sand")
        .set(Part.GROUND_BARE, "sandstone")
        .set(Part.GROUND_LOOSE, "smooth_sandstone")
        .set(Part.SOIL, "sandstone")
        .set(Part.PATH, "smooth_sandstone")
        .set(Part.BRICK, "cut_sandstone")
        .set(Part.BRICK_MOSSY, "sandstone")
        .set(Part.BRICK_CRACKED, "smooth_sandstone")
        .set(Part.BRICK_CHISELED, "chiseled_sandstone")
        .set(Part.TRIM, "cut_red_sandstone")
        .set(Part.RAILING, "sandstone_wall")
        .set(Part.SEAT, "sandstone_stairs")
        .set(Part.SEAT_ALT, "smooth_sandstone_stairs")
        .set(Part.STAIRS, "sandstone_stairs")
        .set(Part.SLAB, "sandstone_slab[type=bottom]")
        .set(Part.POST, "sandstone_wall")
        .set(Part.GATE_SILL, "chiseled_red_sandstone")
        .set(Part.GATE_BACK, "red_sandstone")
        .set(Part.GATE_TOP, "chiseled_red_sandstone")
        .set(Part.PAD_RIM, "cut_red_sandstone")
        .set(Part.LOG, "acacia_log")
        .set(Part.LEAVES, "acacia_leaves[persistent=true]")
        .set(Part.ALT_LOG, "jungle_log")
        .set(Part.ALT_LEAVES, "jungle_leaves[persistent=true]")
        .set(Part.UNDERSIDE, "sandstone")
        .rocks("sandstone", "red_sandstone", "smooth_sandstone", "terracotta")
        .underside("sandstone", "red_sandstone", "terracotta", "orange_terracotta")
        .flora("dead_bush", "dead_bush", "short_dry_grass", "cactus")
        .build();

    public static final Theme SNOW = COLOSSEUM.toBuilder("snow", "Frostkeep")
        .set(Part.GROUND, "snow_block")
        .set(Part.GROUND_SPECKLE, "calcite")
        .set(Part.GROUND_LUSH, "packed_ice")
        .set(Part.GROUND_BARE, "gravel")
        .set(Part.GROUND_LOOSE, "stone")
        .set(Part.BRICK, "deepslate_bricks")
        .set(Part.BRICK_MOSSY, "deepslate_tiles")
        .set(Part.BRICK_CRACKED, "cracked_deepslate_bricks")
        .set(Part.BRICK_CHISELED, "chiseled_deepslate")
        .set(Part.TRIM, "polished_diorite")
        .set(Part.RAILING, "deepslate_brick_wall")
        .set(Part.SEAT, "deepslate_brick_stairs")
        .set(Part.SEAT_ALT, "polished_deepslate_stairs")
        .set(Part.STAIRS, "deepslate_brick_stairs")
        .set(Part.SLAB, "deepslate_brick_slab[type=bottom]")
        .set(Part.POST, "deepslate_brick_wall")
        .set(Part.GATE_SILL, "polished_deepslate")
        .set(Part.GATE_BACK, "deepslate_tiles")
        .set(Part.GATE_TOP, "chiseled_deepslate")
        .set(Part.PAD, "diamond_block")
        .set(Part.PAD_RIM, "polished_diorite")
        // a frozen pond, ice on top with water underneath
        .set(Part.FLUID, "ice")
        .set(Part.SHORE, "gravel")
        .set(Part.BED, "packed_ice")
        .set(Part.FLOATER, null)
        .set(Part.REED, null)
        .set(Part.LOG, "spruce_log")
        .set(Part.LEAVES, "spruce_leaves[persistent=true]")
        .set(Part.CRATE_ALT, "spruce_planks")
        .rocks("stone", "cobblestone", "andesite", "diorite")
        .underside("stone", "deepslate", "tuff", "cobblestone")
        .flora("snow[layers=1]", "snow[layers=1]", "snow[layers=2]", "fern")
        .build();

    public static final Theme NETHER = COLOSSEUM.toBuilder("nether", "Inferno")
        .set(Part.GROUND, "crimson_nylium")
        .set(Part.GROUND_SPECKLE, "shroomlight")
        .set(Part.GROUND_LUSH, "warped_nylium")
        .set(Part.GROUND_BARE, "netherrack")
        .set(Part.GROUND_LOOSE, "blackstone")
        .set(Part.SOIL, "netherrack")
        .set(Part.PATH, "soul_soil")
        .set(Part.BRICK, "nether_bricks")
        .set(Part.BRICK_MOSSY, "red_nether_bricks")
        .set(Part.BRICK_CRACKED, "cracked_nether_bricks")
        .set(Part.BRICK_CHISELED, "chiseled_nether_bricks")
        .set(Part.TRIM, "polished_blackstone")
        .set(Part.RAILING, "nether_brick_fence")
        .set(Part.SEAT, "nether_brick_stairs")
        .set(Part.SEAT_ALT, "red_nether_brick_stairs")
        .set(Part.STAIRS, "nether_brick_stairs")
        .set(Part.SLAB, "nether_brick_slab[type=bottom]")
        .set(Part.POST, "red_nether_brick_wall")
        .set(Part.LIGHT, "soul_lantern")
        .set(Part.PAD, "crying_obsidian")
        .set(Part.PAD_RIM, "polished_blackstone")
        .set(Part.FLUID, "lava")
        .set(Part.FLUID_DEEP, "lava")
        .set(Part.SHORE, "basalt")
        .set(Part.BED, "obsidian")
        .set(Part.FLOATER, null)
        .set(Part.REED, null)
        // nether wood doesn't burn, so the lava pond can't set the arena on fire
        .set(Part.LOG, "crimson_stem")
        .set(Part.LEAVES, "nether_wart_block")
        .set(Part.ALT_LOG, "warped_stem")
        .set(Part.ALT_LEAVES, "warped_wart_block")
        .set(Part.CRATE_ALT, "bone_block")
        .set(Part.UNDERSIDE, "netherrack")
        .rocks("blackstone", "basalt", "netherrack", "polished_basalt")
        .underside("netherrack", "blackstone", "basalt", "soul_soil")
        .flora("crimson_roots", "crimson_roots", "crimson_fungus", "nether_sprouts", "warped_roots")
        .build();

    public static final List<Theme> ALL = List.of(COLOSSEUM, DESERT, SNOW, NETHER);

    private final String id;
    private final String displayName;
    private final Map<Part, String> parts;
    private final String[] rocks;
    private final String[] underside;
    private final String[] flora;

    private Theme(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.parts = new EnumMap<>(builder.parts);
        this.rocks = builder.rocks;
        this.underside = builder.underside;
        this.flora = builder.flora;
    }

    public static Optional<Theme> byId(String id) {
        String wanted = id.toLowerCase(Locale.ROOT);
        return ALL.stream().filter(theme -> theme.id.equals(wanted)).findFirst();
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    // Null when the theme leaves that part out, like lily pads on a frozen pond
    public @Nullable String get(Part part) {
        return parts.get(part);
    }

    // weathered brick mix, the same block at the same spot for the same seed
    public String brick(long seed, int x, int y, int z) {
        double r = Geo.hash(seed + 13, x, y, z);
        if (r < 0.15) return get(Part.BRICK_CRACKED);
        return r < 0.35 ? get(Part.BRICK_MOSSY) : get(Part.BRICK);
    }

    // patchy ground, mostly the main block with lush, bare and loose spots from noise
    public String surface(long seed, int x, int z) {
        double n = Geo.noise(seed + 77, x / 6.0, z / 6.0), r = Geo.hash(seed + 3, x, z);
        if (n > 0.78) return get(Part.GROUND_LUSH);
        if (n < 0.16) return r < 0.5 ? get(Part.GROUND_BARE) : get(Part.GROUND_LOOSE);
        if (n < 0.24) return get(Part.GROUND_BARE);
        return r < 0.04 ? get(Part.GROUND_SPECKLE) : get(Part.GROUND);
    }

    public String[] rocks() {
        return rocks.clone();
    }

    public String[] underside() {
        return underside.clone();
    }

    public String[] flora() {
        return flora.clone();
    }

    @Override
    public String toString() {
        return id;
    }

    Builder toBuilder(String newId, String newDisplayName) {
        Builder builder = new Builder(newId, newDisplayName);
        builder.parts.putAll(parts);
        builder.rocks = rocks;
        builder.underside = underside;
        builder.flora = flora;
        return builder;
    }

    static final class Builder {
        private final String id;
        private final String displayName;
        private final Map<Part, String> parts = new EnumMap<>(Part.class);
        private String[] rocks;
        private String[] underside;
        private String[] flora;

        Builder(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        Builder set(Part part, @Nullable String state) {
            parts.put(part, state);
            return this;
        }

        Builder rocks(String... states) {
            rocks = states;
            return this;
        }

        Builder underside(String... states) {
            underside = states;
            return this;
        }

        Builder flora(String... states) {
            flora = states;
            return this;
        }

        Theme build() {
            for (Part part : Part.values()) {
                if (!parts.containsKey(part)) {
                    throw new IllegalStateException(id + " is missing " + part);
                }
            }
            Objects.requireNonNull(rocks, "rocks");
            Objects.requireNonNull(underside, "underside");
            Objects.requireNonNull(flora, "flora");
            return new Theme(this);
        }
    }
}
