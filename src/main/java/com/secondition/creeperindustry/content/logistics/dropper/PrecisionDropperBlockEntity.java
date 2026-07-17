package com.secondition.creeperindustry.content.logistics.dropper;

import com.secondition.creeperindustry.CIBlockEntityTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PrecisionDropperBlockEntity extends BlockEntity implements MenuProvider {
    private int targetX;
    private int targetZ;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> targetX;
                case 1 -> targetZ;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                targetX = value;
            } else if (index == 1) {
                targetZ = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public PrecisionDropperBlockEntity(BlockPos pos, BlockState blockState) {
        super(CIBlockEntityTypes.PRECISION_DROPPER.get(), pos, blockState);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new PrecisionDropperMenu(containerId, inventory, this, dataAccess);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(worldPosition);
        buffer.writeInt(targetX);
        buffer.writeInt(targetZ);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("target_x", targetX);
        tag.putInt("target_z", targetZ);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        targetX = tag.getInt("target_x");
        targetZ = tag.getInt("target_z");
    }

    public void setTarget(int targetX, int targetZ) {
        if (this.targetX == targetX && this.targetZ == targetZ) {
            return;
        }
        this.targetX = targetX;
        this.targetZ = targetZ;
        setChanged();
    }

    public int targetX() {
        return targetX;
    }

    public int targetZ() {
        return targetZ;
    }
}
