package com.secondition.creeperindustry.content.energy.signal;

import com.secondition.creeperindustry.CIBlocks;
import com.secondition.creeperindustry.content.energy.transmission.DuctTransmissionHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DuctNetworkManager {
    private final LevelCache cache = new LevelCache();
    private long lastOversizedWarning = Long.MIN_VALUE;

    private record RouteQuery(Vec3 source, BlockPos target, int amplitude) {}

    private final Map<RouteQuery, List<Route>> routeCache = new HashMap<>();
    private final Set<BlockPos> loadedDucts = new HashSet<>();

    public void indexDuct(BlockPos pos) {
        loadedDucts.add(pos.immutable());
        routeCache.clear();
        cache.posToNetwork.clear();
    }

    public boolean unindexChunk(int x, int z) {
        boolean changed = loadedDucts.removeIf(p -> (p.getX() >> 4) == x && (p.getZ() >> 4) == z);
        if (changed) {
            routeCache.clear();
            cache.posToNetwork.clear();
        }
        return changed;
    }

    public record Route(String id, double attenuationDistance, double travelDistance) {}

    /** Direct air plus one shortest viable path per connected network; no recursive re-entry. */
    public List<Route> routes(Level level, Vec3 source, BlockPos target, int amplitude) {
        RouteQuery query = new RouteQuery(source, target.immutable(), amplitude);
        List<Route> cached = routeCache.get(query);
        if (cached != null) return cached;
        java.util.ArrayList<Route> result = new java.util.ArrayList<>();
        double direct = manhattanDistance(source, Vec3.atCenterOf(target));
        if (direct < amplitude) result.add(new Route("air", direct, direct));
        for (DuctNetwork network : findEnterableNetworks(level, source, amplitude)) {
            Route best = null;
            Set<BlockPos> positions = new HashSet<>(network.positions());
            for (DuctInterfaceEndpoint entry : network.interfaceEndpoints) {
                double before = manhattanDistance(source, Vec3.atCenterOf(entry.externalPos()));
                if (before >= amplitude) continue;
                Map<BlockPos, Integer> distances = new HashMap<>();
                ArrayDeque<BlockPos> pending = new ArrayDeque<>();
                distances.put(entry.ductPos(), 0);
                pending.add(entry.ductPos());
                while (!pending.isEmpty()) {
                    BlockPos current = pending.removeFirst();
                    for (Direction direction : Direction.values()) {
                        BlockPos next = current.relative(direction);
                        if (positions.contains(next) && !distances.containsKey(next)) {
                            distances.put(next, distances.get(current) + 1);
                            pending.add(next);
                        }
                    }
                }
                for (DuctInterfaceEndpoint exit : network.interfaceEndpoints) {
                    if (entry.equals(exit)) continue;
                    double after =
                            manhattanDistance(
                                    Vec3.atCenterOf(exit.externalPos()), Vec3.atCenterOf(target));
                    double loss = before + after;
                    if (loss >= amplitude || !distances.containsKey(exit.ductPos())) continue;
                    double length = loss + distances.get(exit.ductPos()) + 2;
                    String id =
                            entry.ductPos().asLong()
                                    + ":"
                                    + entry.face()
                                    + ":"
                                    + exit.ductPos().asLong()
                                    + ":"
                                    + exit.face();
                    Route route = new Route(id, loss, length);
                    if (best == null
                            || route.travelDistance() < best.travelDistance()
                            || (route.travelDistance() == best.travelDistance()
                                    && route.id().compareTo(best.id()) < 0)) best = route;
                }
            }
            if (best != null) result.add(best);
        }
        List<Route> compiled = List.copyOf(result);
        if (routeCache.size() >= 4096) routeCache.clear();
        routeCache.put(query, compiled);
        return compiled;
    }

    public void clear() {
        routeCache.clear();
        cache.posToNetwork.clear();
        loadedDucts.clear();
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

    public Collection<BlockPos> getPotentialTargets(
            Level level,
            Vec3 sourcePos,
            int maxPropagationCost,
            SignalReceiverIndex receiverIndex) {
        Collection<BlockPos> directTargets =
                receiverIndex.getWithinManhattanDistance(
                        BlockPos.containing(sourcePos), maxPropagationCost);
        Set<DuctNetwork> networks = findEnterableNetworks(level, sourcePos, maxPropagationCost);
        if (networks.isEmpty()) {
            return directTargets;
        }

        LinkedHashSet<BlockPos> candidates = new LinkedHashSet<>(directTargets);
        for (BlockPos receiverPos : receiverIndex.getAll()) {
            for (DuctNetwork network : networks) {
                if (network.canPotentiallyReach(receiverPos, maxPropagationCost)) {
                    candidates.add(receiverPos.immutable());
                    break;
                }
            }
        }
        return List.copyOf(candidates);
    }

    public Collection<BlockPos> getAffectedReceiversForChange(
            Level level,
            BlockPos pos,
            BlockState previousState,
            SignalReceiverIndex receiverIndex,
            ContinuousSignalSourceRepository repository) {
        if (isDuct(level, pos)) loadedDucts.add(pos.immutable());
        else loadedDucts.remove(pos);
        routeCache.clear();
        cache.posToNetwork.clear();

        // Removed interfaces remain notification origins even after their network cache expires.
        Set<BlockPos> previousEndpoints = new LinkedHashSet<>();
        Set<BlockPos> seeds = new LinkedHashSet<>();
        seeds.add(pos);
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            seeds.add(neighbor);
            if (previousState.is(CIBlocks.BLASTPROOF_DUCT.get())
                    && DuctTransmissionHelper.hasInterface(previousState, direction))
                previousEndpoints.add(neighbor);
            if (isDuct(level, neighbor)
                    && DuctTransmissionHelper.hasInterface(
                            level.getBlockState(neighbor), direction.getOpposite()))
                previousEndpoints.add(pos);
        }

        Set<DuctNetwork> networks = new LinkedHashSet<>();
        for (BlockPos seed : seeds) {
            if (!isDuct(level, seed)) continue;
            DuctNetwork network = getCachedOrDiscoveredNetwork(level, seed);
            // An oversized component can disable previously valid paths anywhere in the network.
            if (network == null) return List.copyOf(receiverIndex.getAll());
            networks.add(network);
        }
        return collectAffectedReceivers(receiverIndex, repository, networks, previousEndpoints);
    }

    private Collection<BlockPos> collectAffectedReceivers(
            SignalReceiverIndex receiverIndex,
            ContinuousSignalSourceRepository repository,
            Collection<DuctNetwork> networks,
            Collection<BlockPos> previousEndpoints) {
        int maxPropagationCost =
                repository.getActiveSources().stream()
                        .mapToInt(source -> Math.abs(source.signal().amplitude()))
                        .max()
                        .orElse(-1);
        if (maxPropagationCost < 0) {
            return List.of();
        }

        LinkedHashSet<BlockPos> receivers = new LinkedHashSet<>();
        for (BlockPos endpoint : previousEndpoints)
            receivers.addAll(receiverIndex.getWithinManhattanDistance(endpoint, maxPropagationCost));
        for (BlockPos receiverPos : receiverIndex.getAll()) {
            for (DuctNetwork network : networks) {
                if (network.canPotentiallyReach(receiverPos, maxPropagationCost)) {
                    receivers.add(receiverPos.immutable());
                    break;
                }
            }
        }
        return List.copyOf(receivers);
    }

    private Set<DuctNetwork> findEnterableNetworks(
            Level level, Vec3 sourcePos, int maxPropagationCost) {
        LinkedHashSet<DuctNetwork> networks = new LinkedHashSet<>();
        for (DuctInterfaceEndpoint endpoint :
                findEnterableInterfaces(level, sourcePos, maxPropagationCost)) {
            DuctNetwork network = getCachedOrDiscoveredNetwork(level, endpoint.ductPos());
            if (network != null
                    && network.minCostFrom(sourcePos, maxPropagationCost) != Integer.MAX_VALUE) {
                networks.add(network);
            }
        }
        return networks;
    }

    private Set<DuctInterfaceEndpoint> findEnterableInterfaces(
            Level level, Vec3 sourcePos, int maxPropagationCost) {
        LinkedHashSet<DuctInterfaceEndpoint> interfaces = new LinkedHashSet<>();
        if (maxPropagationCost <= 0) {
            return interfaces;
        }

        for (BlockPos candidate : loadedDucts) {
            if (!level.hasChunkAt(candidate)
                    || manhattanDistance(sourcePos, Vec3.atCenterOf(candidate))
                            > maxPropagationCost + 2)
                continue;
            BlockState state = level.getBlockState(candidate);
            if (!(state.getBlock()
                    instanceof
                    com.secondition.creeperindustry.content.energy.transmission
                            .BlastproofDuctBlock)) {
                continue;
            }
            for (Direction direction : Direction.values()) {
                if (!DuctTransmissionHelper.hasInterface(state, direction)) {
                    continue;
                }
                DuctInterfaceEndpoint endpoint =
                        new DuctInterfaceEndpoint(candidate.immutable(), direction);
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
            if (component.size() > 4096) {
                if (lastOversizedWarning == Long.MIN_VALUE
                        || level.getGameTime() - lastOversizedWarning >= 200) {
                    com.secondition.creeperindustry.CreeperIndustry.LOGGER.warn(
                            "Duct network near {} exceeds the 4096 block limit; transmission is"
                                + " disabled",
                            seed);
                    lastOversizedWarning = level.getGameTime();
                }
                return Set.of();
            }
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.relative(direction).immutable();
                if (visited.add(neighbor)) {
                    queue.addLast(neighbor);
                }
            }
        }

        return component;
    }

    private DuctNetwork createNetwork(
            Level level, LevelCache cache, Collection<BlockPos> positions) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        LinkedHashSet<DuctInterfaceEndpoint> interfaceEndpoints = new LinkedHashSet<>();
        Set<BlockPos> positionSet =
                positions instanceof Set<BlockPos> existingSet
                        ? existingSet
                        : new HashSet<>(positions);

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
        return new DuctNetwork(
                immutablePositions,
                immutableInterfaceEndpoints,
                minX,
                minY,
                minZ,
                maxX,
                maxY,
                maxZ);
    }

    private void registerNetwork(LevelCache cache, DuctNetwork network) {
        for (BlockPos pos : network.positions()) {
            cache.posToNetwork.put(pos, network);
        }
    }

    private void collectAdjacentInterfaces(
            Level level,
            BlockPos ductPos,
            Set<BlockPos> positionSet,
            Set<DuctInterfaceEndpoint> interfaceEndpoints) {
        BlockState state = level.getBlockState(ductPos);
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = ductPos.relative(direction);
            if (positionSet.contains(neighbor)
                    || !DuctTransmissionHelper.hasInterface(state, direction)) {
                continue;
            }
            interfaceEndpoints.add(new DuctInterfaceEndpoint(ductPos.immutable(), direction));
        }
    }

    private boolean isDuct(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return false;
        BlockState state = level.getBlockState(pos);
        return state.is(CIBlocks.BLASTPROOF_DUCT.get());
    }

    static int computePropagationCost(Vec3 sourcePos, BlockPos targetPos) {
        return (int) Math.ceil(manhattanDistance(sourcePos, Vec3.atCenterOf(targetPos)));
    }

    static double manhattanDistance(Vec3 a, Vec3 b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y) + Math.abs(a.z - b.z);
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

        private DuctNetwork(
                List<BlockPos> positions,
                List<DuctInterfaceEndpoint> interfaceEndpoints,
                int minX,
                int minY,
                int minZ,
                int maxX,
                int maxY,
                int maxZ) {
            this.positions = positions;
            this.interfaceEndpoints = interfaceEndpoints;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            this.minEntryX =
                    interfaceEndpoints.stream()
                            .mapToInt(endpoint -> endpoint.externalPos().getX())
                            .min()
                            .orElse(minX);
            this.minEntryY =
                    interfaceEndpoints.stream()
                            .mapToInt(endpoint -> endpoint.externalPos().getY())
                            .min()
                            .orElse(minY);
            this.minEntryZ =
                    interfaceEndpoints.stream()
                            .mapToInt(endpoint -> endpoint.externalPos().getZ())
                            .min()
                            .orElse(minZ);
            this.maxEntryX =
                    interfaceEndpoints.stream()
                            .mapToInt(endpoint -> endpoint.externalPos().getX())
                            .max()
                            .orElse(maxX);
            this.maxEntryY =
                    interfaceEndpoints.stream()
                            .mapToInt(endpoint -> endpoint.externalPos().getY())
                            .max()
                            .orElse(maxY);
            this.maxEntryZ =
                    interfaceEndpoints.stream()
                            .mapToInt(endpoint -> endpoint.externalPos().getZ())
                            .max()
                            .orElse(maxZ);
        }

        private List<BlockPos> positions() {
            return positions;
        }

        private boolean canPotentiallyReach(BlockPos targetPos, int maxPropagationCost) {
            return !interfaceEndpoints.isEmpty()
                    && lowerBoundCostTo(targetPos) <= maxPropagationCost;
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
                    sourcePos.x,
                    sourcePos.y,
                    sourcePos.z,
                    minEntryX + 0.5D,
                    minEntryY + 0.5D,
                    minEntryZ + 0.5D,
                    maxEntryX + 0.5D,
                    maxEntryY + 0.5D,
                    maxEntryZ + 0.5D);
        }

        private int lowerBoundCostTo(BlockPos targetPos) {
            Vec3 center = Vec3.atCenterOf(targetPos);
            return lowerBoundDistance(
                    center.x,
                    center.y,
                    center.z,
                    minEntryX + 0.5D,
                    minEntryY + 0.5D,
                    minEntryZ + 0.5D,
                    maxEntryX + 0.5D,
                    maxEntryY + 0.5D,
                    maxEntryZ + 0.5D);
        }

        private int lowerBoundDistance(
                double x,
                double y,
                double z,
                double minX,
                double minY,
                double minZ,
                double maxX,
                double maxY,
                double maxZ) {
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
