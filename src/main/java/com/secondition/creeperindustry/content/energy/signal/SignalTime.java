package com.secondition.creeperindustry.content.energy.signal;

import com.secondition.creeperindustry.content.snapshot.SnapshotDimensionManager;
import net.minecraft.world.level.Level;

public final class SignalTime {
    private SignalTime() {}

    public static long now(Level level) {
        return SnapshotDimensionManager.time(level).orElseGet(level::getGameTime);
    }
}
