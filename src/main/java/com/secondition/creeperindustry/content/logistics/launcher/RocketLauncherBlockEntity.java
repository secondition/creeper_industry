package com.secondition.creeperindustry.content.logistics.launcher;

import com.secondition.creeperindustry.CIBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class RocketLauncherBlockEntity extends BlockEntity {
    public RocketLauncherBlockEntity(BlockPos pos, BlockState blockState) {
        super(CIBlockEntityTypes.ROCKET_LAUNCHER.get(), pos, blockState);
    }
}
