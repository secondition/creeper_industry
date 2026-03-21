package com.secondition.creeperindustry.content.energy.signal;

import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;

public class DefaultExplosionSignalFilter implements ExplosionSignalFilter {
    @Override
    public boolean shouldCreateSignal(Level level, Explosion explosion) {
        return explosion.radius() > 0.0F;
    }
}
