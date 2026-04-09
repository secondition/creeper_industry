package com.secondition.creeperindustry.content.logistics.storage;

import javax.annotation.Nullable;

import com.secondition.creeperindustry.CIMenuTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class DiscBurnerMenu extends AbstractContainerMenu {
    private static final int PLAYER_INV_START = DiscBurnerBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    private static final int HOTBAR_START = PLAYER_INV_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container burner;
    private final ContainerData data;

    public DiscBurnerMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, createClientContext(playerInventory, extraData));
    }

    private DiscBurnerMenu(int containerId, Inventory playerInventory, ClientContext context) {
        this(containerId, playerInventory, context.container(), context.data());
    }

    public DiscBurnerMenu(int containerId, Inventory playerInventory, Container burner, ContainerData data) {
        super(CIMenuTypes.DISC_BURNER.get(), containerId);
        checkContainerSize(burner, DiscBurnerBlockEntity.SLOT_COUNT);
        checkContainerDataCount(data, 2);
        this.burner = burner;
        this.data = data;

        addSlot(new Slot(burner, DiscBurnerBlockEntity.INPUT_SLOT, 44, 35));
        addSlot(new Slot(burner, DiscBurnerBlockEntity.DISC_SLOT, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return DiscBurnerBlockEntity.isValidDisc(stack);
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }

        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stackInSlot = slot.getItem();
        ItemStack originalStack = stackInSlot.copy();

        if (index < DiscBurnerBlockEntity.SLOT_COUNT) {
            if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (DiscBurnerBlockEntity.isValidDisc(stackInSlot)) {
            if (!moveItemStackTo(stackInSlot, DiscBurnerBlockEntity.DISC_SLOT, DiscBurnerBlockEntity.DISC_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stackInSlot, DiscBurnerBlockEntity.INPUT_SLOT, DiscBurnerBlockEntity.INPUT_SLOT + 1, false)) {
            if (index < PLAYER_INV_END) {
                if (!moveItemStackTo(stackInSlot, HOTBAR_START, HOTBAR_END, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, PLAYER_INV_END, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stackInSlot.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY, originalStack);
        } else {
            slot.setChanged();
        }

        if (stackInSlot.getCount() == originalStack.getCount()) {
            return ItemStack.EMPTY;
        }

        slot.onTake(player, stackInSlot);
        return originalStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return burner.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        burner.stopOpen(player);
    }

    public int getScaledProgress(int width) {
        int progress = data.get(0);
        int maxProgress = data.get(1);
        if (progress <= 0 || maxProgress <= 0) {
            return 0;
        }
        return progress * width / maxProgress;
    }

    private static ClientContext createClientContext(Inventory playerInventory, @Nullable RegistryFriendlyByteBuf extraData) {
        if (extraData == null) {
            return new ClientContext(new SimpleContainer(DiscBurnerBlockEntity.SLOT_COUNT), new SimpleContainerData(2));
        }

        BlockPos pos = extraData.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof DiscBurnerBlockEntity burner) {
            return new ClientContext(burner, burner.dataAccess());
        }
        return new ClientContext(new SimpleContainer(DiscBurnerBlockEntity.SLOT_COUNT), new SimpleContainerData(2));
    }

    private record ClientContext(Container container, ContainerData data) {
    }
}
