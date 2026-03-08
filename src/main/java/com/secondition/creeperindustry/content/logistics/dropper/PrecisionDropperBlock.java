package com.secondition.creeperindustry.content.logistics.dropper;

import com.mojang.serialization.MapCodec;
import com.secondition.creeperindustry.foundation.blockEntity.SimpleEntityBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class PrecisionDropperBlock extends SimpleEntityBlock {
    public static final MapCodec<PrecisionDropperBlock> CODEC = simpleCodec(PrecisionDropperBlock::new);

    public PrecisionDropperBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends SimpleEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PrecisionDropperBlockEntity(pos, state);
    }
}
