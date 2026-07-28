package com.secondition.creeperindustry.content.logistics.launcher;

import com.secondition.creeperindustry.CIEntityTypes;
import com.secondition.creeperindustry.CIItems;
import com.secondition.creeperindustry.content.logistics.rocket.GuidedFireworkRocketEntity;
import com.secondition.creeperindustry.content.logistics.rocket.RocketDestination;
import com.secondition.creeperindustry.content.logistics.rocket.runtime.RocketFlightRuntimeAccess;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

public final class RocketLaunchService {
    public static final double MAX_DISTANCE = 2048.0D;
    private RocketLaunchService() {}

    public static RocketLaunchResult tryLaunch(ServerLevel level, RocketLauncherBlockEntity launcher) {
        if (launcher.cooldown() > 0) return RocketLaunchResult.COOLDOWN;
        ItemStack rockets = launcher.getItem(RocketLauncherBlockEntity.ROCKET_SLOT);
        ItemStack cargoSlot = launcher.getItem(RocketLauncherBlockEntity.CARGO_SLOT);
        RocketDestination destination = launcher.destination();
        if (!rockets.is(CIItems.GUIDED_FIREWORK_ROCKET.get())) return RocketLaunchResult.MISSING_ROCKET;
        if (!cargoSlot.is(CIItems.STORAGE_DISC.get())) return RocketLaunchResult.MISSING_CARGO;
        if (destination == null) return RocketLaunchResult.MISSING_ADDRESS;
        if (!destination.dimension().equals(level.dimension())) return RocketLaunchResult.WRONG_DIMENSION;
        BlockPos origin = launcher.getBlockPos();
        if (!level.getWorldBorder().isWithinBounds(destination.receiverPos())) return RocketLaunchResult.INVALID_TARGET;
        if (origin.distSqr(destination.receiverPos()) > MAX_DISTANCE * MAX_DISTANCE) return RocketLaunchResult.TOO_FAR;

        GuidedFireworkRocketEntity entity = new GuidedFireworkRocketEntity(CIEntityTypes.GUIDED_FIREWORK_ROCKET.get(), level);
        entity.setPos(origin.getX() + 0.5D, origin.getY() + 1.1D, origin.getZ() + 0.5D);
        ItemStack cargo = cargoSlot.copy();
        entity.initialize(destination, cargo, origin);
        if (!RocketFlightRuntimeAccess.get(level).reserve(level, entity.getUUID(), entity.chunkPosition())) return RocketLaunchResult.FLIGHT_LIMIT;
        rockets.shrink(1);
        launcher.setItem(RocketLauncherBlockEntity.CARGO_SLOT, ItemStack.EMPTY);
        if (!level.addFreshEntity(entity)) {
            RocketFlightRuntimeAccess.get(level).finish(level, entity.getUUID());
            rockets.grow(1);
            launcher.setItem(RocketLauncherBlockEntity.CARGO_SLOT, cargo);
            return RocketLaunchResult.ENTITY_CREATION_FAILED;
        }
        launcher.setChanged();
        level.playSound(null, origin, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.BLOCKS, 1.0F, 1.0F);
        return RocketLaunchResult.SUCCESS;
    }
}
