package com.secondition.creeperindustry;

import java.util.function.Supplier;

import com.secondition.creeperindustry.content.logistics.rocket.GuidedFireworkRocketEntity;

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

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
    }
}
