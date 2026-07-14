package com.secondition.creeperindustry.content.energy.signal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class DuctPropagationCostResolver {
    private DuctPropagationCostResolver() {
    }

    public static boolean hasNearbyDuctEntrance(
            DuctNetworkManager ductNetworkManager,
            Level level,
            Vec3 sourcePos,
            int maxPropagationCost
    ) {
        return ductNetworkManager.hasNearbyDuctEntrance(level, sourcePos, maxPropagationCost);
    }

    public static int resolve(DuctNetworkManager ductNetworkManager, Level level, Vec3 sourcePos, BlockPos targetPos) {
        return ductNetworkManager.resolvePropagationCost(level, sourcePos, targetPos);
    }
}
