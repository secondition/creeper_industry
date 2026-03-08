package com.secondition.creeperindustry.content.production.biosphere;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.state.BlockBehaviour;

public class BotanicalBiosphereBlock extends BiosphereBlock {
    public static final MapCodec<BotanicalBiosphereBlock> CODEC = simpleCodec(BotanicalBiosphereBlock::new);

    public BotanicalBiosphereBlock(BlockBehaviour.Properties properties) {
        super(BiosphereType.BOTANICAL, properties);
    }

    @Override
    protected MapCodec<? extends BiosphereBlock> codec() {
        return CODEC;
    }
}
