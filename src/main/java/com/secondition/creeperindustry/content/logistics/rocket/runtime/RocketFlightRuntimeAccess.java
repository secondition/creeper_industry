package com.secondition.creeperindustry.content.logistics.rocket.runtime;

import java.util.Optional;
import com.secondition.creeperindustry.CIAttachmentTypes;
import net.minecraft.server.level.ServerLevel;

public final class RocketFlightRuntimeAccess {
    private RocketFlightRuntimeAccess() {}
    public static RocketFlightRuntime get(ServerLevel level) { return level.getData(CIAttachmentTypes.ROCKET_FLIGHT_RUNTIME); }
    public static Optional<RocketFlightRuntime> remove(ServerLevel level) { return Optional.ofNullable(level.removeData(CIAttachmentTypes.ROCKET_FLIGHT_RUNTIME)); }
}
