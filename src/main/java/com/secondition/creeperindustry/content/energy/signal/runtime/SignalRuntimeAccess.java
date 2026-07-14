package com.secondition.creeperindustry.content.energy.signal.runtime;

import java.util.Optional;

import com.secondition.creeperindustry.CIAttachmentTypes;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class SignalRuntimeAccess {
    private SignalRuntimeAccess() {
    }

    public static SignalRuntime get(Level level) {
        return requireServerLevel(level).getData(CIAttachmentTypes.SIGNAL_RUNTIME);
    }

    public static Optional<SignalRuntime> getExisting(Level level) {
        return requireServerLevel(level).getExistingData(CIAttachmentTypes.SIGNAL_RUNTIME);
    }

    public static Optional<SignalRuntime> remove(ServerLevel level) {
        return Optional.ofNullable(level.removeData(CIAttachmentTypes.SIGNAL_RUNTIME));
    }

    private static ServerLevel requireServerLevel(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            return serverLevel;
        }
        throw new IllegalArgumentException("Signal runtime is only available for server levels");
    }
}
