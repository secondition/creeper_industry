package com.secondition.creeperindustry.content.logistics.launcher;

import com.secondition.creeperindustry.CIBlockEntityTypes;
import com.secondition.creeperindustry.CIDataComponents;
import com.secondition.creeperindustry.CIItems;
import com.secondition.creeperindustry.content.energy.signal.AggregatedSignal;
import com.secondition.creeperindustry.content.energy.signal.SignalReceiver;
import com.secondition.creeperindustry.content.energy.signal.runtime.SignalRuntimeAccess;
import com.secondition.creeperindustry.content.logistics.rocket.RocketDestination;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
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

public class RocketLauncherBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider, SignalReceiver {
    public static final int ROCKET_SLOT = 0;
    public static final int CARGO_SLOT = 1;
    public static final int ADDRESS_SLOT = 2;
    public static final int SLOT_COUNT = 3;
    public static final int LAUNCH_COOLDOWN_TICKS = 10;
    private static final int[] TOP_SLOTS = {ROCKET_SLOT};
    private static final int[] SIDE_SLOTS = {CARGO_SLOT, ADDRESS_SLOT};
    private static final int[] NO_SLOTS = new int[0];

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private int cooldown;
    private boolean signalActive;
    private long lastLaunchAttemptGameTime = Long.MIN_VALUE;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? cooldown : 0;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                cooldown = Math.max(0, value);
            }
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

    public RocketLauncherBlockEntity(BlockPos pos, BlockState state) {
        super(CIBlockEntityTypes.ROCKET_LAUNCHER.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RocketLauncherBlockEntity launcher) {
        if (launcher.cooldown > 0) {
            launcher.cooldown--;
            if (launcher.cooldown == 0) {
                launcher.setChanged();
            }
        }
    }

    public RocketLaunchResult tryLaunch() {
        if (!(level instanceof ServerLevel serverLevel) || lastLaunchAttemptGameTime == level.getGameTime()) {
            return RocketLaunchResult.INVALID_LAUNCHER;
        }
        lastLaunchAttemptGameTime = level.getGameTime();
        RocketLaunchResult result = RocketLaunchService.tryLaunch(serverLevel, this);
        if (result == RocketLaunchResult.SUCCESS) {
            cooldown = LAUNCH_COOLDOWN_TICKS;
            setChanged();
        }
        return result;
    }

    public RocketDestination destination() {
        return items.get(ADDRESS_SLOT).get(CIDataComponents.ROCKET_DESTINATION.get());
    }

    public ContainerData dataAccess() {
        return dataAccess;
    }

    public int cooldown() {
        return cooldown;
    }

    @Override
    public void receiveSignal(AggregatedSignal signal) {
        boolean active = signal.signal().amplitude() > 0 && signal.instantaneousValue() > 0;
        if (active && !signalActive) {
            signalActive = true;
            tryLaunch();
        } else if (!active) {
            signalActive = false;
        }
    }

    @Override
    public void clearSignal() {
        signalActive = false;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            SignalRuntimeAccess.get(level).registerReceiver(worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        unregisterSignalReceiver();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        unregisterSignalReceiver();
        super.onChunkUnloaded();
    }

    private void unregisterSignalReceiver() {
        if (level != null && !level.isClientSide()) {
            SignalRuntimeAccess.getExisting(level).ifPresent(runtime -> runtime.unregisterReceiver(worldPosition));
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new RocketLauncherMenu(containerId, inventory, this, dataAccess);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(worldPosition);
        buffer.writeVarInt(cooldown);
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
        int limit = slot == ADDRESS_SLOT ? 1 : getMaxStackSize(stack);
        if (!stack.isEmpty() && stack.getCount() > limit) {
            stack.setCount(limit);
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case ROCKET_SLOT -> stack.is(CIItems.GUIDED_FIREWORK_ROCKET.get());
            case CARGO_SLOT -> stack.is(CIItems.STORAGE_DISC.get());
            case ADDRESS_SLOT -> stack.is(CIItems.RECEIVER_ADDRESS.get())
                    && stack.has(CIDataComponents.ROCKET_DESTINATION.get());
            default -> false;
        };
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        if (direction == Direction.UP) {
            return TOP_SLOTS;
        }
        return direction == Direction.DOWN ? NO_SLOTS : SIDE_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return false;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
        cooldown = Math.max(0, tag.getInt("Cooldown"));
        signalActive = false;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("Cooldown", cooldown);
    }
}
