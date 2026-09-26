package com.secondition.creeperindustry.content.snapshot;

import com.secondition.creeperindustry.CIBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public final class SnapshotTableBlockEntity extends BlockEntity implements MenuProvider {
    static final String SNAPSHOT_ID_TAG = "snapshot_id";
    private UUID snapshotId;
    private final net.minecraft.world.inventory.ContainerData data = new net.minecraft.world.inventory.ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> snapshotId == null ? 0 : 1;
                case 1 -> snapshotId == null ? 0 : SnapshotDimensionManager.slowdown(snapshotId);
                default -> 0;
            };
        }
        @Override public void set(int index, int value) {}
        @Override public int getCount() { return 2; }
    };

    public SnapshotTableBlockEntity(BlockPos pos, BlockState state) {
        super(CIBlockEntityTypes.SNAPSHOT_TABLE.get(), pos, state);
    }

    public net.minecraft.world.inventory.ContainerData dataAccess() { return data; }

    public void capture(int slowdown, int viewDistance) {
        if (level instanceof net.minecraft.server.level.ServerLevel server
                && !SnapshotDimensionManager.isSnapshot(server)) {
            SnapshotDimensionManager.requestCapture(server.getServer(), server, worldPosition,
                    slowdown, Math.min(viewDistance, server.getServer().getPlayerList().getViewDistance()),
                    record -> {
                        snapshotId = record.id;
                        setChanged();
                    });
        }
    }

    public void enter(Player player) {
        if (level instanceof net.minecraft.server.level.ServerLevel server
                && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                && snapshotId != null) SnapshotDimensionManager.toggle(serverPlayer, snapshotId);
    }

    public void replay() {
        if (snapshotId != null) SnapshotDimensionManager.replay(snapshotId);
    }

    @Override public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new SnapshotTableMenu(id, inventory, this, data);
    }

    @Override public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
        for (int i = 0; i < data.getCount(); i++) buf.writeVarInt(data.get(i));
    }

    @Override protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (snapshotId != null) tag.putUUID(SNAPSHOT_ID_TAG, snapshotId);
    }

    @Override protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID(SNAPSHOT_ID_TAG)) snapshotId = tag.getUUID(SNAPSHOT_ID_TAG);
    }
}
