package com.secondition.creeperindustry.content.logistics.launcher;

import javax.annotation.Nullable;

import com.secondition.creeperindustry.CIBlocks;
import com.secondition.creeperindustry.CIDataComponents;
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

public class RocketLauncherMenu extends AbstractContainerMenu {
    public static final int BUTTON_LAUNCH = 0;
    private static final int PLAYER_START = RocketLauncherBlockEntity.SLOT_COUNT;
    @Nullable private final RocketLauncherBlockEntity launcher;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    public RocketLauncherMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(id, inventory, clientContext(inventory, buffer));
    }

    private RocketLauncherMenu(int id, Inventory inventory, ClientContext context) {
        this(id, inventory, context.launcher, context.container, context.data);
    }

    public RocketLauncherMenu(int id, Inventory inventory, RocketLauncherBlockEntity launcher, ContainerData data) {
        this(id, inventory, launcher, launcher, data);
    }

    private RocketLauncherMenu(int id, Inventory inventory, @Nullable RocketLauncherBlockEntity launcher, Container container, ContainerData data) {
        super(CIMenuTypes.ROCKET_LAUNCHER.get(), id);
        this.launcher = launcher;
        this.data = data;
        this.access = launcher != null && launcher.getLevel() != null ? ContainerLevelAccess.create(launcher.getLevel(), launcher.getBlockPos()) : ContainerLevelAccess.NULL;
        checkContainerSize(container, 3);
        addSlot(new FilteredSlot(container, 0, 44, 35, CIItems.GUIDED_FIREWORK_ROCKET.get(), 16));
        addSlot(new FilteredSlot(container, 1, 80, 35, CIItems.STORAGE_DISC.get(), 1));
        addSlot(new Slot(container, 2, 116, 35) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.is(CIItems.RECEIVER_ADDRESS.get()) && stack.has(CIDataComponents.ROCKET_DESTINATION.get()); }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        addDataSlots(data);
    }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (id != BUTTON_LAUNCH || launcher == null || !stillValid(player)) return false;
        RocketLaunchResult result = launcher.tryLaunch();
        player.displayClientMessage(Component.translatable(result.translationKey()), false);
        return result == RocketLaunchResult.SUCCESS;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < PLAYER_START) {
            if (!moveItemStackTo(stack, PLAYER_START, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            int target = stack.is(CIItems.GUIDED_FIREWORK_ROCKET.get()) ? 0 : stack.is(CIItems.STORAGE_DISC.get()) ? 1 : stack.is(CIItems.RECEIVER_ADDRESS.get()) ? 2 : -1;
            if (target < 0 || !moveItemStackTo(stack, target, target + 1, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY, copy); else slot.setChanged();
        return copy;
    }

    @Override public boolean stillValid(Player player) { return launcher == null || AbstractContainerMenu.stillValid(access, player, CIBlocks.ROCKET_LAUNCHER.get()); }
    public int cooldown() { return data.get(0); }

    private static ClientContext clientContext(Inventory inventory, RegistryFriendlyByteBuf buffer) {
        SimpleContainer fallback = new SimpleContainer(3);
        SimpleContainerData data = new SimpleContainerData(1);
        if (buffer == null) return new ClientContext(null, fallback, data);
        BlockPos pos = buffer.readBlockPos(); data.set(0, buffer.readVarInt());
        if (inventory.player.level().getBlockEntity(pos) instanceof RocketLauncherBlockEntity launcher) return new ClientContext(launcher, launcher, data);
        return new ClientContext(null, fallback, data);
    }

    private static class FilteredSlot extends Slot {
        private final net.minecraft.world.item.Item item; private final int max;
        FilteredSlot(Container container, int slot, int x, int y, net.minecraft.world.item.Item item, int max) { super(container, slot, x, y); this.item = item; this.max = max; }
        @Override public boolean mayPlace(ItemStack stack) { return stack.is(item); }
        @Override public int getMaxStackSize() { return max; }
    }
    private record ClientContext(@Nullable RocketLauncherBlockEntity launcher, Container container, ContainerData data) {}
}
