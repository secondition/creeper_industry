package com.secondition.creeperindustry.content.energy.signal;

import net.minecraft.world.level.Level;

public class NoOpTransientSignalDispatcher implements TransientSignalDispatcher {
    @Override
    public void dispatch(Level level, SignalSource source) {
    }
}
