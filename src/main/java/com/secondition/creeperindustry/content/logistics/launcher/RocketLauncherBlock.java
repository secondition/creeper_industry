package com.secondition.creeperindustry.content.logistics.launcher;

import com.mojang.serialization.MapCodec;
import com.secondition.creeperindustry.foundation.blockEntity.SimpleEntityBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class RocketLauncherBlock extends SimpleEntityBlock {
    public static final MapCodec<RocketLauncherBlock> CODEC = simpleCodec(RocketLauncherBlock::new);

    public RocketLauncherBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends SimpleEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RocketLauncherBlockEntity(pos, state);
    }
}
