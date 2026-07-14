package com.secondition.creeperindustry.content.energy.signal;

import java.util.Collection;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class SignalReceiverSelector {
    private final SignalReceiverIndex receiverIndex;
    private final DuctNetworkManager ductNetworkManager;

    public SignalReceiverSelector(SignalReceiverIndex receiverIndex, DuctNetworkManager ductNetworkManager) {
        this.receiverIndex = receiverIndex;
        this.ductNetworkManager = ductNetworkManager;
    }

    public Collection<BlockPos> getPotentialTargets(Level level, SignalSource source) {
        int maxDistance = source.signal().amplitude() - 1;
        if (maxDistance < 0) {
            return java.util.List.of();
        }

        return ductNetworkManager.getPotentialTargets(
                level,
                source.position(),
                maxDistance
                ,
                receiverIndex
        );
    }
}
