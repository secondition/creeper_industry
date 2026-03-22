package com.secondition.creeperindustry.content.energy.signal;

import java.util.Collection;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class SignalReachEvaluator {
    private SignalReachEvaluator() {
    }

    public static List<SignalReach> findReachableTargets(SignalSource source, Collection<BlockPos> candidateTargets) {
        int sourceAmplitude = source.signal().amplitude();
        return candidateTargets.stream()
                .map(targetPos -> evaluate(source.position(), sourceAmplitude, targetPos))
                .filter(reach -> reach != null)
                .toList();
    }

    public static SignalReach evaluate(SignalSource source, BlockPos targetPos) {
        return evaluate(source.position(), source.signal().amplitude(), targetPos);
    }

    private static SignalReach evaluate(Vec3 sourcePos, int sourceAmplitude, BlockPos targetPos) {
        int propagationCost = computePropagationCost(sourcePos, targetPos);
        int effectiveAmplitude = sourceAmplitude - propagationCost;
        if (effectiveAmplitude <= 0) {
            return null;
        }

        return new SignalReach(targetPos.immutable(), propagationCost, effectiveAmplitude);
    }

    private static int computePropagationCost(Vec3 sourcePos, BlockPos targetPos) {
        Vec3 targetCenter = Vec3.atCenterOf(targetPos);
        double manhattanDistance = Math.abs(sourcePos.x - targetCenter.x)
                + Math.abs(sourcePos.y - targetCenter.y)
                + Math.abs(sourcePos.z - targetCenter.z);
        return (int) Math.ceil(manhattanDistance);
    }
}
