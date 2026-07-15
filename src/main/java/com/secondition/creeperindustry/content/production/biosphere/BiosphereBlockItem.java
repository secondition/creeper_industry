package com.secondition.creeperindustry.content.production.biosphere;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

public final class BiosphereBlockItem extends BlockItem {
    public BiosphereBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        if (!context.getLevel().isClientSide()) {
            return super.place(context);
        }
        if (!getBlock().isEnabled(context.getLevel().enabledFeatures()) || !context.canPlace()) {
            return InteractionResult.FAIL;
        }

        BlockPlaceContext placementContext = updatePlacementContext(context);
        if (placementContext == null) {
            return InteractionResult.FAIL;
        }

        BlockState placementState = getPlacementState(placementContext);
        if (placementState == null) {
            return InteractionResult.FAIL;
        }

        Level level = placementContext.getLevel();
        BlockPos pos = placementContext.getClickedPos();
        SoundType soundType = placementState.getSoundType(level, pos, placementContext.getPlayer());
        level.playSound(
                placementContext.getPlayer(),
                pos,
                getPlaceSound(placementState, level, pos, placementContext.getPlayer()),
                SoundSource.BLOCKS,
                (soundType.getVolume() + 1.0F) / 2.0F,
                soundType.getPitch() * 0.8F
        );
        return InteractionResult.SUCCESS;
    }
}
