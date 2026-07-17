package com.secondition.creeperindustry.content.logistics.dropper;

import com.secondition.creeperindustry.CreeperIndustry;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = CreeperIndustry.MODID)
public final class PrecisionDropEventHandler {
    private PrecisionDropEventHandler() {
    }

    @SubscribeEvent
    public static void onLevelTickPost(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            PrecisionDropRuntimeAccess.getExisting(level).ifPresent(runtime -> runtime.tick(level));
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.level() instanceof ServerLevel level
                && PrecisionDropRuntimeAccess.getExisting(level)
                        .map(runtime -> runtime.shouldCancelDamage(level, player, event.getSource()))
                        .orElse(false)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level) {
            PrecisionDropRuntimeAccess.getExisting(level).ifPresent(runtime -> runtime.cancel(level, player.getUUID()));
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.level() instanceof ServerLevel level) {
            PrecisionDropRuntimeAccess.getExisting(level).ifPresent(runtime -> runtime.cancel(level, player.getUUID()));
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel previousLevel = player.getServer().getLevel(event.getFrom());
        if (previousLevel != null) {
            PrecisionDropRuntimeAccess.getExisting(previousLevel)
                    .ifPresent(runtime -> runtime.cancel(previousLevel, player.getUUID()));
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            PrecisionDropRuntimeAccess.remove(level).ifPresent(runtime -> runtime.close(level));
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            PrecisionDropRuntimeAccess.remove(level).ifPresent(runtime -> runtime.close(level));
        }
    }
}
