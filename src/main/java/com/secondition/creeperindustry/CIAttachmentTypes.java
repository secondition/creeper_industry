package com.secondition.creeperindustry;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.secondition.creeperindustry.content.energy.signal.runtime.SignalRuntime;
import com.secondition.creeperindustry.content.logistics.dropper.PrecisionDropRuntime;
import com.secondition.creeperindustry.content.logistics.rocket.runtime.RocketFlightRuntime;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class CIAttachmentTypes {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, CreeperIndustry.MODID);

    public static final Supplier<AttachmentType<Boolean>> CONTROLLED_EXPLOSION_UNLOCKED = ATTACHMENT_TYPES.register("controlled_explosion_unlocked",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).build());
    public static final Supplier<AttachmentType<SignalRuntime>> SIGNAL_RUNTIME = ATTACHMENT_TYPES.register("signal_runtime",
            () -> AttachmentType.builder(SignalRuntime::new).build());
    public static final Supplier<AttachmentType<PrecisionDropRuntime>> PRECISION_DROP_RUNTIME = ATTACHMENT_TYPES.register("precision_drop_runtime",
            () -> AttachmentType.builder(PrecisionDropRuntime::new).build());
    public static final Supplier<AttachmentType<RocketFlightRuntime>> ROCKET_FLIGHT_RUNTIME = ATTACHMENT_TYPES.register("rocket_flight_runtime",
            () -> AttachmentType.builder(RocketFlightRuntime::new).build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
