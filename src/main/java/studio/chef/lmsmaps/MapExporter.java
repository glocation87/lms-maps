package studio.chef.lmsmaps;

import io.papermc.paper.math.Position;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Stream;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import studio.chef.lmsmaps.gen.BlockBuffer;
import studio.chef.lmsmaps.gen.GameKind;
import studio.chef.lmsmaps.gen.Theme;

// Builds an arena into its own void world, then writes it out as a map folder any minigame engine can load
public final class MapExporter {
    private static final int ORIGIN_Y = 64;
    private static final int SPECTATOR_HEIGHT = 24;
    private static final List<String> WORLD_FOLDERS = List.of("region", "entities", "poi");

    private final JavaPlugin plugin;
    private final Executor mainThread;
    private final ExecutorService io = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "LmsMaps-Export"));

    public MapExporter(JavaPlugin plugin) {
        this.plugin = plugin;
        this.mainThread = task -> plugin.getServer().getScheduler().runTask(plugin, task);
    }

    public record Request(String id, GameKind game, int players, long seed, Theme theme, Path target) {
    }

    public void export(Request request, int perTick, Consumer<String> progress, Runnable onFinish) {
        String worldName = "export_" + request.id();
        try {
            deleteTree(plugin.getServer().getLevelDirectory().resolve("dimensions").resolve(plugin.namespace()).resolve(worldName));
        } catch (IOException e) {
            progress.accept("&cCould not clear an old export world: " + e.getMessage());
            onFinish.run();
            return;
        }

        World world = WorldCreator.ofKey(new NamespacedKey(plugin, worldName))
                .generator(new VoidGenerator())
                .forcedSpawnPosition(Position.fine(0.5, ORIGIN_Y + SPECTATOR_HEIGHT, 0.5), 0, 0)
                .createWorld();
        if (world == null) {
            progress.accept("&cCould not create the export world.");
            onFinish.run();
            return;
        }

        BlockBuffer buffer = request.game().generate(request.seed(), request.players(), request.theme());
        String mapYaml = mapYaml(request.theme().displayName() + " " + request.seed(), buffer);
        int radius = buffer.clearRadius();
        // The world is empty void, so skip BuildTask's clearing pass
        buffer.clearCylinder(-1, 0, 0);

        // Keep the chunks loaded while building so they aren't saved and reloaded between ticks
        for (int cx = Math.floorDiv(-radius, 16); cx <= Math.floorDiv(radius, 16); cx++) {
            for (int cz = Math.floorDiv(-radius, 16); cz <= Math.floorDiv(radius, 16); cz++) {
                world.addPluginChunkTicket(cx, cz, plugin);
            }
        }

        new BuildTask(world, 0, ORIGIN_Y, 0, buffer, perTick, progress,
                () -> save(world, mapYaml, request.target(), progress, onFinish)).runTaskTimer(plugin, 1L, 1L);
    }

    public void shutdown() {
        io.shutdown();
        try {
            if (!io.awaitTermination(30, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Map export did not finish in time");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void save(World world, String mapYaml, Path target, Consumer<String> progress, Runnable onFinish) {
        Path worldFolder = world.getWorldPath();
        world.removePluginChunkTickets(plugin);
        if (!plugin.getServer().unloadWorld(world, true)) {
            progress.accept("&cCould not unload the export world, nothing was exported.");
            onFinish.run();
            return;
        }
        progress.accept("&7Writing map files...");
        CompletableFuture.runAsync(() -> writeMap(worldFolder, mapYaml, target), io)
                .whenCompleteAsync((ignored, error) -> {
                    if (error == null) {
                        progress.accept("&aExported to " + target);
                    } else {
                        Throwable cause = error.getCause() == null ? error : error.getCause();
                        progress.accept("&cExport failed: " + cause.getMessage());
                        plugin.getLogger().log(Level.SEVERE, "Export to " + target + " failed", cause);
                    }
                    onFinish.run();
                }, mainThread);
    }

    private static void writeMap(Path worldFolder, String mapYaml, Path target) {
        try {
            Path worldTarget = target.resolve("world");
            Files.createDirectories(worldTarget);
            for (String name : WORLD_FOLDERS) {
                Path folder = worldFolder.resolve(name);
                if (Files.isDirectory(folder)) {
                    copyTree(folder, worldTarget.resolve(name));
                }
            }
            Files.writeString(target.resolve("map.yml"), mapYaml);
            deleteTree(worldFolder);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String mapYaml(String name, BlockBuffer buffer) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("name", name);
        yaml.set("authors", List.of("LmsMaps"));
        yaml.set("spectator-spawn", point(0.5, ORIGIN_Y + SPECTATOR_HEIGHT, 0.5, 0, 90));
        BlockBuffer.Region bounds = buffer.bounds();
        if (bounds == null) {
            int radius = buffer.clearRadius();
            bounds = new BlockBuffer.Region(-radius, buffer.clearMinY(), -radius, radius, buffer.clearMaxY(), radius);
        }
        yaml.set("bounds.min", block(bounds.minX(), ORIGIN_Y + bounds.minY(), bounds.minZ()));
        yaml.set("bounds.max", block(bounds.maxX(), ORIGIN_Y + bounds.maxY(), bounds.maxZ()));
        if (!buffer.spawns().isEmpty()) {
            yaml.set("game.spawns", points(buffer.spawns()));
        }
        for (Map.Entry<String, List<BlockBuffer.Spawn>> marker : buffer.markers().entrySet()) {
            yaml.set("game." + marker.getKey(), points(marker.getValue()));
        }
        for (Map.Entry<String, BlockBuffer.Spawn> single : buffer.points().entrySet()) {
            BlockBuffer.Spawn at = single.getValue();
            yaml.set("game." + single.getKey(), point(at.x(), ORIGIN_Y + at.y(), at.z(), at.yaw(), 0));
        }
        return yaml.saveToString();
    }

    private static List<Map<String, Object>> points(List<BlockBuffer.Spawn> spawns) {
        List<Map<String, Object>> points = new ArrayList<>();
        for (BlockBuffer.Spawn spawn : spawns) {
            points.add(point(spawn.x(), ORIGIN_Y + spawn.y(), spawn.z(), spawn.yaw(), 0));
        }
        return points;
    }

    private static Map<String, Object> point(double x, double y, double z, float yaw, float pitch) {
        Map<String, Object> point = new LinkedHashMap<>();
        point.put("x", x);
        point.put("y", y);
        point.put("z", z);
        point.put("yaw", yaw);
        point.put("pitch", pitch);
        return point;
    }

    private static Map<String, Object> block(int x, int y, int z) {
        Map<String, Object> block = new LinkedHashMap<>();
        block.put("x", x);
        block.put("y", y);
        block.put("z", z);
        return block;
    }

    private static void copyTree(Path source, Path target) throws IOException {
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path path : (Iterable<Path>) paths::iterator) {
                Path destination = target.resolve(source.relativize(path).toString());
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void deleteTree(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        List<Path> paths;
        try (Stream<Path> walk = Files.walk(root)) {
            paths = walk.sorted(Comparator.reverseOrder()).toList();
        }
        for (Path path : paths) {
            Files.delete(path);
        }
    }

    private static final class VoidGenerator extends ChunkGenerator {
    }
}
