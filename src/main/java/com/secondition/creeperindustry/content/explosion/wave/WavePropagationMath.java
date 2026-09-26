package com.secondition.creeperindustry.content.explosion.wave;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class WavePropagationMath {
    /**
     * The drawn and applied front is the sphere inscribed in the L1 front, touching all eight
     * faces of that octahedron; its radius and expansion speed are 1/sqrt(3) of the signal's.
     */
    private static final double INSCRIBED_FRONT_SCALE = 1.0 / Math.sqrt(3.0);

    private WavePropagationMath() {
    }

    public static double euclideanDistance(Vec3 a, Vec3 b) {
        return a.distanceTo(b);
    }

    public static double inscribedRadius(double propagationRadius) {
        return propagationRadius * INSCRIBED_FRONT_SCALE;
    }

    public static double inscribedSpeed(double propagationSpeed) {
        return propagationSpeed * INSCRIBED_FRONT_SCALE;
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
        return inscribedSpeed(speed) * Math.max(0, ageTicks);
    }

    public static double maxEffectiveRadius(double sourceAmplitude, double attenuationPerBlock) {
        return inscribedRadius(sourceAmplitude / attenuationPerBlock);
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
