package studio.chef.lmsmaps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import studio.chef.lmsmaps.gen.CaptureTheFlagGenerator;
import studio.chef.lmsmaps.gen.GameKind;
import studio.chef.lmsmaps.gen.SpleefGenerator;
import studio.chef.lmsmaps.gen.Theme;

// the keys here have to match Nature7's map schemas, that's the whole contract between the two plugins
class MapExporterTest {

    private static YamlConfiguration export(GameKind game) throws InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(MapExporter.mapYaml("Test", game.generate(3L, 8, Theme.COLOSSEUM)));
        return yaml;
    }

    @Test
    void lastStandingWritesSpawns() throws Exception {
        YamlConfiguration yaml = export(GameKind.LAST_STANDING);
        assertEquals(8, yaml.getMapList("game.spawns").size());
        assertTrue(yaml.contains("spectator-spawn.x"));
    }

    @Test
    void spleefBoundsComeFromTheGenerator() throws Exception {
        YamlConfiguration yaml = export(GameKind.SPLEEF);
        assertEquals(64 + SpleefGenerator.generate(3L, 8, Theme.COLOSSEUM).bounds().minY(), yaml.getInt("bounds.min.y"));
        assertEquals(8, yaml.getMapList("game.spawns").size());
    }

    @Test
    void skywarsWritesChestListsAndTheCentre() throws Exception {
        YamlConfiguration yaml = export(GameKind.SKYWARS);
        assertEquals(8, yaml.getMapList("game.island-chests").size());
        assertEquals(4, yaml.getMapList("game.center-chests").size());
        assertEquals(64 + 2, yaml.getDouble("game.center.y"));
    }

    @Test
    void ctfWritesTeamSpawnsAndFlags() throws Exception {
        YamlConfiguration yaml = export(GameKind.CAPTURE_THE_FLAG);
        assertEquals(4, yaml.getMapList("game.red-spawns").size());
        assertEquals(4, yaml.getMapList("game.blue-spawns").size());
        assertEquals(CaptureTheFlagGenerator.FLAG_Z, yaml.getDouble("game.red-flag.z"));
        assertEquals(-CaptureTheFlagGenerator.FLAG_Z, yaml.getDouble("game.blue-flag.z"));
    }
}
