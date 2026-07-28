package com.secondition.creeperindustry.client.explosion;

import java.io.IOException;

import com.mojang.blaze3d.systems.RenderSystem;
import com.secondition.creeperindustry.CreeperIndustry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector4f;

final class ShockwavePostProcessor {
    private static final ResourceLocation POST_CHAIN = CreeperIndustry.asResource(
            "shaders/post/explosion_shockwave.json"
    );

    private PostChain chain;
    private PendingImpact pendingImpact;
    private int width;
    private int height;

    void reload(ResourceManager resourceManager) {
        closeChain();
        Minecraft minecraft = Minecraft.getInstance();
        try {
            chain = new PostChain(
                    minecraft.getTextureManager(),
                    resourceManager,
                    minecraft.getMainRenderTarget(),
                    POST_CHAIN
            );
            resizeIfNeeded(minecraft);
        } catch (IOException | RuntimeException exception) {
            CreeperIndustry.LOGGER.warn("Unable to load explosion shockwave post effect; using world-only fallback", exception);
            chain = null;
        }
    }

    void prepare(RenderLevelStageEvent event, ShockwaveImpact impact) {
        Vector4f clipPosition = new Vector4f(
                (float) (impact.center().x - event.getCamera().getPosition().x),
                (float) (impact.center().y - event.getCamera().getPosition().y),
                (float) (impact.center().z - event.getCamera().getPosition().z),
                1.0F
        );
        event.getModelViewMatrix().transform(clipPosition);
        event.getProjectionMatrix().transform(clipPosition);

        float centerX = 0.5F;
        float centerY = 0.5F;
        if (clipPosition.w > 0.01F) {
            float inverseW = 1.0F / clipPosition.w;
            centerX = Mth.clamp(clipPosition.x * inverseW * 0.5F + 0.5F, -0.25F, 1.25F);
            centerY = Mth.clamp(clipPosition.y * inverseW * 0.5F + 0.5F, -0.25F, 1.25F);
        }
        pendingImpact = new PendingImpact(centerX, centerY, impact.strength(), impact.phase());
    }

    void clearPending() {
        pendingImpact = null;
    }

    void processPending() {
        PendingImpact impact = pendingImpact;
        pendingImpact = null;
        if (impact == null || chain == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        float screenEffectScale = minecraft.options.screenEffectScale().get().floatValue();
        float strength = impact.strength() * screenEffectScale;
        if (strength <= 0.001F) {
            return;
        }

        try {
            resizeIfNeeded(minecraft);
            chain.setUniform("ShockwaveCenterX", impact.centerX());
            chain.setUniform("ShockwaveCenterY", impact.centerY());
            chain.setUniform("ShockwaveStrength", strength);
            chain.setUniform("ShockwavePhase", impact.phase());
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.resetTextureMatrix();
            chain.process(0.0F);
        } catch (RuntimeException exception) {
            CreeperIndustry.LOGGER.warn(
                    "Disabling explosion shockwave refraction after a rendering failure",
                    exception
            );
            closeChain();
        } finally {
            minecraft.getMainRenderTarget().bindWrite(true);
        }
    }

    private void resizeIfNeeded(Minecraft minecraft) {
        int currentWidth = minecraft.getWindow().getWidth();
        int currentHeight = minecraft.getWindow().getHeight();
        if (chain != null && (currentWidth != width || currentHeight != height)) {
            width = currentWidth;
            height = currentHeight;
            chain.resize(width, height);
        }
    }

    private void closeChain() {
        if (chain != null) {
            chain.close();
            chain = null;
        }
        width = 0;
        height = 0;
        pendingImpact = null;
    }

    private record PendingImpact(float centerX, float centerY, float strength, float phase) {
    }
}
