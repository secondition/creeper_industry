package com.secondition.creeperindustry.content.explosion.wave.runtime;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.secondition.creeperindustry.content.explosion.wave.ActivePulseWave;
import com.secondition.creeperindustry.content.explosion.wave.EntityWaveImpactService;
import com.secondition.creeperindustry.content.explosion.wave.PulseWaveEmission;
import com.secondition.creeperindustry.content.explosion.wave.WaveEffectProfile;
import com.secondition.creeperindustry.content.explosion.wave.network.PulseWaveSpawnPacket;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.network.PacketDistributor;

public final class WaveRuntime implements AutoCloseable {
    private final List<ActivePulseWave> activeWaves = new ArrayList<>();
    private final EntityWaveImpactService impactService = new EntityWaveImpactService(WaveEffectProfile.DEFAULT);

    public void spawnPulse(ServerLevel level, PulseWaveEmission emission) {
        ActivePulseWave wave = new ActivePulseWave(emission);
        activeWaves.add(wave);
        PacketDistributor.sendToPlayersInDimension(level, PulseWaveSpawnPacket.fromEmission(emission));
    }

    public void tick(ServerLevel level) {
        long gameTime = level.getGameTime();
        Iterator<ActivePulseWave> iterator = activeWaves.iterator();
        while (iterator.hasNext()) {
            ActivePulseWave wave = iterator.next();
            while (wave.lastProcessedGameTime() < gameTime) {
                long t = wave.lastProcessedGameTime() + 1;
                impactService.applyShell(level, wave, t);
                wave.lastProcessedGameTime(t);
                if (wave.isExpired(t)) {
                    iterator.remove();
                    break;
                }
            }
        }
    }

    @Override
    public void close() {
        activeWaves.clear();
    }
}
