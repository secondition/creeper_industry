package com.secondition.creeperindustry.content.logistics.dropper;

import javax.annotation.Nullable;

import com.secondition.creeperindustry.CIBlocks;
import com.secondition.creeperindustry.CIMenuTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public class PrecisionDropperMenu extends AbstractContainerMenu {
    @Nullable
    private final PrecisionDropperBlockEntity dropper;
    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final BlockPos dropperPos;

    public PrecisionDropperMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, inventory, createClientContext(inventory, extraData));
    }

    private PrecisionDropperMenu(int containerId, Inventory inventory, ClientContext context) {
        this(containerId, inventory, context.dropper(), context.data(), context.pos());
    }

    public PrecisionDropperMenu(
            int containerId,
            Inventory inventory,
            @Nullable PrecisionDropperBlockEntity dropper,
            ContainerData data
    ) {
        this(containerId, inventory, dropper, data, dropper != null ? dropper.getBlockPos() : BlockPos.ZERO);
    }

    private PrecisionDropperMenu(
            int containerId,
            Inventory inventory,
            @Nullable PrecisionDropperBlockEntity dropper,
            ContainerData data,
            BlockPos dropperPos
    ) {
        super(CIMenuTypes.PRECISION_DROPPER.get(), containerId);
        checkContainerDataCount(data, 2);
        this.dropper = dropper;
        this.data = data;
        this.dropperPos = dropperPos.immutable();
        this.access = dropper != null && dropper.getLevel() != null
                ? ContainerLevelAccess.create(dropper.getLevel(), dropper.getBlockPos())
                : ContainerLevelAccess.NULL;
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (dropper == null) {
            return true;
        }
        return AbstractContainerMenu.stillValid(access, player, CIBlocks.PRECISION_DROPPER.get());
    }

    @Nullable
    public PrecisionDropperBlockEntity dropper() {
        return dropper;
    }

    public BlockPos dropperPos() {
        return dropperPos;
    }

    public int targetX() {
        return data.get(0);
    }

    public int targetZ() {
        return data.get(1);
    }

    private static ClientContext createClientContext(Inventory inventory, @Nullable RegistryFriendlyByteBuf extraData) {
        SimpleContainerData data = new SimpleContainerData(2);
        if (extraData == null) {
            return new ClientContext(null, data, BlockPos.ZERO);
        }

        BlockPos pos = extraData.readBlockPos();
        data.set(0, extraData.readInt());
        data.set(1, extraData.readInt());
        if (inventory.player.level().getBlockEntity(pos) instanceof PrecisionDropperBlockEntity dropper) {
            return new ClientContext(dropper, data, pos);
        }
        return new ClientContext(null, data, pos);
    }

    private record ClientContext(
            @Nullable PrecisionDropperBlockEntity dropper,
            ContainerData data,
            BlockPos pos
    ) {
    }
}
