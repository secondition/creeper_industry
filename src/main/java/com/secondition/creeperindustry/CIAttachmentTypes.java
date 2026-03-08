package com.secondition.creeperindustry;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class CIAttachmentTypes {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, CreeperIndustry.MODID);

    public static final Supplier<AttachmentType<Boolean>> CONTROLLED_EXPLOSION_UNLOCKED = ATTACHMENT_TYPES.register("controlled_explosion_unlocked",
            () -> AttachmentType.builder(() -> false).serialize(Codec.BOOL).build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
