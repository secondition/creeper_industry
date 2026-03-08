package com.secondition.creeperindustry.content.production.biosphere;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.state.BlockBehaviour;

public class ZoologicalBiosphereBlock extends BiosphereBlock {
    public static final MapCodec<ZoologicalBiosphereBlock> CODEC = simpleCodec(ZoologicalBiosphereBlock::new);

    public ZoologicalBiosphereBlock(BlockBehaviour.Properties properties) {
        super(BiosphereType.ZOOLOGICAL, properties);
    }

    @Override
    protected MapCodec<? extends BiosphereBlock> codec() {
        return CODEC;
    }
}
