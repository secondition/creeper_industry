package com.secondition.creeperindustry.content.explosion;

import com.secondition.creeperindustry.CreeperIndustry;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;

@EventBusSubscriber(modid = CreeperIndustry.MODID)
public final class ExplosionVisualEventHandler {
    private ExplosionVisualEventHandler() {
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ExplosionShockwaveDispatcher.dispatch(level, event.getExplosion());
        }
    }
}
