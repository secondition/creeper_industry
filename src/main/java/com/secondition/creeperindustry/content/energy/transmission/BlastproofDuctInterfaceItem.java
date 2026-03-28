package com.secondition.creeperindustry.content.energy.transmission;

import com.secondition.creeperindustry.CISignalSourceTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class BlastproofDuctInterfaceItem extends Item {
    public BlastproofDuctInterfaceItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BlastproofDuctBlock)) {
            return InteractionResult.PASS;
        }

        Direction face = context.getClickedFace();
        if (!DuctTransmissionHelper.canInstallInterface(state, face)) {
            return InteractionResult.FAIL;
        }

        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockState updatedState = state.setValue(DuctTransmissionHelper.interfaceProperty(face), true);
        level.setBlock(pos, updatedState, Block.UPDATE_ALL);
        level.sendBlockUpdated(pos, state, updatedState, Block.UPDATE_ALL);
        level.updateNeighborsAt(pos, updatedState.getBlock());
        CISignalSourceTypes.deferredSignalRefreshQueue().enqueue(
                level.dimension(),
                CISignalSourceTypes.ductNetworkManager().getAffectedReceiversForInterfaceChange(
                        level,
                        pos,
                        CISignalSourceTypes.signalReceiverIndex(),
                        CISignalSourceTypes.continuousSignalSourceRepository()
                )
        );

        ItemStack stack = context.getItemInHand();
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResult.CONSUME;
    }
}
