package com.secondition.creeperindustry.content.energy.signal;

import com.mojang.serialization.MapCodec;
import com.secondition.creeperindustry.foundation.blockEntity.SimpleEntityBlock;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class ContinuousSignalEmitterBlock extends SimpleEntityBlock {
    public static final MapCodec<ContinuousSignalEmitterBlock> CODEC = simpleCodec(ContinuousSignalEmitterBlock::new);

    public ContinuousSignalEmitterBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends SimpleEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ContinuousSignalEmitterBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if (!(level.getBlockEntity(pos) instanceof ContinuousSignalEmitterBlockEntity emitter)) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            emitter.cyclePeriod();
            player.displayClientMessage(Component.translatable(
                    "message.creeper_industry.continuous_signal_emitter.period",
                    emitter.getPeriodTicks(),
                    emitter.getAmplitude()
            ).withStyle(ChatFormatting.AQUA), false);
        } else {
            emitter.cycleAmplitude();
            player.displayClientMessage(Component.translatable(
                    "message.creeper_industry.continuous_signal_emitter.amplitude",
                    emitter.getAmplitude(),
                    emitter.getPeriodTicks()
            ).withStyle(ChatFormatting.AQUA), false);
        }

        return InteractionResult.CONSUME;
    }
}
