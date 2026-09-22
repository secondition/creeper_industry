package com.secondition.creeperindustry.content.production.biosphere;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.*;

/**
 * Per-level watcher index: edits invalidate nearby structures, never a global per-tick volume scan.
 */
public final class BiosphereStructureManager {
    private final Set<BlockPos> pending = new LinkedHashSet<>();
    private final Map<BlockPos, Set<BlockPos>> watchers = new HashMap<>();
    private final Map<BlockPos, List<BlockPos>> watched = new HashMap<>();
    private final Map<BlockPos, BlockPos> linked = new HashMap<>();

    public void register(BlockPos controller) {
        pending.add(controller.immutable());
    }

    public void changed(BlockPos pos) {
        pending.addAll(watchers.getOrDefault(pos, Set.of()));
    }

    public void chunkChanged(int x, int z) {
        for (var entry : watched.entrySet())
            if (entry.getValue().stream()
                    .anyMatch(p -> (p.getX() >> 4) == x && (p.getZ() >> 4) == z))
                pending.add(entry.getKey());
    }

    public void unregister(Level level, BlockPos controller) {
        pending.remove(controller);
        unlink(level, controller);
        for (BlockPos pos : watched.getOrDefault(controller, List.of())) {
            Set<BlockPos> set = watchers.get(pos);
            if (set != null) {
                set.remove(controller);
                if (set.isEmpty()) watchers.remove(pos);
            }
        }
        watched.remove(controller);
    }

    private void unlink(Level level, BlockPos controller) {
        BlockPos receiver = linked.remove(controller);
        if (receiver != null
                && level.hasChunkAt(receiver)
                && level.getBlockEntity(receiver) instanceof WaveReceiverBlockEntity be)
            be.unbind(controller);
    }

    public void tick(Level level) {
        for (BlockPos controller : pending)
            if (level.hasChunkAt(controller)
                    && level.getBlockEntity(controller) instanceof BiosphereBlockEntity machine)
                machine.setStructureSize(0);
        int budget = 16;
        for (BlockPos controller : List.copyOf(pending)) {
            if (budget-- <= 0) break;
            pending.remove(controller);
            unlink(level, controller);
            if (!level.hasChunkAt(controller)
                    || !(level.getBlockEntity(controller)
                            instanceof BiosphereBlockEntity machine)) {
                unregister(level, controller);
                continue;
            }
            var state = machine.getBlockState();
            if (!BiosphereBlock.isController(state)) continue;
            if (!watched.containsKey(controller)) {
                List<BlockPos> positions =
                        BiosphereStructure.cells(controller, state, 7).stream()
                                .map(BiosphereStructure.Cell::pos)
                                .toList();
                watched.put(controller, positions);
                for (BlockPos pos : positions)
                    watchers.computeIfAbsent(pos, k -> new HashSet<>()).add(controller);
            }
            int matched = 0;
            for (int size : BiosphereStructure.SIZES) {
                boolean valid = true;
                for (var cell : BiosphereStructure.cells(controller, state, size)) {
                    if (!level.hasChunkAt(cell.pos())) {
                        valid = false;
                        break;
                    }
                    var actual = level.getBlockState(cell.pos());
                    if (cell.state().isAir()
                            ? !actual.isAir()
                            : !actual.is(cell.state().getBlock())) {
                        valid = false;
                        break;
                    }
                }
                if (!valid) continue;
                BlockPos receiver =
                        BiosphereStructure.receiver(
                                controller, state.getValue(BiosphereBlock.FACING), size);
                if (level.getBlockEntity(receiver) instanceof WaveReceiverBlockEntity be
                        && be.bind(controller)) {
                    matched = size;
                    linked.put(controller, receiver);
                    break;
                }
            }
            machine.setStructureSize(matched);
        }
    }

    public void close() {
        pending.clear();
        watchers.clear();
        watched.clear();
        linked.clear();
    }
}
