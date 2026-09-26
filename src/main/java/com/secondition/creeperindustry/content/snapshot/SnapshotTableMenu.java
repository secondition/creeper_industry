package com.secondition.creeperindustry.content.snapshot;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class SnapshotTableMenu extends AbstractContainerMenu {
    private final SnapshotTableBlockEntity table;
    private final ContainerData data;

    public SnapshotTableMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, (SnapshotTableBlockEntity) inventory.player.level().getBlockEntity(buffer.readBlockPos()),
                new SimpleContainerData(2));
        for (int i = 0; i < data.getCount(); i++) data.set(i, buffer.readVarInt());
    }

    public SnapshotTableMenu(int id, Inventory inventory, SnapshotTableBlockEntity table,
            ContainerData data) {
        super(com.secondition.creeperindustry.CIMenuTypes.SNAPSHOT_TABLE.get(), id);
        this.table = table;
        this.data = data;
        addDataSlots(data);
    }

    @Override public boolean stillValid(Player player) {
        return player.distanceToSqr(table.getBlockPos().getX() + .5, table.getBlockPos().getY() + .5,
                table.getBlockPos().getZ() + .5) < 64;
    }

    @Override public boolean clickMenuButton(Player player, int buttonId) {
        if (!(player.level().getBlockEntity(table.getBlockPos()) instanceof SnapshotTableBlockEntity current)) return false;
        switch (buttonId) {
            case 1 -> current.capture(1, player instanceof net.minecraft.server.level.ServerPlayer server
                    ? server.requestedViewDistance() : 2);
            case 2 -> current.capture(2, player instanceof net.minecraft.server.level.ServerPlayer server
                    ? server.requestedViewDistance() : 2);
            case 3 -> current.capture(4, player instanceof net.minecraft.server.level.ServerPlayer server
                    ? server.requestedViewDistance() : 2);
            case 4 -> current.capture(8, player instanceof net.minecraft.server.level.ServerPlayer server
                    ? server.requestedViewDistance() : 2);
            case 10 -> current.enter(player);
            case 11 -> current.replay();
            default -> {}
        }
        return true;
    }

    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }

    public boolean hasSnapshot() { return data.get(0) != 0; }
    public int slowdown() { return data.get(1); }
}
