package studio.chef.lmsmaps.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockbukkit.mockbukkit.MockBukkit;

class GeneratorsTest {

    @BeforeAll
    static void startServer() {
        MockBukkit.mock();
    }

    @AfterAll
    static void stopServer() {
        MockBukkit.unmock();
    }

    static List<Arguments> everyGameAndTheme() {
        List<Arguments> cases = new ArrayList<>();
        for (GameKind game : GameKind.values()) {
            for (Theme theme : Theme.ALL) {
                cases.add(Arguments.of(game, theme));
            }
        }
        return cases;
    }

    @ParameterizedTest
    @MethodSource("everyGameAndTheme")
    void everyBlockIsReal(GameKind game, Theme theme) {
        BlockBuffer buffer = game.generate(7L, 8, theme);
        for (String state : buffer.blocks().values()) {
            String id = BlockBuffer.id(state);
            Material material = Material.matchMaterial(id);
            assertTrue(material != null && material.isBlock(), game.id() + "/" + theme.id() + " uses unknown block " + id);
        }
    }

    @ParameterizedTest
    @MethodSource("everyGameAndTheme")
    void everyoneSpawnsOnSomethingSolid(GameKind game, Theme theme) {
        BlockBuffer buffer = game.generate(7L, 8, theme);
        List<BlockBuffer.Spawn> spawns = new ArrayList<>(buffer.spawns());
        buffer.markers().forEach((key, list) -> {
            if (key.endsWith("spawns")) spawns.addAll(list);
        });
        assertTrue(spawns.size() >= 2, game.id() + " has " + spawns.size() + " spawns");
        for (BlockBuffer.Spawn spawn : spawns) {
            int x = (int) Math.floor(spawn.x()), y = (int) spawn.y(), z = (int) Math.floor(spawn.z());
            assertTrue(buffer.has(x, y - 1, z), game.id() + " spawn at " + x + "," + y + "," + z + " is over nothing");
        }
    }

    @Test
    void spleefFloorIsSnowAndThePitIsOutOfBounds() {
        BlockBuffer buffer = SpleefGenerator.generate(1L, 8, Theme.DESERT);
        assertEquals("snow_block", buffer.get(0, 0, 0));
        assertEquals(8, buffer.spawns().size());
        // falling through has to leave the bounds before landing on the pit floor
        assertTrue(buffer.bounds().minY() < 0 && buffer.bounds().minY() > -8);
    }

    @Test
    void skywarsHasAChestPerIslandAndAStockedCentre() {
        BlockBuffer buffer = SkyWarsGenerator.generate(1L, 6, Theme.COLOSSEUM);
        assertEquals(6, buffer.spawns().size());
        assertEquals(6, buffer.markers().get("island-chests").size());
        assertEquals(4, buffer.markers().get("center-chests").size());
        assertNotNull(buffer.points().get("center"));
        for (BlockBuffer.Spawn chest : buffer.markers().get("island-chests")) {
            String state = buffer.get((int) chest.x(), (int) chest.y(), (int) chest.z());
            assertTrue(state.startsWith("chest"), "expected a chest, got " + state);
        }
    }

    @Test
    void ctfBasesMirrorEachOther() {
        BlockBuffer buffer = CaptureTheFlagGenerator.generate(1L, 10, Theme.NETHER);
        BlockBuffer.Spawn red = buffer.points().get("red-flag");
        BlockBuffer.Spawn blue = buffer.points().get("blue-flag");
        assertEquals(red.z(), -blue.z());
        assertEquals(5, buffer.markers().get("red-spawns").size());
        assertEquals(5, buffer.markers().get("blue-spawns").size());
        // the pedestal the banner stands on
        assertTrue(buffer.has(0, 0, (int) red.z()));
    }

    @Test
    void playerCountsAreClamped() {
        assertEquals(16, SpleefGenerator.generate(1L, 99, Theme.SNOW).spawns().size());
        assertEquals(12, SkyWarsGenerator.generate(1L, 99, Theme.SNOW).spawns().size());
        assertEquals(2, SkyWarsGenerator.generate(1L, 0, Theme.SNOW).spawns().size());
    }
}
