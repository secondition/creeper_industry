package com.secondition.creeperindustry.content.production.printer;

import com.mojang.serialization.MapCodec;
import com.secondition.creeperindustry.foundation.blockEntity.SimpleEntityBlock;

import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;

public class ThreeDPrinterBlock extends SimpleEntityBlock {
    public static final MapCodec<ThreeDPrinterBlock> CODEC = simpleCodec(ThreeDPrinterBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public ThreeDPrinterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    protected MapCodec<? extends SimpleEntityBlock> codec() {
        return CODEC;
    }

    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ThreeDPrinterBlockEntity(pos, state);
    }

    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide()
                ? null
                : (l, p, s, be) -> {
                    if (be instanceof ThreeDPrinterBlockEntity printer) printer.tick();
                };
    }

    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!level.isClientSide()
                && level.getBlockEntity(pos) instanceof ThreeDPrinterBlockEntity printer)
            printer.interact(player, stack);
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()
                && level.getBlockEntity(pos) instanceof ThreeDPrinterBlockEntity printer)
            printer.interact(player, ItemStack.EMPTY);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
