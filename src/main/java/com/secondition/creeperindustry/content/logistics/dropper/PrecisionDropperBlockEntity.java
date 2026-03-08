package com.secondition.creeperindustry.content.logistics.dropper;

import com.secondition.creeperindustry.CIBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PrecisionDropperBlockEntity extends BlockEntity {
    public PrecisionDropperBlockEntity(BlockPos pos, BlockState blockState) {
        super(CIBlockEntityTypes.PRECISION_DROPPER.get(), pos, blockState);
    }
}
