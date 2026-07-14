package com.secondition.creeperindustry.content.production.biosphere;

import java.util.List;
import java.util.Map;

import com.secondition.creeperindustry.CIBlockEntityTypes;
import com.secondition.creeperindustry.content.energy.signal.AggregatedSignal;
import com.secondition.creeperindustry.content.energy.signal.SignalReceiver;
import com.secondition.creeperindustry.content.energy.signal.runtime.SignalRuntimeAccess;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
    private static final int[] BOTANICAL_INPUT_SLOTS = {SAPLING_SLOT, BONE_MEAL_SLOT};
    private static final int[] MONSTER_INPUT_SLOTS = {SAPLING_SLOT};
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
    private static final Map<Item, List<Item>> MONSTER_OUTPUTS = Map.ofEntries(
            Map.entry(Items.CREEPER_SPAWN_EGG, List.of(Items.GUNPOWDER)),
            Map.entry(Items.SKELETON_SPAWN_EGG, List.of(Items.BONE)),
            Map.entry(Items.STRAY_SPAWN_EGG, List.of(Items.BONE)),
            Map.entry(Items.WITHER_SKELETON_SPAWN_EGG, List.of(Items.BONE, Items.COAL)),
            Map.entry(Items.SPIDER_SPAWN_EGG, List.of(Items.STRING, Items.SPIDER_EYE)),
            Map.entry(Items.CAVE_SPIDER_SPAWN_EGG, List.of(Items.STRING, Items.SPIDER_EYE)),
            Map.entry(Items.ZOMBIE_SPAWN_EGG, List.of(Items.ROTTEN_FLESH)),
            Map.entry(Items.HUSK_SPAWN_EGG, List.of(Items.ROTTEN_FLESH)),
            Map.entry(Items.DROWNED_SPAWN_EGG, List.of(Items.ROTTEN_FLESH)),
            Map.entry(Items.ZOMBIE_VILLAGER_SPAWN_EGG, List.of(Items.ROTTEN_FLESH)),
            Map.entry(Items.ZOMBIFIED_PIGLIN_SPAWN_EGG, List.of(Items.ROTTEN_FLESH)),
            Map.entry(Items.SLIME_SPAWN_EGG, List.of(Items.SLIME_BALL)),
            Map.entry(Items.MAGMA_CUBE_SPAWN_EGG, List.of(Items.MAGMA_CREAM)),
            Map.entry(Items.ENDERMAN_SPAWN_EGG, List.of(Items.ENDER_PEARL)),
            Map.entry(Items.BLAZE_SPAWN_EGG, List.of(Items.BLAZE_ROD)),
            Map.entry(Items.GHAST_SPAWN_EGG, List.of(Items.GUNPOWDER)),
            Map.entry(Items.PHANTOM_SPAWN_EGG, List.of(Items.PHANTOM_MEMBRANE)),
            Map.entry(Items.SHULKER_SPAWN_EGG, List.of(Items.SHULKER_SHELL)),
            Map.entry(Items.GUARDIAN_SPAWN_EGG, List.of(Items.PRISMARINE_SHARD)),
            Map.entry(Items.ELDER_GUARDIAN_SPAWN_EGG, List.of(Items.PRISMARINE_SHARD)),
            Map.entry(Items.WITCH_SPAWN_EGG, List.of(
                    Items.GLASS_BOTTLE,
                    Items.GLOWSTONE_DUST,
                    Items.GUNPOWDER,
                    Items.REDSTONE,
                    Items.SPIDER_EYE,
                    Items.STICK,
                    Items.SUGAR
            ))
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
    private int selectedMonsterOutputIndex;

    public BiosphereBlockEntity(BlockPos pos, BlockState blockState) {
        super(CIBlockEntityTypes.BIOSPHERE.get(), pos, blockState);
    }

    public ContainerData dataAccess() {
        return dataAccess;
    }

    public ItemStack getDisplayedPrimaryInput() {
        BiosphereBlockEntity controller = getInventoryController();
        if (controller != this) {
            return controller.getDisplayedPrimaryInput();
        }
        return items.get(SAPLING_SLOT);
    }

    public ItemStack getSelectedMonsterOutputPreview() {
        BiosphereBlockEntity controller = getInventoryController();
        if (controller != this) {
            return controller.getSelectedMonsterOutputPreview();
        }
        return controller.resolveMonsterResult(controller.items.get(SAPLING_SLOT));
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
            normalizeSelectionAfterInventoryChange(slot);
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

        ItemStack removed = ContainerHelper.takeItem(items, slot);
        if (!removed.isEmpty()) {
            normalizeSelectionAfterInventoryChange(slot);
        }
        return removed;
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
        normalizeSelectionAfterInventoryChange(slot);
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

        return switch (getBiosphereType()) {
            case BOTANICAL -> switch (slot) {
                case SAPLING_SLOT -> isValidSapling(stack);
                case BONE_MEAL_SLOT -> isValidBoneMeal(stack);
                case OUTPUT_SLOT -> false;
                default -> false;
            };
            case MONSTER -> switch (slot) {
                case SAPLING_SLOT -> isValidMonsterEgg(stack);
                case BONE_MEAL_SLOT, OUTPUT_SLOT -> false;
                default -> false;
            };
            case ZOOLOGICAL -> false;
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
        selectedMonsterOutputIndex = 0;
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        signalActive = false;
        if (isControllerPart()) {
            ContainerHelper.loadAllItems(tag, items, registries);
            selectedMonsterOutputIndex = Math.max(tag.getInt("SelectedMonsterOutput"), 0);
            normalizeMonsterSelection();
        } else {
            selectedMonsterOutputIndex = 0;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (isControllerPart()) {
            ContainerHelper.saveAllItems(tag, items, registries);
            tag.putInt("SelectedMonsterOutput", selectedMonsterOutputIndex);
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

    @Override
    public void onLoad() {
        super.onLoad();
        registerReceiver();
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
        if (direction == Direction.DOWN) {
            return OUTPUT_SLOTS;
        }

        return switch (getBiosphereType()) {
            case BOTANICAL -> BOTANICAL_INPUT_SLOTS;
            case MONSTER -> MONSTER_INPUT_SLOTS;
            case ZOOLOGICAL -> BOTANICAL_INPUT_SLOTS;
        };
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
        if (!supportsSignalProduction()) {
            return false;
        }
        if (getBiosphereType() == BiosphereType.BOTANICAL && !isValidBoneMeal(items.get(BONE_MEAL_SLOT))) {
            return false;
        }

        ItemStack result = resolveProductionResult();
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

    public static boolean isValidMonsterEgg(ItemStack stack) {
        return MONSTER_OUTPUTS.containsKey(stack.getItem());
    }

    public static boolean isValidPrimaryInput(BiosphereType type, ItemStack stack) {
        return switch (type) {
            case BOTANICAL -> isValidSapling(stack);
            case MONSTER -> isValidMonsterEgg(stack);
            case ZOOLOGICAL -> false;
        };
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

        ItemStack result = resolveProductionResult();
        ItemStack output = items.get(OUTPUT_SLOT);
        if (level != null && lastProductionGameTime == level.getGameTime()) {
            return;
        }

        if (getBiosphereType() == BiosphereType.BOTANICAL) {
            ItemStack boneMeal = items.get(BONE_MEAL_SLOT);
            boneMeal.shrink(1);
            if (boneMeal.isEmpty()) {
                items.set(BONE_MEAL_SLOT, ItemStack.EMPTY);
            }
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
        return supportsSignalProduction()
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
        SignalRuntimeAccess.get(level).registerReceiver(level, worldPosition);
    }

    private void unregisterReceiver() {
        if (level == null || level.isClientSide()) {
            return;
        }
        SignalRuntimeAccess.getExisting(level).ifPresent(runtime -> runtime.unregisterReceiver(worldPosition));
    }

    public boolean cycleMonsterOutputSelection(Player player) {
        BiosphereBlockEntity controller = getInventoryController();
        if (controller != this) {
            return controller.cycleMonsterOutputSelection(player);
        }

        if (getBiosphereType() != BiosphereType.MONSTER) {
            return false;
        }

        List<Item> options = getMonsterOutputOptions(items.get(SAPLING_SLOT));
        if (options.isEmpty()) {
            player.displayClientMessage(Component.translatable(
                    "message.creeper_industry.monster_biosphere.no_output"
            ).withStyle(ChatFormatting.RED), false);
            return true;
        }

        selectedMonsterOutputIndex = Math.floorMod(selectedMonsterOutputIndex + 1, options.size());
        setChanged();
        player.displayClientMessage(Component.translatable(
                "message.creeper_industry.monster_biosphere.selected_drop",
                resolveMonsterResult(items.get(SAPLING_SLOT)).getHoverName()
        ).withStyle(ChatFormatting.AQUA), false);
        return true;
    }

    private boolean supportsSignalProduction() {
        return getBiosphereType() == BiosphereType.BOTANICAL
                || getBiosphereType() == BiosphereType.MONSTER;
    }

    private ItemStack resolveProductionResult() {
        return switch (getBiosphereType()) {
            case BOTANICAL -> getBotanicalResult(items.get(SAPLING_SLOT));
            case MONSTER -> resolveMonsterResult(items.get(SAPLING_SLOT));
            case ZOOLOGICAL -> ItemStack.EMPTY;
        };
    }

    private ItemStack resolveMonsterResult(ItemStack spawnEggStack) {
        List<Item> options = getMonsterOutputOptions(spawnEggStack);
        if (options.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int index = Math.floorMod(selectedMonsterOutputIndex, options.size());
        return new ItemStack(options.get(index));
    }

    private void normalizeSelectionAfterInventoryChange(int slot) {
        if (slot == SAPLING_SLOT) {
            normalizeMonsterSelection();
        }
    }

    private void normalizeMonsterSelection() {
        if (getBiosphereType() != BiosphereType.MONSTER) {
            selectedMonsterOutputIndex = 0;
            return;
        }

        List<Item> options = getMonsterOutputOptions(items.get(SAPLING_SLOT));
        if (options.isEmpty()) {
            selectedMonsterOutputIndex = 0;
            return;
        }

        selectedMonsterOutputIndex = Math.floorMod(selectedMonsterOutputIndex, options.size());
    }

    private static List<Item> getMonsterOutputOptions(ItemStack spawnEggStack) {
        if (spawnEggStack.isEmpty()) {
            return List.of();
        }
        return MONSTER_OUTPUTS.getOrDefault(spawnEggStack.getItem(), List.of());
    }
}
