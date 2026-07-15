package com.secondition.creeperindustry.content.production.biosphere;

import javax.annotation.Nullable;

import com.secondition.creeperindustry.CIMenuTypes;
import com.secondition.creeperindustry.content.production.biosphere.recipe.BiosphereRecipeService;

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

public class BiosphereMenu extends AbstractContainerMenu {
    private static final int TEMPLATE_SLOT_INDEX = 0;
    private static final int CATALYST_SLOT_INDEX = 1;
    private static final int OUTPUT_SLOT_INDEX = 2;
    private static final int MACHINE_SLOT_COUNT = 3;
    private static final int PLAYER_INV_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 27;
    private static final int HOTBAR_START = PLAYER_INV_END;
    private static final int HOTBAR_END = HOTBAR_START + 9;

    private final Container biosphere;
    private final BiosphereType biosphereType;

    public BiosphereMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, playerInventory, createClientContext(playerInventory, extraData));
    }

    private BiosphereMenu(int containerId, Inventory playerInventory, ClientContext context) {
        this(containerId, playerInventory, context.container(), context.data(), context.type());
    }

    public BiosphereMenu(int containerId, Inventory playerInventory, Container biosphere, ContainerData data) {
        this(containerId, playerInventory, biosphere, data, resolveBiosphereType(biosphere));
    }

    private BiosphereMenu(int containerId, Inventory playerInventory, Container biosphere, ContainerData data, BiosphereType biosphereType) {
        super(CIMenuTypes.BIOSPHERE.get(), containerId);
        checkContainerSize(biosphere, MACHINE_SLOT_COUNT);
        checkContainerDataCount(data, 0);
        this.biosphere = biosphere;
        this.biosphereType = biosphereType;

        addSlot(new Slot(biosphere, TEMPLATE_SLOT_INDEX, 44, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return BiosphereRecipeService.isValidTemplate(
                        playerInventory.player.level(),
                        BiosphereMenu.this.biosphereType,
                        stack
                );
            }
        });
        addSlot(new Slot(biosphere, CATALYST_SLOT_INDEX, 80, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return BiosphereRecipeService.isValidCatalyst(
                        playerInventory.player.level(),
                        BiosphereMenu.this.biosphereType,
                        stack
                );
            }
        });
        addSlot(new Slot(biosphere, OUTPUT_SLOT_INDEX, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
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

        if (index < MACHINE_SLOT_COUNT) {
            if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (BiosphereRecipeService.isValidTemplate(player.level(), biosphereType, stackInSlot)) {
            if (!moveItemStackTo(stackInSlot, TEMPLATE_SLOT_INDEX, TEMPLATE_SLOT_INDEX + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (BiosphereRecipeService.isValidCatalyst(player.level(), biosphereType, stackInSlot)) {
            if (!moveItemStackTo(stackInSlot, CATALYST_SLOT_INDEX, CATALYST_SLOT_INDEX + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < PLAYER_INV_END) {
            if (!moveItemStackTo(stackInSlot, HOTBAR_START, HOTBAR_END, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stackInSlot, PLAYER_INV_START, PLAYER_INV_END, false)) {
            return ItemStack.EMPTY;
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
        return biosphere.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        biosphere.stopOpen(player);
    }

    public BiosphereType getBiosphereType() {
        return biosphereType;
    }

    @Nullable
    public BiosphereBlockEntity getBiosphereBlockEntity() {
        return biosphere instanceof BiosphereBlockEntity biosphereBlockEntity ? biosphereBlockEntity : null;
    }

    private static ClientContext createClientContext(Inventory playerInventory, @Nullable RegistryFriendlyByteBuf extraData) {
        if (extraData == null) {
            return new ClientContext(new SimpleContainer(MACHINE_SLOT_COUNT), new SimpleContainerData(0), BiosphereType.BOTANICAL);
        }

        BlockPos pos = extraData.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof BiosphereBlockEntity biosphere) {
            return new ClientContext(biosphere, biosphere.dataAccess(), biosphere.getBiosphereType());
        }
        return new ClientContext(new SimpleContainer(MACHINE_SLOT_COUNT), new SimpleContainerData(0), BiosphereType.BOTANICAL);
    }

    private static BiosphereType resolveBiosphereType(Container biosphere) {
        return biosphere instanceof BiosphereBlockEntity biosphereBlockEntity
                ? biosphereBlockEntity.getBiosphereType()
                : BiosphereType.BOTANICAL;
    }

    private record ClientContext(Container container, ContainerData data, BiosphereType type) {
    }
}
