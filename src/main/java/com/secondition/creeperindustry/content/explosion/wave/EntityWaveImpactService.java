package com.secondition.creeperindustry.content.explosion.wave;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class EntityWaveImpactService {
    private final WaveEffectProfile effectProfile;

    public EntityWaveImpactService(WaveEffectProfile effectProfile) {
        this.effectProfile = effectProfile;
    }

    public void applyShell(ServerLevel level, ActivePulseWave wave, long gameTime) {
        PulseWaveEmission emission = wave.emission();
        double previousRadius = wave.previousRadius(gameTime);
        double currentRadius = wave.currentRadius(gameTime);
        if (currentRadius < previousRadius) {
            return;
        }

        Vec3 origin = emission.origin();
        AABB queryBox = new AABB(
                origin.x - currentRadius,
                origin.y - currentRadius,
                origin.z - currentRadius,
                origin.x + currentRadius,
                origin.y + currentRadius,
                origin.z + currentRadius
        );

        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, queryBox, LivingEntity::isAlive)) {
            if (wave.hasHit(entity.getUUID())) {
                continue;
            }

            Vec3 samplePoint = entity.getBoundingBox().getCenter();
            double distance = WavePropagationMath.euclideanDistance(origin, samplePoint);
            if (!WavePropagationMath.isShellCrossing(distance, previousRadius, currentRadius)) {
                continue;
            }

            int effectiveAmplitude = WavePropagationMath.effectiveAmplitude(
                    emission.sourceAmplitude(),
                    distance,
                    emission.profile().attenuationPerBlock()
            );
            if (effectiveAmplitude <= 0) {
                wave.markHit(entity.getUUID());
                continue;
            }

            applyImpact(level, entity, origin, samplePoint, effectiveAmplitude);
            wave.markHit(entity.getUUID());
        }
    }

    private void applyImpact(ServerLevel level, LivingEntity entity, Vec3 origin, Vec3 samplePoint, int effectiveAmplitude) {
        double impulse = Math.max(0.0, effectiveAmplitude - effectProfile.impulseThreshold()) * effectProfile.impulseScale();
        double damage = Math.max(0.0, effectiveAmplitude - effectProfile.damageThreshold()) * effectProfile.damageScale();

        Vec3 direction = samplePoint.subtract(origin);
        double lengthSqr = direction.lengthSqr();
        if (lengthSqr < 1.0E-8) {
            direction = new Vec3(0.0, 1.0, 0.0);
        } else {
            direction = direction.normalize();
        }

        if (impulse > 0.0) {
            entity.push(direction.x * impulse, direction.y * impulse + 0.1, direction.z * impulse);
            entity.hurtMarked = true;
        }

        if (damage > 0.0) {
            DamageSource source = level.damageSources().explosion(null, null);
            entity.hurt(source, (float) damage);
        }
    }
}
