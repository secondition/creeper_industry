package com.secondition.creeperindustry.content.energy.signal.runtime;

import com.secondition.creeperindustry.content.energy.signal.*;
import com.secondition.creeperindustry.content.explosion.wave.*;
import com.secondition.creeperindustry.content.explosion.wave.runtime.WaveRuntimeAccess;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.*;

/** Authoritative per-dimension propagation, arrival scheduling and receiver waveform caches. */
public final class SignalRuntime implements AutoCloseable {
    private final com.secondition.creeperindustry.content.production.biosphere
                    .BiosphereStructureManager
            structures =
                    new com.secondition.creeperindustry.content.production.biosphere
                            .BiosphereStructureManager();

    public com.secondition.creeperindustry.content.production.biosphere.BiosphereStructureManager
            structures() {
        return structures;
    }

    private final MachineWaveField machineField = new MachineWaveField();
    private final InMemorySignalReceiverIndex receivers = new InMemorySignalReceiverIndex();
    private final ContinuousSignalSourceRepository sources =
            new InMemoryContinuousSignalSourceRepository();
    private final DuctNetworkManager ducts = new DuctNetworkManager();
    private final Map<BlockPos, ReceiverState> states = new HashMap<>();
    private final Map<UUID, List<SourceChange>> history = new HashMap<>();
    private final Set<BlockPos> activeTargets = new LinkedHashSet<>();
    private final Set<BlockPos> registrations = new LinkedHashSet<>();
    private final Set<BlockPos> topologyChanges = new LinkedHashSet<>();
    private final PriorityQueue<Arrival> arrivals =
            new PriorityQueue<>(
                    Comparator.comparingLong(Arrival::time).thenComparingLong(Arrival::order));
    private final Map<
                    net.minecraft.world.level.ChunkPos, net.minecraft.world.level.chunk.ChunkAccess>
            loadedChunks = new HashMap<>();

    public void queueLoadedChunk(net.minecraft.world.level.chunk.ChunkAccess chunk) {
        loadedChunks.put(chunk.getPos(), chunk);
    }

    public void cancelLoadedChunk(net.minecraft.world.level.ChunkPos pos) {
        loadedChunks.remove(pos);
    }

