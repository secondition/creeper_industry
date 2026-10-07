package com.secondition.creeperindustry.content.explosion.wave;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class WavePropagationMath {
    private WavePropagationMath() {}

    /** Filled radius-r balls contain block centers within r + 0.5 blocks. */
    public static int shell(Vec3 origin, BlockPos pos) {
        BlockPos center = BlockPos.containing(origin);
        double dx = pos.getX() - (double) center.getX();
        double dy = pos.getY() - (double) center.getY();
        double dz = pos.getZ() - (double) center.getZ();
        return (int) Math.floor(Math.sqrt(dx * dx + dy * dy + dz * dz) + 0.5);
    }

    public static int shell(Vec3 origin, Vec3 point) {
        return shell(origin, BlockPos.containing(point));
    }

    public static double effectiveAmplitude(double sourceAmplitude, double shell, double attenuationPerBlock) {
        return Math.max(0.0, sourceAmplitude - shell * attenuationPerBlock);
    }

    public static long travelTicks(double shell, double speedBlocksPerTick) {
        return (long) Math.max(0, Math.ceil(shell / speedBlocksPerTick));
    }

    public static long arrivalTick(long emissionTick, double shell, double speed) {
        return emissionTick + travelTicks(shell, speed);
    }

    public static double radiusAtAge(double speed, long ageTicks) {
        return Math.floor(speed * Math.max(0, ageTicks));
    }

    public static double maxEffectiveRadius(double sourceAmplitude, double attenuationPerBlock) {
        return Math.max(0, Math.ceil(sourceAmplitude / attenuationPerBlock) - 1);
    }
}
