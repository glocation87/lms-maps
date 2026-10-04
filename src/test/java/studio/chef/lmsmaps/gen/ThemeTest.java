package studio.chef.lmsmaps.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.FieldSource;
import org.mockbukkit.mockbukkit.MockBukkit;

class ThemeTest {
    static final List<Theme> THEMES = Theme.ALL;

    private static Set<String> blockIds(BlockBuffer buffer) {
        Set<String> ids = new TreeSet<>();
        buffer.blocks().values().forEach(state -> ids.add(BlockBuffer.id(state)));
        return ids;
    }

    @BeforeAll
    static void startServer() {
        MockBukkit.mock();
    }

    @AfterAll
    static void stopServer() {
        MockBukkit.unmock();
    }

    // A typo in a theme would only blow up halfway through a build on the server, this catches it here
    @ParameterizedTest
    @FieldSource("THEMES")
    void everyBlockIsReal(Theme theme) {
        for (String id : blockIds(ArenaGenerator.generate(42L, 12, theme))) {
            Material material = Material.matchMaterial(id);
            assertTrue(material != null && material.isBlock(), theme.id() + " uses unknown block " + id);
        }
    }

    @ParameterizedTest
    @FieldSource("THEMES")
    void themesKeepTheLayout(Theme theme) {
        BlockBuffer themed = ArenaGenerator.generate(42L, 12, theme);
        BlockBuffer colosseum = ArenaGenerator.generate(42L, 12);
        assertEquals(colosseum.spawns(), themed.spawns());
        assertEquals(colosseum.regions(), themed.regions());
    }

    @Test
    void themesActuallyLookDifferent() {
        Set<String> colosseum = blockIds(ArenaGenerator.generate(42L, 12));
        for (Theme theme : Theme.ALL) {
            if (theme != Theme.COLOSSEUM) {
                assertNotEquals(colosseum, blockIds(ArenaGenerator.generate(42L, 12, theme)), theme.id());
            }
        }
    }

    @Test
    void frozenAndLavaPondsSkipLilyPads() {
        assertFalse(blockIds(ArenaGenerator.generate(42L, 12, Theme.SNOW)).contains("lily_pad"));
        assertFalse(blockIds(ArenaGenerator.generate(42L, 12, Theme.NETHER)).contains("sugar_cane"));
        assertTrue(blockIds(ArenaGenerator.generate(42L, 12, Theme.NETHER)).contains("lava"));
    }

    @Test
    void lookupIgnoresCase() {
        assertEquals(Theme.DESERT, Theme.byId("Desert").orElseThrow());
        assertTrue(Theme.byId("moon").isEmpty());
    }

    @Test
    void snowLayersArePlacedLateButSnowBlocksArent() {
        assertTrue(BlockBuffer.isFragile("snow[layers=1]"));
        assertFalse(BlockBuffer.isFragile("snow_block"));
        assertTrue(BlockBuffer.isFragile("lava"));
    }
}
