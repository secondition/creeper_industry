package com.secondition.creeperindustry.content.logistics.rocket.runtime;

import com.secondition.creeperindustry.CreeperIndustry;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = CreeperIndustry.MODID)
public final class RocketFlightEventHandler {
    private RocketFlightEventHandler() {}
    @SubscribeEvent public static void levelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) level.getExistingData(com.secondition.creeperindustry.CIAttachmentTypes.ROCKET_FLIGHT_RUNTIME).ifPresent(runtime -> runtime.tick(level));
    }
}
