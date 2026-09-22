package com.secondition.creeperindustry.client.explosion.wave;

import com.secondition.creeperindustry.content.explosion.wave.network.*;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/**
 * Client-only analytic reconstruction. Visual crest density is bounded independently of simulation.
 */
final class ClientMachineWaves {
    private record Key(UUID source, long start) {}

    private static final Map<Key, PeriodicWavePacket> versions = new LinkedHashMap<>();

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
                                double amplitude =
                                        (Math.floorMod(edge, 2) == 0 ? 1 : -1) * p.amplitude();
                                UUID id =
                                        new UUID(
                                                p.source().getMostSignificantBits() ^ edge,
                                                p.source().getLeastSignificantBits() ^ p.start());
                                result.add(
                                        new ClientPulseWave(
                                                new PulseWaveSpawnPacket(
                                                        id,
                                                        p.x(),
                                                        p.y(),
                                                        p.z(),
                                                        born,
                                                        1,
                                                        Math.abs(p.amplitude()),
                                                        amplitude,
                                                        1)));
                            }
                            // Show the initial finite-speed front even before an oscillation
                            // reaches the observer.
                            if (time >= p.start()
                                    && time - p.start() < Math.abs(p.amplitude())
                                    && p.end() > p.start())
                                result.add(
                                        new ClientPulseWave(
                                                new PulseWaveSpawnPacket(
                                                        p.source(),
                                                        p.x(),
                                                        p.y(),
                                                        p.z(),
                                                        p.start(),
                                                        1,
                                                        Math.abs(p.amplitude()),
                                                        p.amplitude(),
                                                        1)));
                        });
        return result;
    }

    static void clear() {
        versions.clear();
    }
}
