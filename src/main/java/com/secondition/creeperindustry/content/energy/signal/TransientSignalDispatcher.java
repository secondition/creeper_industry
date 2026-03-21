package com.secondition.creeperindustry.content.energy.signal;

import net.minecraft.world.level.Level;

public interface TransientSignalDispatcher {
    void dispatch(Level level, SignalSource source);
}
