package com.secondition.creeperindustry.client.explosion;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.joml.Vector3f;

final class ShockwaveOcclusion {
    static final int MAX_VISIBILITY_CHECKS = 2;
    static final double RECHECK_DISTANCE_SQUARED = 1.0;

    private static final double SAMPLE_OFFSET = 0.8;
    private static final double TARGET_TOLERANCE_SQUARED = 0.36;

    private ShockwaveOcclusion() {
    }

    static float sample(ClientLevel level, Camera camera, Vec3 center) {
        Vec3 cameraPosition = camera.getPosition();
        Vector3f leftVector = camera.getLeftVector();
        Vector3f upVector = camera.getUpVector();
        Vec3 left = new Vec3(leftVector.x, leftVector.y, leftVector.z).scale(SAMPLE_OFFSET);
        Vec3 up = new Vec3(upVector.x, upVector.y, upVector.z).scale(SAMPLE_OFFSET);
        Vec3[] targets = {
                center,
                center.add(left),
                center.subtract(left),
                center.add(up),
                center.subtract(up)
        };

        int visibleSamples = 0;
        for (Vec3 target : targets) {
            HitResult hit = level.clip(new ClipContext(
                    cameraPosition,
                    target,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    CollisionContext.empty()
            ));
            if (hit.getType() == HitResult.Type.MISS
                    || hit.getLocation().distanceToSqr(target) <= TARGET_TOLERANCE_SQUARED) {
                visibleSamples++;
            }
        }
        return visibleSamples / (float) targets.length;
    }
}
