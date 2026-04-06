package com.secondition.creeperindustry.content.production.biosphere;

import java.util.Map;

import com.secondition.creeperindustry.CIBlockEntityTypes;
import com.secondition.creeperindustry.CISignalSourceTypes;
import com.secondition.creeperindustry.content.energy.signal.AggregatedSignal;
import com.secondition.creeperindustry.content.energy.signal.SignalReceiver;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BiosphereBlockEntity extends BlockEntity implements Container, MenuProvider, SignalReceiver, WorldlyContainer {
    public static final int SAPLING_SLOT = 0;
    public static final int BONE_MEAL_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int SLOT_COUNT = 3;

    private static final int SIGNAL_THRESHOLD = 8;
    private static final int[] INPUT_SLOTS = {SAPLING_SLOT, BONE_MEAL_SLOT};
    private static final int[] OUTPUT_SLOTS = {OUTPUT_SLOT};
    private static final Map<Item, ItemStack> BOTANICAL_OUTPUTS = Map.ofEntries(
            Map.entry(Blocks.OAK_SAPLING.asItem(), new ItemStack(Blocks.OAK_LOG)),
            Map.entry(Blocks.SPRUCE_SAPLING.asItem(), new ItemStack(Blocks.SPRUCE_LOG)),
            Map.entry(Blocks.BIRCH_SAPLING.asItem(), new ItemStack(Blocks.BIRCH_LOG)),
            Map.entry(Blocks.JUNGLE_SAPLING.asItem(), new ItemStack(Blocks.JUNGLE_LOG)),
            Map.entry(Blocks.ACACIA_SAPLING.asItem(), new ItemStack(Blocks.ACACIA_LOG)),
            Map.entry(Blocks.DARK_OAK_SAPLING.asItem(), new ItemStack(Blocks.DARK_OAK_LOG)),
            Map.entry(Blocks.CHERRY_SAPLING.asItem(), new ItemStack(Blocks.CHERRY_LOG)),
            Map.entry(Blocks.MANGROVE_PROPAGULE.asItem(), new ItemStack(Blocks.MANGROVE_LOG)),
            Map.entry(Blocks.AZALEA.asItem(), new ItemStack(Blocks.OAK_LOG)),
            Map.entry(Blocks.FLOWERING_AZALEA.asItem(), new ItemStack(Blocks.OAK_LOG))
    );

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return 0;
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 0;
        }
    };

    private boolean signalActive;
    private long lastProductionGameTime = Long.MIN_VALUE;

    public BiosphereBlockEntity(BlockPos pos, BlockState blockState) {
        super(CIBlockEntityTypes.BIOSPHERE.get(), pos, blockState);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BiosphereBlockEntity biosphere) {
        biosphere.serverTick();
    }

    public ContainerData dataAccess() {
        return dataAccess;
    }

    public ItemStack getDisplayedSapling() {
        BiosphereBlockEntity controller = getInventoryController();
        if (controller != this) {
            return controller.getDisplayedSapling();
        }
        return items.get(SAPLING_SLOT);
    }

    public BiosphereType getBiosphereType() {
        return getBlockState().getBlock() instanceof BiosphereBlock biosphereBlock
                ? biosphereBlock.getType()
                : BiosphereType.BOTANICAL;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        BiosphereBlockEntity controller = getControllerEntity();
        if (controller != null && controller != this) {
            return controller.createMenu(containerId, playerInventory, player);
        }
        startOpen(player);
        return new BiosphereMenu(containerId, playerInventory, this, dataAccess);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        BiosphereBlockEntity controller = getControllerEntity();
        buffer.writeBlockPos(controller != null ? controller.getBlockPos() : getBlockPos());
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        BiosphereBlockEntity controller = getInventoryController();
        if (controller != this) {
            return controller.isEmpty();
        }

        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        BiosphereBlockEntity controller = getInventoryController();
        if (controller != this) {
            return controller.getItem(slot);
        }
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        BiosphereBlockEntity controller = getInventoryController();
        if (controller != this) {
            return controller.removeItem(slot, amount);
        }

        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        BiosphereBlockEntity controller = getInventoryController();
        if (controller != this) {
            return controller.removeItemNoUpdate(slot);
        }
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        BiosphereBlockEntity controller = getInventoryController();
        if (controller != this) {
            controller.setItem(slot, stack);
            return;
        }

        items.set(slot, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize(stack)) {
            stack.setCount(getMaxStackSize(stack));
        }
        setChanged();
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public boolean stillValid(Player player) {
        BiosphereBlockEntity controller = getControllerEntity();
        if (controller != null && controller != this) {
            return controller.stillValid(player);
        }
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        BiosphereBlockEntity controller = getInventoryController();
        if (controller != this) {
            return controller.canPlaceItem(slot, stack);
        }

        if (getBiosphereType() != BiosphereType.BOTANICAL) {
            return false;
        }
        return switch (slot) {
            case SAPLING_SLOT -> isValidSapling(stack);
            case BONE_MEAL_SLOT -> isValidBoneMeal(stack);
            case OUTPUT_SLOT -> false;
            default -> false;
        };
    }

    @Override
    public void clearContent() {
        BiosphereBlockEntity controller = getInventoryController();
        if (controller != this) {
            controller.clearContent();
            return;
        }

        for (int i = 0; i < items.size(); i++) {
            items.set(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        signalActive = false;
        if (isControllerPart()) {
            ContainerHelper.loadAllItems(tag, items, registries);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (isControllerPart()) {
            ContainerHelper.saveAllItems(tag, items, registries);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return isControllerPart() ? saveWithoutMetadata(registries) : super.getUpdateTag(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return isControllerPart() ? ClientboundBlockEntityDataPacket.create(this) : super.getUpdatePacket();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void serverTick() {
        if (level == null || level.isClientSide()) {
            return;
        }

        if (CISignalSourceTypes.continuousSignalSourceRepository().getActiveSources(level.dimension()).isEmpty()) {
            return;
        }

        CISignalSourceTypes.unifiedSignalRefreshService().refreshTarget(level, worldPosition, level.getGameTime());
    }

    @Override
    public void onLoad() {
        super.onLoad();
        registerReceiver();
        refreshOnFirstLoad();
    }

    @Override
    public void setRemoved() {
        unregisterReceiver();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        unregisterReceiver();
        super.onChunkUnloaded();
    }

    @Override
    public void receiveSignal(AggregatedSignal signal) {
        boolean strongSignal = isProductionSignal(signal);
        if (strongSignal && !signalActive) {
            signalActive = true;
            tryTriggerProduction();
            return;
        }

        if (!strongSignal) {
            signalActive = false;
        }
    }

    @Override
    public void clearSignal() {
        signalActive = false;
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        return direction == Direction.DOWN ? OUTPUT_SLOTS : INPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return slot != OUTPUT_SLOT && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return direction == Direction.DOWN && slot == OUTPUT_SLOT;
    }

    private boolean canProcessSignalTrigger() {
        if (getBiosphereType() != BiosphereType.BOTANICAL) {
            return false;
        }
        if (!isValidBoneMeal(items.get(BONE_MEAL_SLOT))) {
            return false;
        }

        ItemStack result = getBotanicalResult(items.get(SAPLING_SLOT));
        if (result.isEmpty()) {
            return false;
        }

        ItemStack output = items.get(OUTPUT_SLOT);
        if (output.isEmpty()) {
            return true;
        }
        if (!ItemStack.isSameItemSameComponents(output, result)) {
            return false;
        }
        return output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    public static boolean isValidBoneMeal(ItemStack stack) {
        return stack.is(Items.BONE_MEAL);
    }

    public static boolean isValidSapling(ItemStack stack) {
        return BOTANICAL_OUTPUTS.containsKey(stack.getItem());
    }

    public static ItemStack getBotanicalResult(ItemStack saplingStack) {
        ItemStack result = BOTANICAL_OUTPUTS.get(saplingStack.getItem());
        return result == null ? ItemStack.EMPTY : result.copy();
    }

    private void tryTriggerProduction() {
        BiosphereBlockEntity controller = getInventoryController();
        if (controller != this) {
            controller.tryTriggerProduction();
            return;
        }

        if (!canProcessSignalTrigger()) {
            return;
        }

        ItemStack result = getBotanicalResult(items.get(SAPLING_SLOT));
        ItemStack boneMeal = items.get(BONE_MEAL_SLOT);
        ItemStack output = items.get(OUTPUT_SLOT);
        if (level != null && lastProductionGameTime == level.getGameTime()) {
            return;
        }

        boneMeal.shrink(1);
        if (boneMeal.isEmpty()) {
            items.set(BONE_MEAL_SLOT, ItemStack.EMPTY);
        }

        if (output.isEmpty()) {
            items.set(OUTPUT_SLOT, result.copy());
        } else {
            output.grow(result.getCount());
        }

        if (level != null) {
            lastProductionGameTime = level.getGameTime();
        }
        setChanged();
    }

    private boolean isProductionSignal(AggregatedSignal signal) {
        return getBiosphereType() == BiosphereType.BOTANICAL
                && signal.signal().amplitude() > SIGNAL_THRESHOLD
                && signal.instantaneousValue() > 0;
    }

    private boolean isControllerPart() {
        return BiosphereBlock.isController(getBlockState());
    }

    private BiosphereBlockEntity getInventoryController() {
        BiosphereBlockEntity controller = getControllerEntity();
        return controller != null ? controller : this;
    }

    private BiosphereBlockEntity getControllerEntity() {
        if (level == null) {
            return isControllerPart() ? this : null;
        }

        BlockPos controllerPos = BiosphereBlock.getControllerPos(worldPosition, getBlockState());
        BlockEntity blockEntity = level.getBlockEntity(controllerPos);
        if (blockEntity instanceof BiosphereBlockEntity biosphere) {
            return biosphere;
        }
        return isControllerPart() ? this : null;
    }

    private void registerReceiver() {
        if (level == null || level.isClientSide()) {
            return;
        }
        CISignalSourceTypes.signalReceiverIndex().register(level.dimension(), worldPosition);
    }

    private void unregisterReceiver() {
        if (level == null || level.isClientSide()) {
            return;
        }
        CISignalSourceTypes.signalReceiverIndex().unregister(level.dimension(), worldPosition);
    }

    private void refreshOnFirstLoad() {
        Level currentLevel = level;
        if (currentLevel == null || currentLevel.isClientSide()) {
            return;
        }

        if (!CISignalSourceTypes.continuousSignalSourceRepository().getActiveSources(currentLevel.dimension()).isEmpty()) {
            CISignalSourceTypes.unifiedSignalRefreshService().refreshTarget(currentLevel, worldPosition);
        }
    }
}
