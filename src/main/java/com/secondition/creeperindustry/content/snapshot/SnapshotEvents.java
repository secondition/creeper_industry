package com.secondition.creeperindustry.content.snapshot;

import com.secondition.creeperindustry.CreeperIndustry;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = CreeperIndustry.MODID)
public final class SnapshotEvents {
    private SnapshotEvents() {}

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent event) {
        SnapshotDimensionManager.restore(event.getServer());
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post event) {
        SnapshotDimensionManager.processCaptureRequests();
    }

    @SubscribeEvent
    public static void blockBroken(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)
            SnapshotDimensionManager.markChanged(level, event.getPos());
    }

    @SubscribeEvent
    public static void blockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)
            SnapshotDimensionManager.markChanged(level, event.getPos());
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        SnapshotDimensionManager.close(event.getServer());
    }

    @SubscribeEvent
    public static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) SnapshotDimensionManager.sync(player);
    }

    @SubscribeEvent
    public static void playerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) SnapshotDimensionManager.exit(player);
    }

}
