package com.secondition.creeperindustry.content.explosion.wave.runtime;

import com.secondition.creeperindustry.CreeperIndustry;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = CreeperIndustry.MODID)
public final class WaveRuntimeEventHandler {
    private WaveRuntimeEventHandler() {
    }

    @SubscribeEvent
    public static void onLevelTickPost(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            WaveRuntimeAccess.get(serverLevel).tick(serverLevel);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            WaveRuntimeAccess.remove(serverLevel).ifPresent(WaveRuntime::close);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            WaveRuntimeAccess.remove(level).ifPresent(WaveRuntime::close);
        }
    }
}
