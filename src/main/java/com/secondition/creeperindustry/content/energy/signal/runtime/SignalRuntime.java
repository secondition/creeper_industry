package com.secondition.creeperindustry.content.energy.signal.runtime;

import com.secondition.creeperindustry.content.energy.signal.*;
import com.secondition.creeperindustry.content.explosion.wave.*;
import com.secondition.creeperindustry.content.explosion.wave.runtime.WaveRuntimeAccess;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.*;

/** Authoritative per-dimension propagation and discrete receiver events. */
public final class SignalRuntime implements AutoCloseable {
    private final com.secondition.creeperindustry.content.production.biosphere
                    .BiosphereStructureManager
            structures = new com.secondition.creeperindustry.content.production.biosphere
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
    private final Map<UUID, List<Version>> history = new HashMap<>();
    private final Map<UUID, Version> current = new HashMap<>();
    private final Set<BlockPos> registrations = new LinkedHashSet<>();
    private final Set<BlockPos> topologyChanges = new LinkedHashSet<>();
    private final NavigableSet<Arrival> arrivals = new TreeSet<>(
            Comparator.comparingDouble(Arrival::time).thenComparingLong(Arrival::order));
    private final Map<net.minecraft.world.level.ChunkPos, net.minecraft.world.level.chunk.ChunkAccess>
            loadedChunks = new HashMap<>();
    private long sequence;
    private long lastTick = Long.MIN_VALUE;

    private static final class Version {
        final UUID id = UUID.randomUUID();
        final SignalSource source;
        long end = Long.MAX_VALUE;

        Version(SignalSource source) {
            this.source = source;
        }

        double speed() {
            return source instanceof ContinuousSignalSource ? source.signal().speed()
                    : WavePropagationProfile.DEFAULT.propagationSpeedBlocksPerTick();
        }
    }

