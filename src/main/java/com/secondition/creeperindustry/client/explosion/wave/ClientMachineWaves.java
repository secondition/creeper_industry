package com.secondition.creeperindustry.client.explosion.wave;

import com.secondition.creeperindustry.content.explosion.wave.WavePropagationMath;
import com.secondition.creeperindustry.content.explosion.wave.network.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Client-only analytic reconstruction. Visual crest density is bounded independently of simulation.
 */
final class ClientMachineWaves {
    private record Key(UUID source, long start) {}

    private static final Map<Key, PeriodicWavePacket> versions = new LinkedHashMap<>();
    private static final Map<Key, Double> lastHeard = new HashMap<>();

    static void accept(PeriodicWavePacket p) {
        if (!Double.isFinite(p.x())
                || !Double.isFinite(p.y())
                || !Double.isFinite(p.z())
                || Math.abs((long) p.amplitude()) > 4096
                || p.amplitude() == 0
                || p.period() < 2
                || p.period() > 2000
                || p.phase() < 0
                || p.phase() >= p.period()
                || p.end() < p.start()) return;
        Key key = new Key(p.source(), p.start());
        if (!versions.containsKey(key) && versions.size() >= 256)
            versions.remove(versions.keySet().iterator().next());
        versions.put(key, p);
    }

    static List<ClientPulseWave> shells(double time) {
        var player = Minecraft.getInstance().player;
        if (player == null) return List.of();
        versions.values()
                .removeIf(
                        p ->
                                p.end() != Long.MAX_VALUE
                                        && time > p.end() + Math.abs(p.amplitude()) + 2);
        List<ClientPulseWave> result = new ArrayList<>();
        // Only the nearest 16 sources contribute candidates; renderer selects the final eight
        // shells.
        versions.values().stream()
                .sorted(
                        Comparator.comparingDouble(
                                p ->
                                        player.position()
                                                .distanceToSqr(new Vec3(p.x(), p.y(), p.z()))))
                .limit(16)
                .forEach(
                        p -> {
                            double half = p.period() / 40.0, phase = p.phase() / 20.0;
                            double distance =
                                    player.position().distanceTo(new Vec3(p.x(), p.y(), p.z()));
                            long center = (long) Math.floor((time - distance + phase) / half);
                            int stride = Math.max(1, (int) Math.ceil(2.0 / half));
                            for (int i = -1; i <= 1; i++) {
                                long edge = center + (long) i * stride;
                                double born = edge * half - phase;
                                if (born < p.start()
                                        || born >= p.end()
                                        || born > time
                                        || time - born > Math.abs(p.amplitude())) continue;
                                result.add(wave(p, edge, born, false));
                            }
                            // Show the initial finite-speed front even before an oscillation
                            // reaches the observer.
                            if (time >= p.start()
                                    && time - p.start() < Math.abs(p.amplitude())
                                    && p.end() > p.start())
                                result.add(
                                        wave(
                                                p,
                                                (long) Math.floor((p.start() + phase) / half),
                                                p.start(),
                                                true));
                        });
        return result;
    }

    static List<ClientPulseWave> arrivals(long time, LocalPlayer player) {
        versions.values()
                .removeIf(
                        p ->
                                p.end() != Long.MAX_VALUE
                                        && time > p.end() + Math.abs(p.amplitude()) + 2);
        lastHeard.keySet().retainAll(versions.keySet());
        List<ClientPulseWave> result = new ArrayList<>();
        for (var entry : versions.entrySet()) {
            PeriodicWavePacket p = entry.getValue();
            if (p.period() <= 20) continue;
            Vec3 origin = new Vec3(p.x(), p.y(), p.z());
            double closest =
                    WavePropagationMath.closestDistanceToAABB(origin, player.getBoundingBox());
            if (closest >= Math.abs(p.amplitude())) continue;
            double farthest =
                    WavePropagationMath.farthestDistanceToAABB(origin, player.getBoundingBox());
            double half = p.period() / 40.0, phase = p.phase() / 20.0;
            long edge = (long) Math.floor((time - closest + phase) / half);
            double born = edge * half - phase;
            boolean initial = born < p.start();
            if (initial) {
                born = p.start();
                edge = (long) Math.floor((born + phase) / half);
            }
            if (born >= p.end()
                    || born <= lastHeard.getOrDefault(entry.getKey(), Double.NEGATIVE_INFINITY))
                continue;
            ClientPulseWave wave = wave(p, edge, born, initial);
            if (wave.canImpactAt(time)
                    && wave.radiusAt(time) >= closest
                    && wave.radiusAt(time - 1) <= farthest
                    && wave.strengthAt(closest) > 0.0F) {
                lastHeard.put(entry.getKey(), born);
                result.add(wave);
            }
        }
        return result;
    }

    private static ClientPulseWave wave(
            PeriodicWavePacket p, long edge, double born, boolean initial) {
        UUID id =
                initial
                        ? p.source()
                        : new UUID(
                                p.source().getMostSignificantBits() ^ edge,
                                p.source().getLeastSignificantBits() ^ p.start());
        return new ClientPulseWave(
                new PulseWaveSpawnPacket(
                        id,
                        p.x(),
                        p.y(),
                        p.z(),
                        born,
                        1,
                        Math.abs(p.amplitude()),
                        (Math.floorMod(edge, 2) == 0 ? 1 : -1) * p.amplitude(),
                        1));
    }

    static void clear() {
        versions.clear();
        lastHeard.clear();
    }
}
