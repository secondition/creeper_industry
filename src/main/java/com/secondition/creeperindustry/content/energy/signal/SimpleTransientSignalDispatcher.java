package com.secondition.creeperindustry.content.energy.signal;

import net.minecraft.core.BlockPos;
import com.secondition.creeperindustry.CISignalSourceTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public class SimpleTransientSignalDispatcher implements TransientSignalDispatcher {
    @Override
    public void dispatch(Level level, SignalSource source) {
        int amplitude = source.signal().amplitude();
        BlockPos center = BlockPos.containing(source.position());
        int coarseRadius = amplitude - 1;
        if (coarseRadius < 0) {
            return;
        }

        for (int dx = -coarseRadius; dx <= coarseRadius; dx++) {
            int remainingAfterX = coarseRadius - Math.abs(dx);
            for (int dy = -remainingAfterX; dy <= remainingAfterX; dy++) {
                int remainingAfterY = remainingAfterX - Math.abs(dy);
                for (int dz = -remainingAfterY; dz <= remainingAfterY; dz++) {
                    BlockPos targetPos = center.offset(dx, dy, dz);
                    BlockEntity blockEntity = level.getBlockEntity(targetPos);
                    if (!(blockEntity instanceof SignalReceiver receiver)) {
                        continue;
                    }

                    int propagationCost = computePropagationCost(source.position(), Vec3.atCenterOf(targetPos));
                    int effectiveAmplitude = amplitude - propagationCost;
                    if (effectiveAmplitude <= 0) {
                        continue;
                    }

                    CISignalSourceTypes.signalAggregationService().submitContribution(
                            level,
                            new DeliveredSignal(source, targetPos.immutable(), effectiveAmplitude, propagationCost)
                    );
                }
            }
        }
    }

    private int computePropagationCost(Vec3 sourcePos, Vec3 targetPos) {
        double manhattanDistance = Math.abs(sourcePos.x - targetPos.x)
                + Math.abs(sourcePos.y - targetPos.y)
                + Math.abs(sourcePos.z - targetPos.z);
        return Mth.ceil(manhattanDistance);
    }
}
