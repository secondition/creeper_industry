package com.secondition.creeperindustry.content.explosion.wave;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

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
        Explosion explosion = emission.originalExplosion();
        double queryRadius = Math.max(currentRadius, 0.25);
        AABB queryBox = new AABB(
                origin.x - queryRadius,
                origin.y - queryRadius,
                origin.z - queryRadius,
                origin.x + queryRadius,
                origin.y + queryRadius,
                origin.z + queryRadius
        );

        for (Entity entity : level.getEntitiesOfClass(Entity.class, queryBox, e -> true)) {
            if (entity.isSpectator() || entity == emission.directSource()) {
                continue;
            }
            if (wave.hasHit(entity.getUUID())) {
                continue;
            }

            AABB entityBox = entity.getBoundingBox();
            double closestDist = WavePropagationMath.closestDistanceToAABB(origin, entityBox);
            double farthestDist = WavePropagationMath.farthestDistanceToAABB(origin, entityBox);

            if (currentRadius < closestDist) {
                continue;
            }
            if (previousRadius > farthestDist) {
                continue;
            }

            wave.markHit(entity.getUUID());
            if (entity.ignoreExplosion(explosion)) {
                continue;
            }

            double effectiveAmplitude = WavePropagationMath.effectiveAmplitude(
                    emission.sourceAmplitude(),
                    closestDist,
                    emission.profile().attenuationPerBlock()
            );
            if (effectiveAmplitude <= 0) {
                continue;
            }

            applyImpact(level, entity, emission, origin, entityBox, effectiveAmplitude);
        }
    }

    private void applyImpact(ServerLevel level, Entity entity, PulseWaveEmission emission, Vec3 origin, AABB entityBox, double effectiveAmplitude) {
        double impulse = Math.max(0.0, effectiveAmplitude - effectProfile.impulseThreshold()) * effectProfile.impulseScale();
        double damage = Math.max(0.0, effectiveAmplitude - effectProfile.damageThreshold()) * effectProfile.damageScale();

        Vec3 center = entityBox.getCenter();
        Vec3 direction = center.subtract(origin);
        double lengthSqr = direction.lengthSqr();
        if (lengthSqr < 1.0E-8) {
            direction = new Vec3(0.0, 1.0, 0.0);
        } else {
            direction = direction.normalize();
        }

        if (impulse > 0.0) {
            double impulseX = direction.x * impulse;
            double impulseY = direction.y * impulse + 0.1;
            double impulseZ = direction.z * impulse;

            if (entity instanceof LivingEntity living) {
                double resistance = living.getAttributeValue(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE);
                impulseX *= (1.0 - resistance);
                impulseY *= (1.0 - resistance);
                impulseZ *= (1.0 - resistance);
            }

            Vec3 knockback = EventHooks.getExplosionKnockback(
                    level,
                    emission.originalExplosion(),
                    entity,
                    new Vec3(impulseX, impulseY, impulseZ)
            );
            entity.push(knockback);
            entity.hurtMarked = true;
        }

        if (damage > 0.0) {
            Entity direct = emission.directSource();
            Entity causing = emission.causingEntity();
            DamageSource source = level.damageSources().explosion(direct, causing);
            entity.hurt(source, (float) damage);
        }

        entity.onExplosionHit(emission.directSource());
    }
}
