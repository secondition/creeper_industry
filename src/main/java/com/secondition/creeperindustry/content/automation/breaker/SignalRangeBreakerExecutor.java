package com.secondition.creeperindustry.content.automation.breaker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.state.BlockState;

public final class SignalRangeBreakerExecutor {
    private final RangeBreakerStrengthRule strengthRule;
    private final RangeBreakerPermissionChecker permissionChecker;

    public SignalRangeBreakerExecutor(
            RangeBreakerStrengthRule strengthRule,
            RangeBreakerPermissionChecker permissionChecker
    ) {
        this.strengthRule = strengthRule;
        this.permissionChecker = permissionChecker;
    }

    public int execute(
            ServerLevel level,
            BlockPos machinePos,
            Direction facing,
            SignalRange range,
            int signalAmplitude
    ) {
        if (signalAmplitude <= 0) {
            return 0;
        }

        List<BlockPos> candidates = SignalRangeGeometry.collectPositions(machinePos, facing, range);
        Explosion resistanceContext = createResistanceContext(level, machinePos, range.volume(), signalAmplitude);
        BreakPlan plan = buildBreakPlan(level, machinePos, candidates, range.volume(), signalAmplitude, resistanceContext);
        int destroyed = destroyApprovedBlocks(level, machinePos, candidates, plan, range.volume(), signalAmplitude, resistanceContext);
        playResultSound(level, machinePos, destroyed);
        return destroyed;
    }

    private BreakPlan buildBreakPlan(
            ServerLevel level,
            BlockPos machinePos,
            List<BlockPos> candidates,
            int rangeVolume,
            int signalAmplitude,
            Explosion resistanceContext
    ) {
        Set<BlockPos> blocksToDestroy = new HashSet<>();
        Map<BlockPos, BlockState> approvedStates = new HashMap<>();
        for (BlockPos targetPos : candidates) {
            if (!isPositionAvailable(level, targetPos)) {
                continue;
            }

            BlockState state = level.getBlockState(targetPos);
            if (!permissionChecker.isStructurallyBreakable(level, targetPos, state)) {
                continue;
            }
            if (!strengthRule.canBreak(state.getExplosionResistance(level, targetPos, resistanceContext), signalAmplitude, rangeVolume)) {
                continue;
            }
            if (!permissionChecker.mayBreak(level, machinePos, targetPos, state)) {
                continue;
            }

            BlockPos immutablePos = targetPos.immutable();
            blocksToDestroy.add(immutablePos);
            approvedStates.put(immutablePos, state);
        }
        return new BreakPlan(blocksToDestroy, approvedStates);
    }

    private int destroyApprovedBlocks(
            ServerLevel level,
            BlockPos machinePos,
            List<BlockPos> candidates,
            BreakPlan plan,
            int rangeVolume,
            int signalAmplitude,
            Explosion resistanceContext
    ) {
        int destroyed = 0;
        for (BlockPos targetPos : candidates) {
            if (!plan.blocksToDestroy().contains(targetPos) || !isPositionAvailable(level, targetPos)) {
                continue;
            }

            BlockState currentState = level.getBlockState(targetPos);
            if (!currentState.equals(plan.approvedStates().get(targetPos))) {
                continue;
            }
            if (!permissionChecker.isStructurallyBreakable(level, targetPos, currentState)) {
                continue;
            }
            if (!strengthRule.canBreak(currentState.getExplosionResistance(level, targetPos, resistanceContext), signalAmplitude, rangeVolume)) {
                continue;
            }
            if (level.destroyBlock(targetPos, true, permissionChecker.breakerActor(level, machinePos))) {
                destroyed++;
            }
        }
        return destroyed;
    }

    private boolean isPositionAvailable(ServerLevel level, BlockPos pos) {
        return level.isInWorldBounds(pos)
                && level.getWorldBorder().isWithinBounds(pos)
                && level.isLoaded(pos);
    }

    private Explosion createResistanceContext(
            ServerLevel level,
            BlockPos machinePos,
            int rangeVolume,
            int signalAmplitude
    ) {
        float strength = (float) strengthRule.maximumBreakableResistance(signalAmplitude, rangeVolume);
        return new Explosion(
                level,
                permissionChecker.breakerActor(level, machinePos),
                machinePos.getX() + 0.5D,
                machinePos.getY() + 0.5D,
                machinePos.getZ() + 0.5D,
                strength,
                false,
                Explosion.BlockInteraction.DESTROY
        );
    }

    private void playResultSound(ServerLevel level, BlockPos machinePos, int destroyed) {
        if (destroyed > 0) {
            level.playSound(null, machinePos, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.45F, 1.35F);
        } else {
            level.playSound(null, machinePos, SoundEvents.DISPENSER_FAIL, SoundSource.BLOCKS, 0.6F, 1.1F);
        }
    }

    private record BreakPlan(Set<BlockPos> blocksToDestroy, Map<BlockPos, BlockState> approvedStates) {
    }
}
