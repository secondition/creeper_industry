package com.secondition.creeperindustry.content.production.biosphere;

import com.mojang.serialization.MapCodec;
import com.secondition.creeperindustry.foundation.blockEntity.SimpleEntityBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class WaveReceiverBlock extends SimpleEntityBlock {
    public static final MapCodec<WaveReceiverBlock> CODEC = simpleCodec(WaveReceiverBlock::new);

    public WaveReceiverBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends SimpleEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WaveReceiverBlockEntity(pos, state);
    }
}
