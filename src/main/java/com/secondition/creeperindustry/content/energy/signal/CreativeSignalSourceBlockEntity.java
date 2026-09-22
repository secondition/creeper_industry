package com.secondition.creeperindustry.content.energy.signal;

import com.secondition.creeperindustry.CIBlockEntityTypes;
import com.secondition.creeperindustry.content.energy.signal.runtime.SignalRuntimeAccess;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class CreativeSignalSourceBlockEntity extends BlockEntity implements MenuProvider {
    public static final int MAX_AMPLITUDE = 64;
    private UUID sourceId = UUID.randomUUID();
    private CreativeSignalSourceSignalType signalType = CreativeSignalSourceSignalType.CONTINUOUS;
    private int amplitude = 4, periodIndex = 10, phaseUnits;
    private boolean registered;
    private final ContainerData data =
            new ContainerData() {
                public int get(int i) {
                    return switch (i) {
                        case 0 -> signalType.getSerializedId();
                        case 1 -> amplitude;
                        case 2 -> periodIndex;
                        case 3 -> phaseUnits;
                        default -> 0;
                    };
                }

                public void set(int i, int v) {
                    switch (i) {
                        case 0 -> signalType = CreativeSignalSourceSignalType.bySerializedId(v);
                        case 1 -> amplitude = Math.clamp(v, -64, 64);
                        case 2 -> periodIndex = Math.clamp(v, 0, 59);
                        case 3 -> phaseUnits = Math.floorMod(v, periodUnits());
                    }
                }

                public int getCount() {
                    return 4;
                }
            };

    public CreativeSignalSourceBlockEntity(BlockPos pos, BlockState state) {
        super(CIBlockEntityTypes.CREATIVE_SIGNAL_SOURCE.get(), pos, state);
    }

    public ContainerData dataAccess() {
        return data;
    }

    public CreativeSignalSourceSignalType getSignalType() {
        return signalType;
    }

    public int getAmplitude() {
        return amplitude;
    }

    public double getPeriodTicks() {
        return SignalTime.periodAt(periodIndex);
    }

    private int periodUnits() {
        return (int) Math.round(getPeriodTicks() * 20);
    }

    public void setSignalType(CreativeSignalSourceSignalType type) {
        if (type != signalType) {
            signalType = type;
            updateSource();
            setChanged();
        }
    }

    public void setAmplitude(int value) {
        value = Math.clamp(value, -64, 64);
        if (value != amplitude) {
            amplitude = value;
            updateSource();
            setChanged();
        }
    }

    public void setPeriodIndex(int index) {
        if (index < 0 || index >= 60 || index == periodIndex) return;
        int previous = periodUnits();
        periodIndex = index;
        phaseUnits = Math.floorMod(phaseUnits * periodUnits() / previous, periodUnits());
        updateSource();
        setChanged();
    }

    public void stepPhase(int direction) {
        phaseUnits = Math.floorMod(phaseUnits + direction, periodUnits());
        updateSource();
        setChanged();
    }

    public void emitPulse() {
        if (level == null
                || level.isClientSide()
                || amplitude == 0
                || signalType != CreativeSignalSourceSignalType.PULSE) return;
        SignalRuntimeAccess.get(level)
                .emitPulse(
                        level,
                        new CreativeSignalPulseSource(
                                UUID.randomUUID(),
                                level.dimension(),
                                Vec3.atCenterOf(worldPosition),
                                level.getGameTime(),
                                new SignalDefinition(amplitude, 1, 0, SignalWaveform.SQUARE)),
                        true);
    }

    private void updateSource() {
        if (level == null || level.isClientSide()) return;
        if (amplitude == 0 || signalType != CreativeSignalSourceSignalType.CONTINUOUS) {
            removeSource();
            return;
        }
        SignalRuntimeAccess.get(level)
                .upsert(
                        level,
                        new MachineSignalSource(
                                sourceId,
                                level.dimension(),
                                Vec3.atCenterOf(worldPosition),
                                level.getGameTime(),
                                new SignalDefinition(
                                        amplitude,
                                        getPeriodTicks(),
                                        phaseUnits / 20.0,
                                        SignalWaveform.SQUARE)));
        registered = true;
    }

    private void removeSource() {
        if (level != null && !level.isClientSide() && registered)
            SignalRuntimeAccess.getExisting(level).ifPresent(r -> r.removeSource(level, sourceId));
        registered = false;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        updateSource();
    }

    @Override
    public void setRemoved() {
        removeSource();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        removeSource();
        super.onChunkUnloaded();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new CreativeSignalSourceMenu(id, inventory, this, data);
    }

    @Override
    public void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(worldPosition);
        for (int i = 0; i < 4; i++) buf.writeVarInt(data.get(i));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putUUID("continuous_source_id", sourceId);
        tag.putString("signal_type", signalType.getSerializedName());
        tag.putInt("amplitude", amplitude);
        tag.putInt("period_index", periodIndex);
        tag.putInt("phase_units", phaseUnits);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("continuous_source_id")) sourceId = tag.getUUID("continuous_source_id");
        signalType = CreativeSignalSourceSignalType.bySerializedName(tag.getString("signal_type"));
        amplitude = tag.contains("amplitude") ? Math.clamp(tag.getInt("amplitude"), -64, 64) : 4;
        if (tag.contains("period_index"))
            periodIndex = Math.clamp(tag.getInt("period_index"), 0, 59);
        else {
            int old = tag.getInt("period_ticks");
            periodIndex = SignalTime.indexOf(old > 1 ? Math.clamp(old / 2 * 2, 2, 100) : 1);
        }
        phaseUnits =
                Math.floorMod(
                        tag.contains("phase_units")
                                ? tag.getInt("phase_units")
                                : tag.getInt("phase_ticks") * 20,
                        periodUnits());
    }
}
