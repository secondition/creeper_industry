package com.secondition.creeperindustry.content.explosion;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class ExplosionShockwaveDispatcher {
    private ExplosionShockwaveDispatcher() {
    }

    public static void dispatch(ServerLevel level, Explosion explosion) {
        float power = explosion.radius();
        Vec3 center = explosion.center();
        if (!Float.isFinite(power) || power <= 0.0F || !isFinite(center)) {
            return;
        }

        BlockPos centerPos = BlockPos.containing(center);
        long visualSeed = Mth.getSeed(centerPos.getX(), centerPos.getY(), centerPos.getZ())
                ^ Long.rotateLeft(level.getGameTime(), 21);
        PacketDistributor.sendToPlayersNear(
                level,
                null,
                center.x,
                center.y,
                center.z,
                ExplosionShockwaveRules.visualRadius(power),
                new ExplosionShockwavePayload(center, power, visualSeed)
        );
    }

    private static boolean isFinite(Vec3 position) {
        return Double.isFinite(position.x) && Double.isFinite(position.y) && Double.isFinite(position.z);
    }
}
