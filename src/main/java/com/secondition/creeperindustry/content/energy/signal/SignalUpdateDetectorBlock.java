package com.secondition.creeperindustry.content.energy.signal;

import com.mojang.serialization.MapCodec;
import com.secondition.creeperindustry.content.energy.signal.runtime.SignalRuntimeAccess;
import com.secondition.creeperindustry.foundation.blockEntity.SimpleEntityBlock;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public class SignalUpdateDetectorBlock extends SimpleEntityBlock implements EntityBlock {
    public static final MapCodec<SignalUpdateDetectorBlock> CODEC = simpleCodec(SignalUpdateDetectorBlock::new);
    public static final BooleanProperty TRIGGERED = BlockStateProperties.POWERED;

    public SignalUpdateDetectorBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TRIGGERED, false));
    }

    @Override
    protected MapCodec<? extends SimpleEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TRIGGERED);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SignalUpdateDetectorBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        refreshForDebugRead(level, pos);

        if (level.getBlockEntity(pos) instanceof SignalUpdateDetectorBlockEntity detector) {
            player.displayClientMessage(Component.translatable(
                    "message.creeper_industry.signal_update_detector.reading",
                    detector.getCurrentAmplitude(),
                    detector.getCurrentInstantaneousValue(),
                    detector.getLastNonZeroAmplitude()
            ).withStyle(ChatFormatting.YELLOW), false);
        }

        return InteractionResult.CONSUME;
    }

    private void refreshForDebugRead(Level level, BlockPos pos) {
        SignalRuntimeAccess.get(level).refreshService().refreshTarget(level, pos);
    }
}
