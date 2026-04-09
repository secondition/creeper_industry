package com.secondition.creeperindustry.content.logistics.storage;

import com.secondition.creeperindustry.CIBlockEntityTypes;
import com.secondition.creeperindustry.CIItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class DiscBurnerBlockEntity extends BlockEntity implements Container, MenuProvider {
    public static final int INPUT_SLOT = 0;
    public static final int DISC_SLOT = 1;
    public static final int SLOT_COUNT = 2;
    public static final int BURN_DURATION = 1;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> burnProgress;
                case 1 -> BURN_DURATION;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                burnProgress = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };

    private int burnProgress;

    public DiscBurnerBlockEntity(BlockPos pos, BlockState blockState) {
        super(CIBlockEntityTypes.DISC_BURNER.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, DiscBurnerBlockEntity burner) {
        burner.serverTick();
    }

    public ContainerData dataAccess() {
        return dataAccess;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        startOpen(player);
        return new DiscBurnerMenu(containerId, playerInventory, this, dataAccess);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(getBlockPos());
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            resetProgress();
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize(stack)) {
            stack.setCount(getMaxStackSize(stack));
        }
        resetProgress();
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == DISC_SLOT) {
            return isValidDisc(stack);
        }
        return slot == INPUT_SLOT;
    }

    @Override
    public void clearContent() {
        items.clear();
        resetProgress();
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        burnProgress = tag.getInt("BurnProgress");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("BurnProgress", burnProgress);
    }

    public static boolean isValidDisc(ItemStack stack) {
        return isVanillaRecord(stack) || stack.is(CIItems.STORAGE_DISC.get());
    }

    private void serverTick() {
        if (level == null || level.isClientSide()) {
            return;
        }

        if (!canBurn()) {
            if (burnProgress != 0) {
                resetProgress();
                setChanged();
            }
            return;
        }

        burnProgress++;
        if (burnProgress >= BURN_DURATION) {
            burnOne();
            resetProgress();
            setChanged();
        }
    }

    private boolean canBurn() {
        ItemStack input = items.get(INPUT_SLOT);
        ItemStack disc = items.get(DISC_SLOT);
        if (input.isEmpty() || disc.isEmpty()) {
            return false;
        }

        if (isVanillaRecord(disc)) {
            return true;
        }

        if (!disc.is(CIItems.STORAGE_DISC.get())) {
            return false;
        }

        return StorageDiscItem.canBurn(disc, input) && !StorageDiscItem.isFull(disc);
    }

    private void burnOne() {
        ItemStack input = items.get(INPUT_SLOT);
        ItemStack disc = items.get(DISC_SLOT);
        if (input.isEmpty() || disc.isEmpty()) {
            return;
        }

        if (isVanillaRecord(disc)) {
            ItemStack burnedDisc = new ItemStack(CIItems.STORAGE_DISC.get());
            StorageDiscItem.setContents(burnedDisc, new StorageDiscContents(input.copyWithCount(1), 1));
            items.set(DISC_SLOT, burnedDisc);
        } else if (disc.is(CIItems.STORAGE_DISC.get())) {
            StorageDiscContents contents = StorageDiscItem.getContents(disc);
            ItemStack storedItem = contents.isEmpty() ? input.copyWithCount(1) : contents.storedItem();
            StorageDiscItem.setContents(disc, new StorageDiscContents(storedItem, contents.storedCount() + 1));
        }

        input.shrink(1);
        if (input.isEmpty()) {
            items.set(INPUT_SLOT, ItemStack.EMPTY);
        }
    }

    private void resetProgress() {
        burnProgress = 0;
    }

    private static boolean isVanillaRecord(ItemStack stack) {
        return stack.has(DataComponents.JUKEBOX_PLAYABLE) && !stack.is(CIItems.STORAGE_DISC.get());
    }
}
