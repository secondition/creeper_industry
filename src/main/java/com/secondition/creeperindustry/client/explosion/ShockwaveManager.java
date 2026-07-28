package com.secondition.creeperindustry.client.explosion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.secondition.creeperindustry.content.explosion.ExplosionShockwavePayload;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

final class ShockwaveManager {
    static final double MERGE_DISTANCE_SQUARED = 16.0;

    private static final int MAX_ACTIVE_SHOCKWAVES = 16;
    private static final float MIN_VISIBLE_STRENGTH = 0.002F;

    private final List<ClientShockwave> activeShockwaves = new ArrayList<>();
    private ClientLevel activeLevel;

    void receive(ExplosionShockwavePayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null || !isValid(payload)) {
            return;
        }
        ensureLevel(level);

        float maxRadius = com.secondition.creeperindustry.content.explosion.ExplosionShockwaveRules
                .visualRadius(payload.explosionPower());
        if (minecraft.player.position().distanceToSqr(payload.center()) > maxRadius * maxRadius) {
            return;
        }

        long gameTime = level.getGameTime();
        for (ClientShockwave shockwave : activeShockwaves) {
            if (shockwave.canMerge(payload.center(), gameTime)) {
                shockwave.merge(payload.center(), payload.explosionPower());
                return;
            }
        }

        ClientShockwave incoming = new ClientShockwave(
                payload.center(),
                payload.explosionPower(),
                payload.visualSeed(),
                gameTime
        );
        if (activeShockwaves.size() >= MAX_ACTIVE_SHOCKWAVES) {
            Vec3 cameraPosition = minecraft.gameRenderer.getMainCamera().getPosition();
            ClientShockwave weakest = activeShockwaves.stream()
                    .min(Comparator.comparingDouble(effect -> effect.priority(cameraPosition, gameTime)))
                    .orElse(null);
            if (weakest == null || weakest.priority(cameraPosition, gameTime) >= incoming.priority(cameraPosition, gameTime)) {
                return;
            }
            activeShockwaves.remove(weakest);
        }
        activeShockwaves.add(incoming);
    }

    void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            clear();
            return;
        }
        ensureLevel(level);
        long gameTime = level.getGameTime();
        activeShockwaves.removeIf(shockwave -> shockwave.isExpired(gameTime));
    }

    void prepareFrame(Camera camera) {
        if (activeLevel == null) {
            return;
        }
        Vec3 cameraPosition = camera.getPosition();
        for (ClientShockwave shockwave : activeShockwaves) {
            if (shockwave.needsVisibilityCheck(cameraPosition)) {
                shockwave.updateVisibility(
                        cameraPosition,
                        ShockwaveOcclusion.sample(activeLevel, camera, shockwave.center())
                );
            }
        }
    }

    List<ClientShockwave> visibleShockwaves(long gameTime, float partialTick) {
        activeShockwaves.removeIf(shockwave -> shockwave.isExpired(gameTime));
        return activeShockwaves.stream()
                .filter(shockwave -> shockwave.visibility() > 0.0F)
                .filter(shockwave -> shockwave.radius(gameTime, partialTick) <= shockwave.maxRadius())
                .toList();
    }

    Optional<ShockwaveImpact> strongestImpact(Camera camera, long gameTime, float partialTick) {
        Vec3 cameraPosition = camera.getPosition();
        ClientShockwave strongest = null;
        float strongestValue = 0.0F;
        for (ClientShockwave shockwave : activeShockwaves) {
            float strength = shockwave.impactStrength(cameraPosition, gameTime, partialTick);
            if (strength > strongestValue) {
                strongest = shockwave;
                strongestValue = strength;
            }
        }
        if (strongest == null || strongestValue < MIN_VISIBLE_STRENGTH) {
            return Optional.empty();
        }
        float phase = Mth.frac(strongest.age(gameTime, partialTick) * 0.11F
                + (strongest.visualSeed() & 1023L) / 1024.0F);
        return Optional.of(new ShockwaveImpact(strongest.center(), strongestValue, phase));
    }

    void clear() {
        activeShockwaves.clear();
        activeLevel = null;
    }

    private void ensureLevel(ClientLevel level) {
        if (activeLevel != level) {
            activeShockwaves.clear();
            activeLevel = level;
        }
    }

    private static boolean isValid(ExplosionShockwavePayload payload) {
        Vec3 center = payload.center();
        return center != null
                && Double.isFinite(center.x)
                && Double.isFinite(center.y)
                && Double.isFinite(center.z)
                && Float.isFinite(payload.explosionPower())
                && payload.explosionPower() > 0.0F;
    }
}
