package com.secondition.creeperindustry.content.production.biosphere;

import com.secondition.creeperindustry.CIBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BiosphereBlockEntity extends BlockEntity {
    public BiosphereBlockEntity(BlockPos pos, BlockState blockState) {
        super(CIBlockEntityTypes.BIOSPHERE.get(), pos, blockState);
    }
}
