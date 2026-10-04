package studio.chef.lmsmaps;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import studio.chef.lmsmaps.gen.ArenaGenerator;
import studio.chef.lmsmaps.gen.BlockBuffer;
import studio.chef.lmsmaps.gen.HubGenerator;
import studio.chef.lmsmaps.gen.Theme;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * /lmsmap hub [seed]              build the lobby hub where you stand
 * /lmsmap arena [players] [seed] [theme]  build the battle arena where you stand
 * /lmsmap tp <hub|arena>          teleport to the saved spawn
 * /lmsmap info                    show what's saved
 */
public final class LmsMapsPlugin extends JavaPlugin implements TabExecutor, Listener {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final Pattern MAP_ID = Pattern.compile("[a-z0-9_]+");
    private boolean building = false;
    private final Map<UUID, Long> padCooldown = new HashMap<>();
    private MapExporter exporter;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        exporter = new MapExporter(this);
        Objects.requireNonNull(getCommand("lmsmap")).setExecutor(this);
        Objects.requireNonNull(getCommand("lmsmap")).setTabCompleter(this);
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        if (exporter != null) {
            exporter.shutdown();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) { msg(sender, "&e/lmsmap <hub|arena|export|tp|info>"); return true; }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "hub" -> {
                if (!(sender instanceof Player p)) { msg(sender, "&cRun this in-game."); return true; }
                long seed = args.length > 1 ? parseLong(args[1], 1234L) : 1234L;
                build(p, "hub", HubGenerator.generate(seed));
            }
            case "arena" -> {
                if (!(sender instanceof Player p)) { msg(sender, "&cRun this in-game."); return true; }
                int players = args.length > 1 ? (int) parseLong(args[1], 12) : getConfig().getInt("arena-players", 12);
                long seed = args.length > 2 ? parseLong(args[2], randomSeed()) : 1234L;
                Theme theme = theme(sender, args.length > 3 ? args[3] : defaultTheme());
                if (theme == null) return true;
                BlockBuffer buffer = ArenaGenerator.generate(seed, players, theme);
                buffer.meta("theme", theme.id());
                build(p, "arena", buffer);
            }
            case "export" -> {
                if (args.length < 2) { msg(sender, "&e/lmsmap export <id> [players] [seed] [theme]"); return true; }
                int players = args.length > 2 ? (int) parseLong(args[2], 12) : getConfig().getInt("arena-players", 12);
                long seed = args.length > 3 ? parseLong(args[3], randomSeed()) : randomSeed();
                Theme theme = theme(sender, args.length > 4 ? args[4] : defaultTheme());
                if (theme == null) return true;
                export(sender, args[1].toLowerCase(Locale.ROOT), players, seed, theme);
            }
            case "tp" -> {
                if (!(sender instanceof Player p) || args.length < 2) { msg(sender, "&e/lmsmap tp <hub|arena>"); return true; }
                Location loc = savedSpawn(args[1].toLowerCase(Locale.ROOT), 0);
                if (loc == null) msg(p, "&cNothing saved for " + args[1] + " yet.");
                else p.teleport(loc);
            }
            case "info" -> {
                FileConfiguration c = getConfig();
                for (String map : List.of("hub", "arena")) {
                    if (!c.contains(map + ".world")) { msg(sender, "&7" + map + ": &cnot built"); continue; }
                    msg(sender, "&7" + map + ": &f" + c.getString(map + ".world") + " @ " + c.getString(map + ".origin")
                            + " &7spawns: &f" + c.getStringList(map + ".spawns").size());
                }
            }
            default -> msg(sender, "&e/lmsmap <hub|arena|export|tp|info>");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return List.of("hub", "arena", "export", "tp", "info");
        if (args.length == 2 && args[0].equalsIgnoreCase("tp")) return List.of("hub", "arena");
        if (args.length == 2 && args[0].equalsIgnoreCase("arena")) return List.of("8", "12", "16", "24");
        if (args.length == 3 && args[0].equalsIgnoreCase("export")) return List.of("8", "12", "16", "24");
        if (args.length == 3 && args[0].equalsIgnoreCase("arena")) return List.of("random");
        if (args.length == 4 && args[0].equalsIgnoreCase("export")) return List.of("random");
        if ((args.length == 4 && args[0].equalsIgnoreCase("arena")) || (args.length == 5 && args[0].equalsIgnoreCase("export"))) {
            List<String> names = new ArrayList<>(themeNames());
            names.add("random");
            return names;
        }
        return List.of();
    }

    private String defaultTheme() {
        return getConfig().getString("default-theme", Theme.COLOSSEUM.id());
    }

    // "random" picks one, anything unknown gets the list of real themes back
    private static Theme theme(CommandSender sender, String name) {
        if (name.equalsIgnoreCase("random")) return Theme.ALL.get(ThreadLocalRandom.current().nextInt(Theme.ALL.size()));
        Optional<Theme> theme = Theme.byId(name);
        if (theme.isEmpty()) msg(sender, "&cUnknown theme '" + name + "', pick one of: " + String.join(", ", themeNames()) + ", random");
        return theme.orElse(null);
    }

    private static List<String> themeNames() {
        return Theme.ALL.stream().map(Theme::id).toList();
    }

    private static long randomSeed() {
        return ThreadLocalRandom.current().nextInt(100_000);
    }

    private void build(Player p, String name, BlockBuffer buffer) {
        if (building) { msg(p, "&cA build is already running."); return; }
        building = true;
        World world = p.getWorld();
        // The floor replaces the block under your feet.
        int ox = p.getLocation().getBlockX(), oy = p.getLocation().getBlockY() - 1, oz = p.getLocation().getBlockZ();
        msg(p, "&eBuilding " + name + " at " + ox + " " + oy + " " + oz + " (" + buffer.blocks().size() + " blocks)...");
        int perTick = getConfig().getInt("blocks-per-tick", 4000);
        new BuildTask(world, ox, oy, oz, buffer, perTick, s -> msg(p, s), () -> {
            save(name, world, ox, oy, oz, buffer);
            building = false;
            Location spawn = savedSpawn(name, 0);
            if (spawn != null) p.teleport(spawn);
        }).runTaskTimer(this, 1L, 1L);
    }

    private void export(CommandSender sender, String id, int players, long seed, Theme theme) {
        if (!MAP_ID.matcher(id).matches()) { msg(sender, "&cMap ids use lowercase letters, digits and underscores."); return; }
        if (building) { msg(sender, "&cA build is already running."); return; }
        Path target = getDataFolder().toPath().getParent()
                .resolve(getConfig().getString("export-folder", "Nature7/maps/last_standing"))
                .resolve(id);
        if (Files.exists(target)) { msg(sender, "&c" + target + " already exists, delete it or pick another id."); return; }
        building = true;
        msg(sender, "&eExporting " + theme.displayName() + " arena '" + id + "' (" + players + " spawns, seed " + seed + ")...");
        exporter.export(new MapExporter.Request(id, players, seed, theme, target), getConfig().getInt("blocks-per-tick", 4000),
                s -> msg(sender, s), () -> building = false);
    }

    // ------------------------------------------------------------ saving
    private void save(String name, World w, int ox, int oy, int oz, BlockBuffer b) {
        FileConfiguration c = getConfig();
        c.set(name, null);
        c.set(name + ".world", w.getName());
        c.set(name + ".origin", ox + "," + oy + "," + oz);
        List<String> spawns = new ArrayList<>();
        for (BlockBuffer.Spawn s : b.spawns())
            spawns.add((ox + s.x()) + "," + (oy + s.y()) + "," + (oz + s.z()) + "," + s.yaw());
        c.set(name + ".spawns", spawns);
        for (Map.Entry<String, BlockBuffer.Region> e : b.regions().entrySet()) {
            BlockBuffer.Region r = e.getValue();
            c.set(name + ".regions." + e.getKey(), (ox + r.minX()) + "," + (oy + r.minY()) + "," + (oz + r.minZ()) + ","
                    + (ox + r.maxX()) + "," + (oy + r.maxY()) + "," + (oz + r.maxZ()));
        }
        for (Map.Entry<String, String> e : b.meta().entrySet()) c.set(name + "." + e.getKey(), e.getValue());
        saveConfig();
    }

    private Location savedSpawn(String name, int index) {
        FileConfiguration c = getConfig();
        List<String> spawns = c.getStringList(name + ".spawns");
        World w = c.getString(name + ".world") == null ? null : Bukkit.getWorld(c.getString(name + ".world"));
        if (w == null || spawns.size() <= index) return null;
        String[] p = spawns.get(index).split(",");
        return new Location(w, Double.parseDouble(p[0]), Double.parseDouble(p[1]), Double.parseDouble(p[2]),
                Float.parseFloat(p[3]), 0f);
    }

    // ------------------------------------------------------------ join pad
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Location to = e.getTo();
        if (to.getBlockX() == e.getFrom().getBlockX() && to.getBlockY() == e.getFrom().getBlockY()
                && to.getBlockZ() == e.getFrom().getBlockZ()) return;
        String pad = getConfig().getString("hub.regions.join_pad");
        String world = getConfig().getString("hub.world");
        if (pad == null || world == null || !to.getWorld().getName().equals(world)) return;
        String[] r = pad.split(",");
        int x = to.getBlockX(), y = to.getBlockY(), z = to.getBlockZ();
        if (x < Integer.parseInt(r[0]) || y < Integer.parseInt(r[1]) || z < Integer.parseInt(r[2])
                || x > Integer.parseInt(r[3]) || y > Integer.parseInt(r[4]) || z > Integer.parseInt(r[5])) return;

        Player p = e.getPlayer();
        long now = System.currentTimeMillis();
        if (now - padCooldown.getOrDefault(p.getUniqueId(), 0L) < 3000) return;
        padCooldown.put(p.getUniqueId(), now);

        String cmd = getConfig().getString("join-pad.command", "");
        if (cmd != null && !cmd.isBlank()) p.performCommand(cmd.replace("%player%", p.getName()));
        String m = getConfig().getString("join-pad.message", "");
        if (m != null && !m.isBlank()) msg(p, m);
    }

    // ------------------------------------------------------------ utils
    private static void msg(CommandSender s, String legacyText) {
        Component c = LEGACY.deserialize("&6[LMS] &r" + legacyText);
        s.sendMessage(c);
    }

    private static long parseLong(String s, long def) {
        try { return Long.parseLong(s); } catch (NumberFormatException e) { return def; }
    }
}
