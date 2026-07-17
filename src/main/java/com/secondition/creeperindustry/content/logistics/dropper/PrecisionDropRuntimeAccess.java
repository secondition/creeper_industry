package com.secondition.creeperindustry.content.logistics.dropper;

import java.util.Optional;

import com.secondition.creeperindustry.CIAttachmentTypes;

import net.minecraft.server.level.ServerLevel;

public final class PrecisionDropRuntimeAccess {
    private PrecisionDropRuntimeAccess() {
    }

    public static PrecisionDropRuntime get(ServerLevel level) {
        return level.getData(CIAttachmentTypes.PRECISION_DROP_RUNTIME);
    }

    public static Optional<PrecisionDropRuntime> getExisting(ServerLevel level) {
        return level.getExistingData(CIAttachmentTypes.PRECISION_DROP_RUNTIME);
    }

    public static Optional<PrecisionDropRuntime> remove(ServerLevel level) {
        return Optional.ofNullable(level.removeData(CIAttachmentTypes.PRECISION_DROP_RUNTIME));
    }
}
