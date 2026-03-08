package com.secondition.creeperindustry.content.production.printer;

import com.secondition.creeperindustry.CIBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ThreeDPrinterBlockEntity extends BlockEntity {
    public ThreeDPrinterBlockEntity(BlockPos pos, BlockState blockState) {
        super(CIBlockEntityTypes.THREE_D_PRINTER.get(), pos, blockState);
    }
}
