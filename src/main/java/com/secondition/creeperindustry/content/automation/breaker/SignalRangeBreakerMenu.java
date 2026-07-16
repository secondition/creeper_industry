package com.secondition.creeperindustry.content.automation.breaker;

import com.secondition.creeperindustry.CIBlocks;
import com.secondition.creeperindustry.CIMenuTypes;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public class SignalRangeBreakerMenu extends AbstractContainerMenu {
    private static final int BUTTON_TOGGLE_WIDTH = 0;
    private static final int BUTTON_TOGGLE_HEIGHT = 1;
    private static final int BUTTON_TOGGLE_DEPTH = 2;

    @Nullable
    private final SignalRangeBreakerBlockEntity breaker;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    public SignalRangeBreakerMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf extraData) {
        this(containerId, inventory, createClientContext(inventory, extraData));
    }

    private SignalRangeBreakerMenu(int containerId, Inventory inventory, ClientContext context) {
        this(containerId, inventory, context.breaker(), context.data());
    }

    public SignalRangeBreakerMenu(
            int containerId,
            Inventory inventory,
            @Nullable SignalRangeBreakerBlockEntity breaker,
            ContainerData data
    ) {
        super(CIMenuTypes.SIGNAL_RANGE_BREAKER.get(), containerId);
        checkContainerDataCount(data, 3);
        this.breaker = breaker;
        this.data = data;
        this.access = breaker != null && breaker.getLevel() != null
                ? ContainerLevelAccess.create(breaker.getLevel(), breaker.getBlockPos())
                : ContainerLevelAccess.NULL;
        addDataSlots(data);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (breaker == null || !stillValid(player)) {
            return false;
        }
        switch (id) {
            case BUTTON_TOGGLE_WIDTH -> breaker.toggleWidth();
            case BUTTON_TOGGLE_HEIGHT -> breaker.toggleHeight();
            case BUTTON_TOGGLE_DEPTH -> breaker.toggleDepth();
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (breaker == null) {
            return true;
        }
        return AbstractContainerMenu.stillValid(access, player, CIBlocks.SIGNAL_RANGE_BREAKER.get());
    }

    public SignalRange range() {
        return new SignalRange(data.get(0), data.get(1), data.get(2));
    }

    public static int toggleWidthButtonId() {
        return BUTTON_TOGGLE_WIDTH;
    }

    public static int toggleHeightButtonId() {
        return BUTTON_TOGGLE_HEIGHT;
    }

    public static int toggleDepthButtonId() {
        return BUTTON_TOGGLE_DEPTH;
    }

    private static ClientContext createClientContext(Inventory inventory, @Nullable RegistryFriendlyByteBuf extraData) {
        SimpleContainerData data = new SimpleContainerData(3);
        if (extraData == null) {
            return new ClientContext(null, data);
        }

        BlockPos pos = extraData.readBlockPos();
        data.set(0, extraData.readVarInt());
        data.set(1, extraData.readVarInt());
        data.set(2, extraData.readVarInt());
        if (inventory.player.level().getBlockEntity(pos) instanceof SignalRangeBreakerBlockEntity breaker) {
            return new ClientContext(breaker, data);
        }
        return new ClientContext(null, data);
    }

    private record ClientContext(@Nullable SignalRangeBreakerBlockEntity breaker, ContainerData data) {
    }
}
