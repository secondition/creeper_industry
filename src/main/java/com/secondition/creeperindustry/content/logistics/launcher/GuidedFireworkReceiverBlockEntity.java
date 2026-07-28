package com.secondition.creeperindustry.content.logistics.launcher;

import java.util.UUID;

import com.secondition.creeperindustry.CIBlockEntityTypes;
import com.secondition.creeperindustry.CIDataComponents;
import com.secondition.creeperindustry.CIItems;
import com.secondition.creeperindustry.CIMenuTypes;
import com.secondition.creeperindustry.content.logistics.rocket.RocketDestination;
import com.secondition.creeperindustry.content.logistics.storage.StorageDiscContents;
import com.secondition.creeperindustry.content.logistics.storage.StorageDiscItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class GuidedFireworkReceiverBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    public static final int DISC_SLOT_COUNT = 5;
    public static final int OUTPUT_SLOT_START = DISC_SLOT_COUNT;
    public static final int OUTPUT_SLOT_COUNT = 5;
    public static final int SLOT_COUNT = DISC_SLOT_COUNT + OUTPUT_SLOT_COUNT;
    private static final int[] NO_SLOTS = new int[0];
    private static final int[] DISC_SLOTS = {0, 1, 2, 3, 4};
    private static final int[] OUTPUT_SLOTS = {5, 6, 7, 8, 9};

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private UUID receiverId;
    private GuidedFireworkReceiverMode mode = GuidedFireworkReceiverMode.STORAGE;
    private int discCursor;
    private int outputCursor;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> mode.ordinal();
                case 1 -> discCursor;
                case 2 -> outputCursor;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> mode = GuidedFireworkReceiverMode.fromId(value);
                case 1 -> discCursor = Math.floorMod(value, DISC_SLOT_COUNT);
                case 2 -> outputCursor = Math.floorMod(value, OUTPUT_SLOT_COUNT);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public GuidedFireworkReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(CIBlockEntityTypes.GUIDED_FIREWORK_RECEIVER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GuidedFireworkReceiverBlockEntity receiver) {
        if (!level.isClientSide() && receiver.mode == GuidedFireworkReceiverMode.UNLOADING) {
            receiver.tryDecompressOneBatch();
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel && receiverId == null) {
            receiverId = UUID.randomUUID();
            setChanged();
        }
    }

    public UUID receiverId() {
        if (receiverId == null) {
            receiverId = UUID.randomUUID();
            setChanged();
        }
        return receiverId;
    }

    public GuidedFireworkReceiverMode mode() {
        return mode;
    }

    public ContainerData dataAccess() {
        return dataAccess;
    }

    public void toggleMode() {
        mode = mode.toggle();
        setChanged();
        if (level != null) {
            level.invalidateCapabilities(worldPosition);
        }
    }

    public ItemStack createAddress() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return ItemStack.EMPTY;
        }
        ItemStack address = new ItemStack(CIItems.RECEIVER_ADDRESS.get());
        address.set(CIDataComponents.ROCKET_DESTINATION.get(), new RocketDestination(
                serverLevel.dimension(),
                worldPosition,
                receiverId()
        ));
        return address;
    }

    public ReceptionResult acceptRocketCargo(RocketDestination destination, ItemStack cargo) {
        if (!(level instanceof ServerLevel serverLevel)
                || !serverLevel.dimension().equals(destination.dimension())
                || !worldPosition.equals(destination.receiverPos())
                || !receiverId().equals(destination.receiverId())
                || !cargo.is(CIItems.STORAGE_DISC.get())) {
            return ReceptionResult.INVALID_TARGET;
        }
        for (int slot = 0; slot < DISC_SLOT_COUNT; slot++) {
            if (items.get(slot).isEmpty()) {
                items.set(slot, cargo);
                setChanged();
                return ReceptionResult.ACCEPTED;
            }
        }
        return ReceptionResult.FULL;
    }

    private void tryDecompressOneBatch() {
        int discSlot = findNextDiscSlot();
        if (discSlot < 0) {
            return;
        }
        ItemStack disc = items.get(discSlot);
        StorageDiscContents contents = StorageDiscItem.getContents(disc);
        if (contents.isEmpty()) {
            items.set(discSlot, ItemStack.EMPTY);
            advanceDiscCursor();
            setChanged();
            return;
        }

        int batch = Math.min(contents.storedCount(), contents.storedItem().getMaxStackSize());
        for (int checked = 0; checked < OUTPUT_SLOT_COUNT; checked++) {
            int logicalOutput = Math.floorMod(outputCursor + checked, OUTPUT_SLOT_COUNT);
            int outputSlot = OUTPUT_SLOT_START + logicalOutput;
            ItemStack output = items.get(outputSlot);
            int movable = movableAmount(output, contents.storedItem(), batch);
            if (movable <= 0) {
                continue;
            }

            if (output.isEmpty()) {
                items.set(outputSlot, contents.storedItem().copyWithCount(movable));
            } else {
                output.grow(movable);
            }
            int remaining = contents.storedCount() - movable;
            if (remaining <= 0) {
                items.set(discSlot, ItemStack.EMPTY);
                advanceDiscCursor();
                outputCursor = (logicalOutput + 1) % OUTPUT_SLOT_COUNT;
            } else {
                StorageDiscItem.setContents(disc, contents.withCount(remaining));
                outputCursor = items.get(outputSlot).getCount() >= items.get(outputSlot).getMaxStackSize()
                        ? (logicalOutput + 1) % OUTPUT_SLOT_COUNT
                        : logicalOutput;
            }
            setChanged();
            return;
        }
    }

    private int findNextDiscSlot() {
        for (int checked = 0; checked < DISC_SLOT_COUNT; checked++) {
            int slot = Math.floorMod(discCursor + checked, DISC_SLOT_COUNT);
            if (items.get(slot).is(CIItems.STORAGE_DISC.get())) {
                discCursor = slot;
                return slot;
            }
        }
        return -1;
    }

    private static int movableAmount(ItemStack output, ItemStack storedItem, int batch) {
        if (output.isEmpty()) {
            return Math.min(batch, storedItem.getMaxStackSize());
        }
        if (!ItemStack.isSameItemSameComponents(output, storedItem)) {
            return 0;
        }
        return Math.min(batch, output.getMaxStackSize() - output.getCount());
    }

    private void advanceDiscCursor() {
        discCursor = (discCursor + 1) % DISC_SLOT_COUNT;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new GuidedFireworkReceiverMenu(containerId, inventory, this, dataAccess);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(worldPosition);
        buffer.writeVarInt(mode.ordinal());
        buffer.writeVarInt(discCursor);
        buffer.writeVarInt(outputCursor);
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
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
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= 0 && slot < DISC_SLOT_COUNT && stack.is(CIItems.STORAGE_DISC.get());
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        if (direction != Direction.DOWN) {
            return NO_SLOTS;
        }
        return mode == GuidedFireworkReceiverMode.STORAGE ? DISC_SLOTS : OUTPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return false;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return direction == Direction.DOWN && (mode == GuidedFireworkReceiverMode.STORAGE
                ? slot < DISC_SLOT_COUNT
                : slot >= OUTPUT_SLOT_START);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        receiverId = tag.hasUUID("ReceiverId") ? tag.getUUID("ReceiverId") : null;
        mode = GuidedFireworkReceiverMode.fromId(tag.getInt("Mode"));
        discCursor = Math.floorMod(tag.getInt("DiscCursor"), DISC_SLOT_COUNT);
        outputCursor = Math.floorMod(tag.getInt("OutputCursor"), OUTPUT_SLOT_COUNT);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putUUID("ReceiverId", receiverId());
        tag.putInt("Mode", mode.ordinal());
        tag.putInt("DiscCursor", discCursor);
        tag.putInt("OutputCursor", outputCursor);
    }

    public enum ReceptionResult {
        ACCEPTED,
        FULL,
        INVALID_TARGET
    }
}
