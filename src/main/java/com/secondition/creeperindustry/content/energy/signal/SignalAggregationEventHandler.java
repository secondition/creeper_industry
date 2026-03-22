package com.secondition.creeperindustry.content.energy.signal;

import com.secondition.creeperindustry.CISignalSourceTypes;
import com.secondition.creeperindustry.CreeperIndustry;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = CreeperIndustry.MODID)
public final class SignalAggregationEventHandler {
    private SignalAggregationEventHandler() {
    }

    @SubscribeEvent
    public static void onLevelTickPost(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        CISignalSourceTypes.signalAggregationService().clearThroughTick(event.getLevel().dimension(), event.getLevel().getGameTime());
        CISignalSourceTypes.continuousSignalUpdateService().refreshReceivers(
                event.getLevel(),
                CISignalSourceTypes.transientSignalImpactTracker()
                        .popAffectedTargetsThroughTick(event.getLevel().dimension(), event.getLevel().getGameTime())
        );
    }
}
