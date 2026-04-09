package com.secondition.creeperindustry;

import java.util.function.Supplier;

import com.secondition.creeperindustry.content.logistics.rocket.GuidedFireworkRocketEntity;
import com.secondition.creeperindustry.content.logistics.storage.StorageDiscMinecartEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CIEntityTypes {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, CreeperIndustry.MODID);

    public static final Supplier<EntityType<GuidedFireworkRocketEntity>> GUIDED_FIREWORK_ROCKET = ENTITY_TYPES.register("guided_firework_rocket",
            () -> EntityType.Builder.<GuidedFireworkRocketEntity>of(GuidedFireworkRocketEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build(CreeperIndustry.asResource("guided_firework_rocket").toString()));
    public static final Supplier<EntityType<StorageDiscMinecartEntity>> STORAGE_DISC_MINECART = ENTITY_TYPES.register("storage_disc_minecart",
            () -> EntityType.Builder.<StorageDiscMinecartEntity>of(StorageDiscMinecartEntity::new, MobCategory.MISC)
                    .sized(0.98F, 0.7F)
                    .passengerAttachments(0.1875F)
                    .clientTrackingRange(8)
                    .build(CreeperIndustry.asResource("storage_disc_minecart").toString()));

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
