package com.secondition.creeperindustry;

import com.mojang.serialization.Codec;
import com.secondition.creeperindustry.content.logistics.storage.StorageDiscContents;
import com.secondition.creeperindustry.content.logistics.rocket.RocketDestination;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CIDataComponents {
    private static final DeferredRegister.DataComponents DATA_COMPONENTS = DeferredRegister.createDataComponents(net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE, CreeperIndustry.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SIGNAL_AMPLITUDE = DATA_COMPONENTS.registerComponentType("signal_amplitude",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SIGNAL_FREQUENCY = DATA_COMPONENTS.registerComponentType("signal_frequency",
            builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> TARGET_POS = DATA_COMPONENTS.registerComponentType("target_pos",
            builder -> builder.persistent(BlockPos.CODEC).networkSynchronized(BlockPos.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<StorageDiscContents>> STORAGE_DISC_CONTENTS = DATA_COMPONENTS.registerComponentType("storage_disc_contents",
            builder -> builder.persistent(StorageDiscContents.CODEC).networkSynchronized(StorageDiscContents.STREAM_CODEC).cacheEncoding());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<RocketDestination>> ROCKET_DESTINATION = DATA_COMPONENTS.registerComponentType("rocket_destination",
            builder -> builder.persistent(RocketDestination.CODEC).networkSynchronized(RocketDestination.STREAM_CODEC).cacheEncoding());

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENTS.register(modEventBus);
    }
}
