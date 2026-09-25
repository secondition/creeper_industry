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
    private int amplitude = 4, wavelength = 8, frequencyNumerator = 1, frequencyDenominator = 8;
    private long anchorTick = -1;
    private double phase;
    private boolean registered;
    private final ContainerData data =
            new ContainerData() {
                public int get(int i) {
                    return switch (i) {
                        case 0 -> signalType.getSerializedId();
                        case 1 -> amplitude;
                        case 2 -> wavelength;
                        case 3 -> frequencyNumerator;
                        case 4 -> frequencyDenominator;
                        default -> 0;
                    };
                }

                public void set(int i, int v) {
                    switch (i) {
                        case 0 -> signalType = CreativeSignalSourceSignalType.bySerializedId(v);
                        case 1 -> amplitude = Math.clamp(v, -64, 64);
                        case 2 -> wavelength = v;
                        case 3 -> frequencyNumerator = v;
                        case 4 -> frequencyDenominator = v;
                    }
                }

                public int getCount() {
                    return 5;
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

    public void setSignalType(CreativeSignalSourceSignalType type) {
        if (type != signalType) {
            signalType = type;
            if (type == CreativeSignalSourceSignalType.PULSE) anchorTick = -1;
            updateSource();
            setChanged();
        }
    }

    public void setAmplitude(int value) {
        value = Math.clamp(value, -64, 64);
        if (value != amplitude) {
            amplitude = value;
            if (value == 0) anchorTick = -1;
            updateSource();
            setChanged();
        }
    }

    public void setFrequency(int numerator, int denominator) {
        if (!SignalDefinition.validFrequency(numerator, denominator)
                || numerator == frequencyNumerator && denominator == frequencyDenominator) return;
        if (registered && level != null) {
            double cycles = phase + (level.getGameTime() - anchorTick)
                    * frequencyNumerator / (double) frequencyDenominator;
            phase = cycles - Math.floor(cycles);
            anchorTick = level.getGameTime();
        }
        frequencyNumerator = numerator;
        frequencyDenominator = denominator;
        wavelength = numerator == 1 ? denominator : 0;
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
                                SignalDefinition.pulse(amplitude)),
                        true);
    }

    private void updateSource() {
        if (level == null || level.isClientSide()) return;
        if (amplitude == 0 || signalType == CreativeSignalSourceSignalType.PULSE) {
            removeSource();
            return;
        }
        if (anchorTick < 0) {
            anchorTick = level.getGameTime();
            phase = 0;
        }
        SignalRuntimeAccess.get(level)
                .upsert(
                        level,
                        new MachineSignalSource(
                                sourceId,
                                level.dimension(),
                                Vec3.atCenterOf(worldPosition),
                                level.getGameTime(),
                                new SignalDefinition(amplitude, wavelength, frequencyNumerator,
                                        frequencyDenominator, anchorTick, phase,
                                        SignalWaveform.TRIANGLE)));
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
        for (int i = 0; i < data.getCount(); i++) buf.writeVarInt(data.get(i));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putUUID("continuous_source_id", sourceId);
        tag.putString("signal_type", signalType.getSerializedName());
        tag.putInt("amplitude", amplitude);
        tag.putInt("frequency_numerator", frequencyNumerator);
        tag.putInt("frequency_denominator", frequencyDenominator);
        tag.putLong("anchor_tick", anchorTick);
        tag.putDouble("phase", phase);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("continuous_source_id")) sourceId = tag.getUUID("continuous_source_id");
        signalType = CreativeSignalSourceSignalType.bySerializedName(tag.getString("signal_type"));
        amplitude = tag.contains("amplitude") ? Math.clamp(tag.getInt("amplitude"), -64, 64) : 4;
        int numerator = tag.contains("frequency_numerator") ? tag.getInt("frequency_numerator") : 1;
        int denominator = tag.contains("frequency_denominator") ? tag.getInt("frequency_denominator") : 8;
        if (SignalDefinition.validFrequency(numerator, denominator)) {
            frequencyNumerator = numerator;
            frequencyDenominator = denominator;
            wavelength = numerator == 1 ? denominator : 0;
        }
        anchorTick = tag.contains("anchor_tick") ? tag.getLong("anchor_tick") : -1;
        double savedPhase = tag.getDouble("phase");
        phase = Double.isFinite(savedPhase) && savedPhase >= 0 && savedPhase < 1
                ? savedPhase : 0;
    }
}
