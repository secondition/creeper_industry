package com.secondition.creeperindustry;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;

public final class CIChunkTickets {
    public static final TicketController PRECISION_DROPPER = new TicketController(
            CreeperIndustry.asResource("precision_dropper"),
            (level, helper) -> helper.getEntityTickets().keySet().forEach(helper::removeAllTickets)
    );

    private CIChunkTickets() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(CIChunkTickets::registerControllers);
    }

    private static void registerControllers(RegisterTicketControllersEvent event) {
        event.register(PRECISION_DROPPER);
    }
}
