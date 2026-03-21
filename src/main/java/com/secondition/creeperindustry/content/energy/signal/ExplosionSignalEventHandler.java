package com.secondition.creeperindustry.content.energy.signal;

import com.secondition.creeperindustry.CISignalSourceTypes;
import com.secondition.creeperindustry.CreeperIndustry;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;

@EventBusSubscriber(modid = CreeperIndustry.MODID)
public final class ExplosionSignalEventHandler {
    private static final ExplosionSignalService EXPLOSION_SIGNAL_SERVICE = new ExplosionSignalService(
            new ExplosionSignalContextFactory(new DefaultExplosionSignalFilter()),
            new DefaultExplosionSignalAmplitudeResolver(),
            CISignalSourceTypes.transientDispatcher()
    );

    private ExplosionSignalEventHandler() {
    }

    @SubscribeEvent
    public static void onExplosionStart(ExplosionEvent.Start event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        EXPLOSION_SIGNAL_SERVICE.capture(event.getLevel(), event.getExplosion());
    }
}
