package com.secondition.creeperindustry.content.explosion.wave;

import java.util.UUID;

import com.secondition.creeperindustry.CreeperIndustry;
import com.secondition.creeperindustry.content.explosion.wave.runtime.WaveRuntimeAccess;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;

@EventBusSubscriber(modid = CreeperIndustry.MODID)
public final class WaveExplosionEventHandler {
    private WaveExplosionEventHandler() {
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        Explosion explosion = event.getExplosion();
        if (!shouldHandle(explosion)) {
            return;
        }

        int amplitude = PulseWaveEmission.amplitudeFromExplosionPower(explosion.radius());
        if (amplitude <= 0) {
            return;
        }

        event.getAffectedEntities().clear();

        PulseWaveEmission emission = new PulseWaveEmission(
                UUID.randomUUID(),
                explosion.center(),
                serverLevel.getGameTime(),
                amplitude,
                WavePropagationProfile.DEFAULT
        );
        WaveRuntimeAccess.get(serverLevel).spawnPulse(serverLevel, emission);
    }

    private static boolean shouldHandle(Explosion explosion) {
        if (explosion.radius() <= 0.0F) {
            return false;
        }

        Entity directSource = explosion.getDirectSourceEntity();
        return directSource instanceof PrimedTnt || directSource instanceof Creeper;
    }
}