    private record PathKey(UUID source, UUID version, String path, long epoch) {}
    private record Contribution(CompositeWaveform.Term term, double delay) {}
    private record Arrival(double time, long order, BlockPos target, ReceiverState owner,
            PathKey key, Contribution value, boolean sample) {}

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
                        || !section.maybeHas(s -> s.is(com.secondition.creeperindustry.CIBlocks
                                .BLASTPROOF_DUCT.get()))) continue;
                int baseY = chunk.getMinBuildHeight() + sectionIndex * 16;
                for (int y = 0; y < 16; y++)
                    for (int z = 0; z < 16; z++)
                        for (int x = 0; x < 16; x++)
                            if (section.getBlockState(x, y, z).is(
                                    com.secondition.creeperindustry.CIBlocks.BLASTPROOF_DUCT.get())) {
                                ducts.indexDuct(new BlockPos(
                                        chunk.getPos().getMinBlockX() + x, baseY + y,
                                        chunk.getPos().getMinBlockZ() + z));
                                found = true;
                            }
            }
            structures.chunkChanged(chunk.getPos().x, chunk.getPos().z);
            if (found) topologyChanges.addAll(receivers.getAll());
        }
    }

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
        topologyChanges.remove(pos);
        ReceiverState state = states.remove(pos);
        if (state != null) for (Arrival arrival : state.pending) arrivals.remove(arrival);
    }

    public double readSignal(Level level, BlockPos pos) {
        ReceiverState state = states.get(pos);
        if (state == null) return 0;
        Map<PathKey, Contribution> current = new HashMap<>(state.contributions);
        state.pending.stream()
                .filter(arrival -> !arrival.sample() && arrival.time() <= level.getGameTime() + 1e-7)
                .sorted(Comparator.comparingDouble(Arrival::time).thenComparingLong(Arrival::order))
                .forEach(arrival -> {
                    if (arrival.value() == null) current.remove(arrival.key());
                    else current.put(arrival.key(), arrival.value());
                });
        return new CompositeWaveform(current.values().stream().map(Contribution::term).toList())
                .valueAt(level.getGameTime() + 1e-7) / (double) SignalDefinition.AMPLITUDE_SCALE;
    }

    public void scheduleTopologyRefresh(Level level, Collection<BlockPos> positions) {
        topologyChanges.addAll(positions);
    }

    public void upsert(Level level, ContinuousSignalSource source) {
        sources.put(source);
        machineField.change((ServerLevel) level, source.id(), source);
        endVersion(level, source.id());
        Version version = new Version(source);
        current.put(source.id(), version);
        history.computeIfAbsent(source.id(), k -> new ArrayList<>()).add(version);
        for (BlockPos pos : affectedTargets(level, source)) {
            ReceiverState state = states.get(pos);
            if (state != null) scheduleVersion(level, state, pos, version, false);
        }
    }

    public void removeSource(Level level, UUID id) {
        if (sources.remove(id).isPresent()) {
            machineField.change((ServerLevel) level, id, null);
            endVersion(level, id);
        }
    }

    private void endVersion(Level level, UUID id) {
        Version old = current.remove(id);
        if (old == null) return;
        old.end = level.getGameTime();
        for (var entry : states.entrySet())
            for (var path : entry.getValue().paths.entrySet())
                if (path.getKey().version().equals(old.id))
                    closePath(level, entry.getKey(), entry.getValue(), path.getKey(),
                            level.getGameTime() + path.getValue());
    }

    public void emitPulse(Level level, SignalSource source, boolean present) {
        Version version = new Version(source);
        version.end = source.gameTime() + 3;
        history.computeIfAbsent(source.id(), k -> new ArrayList<>()).add(version);
        for (BlockPos pos : affectedTargets(level, source)) {
            ReceiverState state = states.get(pos);
            if (state != null) scheduleVersion(level, state, pos, version, false);
        }
        if (present && level instanceof ServerLevel server) {
            WaveRuntimeAccess.get(server).spawnPulse(server, new PulseWaveEmission(
                    source.id(), source.position(), source.gameTime(), source.signal().amplitude(),
                    WavePropagationProfile.DEFAULT, null, null, null));
        }
    }

    private Collection<BlockPos> affectedTargets(Level level, SignalSource source) {
        return ducts.getPotentialTargets(level, source.position(),
                Math.abs(source.signal().amplitude()) + 1, receivers);
    }

    private Map<String, DuctNetworkManager.Route> routes(Level level, SignalSource source, BlockPos pos) {
        Map<String, DuctNetworkManager.Route> result = new HashMap<>();
        for (var route : ducts.routes(level, source.position(), pos,
                Math.abs(source.signal().amplitude()))) result.put(route.id(), route);
        return result;
    }

    private static double delay(Version version, DuctNetworkManager.Route route) {
        return route.travelDistance() / version.speed();
    }

    private Contribution contribution(Version version, DuctNetworkManager.Route route, int step) {
        SignalDefinition signal = version.source.signal();
        long amplitude = Math.round(Math.copySign(Math.max(0,
                Math.abs(signal.amplitude()) - route.attenuationDistance()),
                signal.amplitude()) * SignalDefinition.AMPLITUDE_SCALE);
        if (!(version.source instanceof ContinuousSignalSource)) amplitude /= 1L << step;
        return new Contribution(new CompositeWaveform.Term(amplitude, signal, delay(version, route)),
                delay(version, route));
    }

    private void scheduleVersion(Level level, ReceiverState state, BlockPos pos,
            Version version, boolean replay) {
        if (version.end <= version.source.gameTime()) return;
        for (var route : routes(level, version.source, pos).values()) {
            Contribution base = contribution(version, route, 0);
            if (base.term().amplitude() == 0) continue;
            double start = version.source.gameTime() + base.delay();
            double end = version.end == Long.MAX_VALUE ? Double.POSITIVE_INFINITY
                    : version.end + base.delay();
            if (replay && end <= level.getGameTime()) continue;
            PathKey key = new PathKey(version.source.id(), version.id, route.id(), sequence++);
            state.paths.put(key, base.delay());
            if (version.source instanceof ContinuousSignalSource) {
                if (replay && start <= level.getGameTime() && end > level.getGameTime())
                    state.contributions.put(key, base);
                else if (start > level.getGameTime() || !replay)
                    queue(level, start, pos, state, key, base, false);
                if (Double.isFinite(end) && end > level.getGameTime())
                    closePath(level, pos, state, key, end);
            } else {
                for (int step = 0; step < 3; step++) {
                    double at = start + step;
                    if (replay && at <= level.getGameTime() && at + 1 > level.getGameTime())
                        state.contributions.put(key, contribution(version, route, step));
                    else if (at > level.getGameTime() || !replay)
                        queue(level, at, pos, state, key, contribution(version, route, step), false);
                }
                if (end > level.getGameTime()) closePath(level, pos, state, key, end);
            }
        }
    }

    private void closePath(Level level, BlockPos pos, ReceiverState state, PathKey key,
            double at) {
        if (state.closing.add(key)) queue(level, at, pos, state, key, null, false);
    }

    private void queue(Level level, double at, BlockPos pos, ReceiverState state,
            PathKey key, Contribution value, boolean sample) {
        Arrival arrival = new Arrival(at, sequence++, pos.immutable(), state, key, value, sample);
        arrivals.add(arrival);
        state.pending.add(arrival);
        notifyPending(level, pos, state);
    }

    private static void notifyPending(Level level, BlockPos pos, ReceiverState state) {
        if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof SignalReceiver receiver) {
            double next = state.pending.stream().mapToDouble(Arrival::time).min()
                    .orElse(Double.POSITIVE_INFINITY);
            receiver.scheduleSignalChange(next);
        }
    }

    private void nextSample(Level level, BlockPos pos, ReceiverState state, double from) {
        if (state.sample != null) {
            arrivals.remove(state.sample);
            state.pending.remove(state.sample);
            state.sample = null;
        }
        if (state.wave.isZero()) return;
        double next = Math.min(state.wave.nextChange(from + 1e-7), Math.floor(from + 1e-7) + 1);
        Arrival arrival = new Arrival(next, sequence++, pos, state, null, null, true);
        arrivals.add(arrival);
        state.pending.add(arrival);
        state.sample = arrival;
        notifyPending(level, pos, state);
    }

    private void reroute(ServerLevel level, BlockPos pos, ReceiverState state, long now) {
        for (Version version : current.values()) {
            Map<String, DuctNetworkManager.Route> next = routes(level, version.source, pos);
            for (var path : List.copyOf(state.paths.entrySet())) {
                if (!path.getKey().version().equals(version.id) || state.closing.contains(path.getKey()))
                    continue;
                DuctNetworkManager.Route route = next.get(path.getKey().path());
                if (route == null || Double.compare(path.getValue(), delay(version, route)) != 0)
                    closePath(level, pos, state, path.getKey(), now + path.getValue());
                else next.remove(path.getKey().path());
            }
            for (var route : next.values()) {
                Contribution base = contribution(version, route, 0);
                if (base.term().amplitude() == 0) continue;
                PathKey key = new PathKey(version.source.id(), version.id, route.id(), sequence++);
                state.paths.put(key, base.delay());
                queue(level, now + base.delay(), pos, state, key, base, false);
            }
        }
    }

    public void tick(ServerLevel level) {
        long now = level.getGameTime();
        if (lastTick == now) return;
        lastTick = now;
        indexLoadedChunks(level);
        structures.tick(level);
        machineField.tick(level);
        Set<ReceiverState> touched = new HashSet<>();
        for (BlockPos pos : List.copyOf(registrations)) {
            registrations.remove(pos);
            if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof SignalReceiver)
                    || states.containsKey(pos)) continue;
            ReceiverState state = new ReceiverState(pos);
            states.put(pos, state);
            for (List<Version> versions : history.values())
                for (Version version : versions) scheduleVersion(level, state, pos, version, true);
            state.rebuild();
            state.previous = state.wave.valueAt(now + 1e-7);
            state.previousSlow = state.slowWave.valueAt(now + 1e-7);
            nextSample(level, pos, state, now);
            touched.add(state);
        }
        for (BlockPos pos : List.copyOf(topologyChanges)) {
            ReceiverState state = states.get(pos);
            if (state != null && level.hasChunkAt(pos)) reroute(level, pos, state, now);
        }
        topologyChanges.clear();
        while (!arrivals.isEmpty() && arrivals.first().time() <= now + 1e-7) {
            double at = arrivals.first().time();
            Map<ReceiverState, List<Arrival>> batch = new HashMap<>();
            while (!arrivals.isEmpty() && arrivals.first().time() - at < 1e-9) {
                Arrival arrival = arrivals.pollFirst();
                ReceiverState state = states.get(arrival.target());
                if (state != arrival.owner()) continue;
                state.pending.remove(arrival);
                batch.computeIfAbsent(state, k -> new ArrayList<>()).add(arrival);
            }
            for (var entry : batch.entrySet()) {
                ReceiverState state = entry.getKey();
                long before = state.previous;
                long beforeSlow = state.previousSlow;
                boolean changed = false;
                for (Arrival arrival : entry.getValue()) {
                    if (arrival.sample()) {
                        if (state.sample == arrival) state.sample = null;
                        continue;
                    }
                    if (arrival.value() == null) {
                        state.contributions.remove(arrival.key());
                        state.paths.remove(arrival.key());
                        state.closing.remove(arrival.key());
                    } else state.contributions.put(arrival.key(), arrival.value());
                    changed = true;
                }
                if (changed) {
                    long beforeArrival = state.wave.valueAt(at - 1e-7);
                    long beforeSlowArrival = state.slowWave.valueAt(at - 1e-7);
                    recordStep(state.steps, before, beforeArrival, true);
                    recordStep(state.slowSteps, beforeSlow, beforeSlowArrival, true);
                    before = beforeArrival;
                    beforeSlow = beforeSlowArrival;
                    state.rebuild();
                }
                long after = state.wave.valueAt(at + 1e-7);
                long afterSlow = state.slowWave.valueAt(at + 1e-7);
                if (before != after || beforeSlow != afterSlow || changed) touched.add(state);
                recordStep(state.steps, before, after, !changed);
                recordStep(state.slowSteps, beforeSlow, afterSlow, !changed);
                state.previous = after;
                state.previousSlow = afterSlow;
                nextSample(level, state.pos, state, at);
                notifyPending(level, state.pos, state);
            }
        }
        for (ReceiverState state : touched) {
            BlockPos pos = state.pos;
            if (pos == null) continue;
            if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof SignalReceiver receiver)) {
                unregisterReceiver(pos);
                continue;
            }
            boolean slow = receiver.slowOnly();
            long value = slow ? state.previousSlow : state.previous;
            List<AggregatedSignal.Step> steps = slow ? state.slowSteps : state.steps;
            if (value == 0 && steps.isEmpty() && state.contributions.isEmpty()) receiver.clearSignal();
            else receiver.receiveSignal(new AggregatedSignal(level.dimension(), pos, now,
                    Math.abs(value) / (double) SignalDefinition.AMPLITUDE_SCALE,
                    value / (double) SignalDefinition.AMPLITUDE_SCALE,
                    state.contributions.size(), List.copyOf(steps)));
            state.steps.clear();
            state.slowSteps.clear();
        }
        if (now % 200 == 0) history.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(version -> version.end != Long.MAX_VALUE
                    && now > version.end + (4096 + 2.0 * Math.abs(version.source.signal().amplitude()))
                            / version.speed() + 3);
            return entry.getValue().isEmpty();
        });
    }

    private static void recordStep(List<AggregatedSignal.Step> steps,
            long before, long after, boolean continuous) {
        if (before != after) steps.add(new AggregatedSignal.Step(
                before / (double) SignalDefinition.AMPLITUDE_SCALE,
                after / (double) SignalDefinition.AMPLITUDE_SCALE, continuous));
    }

    private static final class ReceiverState {
        final BlockPos pos;

        ReceiverState(BlockPos pos) {
            this.pos = pos;
        }
        final Set<Arrival> pending = new HashSet<>();
        final Map<PathKey, Double> paths = new HashMap<>();
        final Set<PathKey> closing = new HashSet<>();
        final Map<PathKey, Contribution> contributions = new HashMap<>();
        final List<AggregatedSignal.Step> steps = new ArrayList<>();
        final List<AggregatedSignal.Step> slowSteps = new ArrayList<>();
        CompositeWaveform wave = new CompositeWaveform(List.of());
        CompositeWaveform slowWave = wave;
        Arrival sample;
        long previous;
        long previousSlow;

        void rebuild() {
            wave = new CompositeWaveform(contributions.values().stream()
                    .map(Contribution::term).toList());
            slowWave = wave.slow();
        }
    }

    @Override
    public void close() {
        loadedChunks.clear();
        machineField.clear();
        structures.close();
        receivers.clear();
        sources.clear();
        ducts.clear();
        states.clear();
        history.clear();
        current.clear();
        arrivals.clear();
        registrations.clear();
        topologyChanges.clear();
    }
}
