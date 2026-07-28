package com.secondition.creeperindustry.client.explosion;

import com.secondition.creeperindustry.content.explosion.ExplosionShockwaveClientBridge;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class ExplosionVisualClient {
    private static final ShockwaveManager MANAGER = new ShockwaveManager();
    private static final ShockwaveRenderer RENDERER = new ShockwaveRenderer();
    private static final ShockwavePostProcessor POST_PROCESSOR = new ShockwavePostProcessor();

    private ExplosionVisualClient() {
    }

    public static void initialize() {
        ExplosionShockwaveClientBridge.register(MANAGER::receive);
        NeoForge.EVENT_BUS.addListener(ExplosionVisualClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(ExplosionVisualClient::onRenderLevelStage);
        NeoForge.EVENT_BUS.addListener(ExplosionVisualClient::onRenderGui);
        NeoForge.EVENT_BUS.addListener(ExplosionVisualClient::onCameraAngles);
        NeoForge.EVENT_BUS.addListener(ExplosionVisualClient::onLogout);
    }

    public static void registerShaders(RegisterShadersEvent event) {
        ShockwaveRenderer.registerShader(event);
    }

    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) POST_PROCESSOR::reload);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        MANAGER.tick();
    }

    private static void onRenderLevelStage(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            POST_PROCESSOR.clearPending();
            return;
        }

        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            MANAGER.prepareFrame(event.getCamera());
            RENDERER.render(event, MANAGER);
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
            MANAGER.strongestImpact(event.getCamera(), minecraft.level.getGameTime(), partialTick)
                    .ifPresentOrElse(
                            impact -> POST_PROCESSOR.prepare(event, impact),
                            POST_PROCESSOR::clearPending
                    );
        }
    }

    private static void onRenderGui(RenderGuiEvent.Pre event) {
        POST_PROCESSOR.processPending();
    }

    private static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        MANAGER.prepareFrame(event.getCamera());
        float partialTick = (float) event.getPartialTick();
        MANAGER.strongestImpact(event.getCamera(), minecraft.level.getGameTime(), partialTick).ifPresent(impact -> {
            float effectScale = minecraft.options.screenEffectScale().get().floatValue();
            float strength = impact.strength() * effectScale;
            if (strength <= 0.001F) {
                return;
            }
            float time = minecraft.level.getGameTime() + partialTick;
            float oscillation = time * 2.6F + impact.phase() * (float) (Math.PI * 2.0);
            event.setYaw(event.getYaw() + Mth.sin(oscillation) * strength * 0.45F);
            event.setPitch(event.getPitch() + Mth.cos(oscillation * 1.13F) * strength * 0.65F);
            event.setRoll(event.getRoll() + Mth.sin(oscillation * 0.79F) * strength * 0.35F);
        });
    }

    private static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        MANAGER.clear();
        POST_PROCESSOR.clearPending();
    }
}
