package com.secondition.creeperindustry.client.explosion;

import com.secondition.creeperindustry.content.explosion.ExplosionShockwaveRules;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

final class ClientShockwave {
    private static final float MAX_MERGED_POWER = 16.0F;

    private Vec3 center;
    private float explosionPower;
    private final long visualSeed;
    private final long startedGameTime;
    private float visibility = Float.NaN;
    private Vec3 visibilityCameraPosition;
    private int visibilityChecks;

    ClientShockwave(Vec3 center, float explosionPower, long visualSeed, long startedGameTime) {
        this.center = center;
        this.explosionPower = Mth.clamp(explosionPower, 0.01F, MAX_MERGED_POWER);
        this.visualSeed = visualSeed;
        this.startedGameTime = startedGameTime;
    }

    Vec3 center() {
        return center;
    }

    float explosionPower() {
        return explosionPower;
    }

    long visualSeed() {
        return visualSeed;
    }

    long startedGameTime() {
        return startedGameTime;
    }

    float age(long gameTime, float partialTick) {
        return Math.max(0.0F, gameTime - startedGameTime + partialTick);
    }

    float radius(long gameTime, float partialTick) {
        return age(gameTime, partialTick) * ExplosionShockwaveRules.WAVE_SPEED_BLOCKS_PER_TICK;
    }

    float maxRadius() {
        return ExplosionShockwaveRules.visualRadius(explosionPower);
    }

    boolean isExpired(long gameTime) {
        return gameTime - startedGameTime > ExplosionShockwaveRules.lifetimeTicks(explosionPower);
    }

    boolean canMerge(Vec3 otherCenter, long otherStartedGameTime) {
        return Math.abs(otherStartedGameTime - startedGameTime) <= 1L
                && center.distanceToSqr(otherCenter) <= ShockwaveManager.MERGE_DISTANCE_SQUARED;
    }

    void merge(Vec3 otherCenter, float otherPower) {
        float clampedOtherPower = Mth.clamp(otherPower, 0.01F, MAX_MERGED_POWER);
        float combinedPower = Mth.clamp(
                (float) Math.sqrt(explosionPower * explosionPower + clampedOtherPower * clampedOtherPower),
                explosionPower,
                MAX_MERGED_POWER
        );
        double weight = clampedOtherPower / Math.max(0.001F, explosionPower + clampedOtherPower);
        center = center.lerp(otherCenter, weight);
        explosionPower = combinedPower;
        visibility = Float.NaN;
        visibilityChecks = 0;
    }

    float visibility() {
        return Float.isNaN(visibility) ? 1.0F : visibility;
    }

    boolean needsVisibilityCheck(Vec3 cameraPosition) {
        if (Float.isNaN(visibility)) {
            return true;
        }
        return visibilityChecks < ShockwaveOcclusion.MAX_VISIBILITY_CHECKS
                && visibilityCameraPosition != null
                && visibilityCameraPosition.distanceToSqr(cameraPosition) > ShockwaveOcclusion.RECHECK_DISTANCE_SQUARED;
    }

    void updateVisibility(Vec3 cameraPosition, float visibility) {
        this.visibilityCameraPosition = cameraPosition;
        this.visibility = Mth.clamp(visibility, 0.0F, 1.0F);
        this.visibilityChecks++;
    }

    float impactStrength(Vec3 cameraPosition, long gameTime, float partialTick) {
        double distance = Math.sqrt(center.distanceToSqr(cameraPosition));
        float maxRadius = maxRadius();
        if (distance >= maxRadius) {
            return 0.0F;
        }

        float waveRadius = radius(gameTime, partialTick);
        float offset = waveRadius - (float) distance;
        float envelope;
        if (offset < -ExplosionShockwaveRules.WAVE_THICKNESS_BLOCKS) {
            return 0.0F;
        } else if (offset < 0.0F) {
            envelope = 1.0F + offset / ExplosionShockwaveRules.WAVE_THICKNESS_BLOCKS;
        } else {
            float lingerDistance = ExplosionShockwaveRules.WAVE_SPEED_BLOCKS_PER_TICK
                    * ExplosionShockwaveRules.IMPACT_LINGER_TICKS;
            envelope = 1.0F - offset / lingerDistance;
        }

        envelope = smoothStep(Mth.clamp(envelope, 0.0F, 1.0F));
        float normalizedDistance = (float) distance / maxRadius;
        float distanceFalloff = 1.0F - normalizedDistance;
        distanceFalloff *= distanceFalloff;
        float powerScale = Mth.clamp(explosionPower / 4.0F, 0.35F, 1.5F);
        return Mth.clamp(envelope * distanceFalloff * powerScale * visibility(), 0.0F, 1.0F);
    }

    double priority(Vec3 cameraPosition, long gameTime) {
        double distanceWeight = 1.0 / (1.0 + center.distanceToSqr(cameraPosition));
        double remainingLife = Math.max(0.0, ExplosionShockwaveRules.lifetimeTicks(explosionPower)
                - (gameTime - startedGameTime));
        return explosionPower * distanceWeight * (1.0 + remainingLife);
    }

    private static float smoothStep(float value) {
        return value * value * (3.0F - 2.0F * value);
    }
}
