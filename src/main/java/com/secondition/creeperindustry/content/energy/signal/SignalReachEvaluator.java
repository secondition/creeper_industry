package com.secondition.creeperindustry.content.energy.signal;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SignalReachEvaluator {
    private SignalReachEvaluator() {
    }

    public static List<SignalReach> findReachableTargets(
            DuctNetworkManager ductNetworkManager,
            Level level,
            SignalSource source,
            Collection<BlockPos> candidateTargets
    ) {
        int sourceAmplitude = source.signal().amplitude();
        return candidateTargets.stream()
                .map(targetPos -> evaluate(ductNetworkManager, level, source.position(), sourceAmplitude, targetPos))
                .filter(Objects::nonNull)
                .toList();
    }

    public static SignalReach evaluate(DuctNetworkManager ductNetworkManager, Level level, SignalSource source, BlockPos targetPos) {
        return evaluate(ductNetworkManager, level, source.position(), source.signal().amplitude(), targetPos);
    }

    private static SignalReach evaluate(
            DuctNetworkManager ductNetworkManager,
            Level level,
            Vec3 sourcePos,
            int sourceAmplitude,
            BlockPos targetPos
    ) {
        int propagationCost = DuctPropagationCostResolver.resolve(ductNetworkManager, level, sourcePos, targetPos);
        int effectiveAmplitude = sourceAmplitude - propagationCost;
        if (effectiveAmplitude <= 0) {
            return null;
        }

        return new SignalReach(targetPos.immutable(), propagationCost, effectiveAmplitude);
    }
}
