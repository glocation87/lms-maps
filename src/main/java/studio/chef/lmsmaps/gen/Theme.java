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

    public static final List<Theme> ALL = List.of(COLOSSEUM);

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

    public String[] rocks() {
        return rocks.clone();
    }

    public String[] underside() {
        return underside.clone();
    }

    public String[] flora() {
        return flora.clone();
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
