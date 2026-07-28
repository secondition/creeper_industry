package com.secondition.creeperindustry.content.explosion.wave;

import net.minecraft.world.phys.AABB;
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

    public static double effectiveAmplitude(double sourceAmplitude, double distance, double attenuationPerBlock) {
        return Math.max(0.0, sourceAmplitude - distance * attenuationPerBlock);
    }

    public static long travelTicks(double distance, double speedBlocksPerTick) {
        return (long) Math.max(0, Math.ceil(distance / speedBlocksPerTick));
    }

    public static long arrivalTick(long emissionTick, double distance, double speed) {
        return emissionTick + travelTicks(distance, speed);
    }

    public static double radiusAtAge(double speed, long ageTicks) {
        return speed * Math.max(0, ageTicks);
    }

    public static double maxEffectiveRadius(double sourceAmplitude, double attenuationPerBlock) {
        return sourceAmplitude / attenuationPerBlock;
    }

    public static double closestDistanceToAABB(Vec3 point, AABB aabb) {
        double dx = 0.0, dy = 0.0, dz = 0.0;
        if (point.x < aabb.minX) {
            dx = aabb.minX - point.x;
        } else if (point.x > aabb.maxX) {
            dx = point.x - aabb.maxX;
        }
        if (point.y < aabb.minY) {
            dy = aabb.minY - point.y;
        } else if (point.y > aabb.maxY) {
            dy = point.y - aabb.maxY;
        }
        if (point.z < aabb.minZ) {
            dz = aabb.minZ - point.z;
        } else if (point.z > aabb.maxZ) {
            dz = point.z - aabb.maxZ;
        }
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static double farthestDistanceToAABB(Vec3 point, AABB aabb) {
        double dx = Math.max(Math.abs(point.x - aabb.minX), Math.abs(point.x - aabb.maxX));
        double dy = Math.max(Math.abs(point.y - aabb.minY), Math.abs(point.y - aabb.maxY));
        double dz = Math.max(Math.abs(point.z - aabb.minZ), Math.abs(point.z - aabb.maxZ));
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
