package com.secondition.creeperindustry.client.explosion.wave;

import com.secondition.creeperindustry.content.energy.signal.SignalTime;
import com.secondition.creeperindustry.content.energy.signal.SignalWaveform;
import com.secondition.creeperindustry.content.explosion.wave.WavePropagationMath;
import com.secondition.creeperindustry.content.explosion.wave.network.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Client-only analytic reconstruction. Visual crest density is bounded independently of simulation. */
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
                || (p.period() == 0 ? p.stages() != 0 || p.phase() != 0
                        : p.period() < 0
                                || p.period() > SignalTime.MAX_PERIOD_UNITS
                                || p.stages() < 2
                                || p.stages() > 16
                                || p.stages() % 2 != 0
                                || p.period() % p.stages() != 0
                                || !SignalTime.isStageUnits(p.period() / p.stages())
                                || p.phase() < 0
                                || p.phase() >= p.period())
                || p.end() < p.start()) return;
        Key key = new Key(p.source(), p.start());
        if (!versions.containsKey(key) && versions.size() >= 256)
            versions.remove(versions.keySet().iterator().next());
        versions.put(key, p);
    }

    static List<ClientPulseWave> shells(double time) {
        var player = Minecraft.getInstance().player;
        if (player == null) return List.of();
        expire(time);
        List<ClientPulseWave> result = new ArrayList<>();
        // Only the nearest 16 sources contribute candidates; renderer selects the final eight shells.
        versions.values().stream()
                .sorted(Comparator.comparingDouble(p -> player.position()
                        .distanceToSqr(new Vec3(p.x(), p.y(), p.z()))))
                .limit(16)
                .forEach(p -> {
                    double distance = player.position().distanceTo(new Vec3(p.x(), p.y(), p.z()));
                    if (p.period() > 0) {
                        double period = p.period() / (double) SignalTime.UNITS_PER_TICK;
                        long center = crest(p, time - distance);
                        int stride = Math.max(1, (int) Math.ceil(2.0 / period));
                        if (stride % 2 == 0) stride++;
                        for (int i = -1; i <= 1; i++) {
                            long edge = center + (long) i * stride;
                            double born = crestTime(p, edge);
                            if (born < p.start() || born >= p.end() || born > time
                                    || time - born > Math.abs(p.amplitude())) continue;
                            result.add(wave(p, edge, born, false));
                        }
                    } else if (time >= p.start() && time - p.start() < Math.abs(p.amplitude())
                            && p.end() > p.start()) {
                        result.add(wave(p, 0, p.start(), true));
                    }
                });
        return result;
    }

    static List<ClientPulseWave> arrivals(long time, LocalPlayer player) {
        expire(time);
        lastHeard.keySet().retainAll(versions.keySet());
        List<ClientPulseWave> result = new ArrayList<>();
        for (var entry : versions.entrySet()) {
            PeriodicWavePacket p = entry.getValue();
            if (p.period() > 0 && p.period() <= SignalTime.UNITS_PER_TICK) continue;
            Vec3 origin = new Vec3(p.x(), p.y(), p.z());
            double closest = WavePropagationMath.closestDistanceToAABB(origin, player.getBoundingBox());
            if (closest >= Math.abs(p.amplitude())) continue;
            double farthest = WavePropagationMath.farthestDistanceToAABB(origin, player.getBoundingBox());
            long center = p.period() == 0 ? 0 : crest(p, time - closest);
            for (long edge = p.period() == 0 ? 0 : center - 2; edge <= center; edge++) {
                double born = p.period() == 0 ? p.start() : crestTime(p, edge);
                if (born < p.start() || born >= p.end()
                        || born <= lastHeard.getOrDefault(entry.getKey(), Double.NEGATIVE_INFINITY))
                    continue;
                ClientPulseWave wave = wave(p, edge, born, p.period() == 0);
                if (wave.canImpactAt(time)
                        && wave.radiusAt(time) >= closest
                        && wave.radiusAt(time - 1) <= farthest
                        && wave.amplitudeAt(closest) > 0) {
                    lastHeard.put(entry.getKey(), born);
                    result.add(wave);
                }
            }
        }
        return result;
    }

    private static long crest(PeriodicWavePacket p, double emissionTime) {
        return (long) Math.floor((emissionTime * SignalTime.UNITS_PER_TICK + p.phase())
                / (p.period() / 2));
    }

    private static double crestTime(PeriodicWavePacket p, long crest) {
        return (crest * (double) (p.period() / 2) - p.phase()) / SignalTime.UNITS_PER_TICK;
    }

    private static ClientPulseWave wave(PeriodicWavePacket p, long edge, double born, boolean initial) {
        long value = p.period() == 0 ? p.amplitude() : SignalWaveform.TRIANGLE.sample(
                p.amplitude(), p.stages(), p.period() / p.stages(), p.phase(),
                Math.round(born * SignalTime.UNITS_PER_TICK));
        UUID id = initial ? p.source() : new UUID(
                p.source().getMostSignificantBits() ^ edge,
                p.source().getLeastSignificantBits() ^ p.start());
        return new ClientPulseWave(new PulseWaveSpawnPacket(
                id, p.x(), p.y(), p.z(), born, 1, Math.abs(p.amplitude()), p.amplitude(), 1),
                value / (double) p.amplitude());
    }

    private static void expire(double time) {
        versions.values().removeIf(p -> p.end() != Long.MAX_VALUE
                && time > p.end() + Math.abs(p.amplitude()) + 2);
    }

    static void clear() {
        versions.clear();
        lastHeard.clear();
    }
}
