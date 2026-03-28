package com.secondition.creeperindustry.content.energy.signal;

import com.secondition.creeperindustry.CISignalSourceTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class DuctPropagationCostResolver {
    private DuctPropagationCostResolver() {
    }

    public static boolean hasNearbyDuctEntrance(Level level, Vec3 sourcePos, int maxPropagationCost) {
        return CISignalSourceTypes.ductNetworkManager().hasNearbyDuctEntrance(level, sourcePos, maxPropagationCost);
    }

    public static int resolve(Level level, Vec3 sourcePos, BlockPos targetPos) {
        return CISignalSourceTypes.ductNetworkManager().resolvePropagationCost(level, sourcePos, targetPos);
    }
}
