package com.secondition.creeperindustry.mixin;

import com.secondition.creeperindustry.content.snapshot.SnapshotDimensionManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Redirect(method = "tickChildren", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;tick(Ljava/util/function/BooleanSupplier;)V"))
    private void creeperIndustry$tickSnapshot(ServerLevel level, BooleanSupplier hasTimeLeft) {
        if (SnapshotDimensionManager.isSnapshot(level)) SnapshotDimensionManager.tick(level);
        else level.tick(hasTimeLeft);
    }
}
