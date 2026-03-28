package com.secondition.creeperindustry.content.energy.signal;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.secondition.creeperindustry.CIBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public final class DuctPropagationCostResolver {
    private DuctPropagationCostResolver() {
    }

    public static boolean hasNearbyDuctEntrance(Level level, Vec3 sourcePos, int maxPropagationCost) {
        if (maxPropagationCost <= 0) {
            return false;
        }

        return !findEntranceSeeds(level, sourcePos, maxPropagationCost).isEmpty();
    }

    public static int resolve(Level level, Vec3 sourcePos, BlockPos targetPos) {
        int directCost = computePropagationCost(sourcePos, targetPos);
        Map<BlockPos, Integer> entranceSeeds = findEntranceSeeds(level, sourcePos, directCost);
        if (entranceSeeds.isEmpty()) {
            return directCost;
        }

        int bestCost = directCost;
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();

        for (BlockPos seed : entranceSeeds.keySet()) {
            if (!visited.add(seed)) {
                continue;
            }

            queue.add(seed);
            int componentEntryCost = entranceSeeds.get(seed);
            int componentExitCost = computePropagationCost(Vec3.atCenterOf(seed), targetPos);

            while (!queue.isEmpty()) {
                BlockPos current = queue.removeFirst();
                Integer seedCost = entranceSeeds.get(current);
                if (seedCost != null && seedCost < componentEntryCost) {
                    componentEntryCost = seedCost;
                }

                int exitCost = computePropagationCost(Vec3.atCenterOf(current), targetPos);
                if (exitCost < componentExitCost) {
                    componentExitCost = exitCost;
                }

                for (Direction direction : Direction.values()) {
                    BlockPos neighbor = current.relative(direction);
                    if (!visited.add(neighbor) || !isDuct(level, neighbor)) {
                        continue;
                    }
                    queue.addLast(neighbor);
                }
            }

            int totalCost = componentEntryCost + componentExitCost;
            if (totalCost < bestCost) {
                bestCost = totalCost;
            }
        }

        return bestCost;
    }

    private static Map<BlockPos, Integer> findEntranceSeeds(Level level, Vec3 sourcePos, int maxPropagationCost) {
        Map<BlockPos, Integer> seeds = new HashMap<>();
        if (maxPropagationCost <= 0) {
            return seeds;
        }

        BlockPos center = BlockPos.containing(sourcePos);
        for (BlockPos candidate : BlockPos.withinManhattan(center, maxPropagationCost, maxPropagationCost, maxPropagationCost)) {
            if (!isDuct(level, candidate)) {
                continue;
            }

            int entryCost = computePropagationCost(sourcePos, candidate);
            if (entryCost >= maxPropagationCost) {
                continue;
            }

            seeds.put(candidate.immutable(), entryCost);
        }
        return seeds;
    }

    private static boolean isDuct(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(CIBlocks.BLASTPROOF_DUCT.get());
    }

    private static int computePropagationCost(Vec3 sourcePos, BlockPos targetPos) {
        Vec3 targetCenter = Vec3.atCenterOf(targetPos);
        double manhattanDistance = Math.abs(sourcePos.x - targetCenter.x)
                + Math.abs(sourcePos.y - targetCenter.y)
                + Math.abs(sourcePos.z - targetCenter.z);
        return (int) Math.ceil(manhattanDistance);
    }
}
