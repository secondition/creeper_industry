package com.secondition.creeperindustry.content.logistics.launcher;

import javax.annotation.Nullable;

import com.secondition.creeperindustry.CIBlocks;
import com.secondition.creeperindustry.CIItems;
import com.secondition.creeperindustry.CIMenuTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class GuidedFireworkReceiverMenu extends AbstractContainerMenu {
    public static final int BUTTON_TOGGLE_MODE = 0;
    public static final int BUTTON_PRINT_ADDRESS = 1;
    private static final int PLAYER_INV_START = GuidedFireworkReceiverBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    private static final int HOTBAR_START = PLAYER_INV_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    @Nullable
    private final GuidedFireworkReceiverBlockEntity receiver;
    private final Container container;
    private final ContainerData data;
    private final ContainerLevelAccess access;
    private int lastAddressPrintTick = -1;

    public GuidedFireworkReceiverMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, inventory, createClientContext(inventory, extraData));
    }

    private GuidedFireworkReceiverMenu(int containerId, Inventory inventory, ClientContext context) {
        this(containerId, inventory, context.receiver(), context.container(), context.data());
    }

    public GuidedFireworkReceiverMenu(
            int containerId,
            Inventory inventory,
            GuidedFireworkReceiverBlockEntity receiver,
            ContainerData data
    ) {
        this(containerId, inventory, receiver, receiver, data);
    }

    private GuidedFireworkReceiverMenu(
            int containerId,
            Inventory inventory,
            @Nullable GuidedFireworkReceiverBlockEntity receiver,
            Container container,
            ContainerData data
    ) {
        super(CIMenuTypes.GUIDED_FIREWORK_RECEIVER.get(), containerId);
        checkContainerSize(container, GuidedFireworkReceiverBlockEntity.SLOT_COUNT);
        checkContainerDataCount(data, 3);
        this.receiver = receiver;
        this.container = container;
        this.data = data;
        this.access = receiver != null && receiver.getLevel() != null
                ? ContainerLevelAccess.create(receiver.getLevel(), receiver.getBlockPos())
                : ContainerLevelAccess.NULL;

        for (int slot = 0; slot < GuidedFireworkReceiverBlockEntity.DISC_SLOT_COUNT; slot++) {
            addSlot(new Slot(container, slot, 44 + slot * 18, 33) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.is(CIItems.STORAGE_DISC.get());
                }
            });
        }
        for (int slot = 0; slot < GuidedFireworkReceiverBlockEntity.OUTPUT_SLOT_COUNT; slot++) {
            addSlot(new Slot(container, GuidedFireworkReceiverBlockEntity.OUTPUT_SLOT_START + slot, 44 + slot * 18, 59) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        addPlayerInventory(inventory);
        addDataSlots(data);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (receiver == null || !stillValid(player)) {
            return false;
        }
        if (id == BUTTON_TOGGLE_MODE) {
            receiver.toggleMode();
            return true;
        }
        if (id == BUTTON_PRINT_ADDRESS) {
            int now = player.tickCount;
            if (lastAddressPrintTick >= 0 && now - lastAddressPrintTick < 5) {
                return false;
            }
            ItemStack address = receiver.createAddress();
            if (address.isEmpty() || !player.getInventory().add(address)) {
                player.displayClientMessage(Component.translatable("message.creeper_industry.receiver.inventory_full"), false);
                return false;
            }
            lastAddressPrintTick = now;
            player.displayClientMessage(Component.translatable("message.creeper_industry.receiver.address_created"), false);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < GuidedFireworkReceiverBlockEntity.SLOT_COUNT) {
            if (!moveItemStackTo(stack, PLAYER_INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(CIItems.STORAGE_DISC.get())) {
            if (!moveItemStackTo(stack, 0, GuidedFireworkReceiverBlockEntity.DISC_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_INV_END) {
            if (!moveItemStackTo(stack, HOTBAR_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, PLAYER_INV_START, PLAYER_INV_END, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY, original);
        } else {
            slot.setChanged();
        }
        return stack.getCount() == original.getCount() ? ItemStack.EMPTY : original;
    }

    @Override
    public boolean stillValid(Player player) {
        return receiver == null || AbstractContainerMenu.stillValid(access, player, CIBlocks.GUIDED_FIREWORK_RECEIVER.get());
    }

    public GuidedFireworkReceiverMode mode() {
        return GuidedFireworkReceiverMode.fromId(data.get(0));
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 92 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 150));
        }
    }

    private static ClientContext createClientContext(Inventory inventory, @Nullable RegistryFriendlyByteBuf extraData) {
        SimpleContainer fallback = new SimpleContainer(GuidedFireworkReceiverBlockEntity.SLOT_COUNT);
        SimpleContainerData data = new SimpleContainerData(3);
        if (extraData == null) {
            return new ClientContext(null, fallback, data);
        }
        BlockPos pos = extraData.readBlockPos();
        data.set(0, extraData.readVarInt());
        data.set(1, extraData.readVarInt());
        data.set(2, extraData.readVarInt());
        if (inventory.player.level().getBlockEntity(pos) instanceof GuidedFireworkReceiverBlockEntity receiver) {
            return new ClientContext(receiver, receiver, data);
        }
        return new ClientContext(null, fallback, data);
    }

    private record ClientContext(
            @Nullable GuidedFireworkReceiverBlockEntity receiver,
            Container container,
            ContainerData data
    ) {
    }
}
