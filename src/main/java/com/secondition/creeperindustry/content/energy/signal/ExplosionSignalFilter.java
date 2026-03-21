package com.secondition.creeperindustry.content.energy.signal;

import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;

public interface ExplosionSignalFilter {
    boolean shouldCreateSignal(Level level, Explosion explosion);
}
