package com.secondition.creeperindustry.content.production.biosphere;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.state.BlockBehaviour;

public class MonsterBiosphereBlock extends BiosphereBlock {
    public static final MapCodec<MonsterBiosphereBlock> CODEC = simpleCodec(MonsterBiosphereBlock::new);

    public MonsterBiosphereBlock(BlockBehaviour.Properties properties) {
        super(BiosphereType.MONSTER, properties);
    }

    @Override
    protected MapCodec<? extends BiosphereBlock> codec() {
        return CODEC;
    }
}
