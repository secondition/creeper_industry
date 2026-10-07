package com.secondition.creeperindustry.client.explosion.wave;

import com.secondition.creeperindustry.content.energy.signal.SignalDefinition;
import com.secondition.creeperindustry.content.energy.signal.SignalWaveform;
import com.secondition.creeperindustry.content.explosion.wave.WavePropagationMath;
import com.secondition.creeperindustry.content.explosion.wave.network.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Client reconstruction of the periodic wave's positive peaks. */
final class ClientMachineWaves {
    private static final double MACHINE_FRONT_SPEED = 1;

    private static final Map<UUID, PeriodicWavePacket> versions = new LinkedHashMap<>();
    private static final Map<UUID, Double> lastHeard = new HashMap<>();

    static void accept(PeriodicWavePacket p) {
        if (p.version() == null
                || !Double.isFinite(p.x()) || !Double.isFinite(p.y()) || !Double.isFinite(p.z())
                || Math.abs((long) p.amplitude()) > 4096 || p.amplitude() == 0
                || !SignalDefinition.validFrequency(p.frequencyNumerator(), p.frequencyDenominator())
                || p.wavelength() != (p.frequencyNumerator() == 1 ? p.frequencyDenominator() : 0)
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
                .filter(p -> p.frequencyNumerator() <= p.frequencyDenominator())
                .sorted(Comparator.comparingDouble(p -> player.position()
                        .distanceToSqr(new Vec3(p.x(), p.y(), p.z()))))
                .limit(16)
                .forEach(p -> {
                    int distance = WavePropagationMath.shell(new Vec3(p.x(), p.y(), p.z()),
                            player.getBoundingBox().getCenter());
                    double emissionTime = time - distance / MACHINE_FRONT_SPEED;
                    long cycle = (long) Math.floor(cycle(p, emissionTime));
                    int stride = Math.max(1, (int) Math.ceil(2 * p.frequencyNumerator()
                            / (double) p.frequencyDenominator()));
                    Set<Integer> radii = new HashSet<>();
                    for (long i = -1; i <= 1; i++) {
                        addShell(result, p, cycle + i * stride, time, radii);
                    }
                });
        return result;
    }

    private static void addShell(List<ClientPulseWave> result, PeriodicWavePacket p,
            long cycle, double time, Set<Integer> radii) {
        double born = emissionTime(p, cycle, 0.25);
        int radius = (int) Math.floor((time - born) * MACHINE_FRONT_SPEED);
        double sourceTime = time - radius / MACHINE_FRONT_SPEED;
        if (born < p.start() || born >= p.end() || born > time
                || radius >= Math.abs(p.amplitude())
                || sourceTime < p.start() || sourceTime >= p.end()
                || !radii.add(radius)) return;
        result.add(wave(p, cycle, 0.25, born, SignalWaveform.value(cycle(p, sourceTime))));
    }

    static List<ClientPulseWave> arrivals(long time, LocalPlayer player) {
        expire(time);
        lastHeard.keySet().retainAll(versions.keySet());
        List<ClientPulseWave> result = new ArrayList<>();
        for (PeriodicWavePacket p : versions.values()) {
            if (p.frequencyNumerator() >= p.frequencyDenominator())
                continue;
            Vec3 origin = new Vec3(p.x(), p.y(), p.z());
            int shell = WavePropagationMath.shell(origin, player.getBoundingBox().getCenter());
            if (shell >= Math.abs(p.amplitude())) continue;
            long center = (long) Math.floor(cycle(p, time - shell / MACHINE_FRONT_SPEED));
            for (long index = center - 2; index <= center; index++) {
                for (double phase : new double[] {0.25, 0.75}) {
                    double born = emissionTime(p, index, phase);
                    if (born < p.start() || born >= p.end()
                            || born <= lastHeard.getOrDefault(p.version(), Double.NEGATIVE_INFINITY))
                        continue;
                    ClientPulseWave wave = wave(p, index, phase, born, phase == 0.25 ? 1 : -1);
                    if (wave.canImpactAt(time)
                            && wave.arrivesAt(shell, time)
                            && wave.amplitudeAt(shell) > 0) {
                        lastHeard.put(p.version(), born);
                        result.add(wave);
                    }
                }
            }
        }
        return result;
    }

    private static double cycle(PeriodicWavePacket p, double emissionTime) {
        return p.phase() + (emissionTime - p.anchorTick())
                * p.frequencyNumerator() / (double) p.frequencyDenominator();
    }

    private static double emissionTime(PeriodicWavePacket p, long cycle, double phase) {
        return p.anchorTick() + (cycle + phase - p.phase())
                * p.frequencyDenominator() / (double) p.frequencyNumerator();
    }

    private static ClientPulseWave wave(PeriodicWavePacket p, long cycle, double phase,
            double born, double level) {
        UUID id = new UUID(p.version().getMostSignificantBits() ^ cycle,
                p.version().getLeastSignificantBits() ^ (long) (phase * 4));
        return new ClientPulseWave(new PulseWaveSpawnPacket(
                id, p.x(), p.y(), p.z(), born, MACHINE_FRONT_SPEED,
                WavePropagationMath.maxEffectiveRadius(Math.abs(p.amplitude()), 1),
                p.amplitude(), 1), level);
    }

    private static void expire(double time) {
        versions.values().removeIf(p -> p.end() != Long.MAX_VALUE
                && time > p.end() + Math.abs(p.amplitude()) / MACHINE_FRONT_SPEED + 2);
    }

    static void clear() {
        versions.clear();
        lastHeard.clear();
    }
}
