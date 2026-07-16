package com.secondition.creeperindustry.content.automation.breaker;

import com.secondition.creeperindustry.CIBlockEntityTypes;
import com.secondition.creeperindustry.content.energy.signal.AggregatedSignal;
import com.secondition.creeperindustry.content.energy.signal.SignalReceiver;
import com.secondition.creeperindustry.content.energy.signal.runtime.SignalRuntimeAccess;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SignalRangeBreakerBlockEntity extends BlockEntity implements MenuProvider, SignalReceiver {
    private static final SignalRangeBreakerExecutor EXECUTOR = new SignalRangeBreakerExecutor(
            new LinearRangeBreakerStrengthRule(),
            new RangeBreakerPermissionChecker()
    );

    private SignalRange range = SignalRange.DEFAULT;
    private boolean signalActive;
    private long lastActivationGameTime = Long.MIN_VALUE;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> range.width();
                case 1 -> range.height();
                case 2 -> range.depth();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            range = switch (index) {
                case 0 -> new SignalRange(value, range.height(), range.depth());
                case 1 -> new SignalRange(range.width(), value, range.depth());
                case 2 -> new SignalRange(range.width(), range.height(), value);
                default -> range;
            };
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public SignalRangeBreakerBlockEntity(BlockPos pos, BlockState state) {
        super(CIBlockEntityTypes.SIGNAL_RANGE_BREAKER.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SignalRangeBreakerMenu(containerId, inventory, this, dataAccess);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(worldPosition);
        buffer.writeVarInt(range.width());
        buffer.writeVarInt(range.height());
        buffer.writeVarInt(range.depth());
    }

    @Override
    public void receiveSignal(AggregatedSignal signal) {
        boolean activationCondition = signal.signal().amplitude() > 0 && signal.instantaneousValue() > 0;
        if (activationCondition && !signalActive) {
            signalActive = true;
            activate(signal.signal().amplitude());
            return;
        }
        if (!activationCondition) {
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("range_width", range.width());
        tag.putInt("range_height", range.height());
        tag.putInt("range_depth", range.depth());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        range = new SignalRange(
                tag.contains("range_width") ? tag.getInt("range_width") : SignalRange.SMALL_SIZE,
                tag.contains("range_height") ? tag.getInt("range_height") : SignalRange.SMALL_SIZE,
                tag.contains("range_depth") ? tag.getInt("range_depth") : SignalRange.SMALL_SIZE
        );
        signalActive = false;
        lastActivationGameTime = Long.MIN_VALUE;
    }

    public ContainerData dataAccess() {
        return dataAccess;
    }

    public SignalRange range() {
        return range;
    }

    public void toggleWidth() {
        setRange(range.toggleWidth());
    }

    public void toggleHeight() {
        setRange(range.toggleHeight());
    }

    public void toggleDepth() {
        setRange(range.toggleDepth());
    }

    private void setRange(SignalRange newRange) {
        if (range.equals(newRange)) {
            return;
        }
        range = newRange;
        setChanged();
    }

    private void activate(int signalAmplitude) {
        if (!(level instanceof ServerLevel serverLevel) || lastActivationGameTime == level.getGameTime()) {
            return;
        }
        lastActivationGameTime = level.getGameTime();
        EXECUTOR.execute(
                serverLevel,
                worldPosition,
                getBlockState().getValue(SignalRangeBreakerBlock.FACING),
                range,
                signalAmplitude
        );
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
}
