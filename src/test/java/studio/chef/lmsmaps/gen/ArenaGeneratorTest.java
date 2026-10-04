package studio.chef.lmsmaps.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class ArenaGeneratorTest {
    // sha-256 of the colosseum as it generated before themes existed, so old seeds keep giving the same map
    private static final String COLOSSEUM_1234_12 = "e80b55ac96dde8a422f6d921f0180c67d736648160801792072ba9d6a1c6c45d";

    static String fingerprint(BlockBuffer buffer) {
        StringBuilder text = new StringBuilder();
        Map<Long, String> sorted = new TreeMap<>(buffer.blocks());
        sorted.forEach((key, state) -> text.append(key).append('=').append(state).append('\n'));
        buffer.spawns().forEach(spawn -> text.append(spawn).append('\n'));
        buffer.regions().forEach((name, region) -> text.append(name).append(region).append('\n'));
        buffer.meta().forEach((key, value) -> text.append(key).append(value).append('\n'));
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(text.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void colosseumIsUnchanged() {
        assertEquals(COLOSSEUM_1234_12, fingerprint(ArenaGenerator.generate(1234L, 12)));
    }

    @Test
    void sameSeedSameMap() {
        assertEquals(fingerprint(ArenaGenerator.generate(77L, 8)), fingerprint(ArenaGenerator.generate(77L, 8)));
    }

    @Test
    void playerCountIsClamped() {
        assertEquals(2, ArenaGenerator.generate(1L, 0).spawns().size());
        assertEquals(24, ArenaGenerator.generate(1L, 100).spawns().size());
        assertEquals(12, ArenaGenerator.generate(1L, 12).spawns().size());
    }

    @Test
    void spawnsFaceTheCentre() {
        for (BlockBuffer.Spawn spawn : ArenaGenerator.generate(5L, 16).spawns()) {
            double yaw = Math.toRadians(spawn.yaw());
            // Minecraft yaw 0 looks along +z and turns clockwise
            double lookX = -Math.sin(yaw);
            double lookZ = Math.cos(yaw);
            double toCentre = -(spawn.x() * lookX + spawn.z() * lookZ) / Geo.dist(spawn.x(), spawn.z());
            assertTrue(toCentre > 0.99, "spawn at " + spawn.x() + "," + spawn.z() + " looks away from the centre");
        }
    }
}
