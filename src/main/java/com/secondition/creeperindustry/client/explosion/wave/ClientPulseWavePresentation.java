package com.secondition.creeperindustry.client.explosion.wave;

import com.secondition.creeperindustry.CreeperIndustry;
import com.secondition.creeperindustry.content.explosion.wave.WavePropagationMath;
import com.secondition.creeperindustry.content.explosion.wave.network.PulseWaveSpawnPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = CreeperIndustry.MODID, value = Dist.CLIENT)
public final class ClientPulseWavePresentation {
    private static final int MAX_TRACKED_WAVES = 128;
    private static final Map<UUID, ClientPulseWave> ACTIVE = new LinkedHashMap<>();
    private static final PulseWaveFeedback FEEDBACK = new PulseWaveFeedback();
    private static ClientLevel currentLevel;
    private static long lastProcessedTime = Long.MIN_VALUE;

    private ClientPulseWavePresentation() {}

    public static void spawn(PulseWaveSpawnPacket packet) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || !ClientPulseWave.isValid(packet)) {
            return;
        }
        useLevel(level);
        ClientPulseWave wave = new ClientPulseWave(packet);
        if (wave.isExpired(level.getGameTime()) || ACTIVE.containsKey(wave.id())) {
            return;
        }
        if (ACTIVE.size() >= MAX_TRACKED_WAVES) {
            ACTIVE.remove(ACTIVE.keySet().iterator().next());
        }
        ACTIVE.put(wave.id(), wave);
    }

    public static void periodic(
            com.secondition.creeperindustry.content.explosion.wave.network.PeriodicWavePacket
                    packet) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        useLevel(level);
        ClientMachineWaves.accept(packet);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        useLevel(level);
        LocalPlayer player = minecraft.player;
        if (level == null || player == null || minecraft.isPaused()) {
            return;
        }
        long gameTime = level.getGameTime();
        if (lastProcessedTime == gameTime) {
            return;
        }
        // Do not sweep all skipped history after time corrections or late packets.
        lastProcessedTime = gameTime;
        Iterator<ClientPulseWave> iterator = ACTIVE.values().iterator();
        while (iterator.hasNext()) {
            ClientPulseWave wave = iterator.next();
            if (wave.isExpired(gameTime)) {
                iterator.remove();
                continue;
            }
            if (wave.hitLocalPlayer() || player.isSpectator() || !wave.canImpactAt(gameTime)) {
                continue;
            }
            AABB box = player.getBoundingBox();
            double closest = WavePropagationMath.closestDistanceToAABB(wave.origin(), box);
            double farthest = WavePropagationMath.farthestDistanceToAABB(wave.origin(), box);
            if (wave.radiusAt(gameTime) >= closest
                    && wave.radiusAt(gameTime - 1.0) <= farthest
                    && wave.strengthAt(closest) > 0.0F) {
                wave.markLocalPlayerHit();
                double arrival = Mth.clamp(wave.arrivalTime(closest), gameTime - 1.0, gameTime);
                FEEDBACK.hit(level, player, wave, closest, arrival);
            }
        }
        if (!player.isSpectator())
            for (ClientPulseWave wave : ClientMachineWaves.arrivals(gameTime, player)) {
                double closest =
                        WavePropagationMath.closestDistanceToAABB(
                                wave.origin(), player.getBoundingBox());
                double arrival = Mth.clamp(wave.arrivalTime(closest), gameTime - 1.0, gameTime);
                FEEDBACK.hit(level, player, wave, closest, arrival);
            }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        useLevel(Minecraft.getInstance().level);
        if (currentLevel != null) {
            double time = renderTime(event.getPartialTick().getGameTimeDeltaPartialTick(true));
            Minecraft minecraft = Minecraft.getInstance();
            boolean localView =
                    minecraft.getCameraEntity() == minecraft.player
                            && minecraft.player != null
                            && !minecraft.player.isSpectator();
            float impact = localView ? FEEDBACK.visualStrength(time) : 0.0F;
            java.util.List<ClientPulseWave> visible = new java.util.ArrayList<>(ACTIVE.values());
            visible.addAll(ClientMachineWaves.shells(time));
            if (!visible.isEmpty() || impact > 0.001F) {
                PulseWaveRenderer.render(
                        event,
                        visible,
                        time,
                        impact,
                        FEEDBACK.visualAge(time),
                        FEEDBACK.visualPolarity());
            }
        }
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        useLevel(Minecraft.getInstance().level);
        if (currentLevel != null) {
            FEEDBACK.apply(event, renderTime(event.getPartialTick()));
        }
    }

    private static double renderTime(double partialTick) {
        return currentLevel.getGameTime() - 1.0 + partialTick;
    }

    private static void useLevel(ClientLevel level) {
        if (currentLevel != level) {
            reset();
            currentLevel = level;
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() == currentLevel) {
            reset();
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
    }

    @SubscribeEvent
    public static void onShutdown(GameShuttingDownEvent event) {
        reset();
    }

    public static void reset() {
        ACTIVE.clear();
        ClientMachineWaves.clear();
        FEEDBACK.clear();
        currentLevel = null;
        lastProcessedTime = Long.MIN_VALUE;
        PulseWaveRenderer.releaseTarget();
    }
}
