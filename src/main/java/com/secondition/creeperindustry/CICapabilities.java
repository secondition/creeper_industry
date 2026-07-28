package com.secondition.creeperindustry;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;

public final class CICapabilities {
    private CICapabilities() {}
    public static void register(IEventBus bus) { bus.addListener(CICapabilities::registerCapabilities); }
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CIBlockEntityTypes.ROCKET_LAUNCHER.get(),
                (blockEntity, side) -> side == null ? null : new SidedInvWrapper(blockEntity, side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CIBlockEntityTypes.GUIDED_FIREWORK_RECEIVER.get(),
                (blockEntity, side) -> side == null ? null : new SidedInvWrapper(blockEntity, side));
    }
}
