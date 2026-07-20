package com.secondition.creeperindustry.content.explosion.wave.runtime;

import java.util.Optional;

import com.secondition.creeperindustry.CIAttachmentTypes;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class WaveRuntimeAccess {
    private WaveRuntimeAccess() {
    }

    public static WaveRuntime get(Level level) {
        return requireServerLevel(level).getData(CIAttachmentTypes.WAVE_RUNTIME);
    }

    public static Optional<WaveRuntime> getExisting(Level level) {
        return requireServerLevel(level).getExistingData(CIAttachmentTypes.WAVE_RUNTIME);
    }

    public static Optional<WaveRuntime> remove(ServerLevel level) {
        return Optional.ofNullable(level.removeData(CIAttachmentTypes.WAVE_RUNTIME));
    }

    private static ServerLevel requireServerLevel(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            return serverLevel;
        }
        throw new IllegalArgumentException("Wave runtime is only available for server levels");
    }
}
