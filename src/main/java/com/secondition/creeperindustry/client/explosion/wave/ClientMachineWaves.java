package com.secondition.creeperindustry.client.explosion.wave;

import com.secondition.creeperindustry.content.energy.signal.SignalWaveform;
import com.secondition.creeperindustry.content.explosion.wave.WavePropagationMath;
import com.secondition.creeperindustry.content.explosion.wave.network.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Client-only reconstruction of the sampled wave's strongest positive and negative windows. */
final class ClientMachineWaves {
    private static final Map<UUID, PeriodicWavePacket> versions = new LinkedHashMap<>();
    private static final Map<UUID, Double> lastHeard = new HashMap<>();

    static void accept(PeriodicWavePacket p) {
        if (p.version() == null
                || !Double.isFinite(p.x()) || !Double.isFinite(p.y()) || !Double.isFinite(p.z())
                || Math.abs((long) p.amplitude()) > 4096 || p.amplitude() == 0
                || p.wavelength() < 1 || p.wavelength() > 16
                || p.frequencyNumerator() < 1 || p.frequencyNumerator() > 32767
                || p.frequencyDenominator() < 1 || p.frequencyDenominator() > 32767
                || p.frequencyNumerator() * 320L < p.frequencyDenominator()
                || p.frequencyNumerator() > 8L * p.frequencyDenominator()
                || !Double.isFinite(p.phase()) || p.phase() < 0 || p.phase() >= 1
                || p.end() < p.start()) return;
        if (!versions.containsKey(p.version()) && versions.size() >= 256)
            versions.remove(versions.keySet().iterator().next());
        versions.put(p.version(), p);
    }

    static List<ClientPulseWave> shells(double time) {
        var player = Minecraft.getInstance().player;
        if (player == null) return List.of();
        expire(time);
        List<ClientPulseWave> result = new ArrayList<>();
        versions.values().stream()
                .sorted(Comparator.comparingDouble(p -> player.position()
                        .distanceToSqr(new Vec3(p.x(), p.y(), p.z()))))
                .limit(16)
                .forEach(p -> {
                    int wavelength = p.wavelength();
                    if (wavelength <= 2) return;
                    double speed = speed(p);
                    double distance = player.position().distanceTo(new Vec3(p.x(), p.y(), p.z()));
                    double emissionTime = time - distance / speed;
                    long cycle = (long) Math.floor(cycle(p, emissionTime));
                    int stride = Math.max(1, (int) Math.ceil(2 * p.frequencyNumerator()
                            / (double) p.frequencyDenominator()));
                    for (long i = -1; i <= 1; i++) {
                        long index = cycle + i * stride;
                        addShell(result, p, index, 0, time);
                        addShell(result, p, index, (wavelength - 1) / 2, time);
                    }
                });
        return result;
    }

    private static void addShell(List<ClientPulseWave> result, PeriodicWavePacket p,
            long cycle, int window, double time) {
        double born = emissionTime(p, cycle, window);
        if (born < p.start() || born >= p.end() || born > time
                || time - born > Math.abs(p.amplitude()) / speed(p)) return;
        double level = SignalWaveform.average(p.wavelength(), window);
        if (level != 0) result.add(wave(p, cycle, window, born));
    }

    static List<ClientPulseWave> arrivals(long time, LocalPlayer player) {
        expire(time);
        lastHeard.keySet().retainAll(versions.keySet());
        List<ClientPulseWave> result = new ArrayList<>();
        for (PeriodicWavePacket p : versions.values()) {
            if (p.frequencyNumerator() >= p.frequencyDenominator() || p.wavelength() <= 2)
                continue;
            Vec3 origin = new Vec3(p.x(), p.y(), p.z());
            double closest = WavePropagationMath.closestDistanceToAABB(origin, player.getBoundingBox());
            if (closest >= Math.abs(p.amplitude())) continue;
            double farthest = WavePropagationMath.farthestDistanceToAABB(origin, player.getBoundingBox());
            long center = (long) Math.floor(cycle(p, time - closest / speed(p)));
            for (long index = center - 2; index <= center; index++) {
                for (int window : new int[] {0, (p.wavelength() - 1) / 2}) {
                    double born = emissionTime(p, index, window);
                    if (born < p.start() || born >= p.end()
                            || born <= lastHeard.getOrDefault(p.version(), Double.NEGATIVE_INFINITY))
                        continue;
                    ClientPulseWave wave = wave(p, index, window, born);
                    if (wave.canImpactAt(time)
                            && wave.radiusAt(time) >= closest
                            && wave.radiusAt(time - 1) <= farthest
                            && wave.amplitudeAt(closest) > 0) {
                        lastHeard.put(p.version(), born);
                        result.add(wave);
                    }
                }
            }
        }
        return result;
    }

    private static double speed(PeriodicWavePacket p) {
        return p.wavelength() * p.frequencyNumerator() / (double) p.frequencyDenominator();
    }

    private static double cycle(PeriodicWavePacket p, double emissionTime) {
        return p.phase() + (emissionTime - p.anchorTick())
                * p.frequencyNumerator() / (double) p.frequencyDenominator();
    }

    private static double emissionTime(PeriodicWavePacket p, long cycle, int window) {
        return p.anchorTick() + (cycle + window / (double) p.wavelength() - p.phase())
                * p.frequencyDenominator() / (double) p.frequencyNumerator();
    }

    private static ClientPulseWave wave(PeriodicWavePacket p, long cycle, int window, double born) {
        double level = SignalWaveform.average(p.wavelength(), window);
        UUID id = new UUID(p.version().getMostSignificantBits() ^ cycle,
                p.version().getLeastSignificantBits() ^ window);
        return new ClientPulseWave(new PulseWaveSpawnPacket(
                id, p.x(), p.y(), p.z(), born, speed(p), Math.abs(p.amplitude()),
                p.amplitude(), 1), level);
    }

    private static void expire(double time) {
        versions.values().removeIf(p -> p.end() != Long.MAX_VALUE
                && time > p.end() + Math.abs(p.amplitude()) / speed(p) + 2);
    }

    static void clear() {
        versions.clear();
        lastHeard.clear();
    }
}
