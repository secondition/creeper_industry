package com.secondition.creeperindustry.content.explosion.wave;

import net.minecraft.world.phys.Vec3;

public final class WavePropagationMath {
    private WavePropagationMath() {
    }

    public static double euclideanDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static double euclideanDistance(Vec3 a, Vec3 b) {
        return a.distanceTo(b);
    }

    public static int attenuationCost(double distance, double attenuationPerBlock) {
        return (int) Math.ceil(distance * attenuationPerBlock);
    }

    public static int effectiveAmplitude(int sourceAmplitude, double distance, double attenuationPerBlock) {
        int cost = attenuationCost(distance, attenuationPerBlock);
        return Math.max(0, sourceAmplitude - cost);
    }

    public static long travelTicks(double distance, double speedBlocksPerTick) {
        return (long) Math.max(0, Math.ceil(distance / speedBlocksPerTick));
    }

    public static long arrivalTick(long emissionTick, double distance, double speed) {
        return emissionTick + travelTicks(distance, speed);
    }

    public static boolean isShellCrossing(double distance, double previousRadius, double currentRadius) {
        if (currentRadius < 0) return false;
        if (distance < 0) return false;
        if (distance > currentRadius) return false;
        if (distance > previousRadius) return true;
        return previousRadius == 0 && distance == 0;
    }

    public static double radiusAtAge(double speed, long ageTicks) {
        return speed * Math.max(0, ageTicks);
    }

    public static double maxEffectiveRadius(int sourceAmplitude, double attenuationPerBlock) {
        if (attenuationPerBlock == 0) {
            return Double.POSITIVE_INFINITY;
        }
        return sourceAmplitude / attenuationPerBlock;
    }
}
