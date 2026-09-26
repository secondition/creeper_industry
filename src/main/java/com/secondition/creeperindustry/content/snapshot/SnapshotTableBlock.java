package com.secondition.creeperindustry.content.snapshot;

import com.mojang.serialization.MapCodec;
import com.secondition.creeperindustry.foundation.blockEntity.SimpleEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class SnapshotTableBlock extends SimpleEntityBlock {
    public static final MapCodec<SnapshotTableBlock> CODEC = simpleCodec(SnapshotTableBlock::new);

    public SnapshotTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends SimpleEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SnapshotTableBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            Player player, BlockHitResult hit) {
        return open(level, pos, player) ? InteractionResult.sidedSuccess(level.isClientSide())
                : InteractionResult.PASS;
    }

    private boolean open(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) return true;
        if (level.getBlockEntity(pos) instanceof SnapshotTableBlockEntity table) {
            player.openMenu(table);
            return true;
        }
        return false;
    }
}
