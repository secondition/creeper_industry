package com.secondition.creeperindustry.content.energy.signal;

import java.util.UUID;

import com.secondition.creeperindustry.CIBlockEntityTypes;
import com.secondition.creeperindustry.CISignalSourceTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class CreativeSignalSourceBlockEntity extends BlockEntity implements MenuProvider {
    public static final int MIN_AMPLITUDE = 0;
    public static final int MAX_AMPLITUDE = 64;
    public static final int MIN_PERIOD_TICKS = 0;
    public static final int MAX_PERIOD_TICKS = 64;
    public static final int PERIOD_STEP = 2;

    private UUID continuousSourceId = UUID.randomUUID();
    private CreativeSignalSourceSignalType signalType = CreativeSignalSourceSignalType.CONTINUOUS;
    private int amplitude = 4;
    private int periodTicks = 2;
    private int phaseTicks;
    private boolean phaseInitialized;
    private boolean sourceRegistered;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> signalType.getSerializedId();
                case 1 -> amplitude;
                case 2 -> periodTicks;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> signalType = CreativeSignalSourceSignalType.bySerializedId(value);
                case 1 -> amplitude = clampAmplitude(value);
                case 2 -> periodTicks = clampEvenPeriod(value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public CreativeSignalSourceBlockEntity(BlockPos pos, BlockState blockState) {
        super(CIBlockEntityTypes.CREATIVE_SIGNAL_SOURCE.get(), pos, blockState);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        updateContinuousSignal();
    }

    @Override
    public void setRemoved() {
        removeContinuousSignalSource(true);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        removeContinuousSignalSource(false);
        super.onChunkUnloaded();
    }

    public ContainerData dataAccess() {
        return dataAccess;
    }

    public CreativeSignalSourceSignalType getSignalType() {
        return signalType;
    }

    public int getAmplitude() {
        return amplitude;
    }

    public int getPeriodTicks() {
        return periodTicks;
    }

    public void setSignalType(CreativeSignalSourceSignalType signalType) {
        if (this.signalType == signalType) {
            return;
        }

        this.signalType = signalType;
        updateContinuousSignal();
        setChanged();
    }

    public void setAmplitude(int amplitude) {
        int clampedAmplitude = clampAmplitude(amplitude);
        if (this.amplitude == clampedAmplitude) {
            return;
        }

        this.amplitude = clampedAmplitude;
        updateContinuousSignal();
        setChanged();
    }

    public void setPeriodTicks(int periodTicks) {
        int clampedPeriod = clampEvenPeriod(periodTicks);
        if (this.periodTicks == clampedPeriod) {
            return;
        }

        int previousPeriod = this.periodTicks;
        this.periodTicks = clampedPeriod;
        remapPhase(previousPeriod, clampedPeriod);
        updateContinuousSignal();
        setChanged();
    }

    public void emitPulse() {
        Level currentLevel = level;
        if (currentLevel == null || currentLevel.isClientSide() || signalType != CreativeSignalSourceSignalType.PULSE || amplitude <= 0) {
            return;
        }

        CreativeSignalPulseSource source = new CreativeSignalPulseSource(
                UUID.randomUUID(),
                currentLevel.dimension(),
                Vec3.atCenterOf(worldPosition),
                currentLevel.getGameTime(),
                new SignalDefinition(
                        amplitude,
                        CreativeSignalPulseSource.PULSE_PERIOD_TICKS,
                        CreativeSignalPulseSource.DEFAULT_PHASE_TICKS,
                        SignalWaveform.SQUARE
                )
        );
        CISignalSourceTypes.transientDispatcher().dispatch(currentLevel, source);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new CreativeSignalSourceMenu(containerId, playerInventory, this, dataAccess);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(getBlockPos());
        buffer.writeVarInt(signalType.getSerializedId());
        buffer.writeVarInt(amplitude);
        buffer.writeVarInt(periodTicks);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putUUID("continuous_source_id", continuousSourceId);
        tag.putString("signal_type", signalType.getSerializedName());
        tag.putInt("amplitude", amplitude);
        tag.putInt("period_ticks", periodTicks);
        if (phaseInitialized) {
            tag.putInt("phase_ticks", phaseTicks);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("continuous_source_id")) {
            continuousSourceId = tag.getUUID("continuous_source_id");
        } else if (tag.hasUUID("source_id")) {
            continuousSourceId = tag.getUUID("source_id");
        }

        signalType = CreativeSignalSourceSignalType.bySerializedName(tag.getString("signal_type"));
        amplitude = tag.contains("amplitude") ? clampAmplitude(tag.getInt("amplitude")) : 4;
        periodTicks = tag.contains("period_ticks") ? clampEvenPeriod(tag.getInt("period_ticks")) : 2;
        if (tag.contains("phase_ticks") && periodTicks > 0) {
            phaseTicks = Math.floorMod(tag.getInt("phase_ticks"), periodTicks);
            phaseInitialized = true;
        } else {
            phaseTicks = 0;
            phaseInitialized = false;
        }
    }

    private void updateContinuousSignal() {
        Level currentLevel = level;
        if (currentLevel == null || currentLevel.isClientSide()) {
            return;
        }

        if (signalType != CreativeSignalSourceSignalType.CONTINUOUS || amplitude <= 0 || periodTicks <= 0) {
            removeContinuousSignalSource(true);
            return;
        }

        ensurePhaseInitialized();
        MachineSignalSource source = new MachineSignalSource(
                continuousSourceId,
                currentLevel.dimension(),
                Vec3.atCenterOf(worldPosition),
                currentLevel.getGameTime(),
                new SignalDefinition(amplitude, periodTicks, phaseTicks, SignalWaveform.SQUARE)
        );
        CISignalSourceTypes.continuousSignalUpdateService().upsert(currentLevel, source);
        sourceRegistered = true;
    }

    private void removeContinuousSignalSource(boolean refreshAffectedReceivers) {
        Level currentLevel = level;
        if (currentLevel == null || currentLevel.isClientSide() || !sourceRegistered) {
            return;
        }

        if (refreshAffectedReceivers) {
            CISignalSourceTypes.continuousSignalUpdateService().remove(currentLevel, continuousSourceId);
        } else {
            CISignalSourceTypes.continuousSignalUpdateService().removeWithoutRefresh(currentLevel, continuousSourceId);
        }
        sourceRegistered = false;
    }

    private void ensurePhaseInitialized() {
        if (phaseInitialized || level == null || periodTicks <= 0) {
            return;
        }
        phaseTicks = Math.floorMod((int) level.getGameTime(), periodTicks);
        phaseInitialized = true;
    }

    private void remapPhase(int previousPeriod, int nextPeriod) {
        if (nextPeriod <= 0) {
            phaseTicks = 0;
            phaseInitialized = false;
            return;
        }
        if (previousPeriod <= 0) {
            phaseTicks = level == null ? 0 : Math.floorMod((int) level.getGameTime(), nextPeriod);
            phaseInitialized = level != null;
            return;
        }
        if (!phaseInitialized || previousPeriod == nextPeriod) {
            return;
        }
        phaseTicks = Math.floorMod((int) (((long) phaseTicks * nextPeriod) / previousPeriod), nextPeriod);
    }

    private static int clampAmplitude(int value) {
        return Math.max(MIN_AMPLITUDE, Math.min(MAX_AMPLITUDE, value));
    }

    private static int clampEvenPeriod(int value) {
        int clamped = Math.max(MIN_PERIOD_TICKS, Math.min(MAX_PERIOD_TICKS, value));
        return clamped - Math.floorMod(clamped, PERIOD_STEP);
    }
}
