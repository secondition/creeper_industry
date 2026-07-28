package com.secondition.creeperindustry.content.explosion.wave;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ActivePulseWave {
    private final PulseWaveEmission emission;
    private long lastProcessedGameTime;
    private final Set<UUID> hitEntityIds;

    public ActivePulseWave(PulseWaveEmission emission) {
        this.emission = emission;
        this.lastProcessedGameTime = emission.emissionGameTime();
        this.hitEntityIds = new HashSet<>();
    }

    public PulseWaveEmission emission() {
        return emission;
    }

    public long lastProcessedGameTime() {
        return lastProcessedGameTime;
    }

    public void lastProcessedGameTime(long lastProcessedGameTime) {
        this.lastProcessedGameTime = lastProcessedGameTime;
    }

    public Set<UUID> hitEntityIds() {
        return hitEntityIds;
    }

    public long ageTicks(long gameTime) {
        return Math.max(0, gameTime - emission.emissionGameTime());
    }

    public double currentRadius(long gameTime) {
        return WavePropagationMath.radiusAtAge(emission.profile().propagationSpeedBlocksPerTick(), ageTicks(gameTime));
    }

    public double previousRadius(long gameTime) {
        long age = ageTicks(gameTime);
        if (age <= 0) {
            return 0;
        }
        return WavePropagationMath.radiusAtAge(emission.profile().propagationSpeedBlocksPerTick(), age - 1);
    }

    public boolean isExpired(long gameTime) {
        return currentRadius(gameTime) >= emission.maxEffectiveRadius();
    }

    public void markHit(UUID entityId) {
        hitEntityIds.add(entityId);
    }

    public boolean hasHit(UUID entityId) {
        return hitEntityIds.contains(entityId);
    }
}
