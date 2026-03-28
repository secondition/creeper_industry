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
import com.secondition.creeperindustry.content.energy.transmission.DuctTransmissionHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class DuctNetworkManager {
    private final Map<ResourceKey<Level>, LevelCache> caches = new ConcurrentHashMap<>();

    public void invalidateLevel(ResourceKey<Level> level) {
        caches.remove(level);
    }

    public boolean hasNearbyDuctEntrance(Level level, Vec3 sourcePos, int maxPropagationCost) {
        if (maxPropagationCost <= 0) {
            return false;
        }
        return !findEnterableInterfaces(level, sourcePos, maxPropagationCost).isEmpty();
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
        LevelCache cache = getLevelCache(level.dimension());
        LinkedHashSet<DuctNetwork> affectedNetworks = new LinkedHashSet<>();

        LinkedHashSet<DuctNetwork> adjacentNetworks = new LinkedHashSet<>();
        for (Direction direction : Direction.values()) {
            DuctNetwork neighborNetwork = getCachedOrDiscoveredNetwork(level, placedPos.relative(direction));
            if (neighborNetwork != null) {
                adjacentNetworks.add(neighborNetwork);
            }
        }
        affectedNetworks.addAll(adjacentNetworks);

        LinkedHashSet<BlockPos> mergedPositions = new LinkedHashSet<>();
        mergedPositions.add(placedPos.immutable());
        for (DuctNetwork network : adjacentNetworks) {
            mergedPositions.addAll(network.positions());
            unregisterNetwork(cache, network);
        }

        DuctNetwork mergedNetwork = createNetwork(level, cache, mergedPositions);
        registerNetwork(cache, mergedNetwork);
        affectedNetworks.add(mergedNetwork);

        return collectAffectedReceivers(level, receiverIndex, repository, affectedNetworks);
    }

    public Collection<BlockPos> getAffectedReceiversForInterfaceChange(
            Level level,
            BlockPos ductPos,
            SignalReceiverIndex receiverIndex,
            ContinuousSignalSourceRepository repository
    ) {
        LevelCache cache = getLevelCache(level.dimension());
        LinkedHashSet<DuctNetwork> affectedNetworks = new LinkedHashSet<>();

        DuctNetwork previousNetwork = cache.posToNetwork.get(ductPos);
        if (previousNetwork != null) {
            affectedNetworks.add(previousNetwork);
            unregisterNetwork(cache, previousNetwork);
        }

        DuctNetwork refreshedNetwork = getCachedOrDiscoveredNetwork(level, ductPos);
        if (refreshedNetwork != null) {
            affectedNetworks.add(refreshedNetwork);
        }

        return collectAffectedReceivers(level, receiverIndex, repository, affectedNetworks);
    }

    public Collection<BlockPos> getAffectedReceiversBeforeRemoval(
            Level level,
            BlockPos removedPos,
            SignalReceiverIndex receiverIndex,
            ContinuousSignalSourceRepository repository
    ) {
        DuctNetwork network = getCachedOrDiscoveredNetwork(level, removedPos);
        if (network == null) {
            return List.of();
        }
        return collectAffectedReceivers(level, receiverIndex, repository, Set.of(network));
    }

    public Collection<BlockPos> getAffectedReceiversAfterRemoval(
            Level level,
            BlockPos removedPos,
            SignalReceiverIndex receiverIndex,
            ContinuousSignalSourceRepository repository
    ) {
        LevelCache cache = getLevelCache(level.dimension());
        DuctNetwork removedNetwork = cache.posToNetwork.remove(removedPos);

        LinkedHashSet<DuctNetwork> affectedNetworks = new LinkedHashSet<>();
        if (removedNetwork != null) {
            unregisterNetwork(cache, removedNetwork);
            affectedNetworks.add(removedNetwork);
        }

        Set<BlockPos> visited = new HashSet<>();
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = removedPos.relative(direction);
            if (!isDuct(level, neighbor) || visited.contains(neighbor)) {
                continue;
            }

            Set<BlockPos> component = discoverComponent(level, neighbor, visited);
            if (component.isEmpty()) {
                continue;
            }

            DuctNetwork splitNetwork = createNetwork(level, cache, component);
            registerNetwork(cache, splitNetwork);
            affectedNetworks.add(splitNetwork);
        }

        return collectAffectedReceivers(level, receiverIndex, repository, affectedNetworks);
    }

    private Collection<BlockPos> collectAffectedReceivers(
            Level level,
            SignalReceiverIndex receiverIndex,
            ContinuousSignalSourceRepository repository,
            Collection<DuctNetwork> networks
    ) {
        int maxPropagationCost = repository.getActiveSources(level.dimension()).stream()
                .mapToInt(source -> source.signal().amplitude() - 1)
                .max()
                .orElse(-1);
        if (maxPropagationCost < 0 || networks.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<BlockPos> receivers = new LinkedHashSet<>();
        for (BlockPos receiverPos : receiverIndex.getAll(level.dimension())) {
            for (DuctNetwork network : networks) {
                if (network.canPotentiallyReach(receiverPos, maxPropagationCost)) {
                    receivers.add(receiverPos.immutable());
                    break;
                }
            }
        }
        return List.copyOf(receivers);
    }

    private Set<DuctNetwork> findEnterableNetworks(Level level, Vec3 sourcePos, int maxPropagationCost) {
        LinkedHashSet<DuctNetwork> networks = new LinkedHashSet<>();
        for (DuctInterfaceEndpoint endpoint : findEnterableInterfaces(level, sourcePos, maxPropagationCost)) {
            DuctNetwork network = getCachedOrDiscoveredNetwork(level, endpoint.ductPos());
            if (network != null && network.minCostFrom(sourcePos, maxPropagationCost - 1) != Integer.MAX_VALUE) {
                networks.add(network);
            }
        }
        return networks;
    }

    private Set<DuctInterfaceEndpoint> findEnterableInterfaces(Level level, Vec3 sourcePos, int maxPropagationCost) {
        LinkedHashSet<DuctInterfaceEndpoint> interfaces = new LinkedHashSet<>();
        if (maxPropagationCost <= 0) {
            return interfaces;
        }

        BlockPos center = BlockPos.containing(sourcePos);
        for (BlockPos candidate : BlockPos.withinManhattan(center, maxPropagationCost, maxPropagationCost, maxPropagationCost)) {
            BlockState state = level.getBlockState(candidate);
            if (!(state.getBlock() instanceof com.secondition.creeperindustry.content.energy.transmission.BlastproofDuctBlock)) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                if (!DuctTransmissionHelper.hasInterface(state, direction)) {
                    continue;
                }
                DuctInterfaceEndpoint endpoint = new DuctInterfaceEndpoint(candidate.immutable(), direction);
                int entryCost = computePropagationCost(sourcePos, endpoint.externalPos());
                if (entryCost <= maxPropagationCost) {
                    interfaces.add(endpoint);
                }
            }
        }
        return interfaces;
    }

    private DuctNetwork getCachedOrDiscoveredNetwork(Level level, BlockPos ductPos) {
        if (!isDuct(level, ductPos)) {
            return null;
        }

        LevelCache cache = getLevelCache(level.dimension());
        DuctNetwork network = cache.posToNetwork.get(ductPos);
        if (network != null) {
            return network;
        }

        Set<BlockPos> component = discoverComponent(level, ductPos, new HashSet<>());
        if (component.isEmpty()) {
            return null;
        }

        DuctNetwork discovered = createNetwork(level, cache, component);
        registerNetwork(cache, discovered);
        return discovered;
    }

    private Set<BlockPos> discoverComponent(Level level, BlockPos seed, Set<BlockPos> visited) {
        LinkedHashSet<BlockPos> component = new LinkedHashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        BlockPos immutableSeed = seed.immutable();
        queue.add(immutableSeed);
        visited.add(immutableSeed);

        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            if (!isDuct(level, current)) {
                continue;
            }

            component.add(current);
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.relative(direction).immutable();
                if (visited.add(neighbor)) {
                    queue.addLast(neighbor);
                }
            }
        }

        return component;
    }

    private DuctNetwork createNetwork(Level level, LevelCache cache, Collection<BlockPos> positions) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        LinkedHashSet<DuctInterfaceEndpoint> interfaceEndpoints = new LinkedHashSet<>();
        Set<BlockPos> positionSet = positions instanceof Set<BlockPos> existingSet ? existingSet : new HashSet<>(positions);

        for (BlockPos pos : positions) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
            collectAdjacentInterfaces(level, pos, positionSet, interfaceEndpoints);
        }

        List<BlockPos> immutablePositions = List.copyOf(positions);
        List<DuctInterfaceEndpoint> immutableInterfaceEndpoints = List.copyOf(interfaceEndpoints);
        return new DuctNetwork(immutablePositions, immutableInterfaceEndpoints, minX, minY, minZ, maxX, maxY, maxZ);
    }

    private void registerNetwork(LevelCache cache, DuctNetwork network) {
        for (BlockPos pos : network.positions()) {
            cache.posToNetwork.put(pos, network);
        }
    }

    private void unregisterNetwork(LevelCache cache, DuctNetwork network) {
        for (BlockPos pos : network.positions()) {
            cache.posToNetwork.remove(pos, network);
        }
    }

    private LevelCache getLevelCache(ResourceKey<Level> level) {
        return caches.computeIfAbsent(level, ignored -> new LevelCache());
    }

    private void collectAdjacentInterfaces(Level level, BlockPos ductPos, Set<BlockPos> positionSet, Set<DuctInterfaceEndpoint> interfaceEndpoints) {
        BlockState state = level.getBlockState(ductPos);
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = ductPos.relative(direction);
            if (positionSet.contains(neighbor) || !DuctTransmissionHelper.hasInterface(state, direction)) {
                continue;
            }
            interfaceEndpoints.add(new DuctInterfaceEndpoint(ductPos.immutable(), direction));
        }
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

    private static final class LevelCache {
        private final Map<BlockPos, DuctNetwork> posToNetwork = new HashMap<>();
    }

    private static final class DuctNetwork {
        private final List<BlockPos> positions;
        private final List<DuctInterfaceEndpoint> interfaceEndpoints;
        private final int minX;
        private final int minY;
        private final int minZ;
        private final int maxX;
        private final int maxY;
        private final int maxZ;
        private final int minEntryX;
        private final int minEntryY;
        private final int minEntryZ;
        private final int maxEntryX;
        private final int maxEntryY;
        private final int maxEntryZ;

        private DuctNetwork(List<BlockPos> positions, List<DuctInterfaceEndpoint> interfaceEndpoints, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
            this.positions = positions;
            this.interfaceEndpoints = interfaceEndpoints;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            this.minEntryX = interfaceEndpoints.stream().mapToInt(endpoint -> endpoint.externalPos().getX()).min().orElse(minX);
            this.minEntryY = interfaceEndpoints.stream().mapToInt(endpoint -> endpoint.externalPos().getY()).min().orElse(minY);
            this.minEntryZ = interfaceEndpoints.stream().mapToInt(endpoint -> endpoint.externalPos().getZ()).min().orElse(minZ);
            this.maxEntryX = interfaceEndpoints.stream().mapToInt(endpoint -> endpoint.externalPos().getX()).max().orElse(maxX);
            this.maxEntryY = interfaceEndpoints.stream().mapToInt(endpoint -> endpoint.externalPos().getY()).max().orElse(maxY);
            this.maxEntryZ = interfaceEndpoints.stream().mapToInt(endpoint -> endpoint.externalPos().getZ()).max().orElse(maxZ);
        }

        private List<BlockPos> positions() {
            return positions;
        }

        private boolean canPotentiallyReach(BlockPos targetPos, int maxPropagationCost) {
            return !interfaceEndpoints.isEmpty() && lowerBoundCostTo(targetPos) <= maxPropagationCost;
        }

        private boolean canImprovePath(Vec3 sourcePos, BlockPos targetPos, int currentBest) {
            return lowerBoundCostFrom(sourcePos) <= currentBest
                    && lowerBoundCostTo(targetPos) <= currentBest;
        }

        private int minCostFrom(Vec3 sourcePos, int maxAcceptedCost) {
            int best = Integer.MAX_VALUE;
            if (interfaceEndpoints.isEmpty() || lowerBoundCostFrom(sourcePos) > maxAcceptedCost) {
                return Integer.MAX_VALUE;
            }

            for (DuctInterfaceEndpoint endpoint : interfaceEndpoints) {
                int cost = computePropagationCost(sourcePos, endpoint.externalPos());
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
            if (interfaceEndpoints.isEmpty() || lowerBoundCostTo(targetPos) > maxAcceptedCost) {
                return Integer.MAX_VALUE;
            }

            Vec3 targetCenter = Vec3.atCenterOf(targetPos);
            for (DuctInterfaceEndpoint endpoint : interfaceEndpoints) {
                int cost = computePropagationCost(targetCenter, endpoint.externalPos());
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
                    minEntryX + 0.5D, minEntryY + 0.5D, minEntryZ + 0.5D,
                    maxEntryX + 0.5D, maxEntryY + 0.5D, maxEntryZ + 0.5D
            );
        }

        private int lowerBoundCostTo(BlockPos targetPos) {
            Vec3 center = Vec3.atCenterOf(targetPos);
            return lowerBoundDistance(
                    center.x, center.y, center.z,
                    minEntryX + 0.5D, minEntryY + 0.5D, minEntryZ + 0.5D,
                    maxEntryX + 0.5D, maxEntryY + 0.5D, maxEntryZ + 0.5D
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

    private record DuctInterfaceEndpoint(BlockPos ductPos, Direction face) {
        private BlockPos externalPos() {
            return ductPos.relative(face);
        }
    }
}
