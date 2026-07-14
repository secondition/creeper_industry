package com.secondition.creeperindustry.content.energy.signal;

import com.secondition.creeperindustry.CreeperIndustry;
import com.secondition.creeperindustry.content.energy.signal.runtime.SignalRuntimeAccess;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;

@EventBusSubscriber(modid = CreeperIndustry.MODID)
public final class ExplosionSignalEventHandler {
    private static final ExplosionSignalContextFactory CONTEXT_FACTORY = new ExplosionSignalContextFactory(new DefaultExplosionSignalFilter());
    private static final DefaultExplosionSignalAmplitudeResolver AMPLITUDE_RESOLVER = new DefaultExplosionSignalAmplitudeResolver();

    private ExplosionSignalEventHandler() {
    }

    @SubscribeEvent
    public static void onExplosionStart(ExplosionEvent.Start event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        new ExplosionSignalService(
                CONTEXT_FACTORY,
                AMPLITUDE_RESOLVER,
                SignalRuntimeAccess.get(event.getLevel()).transientDispatcher()
        ).capture(event.getLevel(), event.getExplosion());
    }
}
