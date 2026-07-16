package com.secondition.creeperindustry;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class CITags {
    private CITags() {
    }

    public static final class Blocks {
        public static final TagKey<Block> SIGNAL_RANGE_BREAKER_IMMUNE = TagKey.create(
                Registries.BLOCK,
                CreeperIndustry.asResource("signal_range_breaker_immune")
        );

        private Blocks() {
        }
    }
}
