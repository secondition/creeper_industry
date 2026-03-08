package com.secondition.creeperindustry.content.production.biosphere;

import com.secondition.creeperindustry.foundation.blockEntity.SimpleEntityBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public abstract class BiosphereBlock extends SimpleEntityBlock {
    private final BiosphereType type;

    public BiosphereBlock(BiosphereType type, BlockBehaviour.Properties properties) {
        super(properties);
        this.type = type;
    }

    public BiosphereType getType() {
        return type;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BiosphereBlockEntity(pos, state);
    }
}
