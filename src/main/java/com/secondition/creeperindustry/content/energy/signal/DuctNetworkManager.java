package com.secondition.creeperindustry.content.energy.signal;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.secondition.creeperindustry.CIBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class DuctNetworkManager {
    private final Map<ResourceKey<Level>, Map<BlockPos, DuctNetwork>> cachedNetworksByLevel = new ConcurrentHashMap<>();

    public void invalidateLevel(ResourceKey<Level> level) {
        cachedNetworksByLevel.remove(level);
    }

    public boolean hasNearbyDuctEntrance(Level level, Vec3 sourcePos, int maxPropagationCost) {
        if (maxPropagationCost <= 0) {
            return false;
        }
        return !findEnterableNetworks(level, sourcePos, maxPropagationCost).isEmpty();
    }

    public int resolvePropagationCost(Level level, Vec3 sourcePos, BlockPos targetPos) {
        int bestCost = computePropagationCost(sourcePos, targetPos);
        for (DuctNetwork network : findEnterableNetworks(level, sourcePos, bestCost - 1)) {
            if (!network.canImprovePath(sourcePos, targetPos, bestCost - 1)) {
                continue;
            }

            int entryCost = network.minCostFrom(sourcePos, bestCost - 1);
            if (entryCost >= bestCost) {
                continue;
            }

            int exitCost = network.minCostTo(targetPos, bestCost - entryCost - 1);
            if (exitCost == Integer.MAX_VALUE) {
                continue;
            }

            int totalCost = entryCost + exitCost;
            if (totalCost < bestCost) {
                bestCost = totalCost;
            }
        }
        return bestCost;
    }

    public Collection<BlockPos> getPotentialTargets(Level level, Vec3 sourcePos, int maxPropagationCost, SignalReceiverIndex receiverIndex) {
        Collection<BlockPos> directTargets = receiverIndex.getWithinManhattanDistance(
                level.dimension(),
                BlockPos.containing(sourcePos),
                maxPropagationCost
        );
        Set<DuctNetwork> networks = findEnterableNetworks(level, sourcePos, maxPropagationCost);
        if (networks.isEmpty()) {
            return directTargets;
        }

        LinkedHashSet<BlockPos> candidates = new LinkedHashSet<>(directTargets);
        for (BlockPos receiverPos : receiverIndex.getAll(level.dimension())) {
            for (DuctNetwork network : networks) {
                if (network.canPotentiallyReach(receiverPos, maxPropagationCost)) {
                    candidates.add(receiverPos.immutable());
                    break;
                }
            }
        }
        return List.copyOf(candidates);
    }

    public Collection<BlockPos> getAffectedReceiversAfterPlacement(
            Level level,
            BlockPos placedPos,
            SignalReceiverIndex receiverIndex,
            ContinuousSignalSourceRepository repository
    ) {
        invalidateLevel(level.dimension());
        Set<DuctNetwork> networks = captureNetworksAround(level, placedPos);
        return collectAffectedReceivers(level, receiverIndex, repository, Set.of(), networks);
    }

    public Collection<BlockPos> getAffectedReceiversBeforeRemoval(
            Level level,
            BlockPos removedPos,
            SignalReceiverIndex receiverIndex,
            ContinuousSignalSourceRepository repository
    ) {
        Set<DuctNetwork> previousNetworks = captureNetworksAround(level, removedPos);
        invalidateLevel(level.dimension());
        return collectAffectedReceivers(level, receiverIndex, repository, previousNetworks, Set.of());
    }

    public Collection<BlockPos> getAffectedReceiversAfterRemoval(
            Level level,
            BlockPos removedPos,
            SignalReceiverIndex receiverIndex,
            ContinuousSignalSourceRepository repository
    ) {
        Set<DuctNetwork> currentNetworks = captureNetworksAround(level, removedPos);
        return collectAffectedReceivers(level, receiverIndex, repository, Set.of(), currentNetworks);
    }

    private Collection<BlockPos> collectAffectedReceivers(
            Level level,
            SignalReceiverIndex receiverIndex,
            ContinuousSignalSourceRepository repository,
            Set<DuctNetwork> previousNetworks,
            Set<DuctNetwork> currentNetworks
    ) {
        int maxPropagationCost = repository.getActiveSources(level.dimension()).stream()
                .mapToInt(source -> source.signal().amplitude() - 1)
                .max()
                .orElse(-1);
        if (maxPropagationCost < 0) {
            return List.of();
        }

        LinkedHashSet<BlockPos> receivers = new LinkedHashSet<>();
        Collection<BlockPos> allReceivers = receiverIndex.getAll(level.dimension());
        for (BlockPos receiverPos : allReceivers) {
            if (isWithinAffectedArea(receiverPos, previousNetworks, maxPropagationCost)
                    || isWithinAffectedArea(receiverPos, currentNetworks, maxPropagationCost)) {
                receivers.add(receiverPos.immutable());
            }
        }
        return List.copyOf(receivers);
    }

    private boolean isWithinAffectedArea(BlockPos receiverPos, Set<DuctNetwork> networks, int maxPropagationCost) {
        for (DuctNetwork network : networks) {
            if (network.canPotentiallyReach(receiverPos, maxPropagationCost)) {
                return true;
            }
        }
        return false;
    }

    private Set<DuctNetwork> captureNetworksAround(Level level, BlockPos centerPos) {
        LinkedHashSet<DuctNetwork> networks = new LinkedHashSet<>();
        maybeAddNetwork(level, centerPos, networks);
        for (Direction direction : Direction.values()) {
            maybeAddNetwork(level, centerPos.relative(direction), networks);
        }
        return networks;
    }

    private void maybeAddNetwork(Level level, BlockPos pos, Set<DuctNetwork> networks) {
        DuctNetwork network = getNetworkAt(level, pos);
        if (network != null) {
            networks.add(network);
        }
    }

    private Set<DuctNetwork> findEnterableNetworks(Level level, Vec3 sourcePos, int maxPropagationCost) {
        LinkedHashSet<DuctNetwork> networks = new LinkedHashSet<>();
        if (maxPropagationCost <= 0) {
            return networks;
        }

        BlockPos center = BlockPos.containing(sourcePos);
        for (BlockPos candidate : BlockPos.withinManhattan(center, maxPropagationCost, maxPropagationCost, maxPropagationCost)) {
            if (!isDuct(level, candidate)) {
                continue;
            }

            DuctNetwork network = getNetworkAt(level, candidate);
            if (network == null) {
                continue;
            }

            if (network.minCostFrom(sourcePos, maxPropagationCost - 1) != Integer.MAX_VALUE) {
                networks.add(network);
            }
        }
        return networks;
    }

    private DuctNetwork getNetworkAt(Level level, BlockPos ductPos) {
        if (!isDuct(level, ductPos)) {
            return null;
        }

        Map<BlockPos, DuctNetwork> levelCache = cachedNetworksByLevel.computeIfAbsent(level.dimension(), ignored -> new ConcurrentHashMap<>());
        DuctNetwork cached = levelCache.get(ductPos);
        if (cached != null) {
            return cached;
        }

        DuctNetwork discovered = discoverNetwork(level, ductPos);
        for (BlockPos pos : discovered.positions()) {
            levelCache.put(pos, discovered);
        }
        return discovered;
    }

    private DuctNetwork discoverNetwork(Level level, BlockPos seed) {
        Set<BlockPos> positions = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(seed.immutable());
        positions.add(seed.immutable());

        int minX = seed.getX();
        int minY = seed.getY();
        int minZ = seed.getZ();
        int maxX = seed.getX();
        int maxY = seed.getY();
        int maxZ = seed.getZ();

        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            minX = Math.min(minX, current.getX());
            minY = Math.min(minY, current.getY());
            minZ = Math.min(minZ, current.getZ());
            maxX = Math.max(maxX, current.getX());
            maxY = Math.max(maxY, current.getY());
            maxZ = Math.max(maxZ, current.getZ());

            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.relative(direction);
                if (!positions.contains(neighbor) && isDuct(level, neighbor)) {
                    BlockPos immutableNeighbor = neighbor.immutable();
                    positions.add(immutableNeighbor);
                    queue.addLast(immutableNeighbor);
                }
            }
        }

        return new DuctNetwork(List.copyOf(positions), minX, minY, minZ, maxX, maxY, maxZ);
    }

    private boolean isDuct(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(CIBlocks.BLASTPROOF_DUCT.get());
    }

    static int computePropagationCost(Vec3 sourcePos, BlockPos targetPos) {
        Vec3 targetCenter = Vec3.atCenterOf(targetPos);
        double manhattanDistance = Math.abs(sourcePos.x - targetCenter.x)
                + Math.abs(sourcePos.y - targetCenter.y)
                + Math.abs(sourcePos.z - targetCenter.z);
        return (int) Math.ceil(manhattanDistance);
    }

    private static final class DuctNetwork {
        private final List<BlockPos> positions;
        private final int minX;
        private final int minY;
        private final int minZ;
        private final int maxX;
        private final int maxY;
        private final int maxZ;

        private DuctNetwork(List<BlockPos> positions, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            this.positions = positions;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        private List<BlockPos> positions() {
            return positions;
        }

        private boolean canPotentiallyReach(BlockPos targetPos, int maxPropagationCost) {
            return lowerBoundCostTo(targetPos) <= maxPropagationCost;
        }

        private boolean canImprovePath(Vec3 sourcePos, BlockPos targetPos, int currentBest) {
            return lowerBoundCostFrom(sourcePos) <= currentBest
                    && lowerBoundCostTo(targetPos) <= currentBest;
        }

        private int minCostFrom(Vec3 sourcePos, int maxAcceptedCost) {
            int best = Integer.MAX_VALUE;
            if (lowerBoundCostFrom(sourcePos) > maxAcceptedCost) {
                return Integer.MAX_VALUE;
            }

            for (BlockPos pos : positions) {
                int cost = computePropagationCost(sourcePos, pos);
                if (cost < best) {
                    best = cost;
                    if (best == 0) {
                        return 0;
                    }
                }
            }
            return best <= maxAcceptedCost ? best : Integer.MAX_VALUE;
        }

        private int minCostTo(BlockPos targetPos, int maxAcceptedCost) {
            int best = Integer.MAX_VALUE;
            if (lowerBoundCostTo(targetPos) > maxAcceptedCost) {
                return Integer.MAX_VALUE;
            }

            Vec3 targetCenter = Vec3.atCenterOf(targetPos);
            for (BlockPos pos : positions) {
                int cost = computePropagationCost(targetCenter, pos);
                if (cost < best) {
                    best = cost;
                    if (best == 0) {
                        return 0;
                    }
                }
            }
            return best <= maxAcceptedCost ? best : Integer.MAX_VALUE;
        }

        private int lowerBoundCostFrom(Vec3 sourcePos) {
            return lowerBoundDistance(
                    sourcePos.x, sourcePos.y, sourcePos.z,
                    minX + 0.5D, minY + 0.5D, minZ + 0.5D,
                    maxX + 0.5D, maxY + 0.5D, maxZ + 0.5D
            );
        }

        private int lowerBoundCostTo(BlockPos targetPos) {
            Vec3 center = Vec3.atCenterOf(targetPos);
            return lowerBoundDistance(
                    center.x, center.y, center.z,
                    minX + 0.5D, minY + 0.5D, minZ + 0.5D,
                    maxX + 0.5D, maxY + 0.5D, maxZ + 0.5D
            );
        }

        private int lowerBoundDistance(double x, double y, double z, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            double dx = clampDistance(x, minX, maxX);
            double dy = clampDistance(y, minY, maxY);
            double dz = clampDistance(z, minZ, maxZ);
            return (int) Math.ceil(dx + dy + dz);
        }

        private double clampDistance(double value, double min, double max) {
            if (value < min) {
                return min - value;
            }
            if (value > max) {
                return value - max;
            }
            return 0.0D;
        }
    }
}
