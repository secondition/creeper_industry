package com.secondition.creeperindustry.content.energy.signal;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record ExplosionSignalContext(
        ResourceKey<Level> level,
        Vec3 position,
        long gameTime,
        ExplosionSignalKind kind,
        float explosionPower,
        ResourceLocation sourceId
) {
    public ExplosionSignalContext {
        if (gameTime < 0) {
            throw new IllegalArgumentException("Game time cannot be negative");
        }
    }
}
