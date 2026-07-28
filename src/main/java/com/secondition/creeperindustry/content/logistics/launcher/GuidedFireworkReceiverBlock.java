package com.secondition.creeperindustry.content.logistics.launcher;

import com.mojang.serialization.MapCodec;
import com.secondition.creeperindustry.CIBlockEntityTypes;
import com.secondition.creeperindustry.foundation.blockEntity.SimpleEntityBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class GuidedFireworkReceiverBlock extends SimpleEntityBlock {
    public static final MapCodec<GuidedFireworkReceiverBlock> CODEC = simpleCodec(GuidedFireworkReceiverBlock::new);

    public GuidedFireworkReceiverBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends SimpleEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GuidedFireworkReceiverBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : createTickerHelper(type, CIBlockEntityTypes.GUIDED_FIREWORK_RECEIVER.get(), GuidedFireworkReceiverBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return openMenu(level, pos, player) ? InteractionResult.sidedSuccess(level.isClientSide()) : InteractionResult.PASS;
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        return openMenu(level, pos, player)
                ? ItemInteractionResult.sidedSuccess(level.isClientSide())
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof GuidedFireworkReceiverBlockEntity receiver) {
            Containers.dropContents(level, pos, receiver);
            level.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static boolean openMenu(Level level, BlockPos pos, Player player) {
        if (level.isClientSide()) {
            return true;
        }
        if (!(level.getBlockEntity(pos) instanceof GuidedFireworkReceiverBlockEntity receiver)) {
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(receiver);
        }
        return true;
    }
}
