package studio.chef.lmsmaps;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.scheduler.BukkitRunnable;
import studio.chef.lmsmaps.gen.BlockBuffer;

import java.util.*;
import java.util.function.Consumer;

public final class BuildTask extends BukkitRunnable {
    private final World world;
    private final int ox, oy, oz, perTick;
    private final BlockBuffer buffer;
    private final Consumer<String> progress;
    private final Runnable onDone;
    private final Map<String, BlockData> dataCache = new HashMap<>();

    // clear phase state
    private final int[] columns; // packed x,z pairs inside the clear radius
    private int colIndex = 0, clearY;
    // place phase state
    private final long[] order;
    private int placeIndex = 0;
    private int phase = 0, lastPct = -1;

    public BuildTask(World world, int ox, int oy, int oz, BlockBuffer buffer, int perTick,
                     Consumer<String> progress, Runnable onDone) {
        this.world = world; this.ox = ox; this.oy = oy; this.oz = oz;
        this.buffer = buffer; this.perTick = perTick; this.progress = progress; this.onDone = onDone;

        int r = buffer.clearRadius();
        List<Integer> cols = new ArrayList<>();
        if (r > 0) for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++)
            if (x * x + z * z <= r * r) { cols.add(x); cols.add(z); }
        columns = cols.stream().mapToInt(Integer::intValue).toArray();
        clearY = buffer.clearMinY();

        List<Long> solid = new ArrayList<>(), fragile = new ArrayList<>();
        for (Map.Entry<Long, String> e : buffer.blocks().entrySet())
            (BlockBuffer.isFragile(e.getValue()) ? fragile : solid).add(e.getKey());
        solid.sort(Comparator.comparingInt(BlockBuffer::ky));
        fragile.sort(Comparator.comparingInt(BlockBuffer::ky));
        order = new long[solid.size() + fragile.size()];
        int i = 0;
        for (long k : solid) order[i++] = k;
        for (long k : fragile) order[i++] = k;
    }

    @Override
    public void run() {
        int budget = perTick;
        if (phase == 0) budget = clearStep(budget);
        if (phase == 1) budget = placeStep(budget);
        if (phase == 2) {
            writeSigns();
            cancel();
            progress.accept("&aDone! " + order.length + " blocks placed.");
            onDone.run();
        }
    }

    private int clearStep(int budget) {
        int total = columns.length / 2;
        while (budget > 0 && colIndex < columns.length) {
            int x = columns[colIndex], z = columns[colIndex + 1];
            if (!buffer.blocks().containsKey(BlockBuffer.key(x, clearY, z))) {
                Block block = world.getBlockAt(ox + x, oy + clearY, oz + z);
                if (!block.isEmpty()) { block.setBlockData(data("air"), false); budget--; }
            }
            budget--; // checking costs a little too
            if (++clearY > buffer.clearMaxY()) { clearY = buffer.clearMinY(); colIndex += 2; }
        }
        report("Clearing", colIndex / 2, Math.max(1, total));
        if (colIndex >= columns.length) { phase = 1; lastPct = -1; }
        return budget;
    }

    private int placeStep(int budget) {
        while (budget-- > 0 && placeIndex < order.length) {
            long k = order[placeIndex++];
            world.getBlockAt(ox + BlockBuffer.kx(k), oy + BlockBuffer.ky(k), oz + BlockBuffer.kz(k))
                    .setBlockData(data(buffer.blocks().get(k)), false);
        }
        report("Building", placeIndex, Math.max(1, order.length));
        if (placeIndex >= order.length) phase = 2;
        return budget;
    }

    private void writeSigns() {
        LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();
        for (BlockBuffer.SignText s : buffer.signs()) {
            BlockState state = world.getBlockAt(ox + s.x(), oy + s.y(), oz + s.z()).getState();
            if (!(state instanceof Sign sign)) continue;
            SignSide side = sign.getSide(Side.FRONT);
            for (int i = 0; i < 4; i++) {
                String line = s.lines()[i] == null ? "" : s.lines()[i];
                side.line(i, legacy.deserialize(line));
            }
            sign.setWaxed(true); // nobody can edit the text
            sign.update(true, false);
        }
    }

    private BlockData data(String state) {
        return dataCache.computeIfAbsent(state, s -> Bukkit.createBlockData(s.contains(":") ? s : "minecraft:" + s));
    }

    private void report(String what, int done, int total) {
        int pct = (int) (done * 100L / total);
        if (pct / 20 != lastPct / 20 || lastPct < 0) {
            lastPct = pct;
            progress.accept("&7" + what + "... &f" + pct + "%");
        }
    }
}
