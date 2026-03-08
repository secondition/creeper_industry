package com.secondition.creeperindustry.content.production.printer;

import com.mojang.serialization.MapCodec;
import com.secondition.creeperindustry.foundation.blockEntity.SimpleEntityBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class ThreeDPrinterBlock extends SimpleEntityBlock {
    public static final MapCodec<ThreeDPrinterBlock> CODEC = simpleCodec(ThreeDPrinterBlock::new);

    public ThreeDPrinterBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends SimpleEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ThreeDPrinterBlockEntity(pos, state);
    }
}
