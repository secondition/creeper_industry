package com.secondition.creeperindustry.content.energy.signal;

import java.util.UUID;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public interface SignalSource {
    UUID id();

    SignalSourceType<? extends SignalSource> type();

    ResourceKey<Level> level();

    Vec3 position();

    long gameTime();

    SignalDefinition signal();
}