    private void indexLoadedChunks(ServerLevel level) {
        for (var chunk : List.copyOf(loadedChunks.values())) {
            if (!level.hasChunk(chunk.getPos().x, chunk.getPos().z)) continue;
            loadedChunks.remove(chunk.getPos());
            boolean found = false;
            var sections = chunk.getSections();
            for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
                var section = sections[sectionIndex];
                if (section.hasOnlyAir()
                        || !section.maybeHas(
                                s ->
                                        s.is(
                                                com.secondition.creeperindustry.CIBlocks
                                                        .BLASTPROOF_DUCT
                                                        .get()))) continue;
                int baseY = chunk.getMinBuildHeight() + sectionIndex * 16;
                for (int y = 0; y < 16; y++)
                    for (int z = 0; z < 16; z++)
                        for (int x = 0; x < 16; x++)
                            if (section.getBlockState(x, y, z)
                                    .is(
                                            com.secondition.creeperindustry.CIBlocks.BLASTPROOF_DUCT
                                                    .get())) {
                                ducts.indexDuct(
                                        new BlockPos(
                                                chunk.getPos().getMinBlockX() + x,
                                                baseY + y,
                                                chunk.getPos().getMinBlockZ() + z));
                                found = true;
                            }
            }
            structures.chunkChanged(chunk.getPos().x, chunk.getPos().z);
            if (found) topologyChanges.addAll(receivers.getAll());
        }
    }

    private long sequence;
    private long lastTick = Long.MIN_VALUE;
    private static final long HISTORY_UNITS =
            250000; // > maximum bounded duct route + source radius

    private record SourceChange(long time, SignalSource before, SignalSource after) {}

    private record PathKey(UUID source, String path) {}

    private record Contribution(CompositeWaveform.Term term, int cost, long delay) {}

    private record Arrival(
            long time,
            long order,
            BlockPos target,
            ReceiverState owner,
            PathKey key,
            Contribution value) {}

    public SignalReceiverIndex receiverIndex() {
        return receivers;
    }

    public DuctNetworkManager ductNetworkManager() {
        return ducts;
    }

    public ContinuousSignalSourceRepository continuousSourceRepository() {
        return sources;
    }

    public TransientSignalDispatcher transientDispatcher() {
        return (level, source) -> emitPulse(level, source, true);
    }

    public void registerReceiver(BlockPos pos) {
        receivers.register(pos);
        registrations.add(pos.immutable());
    }

    public void unregisterReceiver(BlockPos pos) {
        receivers.unregister(pos);
        registrations.remove(pos);
        activeTargets.remove(pos);
        states.remove(pos);
        topologyChanges.remove(pos);
        arrivals.removeIf(a -> a.target().equals(pos));
    }

    public void refreshReceiver(Level level, BlockPos pos) {
        registrations.add(pos.immutable());
    }

    public void scheduleTopologyRefresh(Level level, Collection<BlockPos> positions) {
        topologyChanges.addAll(positions);
    }

    public void upsert(Level level, ContinuousSignalSource source) {
        SignalSource before = sources.get(source.id()).orElse(null);
        sources.put(source);
        machineField.change((ServerLevel) level, source.id(), source);
        SourceChange change = new SourceChange(level.getGameTime() * 20, before, source);
        history.computeIfAbsent(source.id(), k -> new ArrayList<>()).add(change);
        scheduleChange(level, change, affectedTargets(level, change));
    }

    public void removeSource(Level level, UUID id) {
        sources.remove(id)
                .ifPresent(
                        before -> {
                            machineField.change((ServerLevel) level, id, null);
                            SourceChange change =
                                    new SourceChange(level.getGameTime() * 20, before, null);
                            history.computeIfAbsent(id, k -> new ArrayList<>()).add(change);
                            scheduleChange(level, change, affectedTargets(level, change));
                        });
    }

    public void emitPulse(Level level, SignalSource source, boolean present) {
        SourceChange change = new SourceChange(source.gameTime() * 20, null, source);
        history.computeIfAbsent(source.id(), k -> new ArrayList<>()).add(change);
        scheduleChange(level, change, affectedTargets(level, change));
        if (present && level instanceof ServerLevel server) {
            WaveRuntimeAccess.get(server)
                    .spawnPulse(
                            server,
                            new PulseWaveEmission(
                                    source.id(),
                                    source.position(),
                                    source.gameTime(),
                                    source.signal().amplitude(),
                                    WavePropagationProfile.DEFAULT,
                                    null,
                                    null,
                                    null));
        }
    }

    private Collection<BlockPos> affectedTargets(Level level, SourceChange change) {
        Set<BlockPos> result = new HashSet<>();
        for (SignalSource source : new SignalSource[] {change.before(), change.after()})
            if (source != null)
                result.addAll(
                        ducts.getPotentialTargets(
                                level,
                                source.position(),
                                Math.abs(source.signal().amplitude()) + 1,
                                receivers));
        return result;
    }

    private Map<String, DuctNetworkManager.Route> routes(
            Level level, SignalSource source, BlockPos pos) {
        if (source == null) return Map.of();
        Map<String, DuctNetworkManager.Route> result = new HashMap<>();
        for (var route :
                ducts.routes(level, source.position(), pos, Math.abs(source.signal().amplitude())))
            result.put(route.id(), route);
        return result;
    }

    private void scheduleChange(Level level, SourceChange change, Collection<BlockPos> targets) {
        for (BlockPos target : targets) {
            ReceiverState state = states.get(target);
            if (state == null || !level.hasChunkAt(target)) continue;
            Map<String, DuctNetworkManager.Route> oldRoutes =
                    routes(level, change.before(), target);
            Map<String, DuctNetworkManager.Route> newRoutes = routes(level, change.after(), target);
            SignalSource identity = change.after() != null ? change.after() : change.before();
            for (var old : oldRoutes.values())
                if (!newRoutes.containsKey(old.id())) {
                    queue(
                            level,
                            change.time() + delay(old),
                            target,
                            state,
                            new PathKey(identity.id(), old.id()),
                            null);
                }
            if (change.after() == null) continue;
            SignalSource source = change.after();
            for (var route : newRoutes.values()) {
                long at = change.time() + delay(route);
                long amplitude =
                        Math.round(
                                Math.copySign(
                                                Math.max(
                                                        0,
                                                        Math.abs(source.signal().amplitude())
                                                                - route.attenuationDistance()),
                                                source.signal().amplitude())
                                        * 1000);
                if (amplitude == 0) continue;
                PathKey key = new PathKey(source.id(), route.id());
                int cost = (int) Math.ceil(route.attenuationDistance());
                if (source instanceof ContinuousSignalSource) {
                    int period = source.signal().periodUnits();
                    int phase =
                            (int)
                                    Math.floorMod(
                                            source.signal().phaseUnits() - delay(route), period);
                    queue(
                            level,
                            at,
                            target,
                            state,
                            key,
                            new Contribution(
                                    new CompositeWaveform.Term(amplitude, period, phase),
                                    cost,
                                    delay(route)));
                } else {
                    for (int step = 0; step < 3; step++)
                        queue(
                                level,
                                at + step * 20,
                                target,
                                state,
                                key,
                                new Contribution(
                                        new CompositeWaveform.Term(amplitude / (1L << step), 0, 0),
                                        cost,
                                        delay(route)));
                    queue(level, at + 60, target, state, key, null);
                }
            }
        }
    }

    private static long delay(DuctNetworkManager.Route route) {
        return SignalTime.travelUnits(
                route.travelDistance(),
                WavePropagationProfile.DEFAULT.propagationSpeedBlocksPerTick());
    }

    private void queue(
            Level level,
            long at,
            BlockPos pos,
            ReceiverState state,
            PathKey key,
            Contribution value) {
        arrivals.add(new Arrival(at, sequence++, pos.immutable(), state, key, value));
        state.pendingTimes.merge(at, 1, Integer::sum);
        if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof SignalReceiver receiver)
            receiver.scheduleSignalChange(state.pendingTimes.firstKey());
    }

    public void tick(ServerLevel level) {
        long now = level.getGameTime() * 20;
        if (lastTick == now) return;
        lastTick = now;
        indexLoadedChunks(level);
        structures.tick(level);
        machineField.tick(level);
        for (BlockPos pos : List.copyOf(registrations)) {
            registrations.remove(pos);
            if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof SignalReceiver))
                continue;
            // Existing receiver refreshes must not replay history or manufacture an edge.
            if (states.containsKey(pos)) continue;
            ReceiverState state = new ReceiverState(now);
            states.put(pos, state);
            activeTargets.add(pos);
            for (List<SourceChange> changes : history.values())
                for (SourceChange change : changes) scheduleChange(level, change, List.of(pos));
            state.initializing = true;
        }
        if (!topologyChanges.isEmpty()) {
            // Re-route only subscribed targets affected by the changed network.
            for (BlockPos pos : List.copyOf(topologyChanges)) {
                ReceiverState state = states.get(pos);
                if (state == null) continue;
                for (ContinuousSignalSource source : sources.getActiveSources()) {
                    Map<String, DuctNetworkManager.Route> next = routes(level, source, pos);
                    Set<PathKey> keys = new HashSet<>(state.contributions.keySet());
                    for (Arrival pending : arrivals)
                        if (pending.owner() == state) keys.add(pending.key());
                    for (PathKey key : keys)
                        if (key.source().equals(source.id()) && !next.containsKey(key.path())) {
                            long travel =
                                    state.contributions.containsKey(key)
                                            ? state.contributions.get(key).delay()
                                            : 20;
                            for (Arrival pending : arrivals)
                                if (pending.owner() == state
                                        && pending.key().equals(key)
                                        && pending.value() != null)
                                    travel = Math.max(travel, pending.value().delay());
                            // Already emitted versions finish on their original route; a delayed
                            // cutoff follows them.
                            queue(level, now + travel, pos, state, key, null);
                        }
                    scheduleChange(level, new SourceChange(now, null, source), List.of(pos));
                }
            }
            topologyChanges.clear();
        }
        while (!arrivals.isEmpty() && arrivals.peek().time() <= now) {
            long at = arrivals.peek().time();
            Map<ReceiverState, Map<PathKey, Contribution>> changed = new HashMap<>();
            while (!arrivals.isEmpty() && arrivals.peek().time() == at) {
                Arrival arrival = arrivals.remove();
                ReceiverState state = states.get(arrival.target());
                if (state != arrival.owner()) continue;
                state.pendingTimes.computeIfPresent(at, (t, count) -> count > 1 ? count - 1 : null);
                if (level.hasChunkAt(arrival.target())
                        && level.getBlockEntity(arrival.target())
                                instanceof SignalReceiver receiver)
                    receiver.scheduleSignalChange(
                            state.pendingTimes.isEmpty()
                                    ? Long.MAX_VALUE
                                    : state.pendingTimes.firstKey());
                activeTargets.add(arrival.target());
                if (!changed.containsKey(state)) {
                    state.settle(at);
                    changed.put(state, new HashMap<>(state.contributions));
                }
                if (arrival.value() == null) state.contributions.remove(arrival.key());
                else state.contributions.put(arrival.key(), arrival.value());
            }
            for (var entry : changed.entrySet())
                if (!entry.getValue().equals(entry.getKey().contributions))
                    entry.getKey().rebuild(at);
        }
        for (BlockPos pos : List.copyOf(activeTargets)) {
            ReceiverState state = states.get(pos);
            if (state == null) {
                activeTargets.remove(pos);
                continue;
            }
            if (!level.hasChunkAt(pos)
                    || !(level.getBlockEntity(pos) instanceof SignalReceiver receiver)) {
                unregisterReceiver(pos);
                continue;
            }
            state.settle(now);
            long value = state.wave.valueAt(now);
            if (state.initializing) {
                state.previous = value;
                state.cycles.clear();
                state.initializing = false;
            }
            boolean fast = !state.slowDuringTick && (state.wave.fast() || !state.cycles.isEmpty());
            if (fast) {
                // Changes within one tick can have different peaks. Deliver each completed interval
                // separately.
                for (var completed : state.cycles.entrySet())
                    receiver.receiveSignal(
                            new AggregatedSignal(
                                    level.dimension(),
                                    pos,
                                    level.getGameTime(),
                                    completed.getKey() / 1000.0,
                                    value / 1000.0,
                                    state.contributions.size(),
                                    0,
                                    true,
                                    state.wave.periodUnits() / 20.0,
                                    completed.getValue(),
                                    value != state.previous));
                if (state.cycles.isEmpty())
                    receiver.receiveSignal(
                            new AggregatedSignal(
                                    level.dimension(),
                                    pos,
                                    level.getGameTime(),
                                    state.wave.peak() / 1000.0,
                                    value / 1000.0,
                                    state.contributions.size(),
                                    0,
                                    true,
                                    state.wave.periodUnits() / 20.0,
                                    0,
                                    value != state.previous));
            } else if (!state.contributions.isEmpty() || value != state.previous) {
                receiver.receiveSignal(
                        new AggregatedSignal(
                                level.dimension(),
                                pos,
                                level.getGameTime(),
                                Math.abs(value) / 1000.0,
                                value / 1000.0,
                                state.contributions.size(),
                                0,
                                false,
                                0,
                                0,
                                value != state.previous));
            } else receiver.clearSignal();
            if (state.contributions.isEmpty() || state.wave.isZero()) {
                activeTargets.remove(pos);
                if (value == 0) receiver.clearSignal();
            }
            state.previous = value;
            state.cycles.clear();
            state.slowDuringTick = !state.wave.fast() && !state.contributions.isEmpty();
        }
        if (level.getGameTime() % 200 == 0) {
            history.entrySet()
                    .removeIf(
                            entry -> {
                                List<SourceChange> changes = entry.getValue();
                                int expired = 0;
                                while (expired + 1 < changes.size()
                                        && changes.get(expired + 1).time() < now - HISTORY_UNITS)
                                    expired++;
                                if (expired > 0) changes.subList(0, expired).clear();
                                SourceChange last = changes.getLast();
                                return last.time() < now - HISTORY_UNITS
                                        && !(last.after() instanceof ContinuousSignalSource);
                            });
        }
    }

    private static final class ReceiverState {
        final TreeMap<Long, Integer> pendingTimes = new TreeMap<>();
        final Map<PathKey, Contribution> contributions = new HashMap<>();
        final Map<Long, Integer> cycles = new LinkedHashMap<>();
        CompositeWaveform wave = new CompositeWaveform(List.of());
        long anchor, settled, previous;
        boolean initializing, slowDuringTick;

        ReceiverState(long now) {
            anchor = settled = now;
        }

        void settle(long time) {
            if (time <= settled) return;
            if (wave.fast() && wave.peak() > 0) {
                int period = wave.periodUnits();
                int count =
                        (int)
                                (Math.floorDiv(time - anchor, period)
                                        - Math.floorDiv(settled - anchor, period));
                if (count > 0) cycles.merge(wave.peak(), count, Integer::sum);
            }
            settled = time;
        }

        void rebuild(long time) {
            CompositeWaveform next =
                    new CompositeWaveform(
                            contributions.values().stream().map(Contribution::term).toList());
            wave = next;
            anchor = time;
            if (!next.fast() && !contributions.isEmpty()) slowDuringTick = true;
        }
    }

    @Override
    public void close() {
        loadedChunks.clear();
        machineField.clear();
        structures.close();
        activeTargets.clear();
        receivers.clear();
        sources.clear();
        ducts.clear();
        states.clear();
        history.clear();
        arrivals.clear();
        registrations.clear();
        topologyChanges.clear();
    }
}
