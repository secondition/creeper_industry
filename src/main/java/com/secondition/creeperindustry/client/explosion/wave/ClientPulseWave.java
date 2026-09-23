package com.secondition.creeperindustry.client.explosion.wave;

import com.secondition.creeperindustry.content.explosion.wave.WavePropagationMath;
import com.secondition.creeperindustry.content.explosion.wave.network.PulseWaveSpawnPacket;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** A client reconstruction of one server-owned wave, never an authority for impacts. */
final class ClientPulseWave {
    private final PulseWaveSpawnPacket emission;
    private final Vec3 origin;
    private final double level;
    private boolean hitLocalPlayer;

    ClientPulseWave(PulseWaveSpawnPacket emission) {
        this(emission, 1);
    }

    ClientPulseWave(PulseWaveSpawnPacket emission, double level) {
        this.emission = emission;
        this.origin = new Vec3(emission.originX(), emission.originY(), emission.originZ());
        this.level = level;
    }

    static boolean isValid(PulseWaveSpawnPacket packet) {
        return packet.waveId() != null
                && Double.isFinite(packet.originX())
                && Double.isFinite(packet.originY())
                && Double.isFinite(packet.originZ())
                && positiveFinite(packet.speedBlocksPerTick())
                && positiveFinite(packet.maxRadius())
                && positiveFinite(Math.abs(packet.amplitude()))
                && positiveFinite(packet.attenuationPerBlock());
    }

    private static boolean positiveFinite(double value) {
        return Double.isFinite(value) && value > 0.0;
    }

    float polarity() {
        return (float) Math.signum(emission.amplitude() * level);
    }

    UUID id() {
        return emission.waveId();
    }

    Vec3 origin() {
        return origin;
    }

    double radiusAt(double gameTime) {
        return Math.min(
                maxRadius(),
                emission.speedBlocksPerTick()
                        * Math.max(0.0, gameTime - emission.emissionGameTime()));
    }

    double maxRadius() {
        return emission.maxRadius();
    }

    double arrivalTime(double distance) {
        return emission.emissionGameTime() + distance / emission.speedBlocksPerTick();
    }

    boolean isExpired(double gameTime) {
        // Keep the final tick available while the renderer interpolates previous -> current.
        return gameTime > arrivalTime(maxRadius()) + 1.0;
    }

    boolean canImpactAt(long gameTime) {
        return gameTime >= emission.emissionGameTime()
                && gameTime <= Math.ceil(arrivalTime(maxRadius()));
    }

    double amplitudeAt(double distance) {
        return WavePropagationMath.effectiveAmplitude(
                Math.abs(emission.amplitude()), distance, emission.attenuationPerBlock())
                * Math.abs(level);
    }

    float strengthAt(double distance) {
        return (float) Mth.clamp(amplitudeAt(distance) / Math.abs(emission.amplitude()), 0.0, 1.0);
    }

    float shellWidth(double radius) {
        return (float) Mth.clamp(0.22 + radius * 0.02, 0.22, 0.8);
    }

    float visualStrength(double radius) {
        return Math.min(1.0F, strengthAt(radius) * 1.4F)
                * (float) Mth.clamp(radius / 0.6, 0.0, 1.0);
    }

    boolean hitLocalPlayer() {
        return hitLocalPlayer;
    }

    void markLocalPlayerHit() {
        hitLocalPlayer = true;
    }
}
