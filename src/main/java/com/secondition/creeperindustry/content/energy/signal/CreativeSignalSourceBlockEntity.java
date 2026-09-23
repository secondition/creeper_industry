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
    private int amplitude = 4, stages = 2, stageLengthIndex = 4, phaseSteps;
    private boolean registered;
    private final ContainerData data =
            new ContainerData() {
                public int get(int i) {
                    return switch (i) {
                        case 0 -> signalType.getSerializedId();
                        case 1 -> amplitude;
                        case 2 -> stages;
                        case 3 -> stageLengthIndex;
                        case 4 -> phaseSteps;
                        default -> 0;
                    };
                }

                public void set(int i, int v) {
                    switch (i) {
                        case 0 -> signalType = CreativeSignalSourceSignalType.bySerializedId(v);
                        case 1 -> amplitude = Math.clamp(v, -64, 64);
                        case 2 -> stages = Math.clamp(v / 2 * 2, 2, 16);
                        case 3 -> stageLengthIndex = Math.clamp(v, 0, SignalTime.STAGE_LENGTH_COUNT - 1);
                        case 4 -> phaseSteps = Math.floorMod(v, stages);
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

    public void setStages(int value) {
        if (value < 2 || value > 16 || value % 2 != 0 || value == stages) return;
        phaseSteps = Math.floorMod(Math.round((float) phaseSteps * value / stages), value);
        stages = value;
        updateSource();
        setChanged();
    }

    public void setStageLengthIndex(int index) {
        if (index < 0 || index >= SignalTime.STAGE_LENGTH_COUNT || index == stageLengthIndex)
            return;
        stageLengthIndex = index;
        updateSource();
        setChanged();
    }

    public void stepPhase(int direction) {
        if (signalType != CreativeSignalSourceSignalType.CONTINUOUS) return;
        phaseSteps = Math.floorMod(phaseSteps + direction, stages);
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
                                new SignalDefinition(amplitude, 0, 0, 0, SignalWaveform.STATIC)),
                        true);
    }

    private void updateSource() {
        if (level == null || level.isClientSide()) return;
        if (amplitude == 0 || signalType == CreativeSignalSourceSignalType.PULSE) {
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
                                signalType == CreativeSignalSourceSignalType.STATIC
                                        ? new SignalDefinition(amplitude, 0, 0, 0, SignalWaveform.STATIC)
                                        : new SignalDefinition(
                                                amplitude,
                                                stages,
                                                SignalTime.stageUnits(stageLengthIndex),
                                                phaseSteps,
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
        for (int i = 0; i < 5; i++) buf.writeVarInt(data.get(i));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putUUID("continuous_source_id", sourceId);
        tag.putString("signal_type", signalType.getSerializedName());
        tag.putInt("amplitude", amplitude);
        tag.putInt("stages", stages);
        tag.putInt("stage_length_index", stageLengthIndex);
        tag.putInt("phase_steps", phaseSteps);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("continuous_source_id")) sourceId = tag.getUUID("continuous_source_id");
        signalType = CreativeSignalSourceSignalType.bySerializedName(tag.getString("signal_type"));
        amplitude = tag.contains("amplitude") ? Math.clamp(tag.getInt("amplitude"), -64, 64) : 4;
        stages = tag.contains("stages") ? Math.clamp(tag.getInt("stages") / 2 * 2, 2, 16) : 2;
        stageLengthIndex = tag.contains("stage_length_index")
                ? Math.clamp(tag.getInt("stage_length_index"), 0, SignalTime.STAGE_LENGTH_COUNT - 1)
                : 4;
        phaseSteps = Math.floorMod(tag.getInt("phase_steps"), stages);
    }
}
