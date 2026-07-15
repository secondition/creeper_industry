package com.secondition.creeperindustry.content.production.biosphere;

import java.util.Optional;

import com.secondition.creeperindustry.CIBlockEntityTypes;
import com.secondition.creeperindustry.content.energy.signal.AggregatedSignal;
import com.secondition.creeperindustry.content.energy.signal.SignalReceiver;
import com.secondition.creeperindustry.content.energy.signal.runtime.SignalRuntimeAccess;
import com.secondition.creeperindustry.content.production.biosphere.recipe.BiosphereCultivationRecipe;
import com.secondition.creeperindustry.content.production.biosphere.recipe.BiosphereRecipeService;

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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class BiosphereBlockEntity extends BlockEntity implements Container, MenuProvider, SignalReceiver, WorldlyContainer {
    public static final int TEMPLATE_SLOT = 0;
    public static final int CATALYST_SLOT = 1;
    public static final int OUTPUT_SLOT = 2;
    public static final int SLOT_COUNT = 3;

    private static final int[] INPUT_SLOTS = {TEMPLATE_SLOT, CATALYST_SLOT};
    private static final int[] OUTPUT_SLOTS = {OUTPUT_SLOT};

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
    private int selectedOutputIndex;

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
        return items.get(TEMPLATE_SLOT);
    }

    public ItemStack getSelectedOutputPreview() {
        BiosphereBlockEntity controller = getInventoryController();
        if (controller != this) {
            return controller.getSelectedOutputPreview();
        }
        return controller.findTemplateRecipe()
                .map(holder -> holder.value().selectedOutput(selectedOutputIndex))
                .orElse(ItemStack.EMPTY);
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
        if (level == null || stack.isEmpty()) {
            return false;
        }
        return switch (slot) {
            case TEMPLATE_SLOT -> BiosphereRecipeService.isValidTemplate(level, getBiosphereType(), stack);
            case CATALYST_SLOT -> BiosphereRecipeService.isValidCatalyst(level, getBiosphereType(), stack);
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
        selectedOutputIndex = 0;
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        signalActive = false;
        if (isControllerPart()) {
            ContainerHelper.loadAllItems(tag, items, registries);
            selectedOutputIndex = tag.contains("SelectedOutput")
                    ? Math.max(tag.getInt("SelectedOutput"), 0)
                    : Math.max(tag.getInt("SelectedMonsterOutput"), 0);
        } else {
            selectedOutputIndex = 0;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (isControllerPart()) {
            ContainerHelper.saveAllItems(tag, items, registries);
            tag.putInt("SelectedOutput", selectedOutputIndex);
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
        if (isControllerPart()) {
            normalizeOutputSelection();
        }
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
        BiosphereBlockEntity controller = getInventoryController();
        boolean strongSignal = level != null
                && BiosphereRecipeService.hasMatchingSignal(
                        level,
                        controller.getBiosphereType(),
                        controller.items.get(TEMPLATE_SLOT),
                        signal
                );
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
        return INPUT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return slot != OUTPUT_SLOT && canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return direction == Direction.DOWN && slot == OUTPUT_SLOT;
    }

    private boolean canProcessSignalTrigger(BiosphereCultivationRecipe recipe, ItemStack result) {
        if (result.isEmpty()) {
            return false;
        }
        if (recipe.catalyst().isPresent() && !recipe.catalyst().get().matches(items.get(CATALYST_SLOT))) {
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

    private void tryTriggerProduction() {
        BiosphereBlockEntity controller = getInventoryController();
        if (controller != this) {
            controller.tryTriggerProduction();
            return;
        }

        Optional<RecipeHolder<BiosphereCultivationRecipe>> recipeHolder = findCurrentRecipe();
        if (recipeHolder.isEmpty()) {
            return;
        }
        BiosphereCultivationRecipe recipe = recipeHolder.get().value();
        ItemStack result = recipe.selectedOutput(selectedOutputIndex);
        if (!canProcessSignalTrigger(recipe, result)) {
            return;
        }

        ItemStack output = items.get(OUTPUT_SLOT);
        if (level != null && lastProductionGameTime == level.getGameTime()) {
            return;
        }

        recipe.catalyst().ifPresent(catalyst -> {
            ItemStack catalystStack = items.get(CATALYST_SLOT);
            catalystStack.shrink(catalyst.count());
            if (catalystStack.isEmpty()) {
                items.set(CATALYST_SLOT, ItemStack.EMPTY);
            }
        });

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
        SignalRuntimeAccess.get(level).registerReceiver(worldPosition);
    }

    private void unregisterReceiver() {
        if (level == null || level.isClientSide()) {
            return;
        }
        SignalRuntimeAccess.getExisting(level).ifPresent(runtime -> runtime.unregisterReceiver(worldPosition));
    }

    public boolean cycleOutputSelection(Player player) {
        BiosphereBlockEntity controller = getInventoryController();
        if (controller != this) {
            return controller.cycleOutputSelection(player);
        }

        if (getBiosphereType() == BiosphereType.BOTANICAL) {
            return false;
        }

        Optional<RecipeHolder<BiosphereCultivationRecipe>> recipe = findTemplateRecipe();
        if (recipe.isEmpty() || recipe.get().value().outputs().isEmpty()) {
            player.displayClientMessage(Component.translatable(
                    "message.creeper_industry.biosphere.no_output"
            ).withStyle(ChatFormatting.RED), false);
            return true;
        }

        selectedOutputIndex = Math.floorMod(selectedOutputIndex + 1, recipe.get().value().outputs().size());
        setChanged();
        player.displayClientMessage(Component.translatable(
                "message.creeper_industry.biosphere.selected_output",
                recipe.get().value().selectedOutput(selectedOutputIndex).getHoverName()
        ).withStyle(ChatFormatting.AQUA), false);
        return true;
    }

    private void normalizeSelectionAfterInventoryChange(int slot) {
        if (slot == TEMPLATE_SLOT || slot == CATALYST_SLOT) {
            normalizeOutputSelection();
        }
    }

    private void normalizeOutputSelection() {
        if (level == null || getBiosphereType() == BiosphereType.BOTANICAL) {
            selectedOutputIndex = 0;
            return;
        }

        Optional<RecipeHolder<BiosphereCultivationRecipe>> recipe = findTemplateRecipe();
        if (recipe.isEmpty() || recipe.get().value().outputs().isEmpty()) {
            selectedOutputIndex = 0;
            return;
        }

        selectedOutputIndex = Math.floorMod(selectedOutputIndex, recipe.get().value().outputs().size());
    }

    private Optional<RecipeHolder<BiosphereCultivationRecipe>> findCurrentRecipe() {
        if (level == null) {
            return Optional.empty();
        }
        return BiosphereRecipeService.findRecipe(
                level,
                getBiosphereType(),
                items.get(TEMPLATE_SLOT),
                items.get(CATALYST_SLOT)
        );
    }

    private Optional<RecipeHolder<BiosphereCultivationRecipe>> findTemplateRecipe() {
        if (level == null) {
            return Optional.empty();
        }
        return BiosphereRecipeService.findRecipeForTemplate(
                level,
                getBiosphereType(),
                items.get(TEMPLATE_SLOT)
        );
    }
}
