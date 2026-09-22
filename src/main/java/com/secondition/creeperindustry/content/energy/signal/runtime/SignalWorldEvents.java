package com.secondition.creeperindustry.content.energy.signal.runtime;

import com.secondition.creeperindustry.CreeperIndustry;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

@EventBusSubscriber(modid = CreeperIndustry.MODID)
public final class SignalWorldEvents {
    @SubscribeEvent
    public static void neighbors(BlockEvent.NeighborNotifyEvent event) {
        if (event.getLevel() instanceof ServerLevel level)
            SignalRuntimeAccess.getExisting(level)
                    .ifPresent(r -> r.structures().changed(event.getPos()));
    }

    @SubscribeEvent
    public static void placed(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level)
            SignalRuntimeAccess.getExisting(level)
                    .ifPresent(r -> r.structures().changed(event.getPos()));
    }

    @SubscribeEvent
    public static void broken(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level)
            SignalRuntimeAccess.getExisting(level)
                    .ifPresent(r -> r.structures().changed(event.getPos()));
    }

    @SubscribeEvent
    public static void loaded(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        SignalRuntimeAccess.get(level).queueLoadedChunk(event.getChunk());
    }

    @SubscribeEvent
    public static void unloaded(ChunkEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        SignalRuntimeAccess.getExisting(level)
                .ifPresent(
                        r -> {
                            var pos = event.getChunk().getPos();
                            r.cancelLoadedChunk(pos);
                            boolean changed = r.ductNetworkManager().unindexChunk(pos.x, pos.z);
                            r.structures().chunkChanged(pos.x, pos.z);
                            if (changed)
                                r.scheduleTopologyRefresh(level, r.receiverIndex().getAll());
                        });
    }
}
