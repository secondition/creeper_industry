package com.secondition.creeperindustry.client.explosion.wave;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.secondition.creeperindustry.CreeperIndustry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

/** Depth-aware refraction of camera-relative spheres in one bounded fullscreen pass. */
public final class PulseWaveRenderer {
    private static final int MAX_RENDERED_WAVES = 8;
    private static final ClientPulseWave[] SELECTED = new ClientPulseWave[MAX_RENDERED_WAVES];
    private static final double[] SCORES = new double[MAX_RENDERED_WAVES];
    private static ShaderInstance shader;
    private static RenderTarget sceneCopy;

    private PulseWaveRenderer() {}

    public static void registerShader(RegisterShadersEvent event) throws IOException {
        // GameRenderer owns and closes registered shaders during reload/shutdown.
        shader = null;
        ClientPulseWavePresentation.reset();
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        CreeperIndustry.asResource("pulse_wave_refraction"),
                        DefaultVertexFormat.POSITION),
                loaded -> shader = loaded);
    }

    static void render(
            RenderLevelStageEvent event,
            Collection<ClientPulseWave> waves,
            double gameTime,
            float impactStrength,
            float impactAge) {
        Minecraft minecraft = Minecraft.getInstance();
        if (shader == null) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        int count = selectWaves(event, waves, camera, gameTime);
        // Arrival feedback must finish even when the wave has expired or is offscreen.
        if (count == 0 && impactStrength <= 0.001F) {
            return;
        }
        RenderTarget main = minecraft.getMainRenderTarget();
        if (main.width <= 0 || main.height <= 0 || main.getDepthTextureId() < 0) {
            Arrays.fill(SELECTED, null);
            return;
        }

        // AFTER_LEVEL is after Fabulous' transparency composition and before the hand
        // and GUI. Copy both attachments to avoid sampling an attached draw texture.
        RenderState previous = new RenderState();
        try {
            RenderSystem.activeTexture(GL13.GL_TEXTURE0);
            ensureTarget(main);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, sceneCopy.frameBufferId);
            GlStateManager._glBlitFrameBuffer(
                    0,
                    0,
                    main.width,
                    main.height,
                    0,
                    0,
                    main.width,
                    main.height,
                    GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT,
                    GL11.GL_NEAREST);
            main.bindWrite(true);

            Matrix4f viewProjection =
                    new Matrix4f(event.getProjectionMatrix()).mul(event.getModelViewMatrix());
            shader.safeGetUniform("ViewProjection").set(viewProjection);
            shader.safeGetUniform("InverseViewProjection")
                    .set(new Matrix4f(viewProjection).invert());
            shader.safeGetUniform("ViewportSize")
                    .set((float) main.viewWidth, (float) main.viewHeight);
            shader.safeGetUniform("ImpactStrength").set(impactStrength);
            shader.safeGetUniform("ImpactAge").set(impactAge);
            shader.safeGetUniform("WaveCount").set(count);
            for (int i = 0; i < count; i++) {
                ClientPulseWave wave = SELECTED[i];
                Vec3 center = wave.origin().subtract(camera);
                float radius = (float) wave.radiusAt(gameTime);
                shader.safeGetUniform("Wave" + i)
                        .set((float) center.x, (float) center.y, (float) center.z, radius);
                float seed = (wave.id().getLeastSignificantBits() & 0xFFFF) / 65535.0F * 6.283185F;
                shader.safeGetUniform("Style" + i)
                        .set(
                                wave.visualStrength(radius),
                                wave.shellWidth(radius),
                                seed,
                                wave.polarity());
            }
            shader.setSampler("SceneColor", sceneCopy.getColorTextureId());
            shader.setSampler("SceneDepth", sceneCopy.getDepthTextureId());
            RenderSystem.disableBlend();
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            shader.apply();
            BufferBuilder quad =
                    RenderSystem.renderThreadTesselator()
                            .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            quad.addVertex(-1.0F, -1.0F, 0.0F);
            quad.addVertex(1.0F, -1.0F, 0.0F);
            quad.addVertex(1.0F, 1.0F, 0.0F);
            quad.addVertex(-1.0F, 1.0F, 0.0F);
            BufferUploader.draw(quad.buildOrThrow());
        } finally {
            shader.clear();
            previous.restore();
            Arrays.fill(SELECTED, null);
        }
    }

    private static int selectWaves(
            RenderLevelStageEvent event,
            Collection<ClientPulseWave> waves,
            Vec3 camera,
            double gameTime) {
        Arrays.fill(SELECTED, null);
        Arrays.fill(SCORES, -1.0);
        int count = 0;
        for (ClientPulseWave wave : waves) {
            double radius = wave.radiusAt(gameTime);
            float strength = wave.visualStrength(radius);
            if (radius <= 0.0 || strength <= 0.001F || wave.isExpired(gameTime)) {
                continue;
            }
            Vec3 origin = wave.origin();
            if (!event.getFrustum()
                    .isVisible(
                            new AABB(
                                    origin.x - radius,
                                    origin.y - radius,
                                    origin.z - radius,
                                    origin.x + radius,
                                    origin.y + radius,
                                    origin.z + radius))) {
                continue;
            }
            double distanceSquared = origin.distanceToSqr(camera);
            double score =
                    strength * Math.min(1.0, radius * radius / Math.max(1.0, distanceSquared));
            int index = Math.min(count, MAX_RENDERED_WAVES - 1);
            if (count == MAX_RENDERED_WAVES && score <= SCORES[index]) {
                continue;
            }
            while (index > 0 && score > SCORES[index - 1]) {
                SELECTED[index] = SELECTED[index - 1];
                SCORES[index] = SCORES[index - 1];
                index--;
            }
            SELECTED[index] = wave;
            SCORES[index] = score;
            count = Math.min(count + 1, MAX_RENDERED_WAVES);
        }
        return count;
    }

    private static void ensureTarget(RenderTarget main) {
        if (sceneCopy != null
                && (sceneCopy.width != main.width
                        || sceneCopy.height != main.height
                        || sceneCopy.isStencilEnabled() != main.isStencilEnabled())) {
            sceneCopy.destroyBuffers();
            sceneCopy = null;
        }
        if (sceneCopy == null) {
            sceneCopy = new TextureTarget(main.width, main.height, true, Minecraft.ON_OSX);
            if (main.isStencilEnabled()) {
                sceneCopy.enableStencil();
            }
            sceneCopy.setFilterMode(GL11.GL_LINEAR);
        }
    }

    public static void releaseTarget() {
        if (sceneCopy != null) {
            sceneCopy.destroyBuffers();
            sceneCopy = null;
        }
        Arrays.fill(SELECTED, null);
    }

    /** Preserve GL state touched by the pass, including target allocation on resize. */
    private static final class RenderState {
        private final boolean depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        private final boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
        private final boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        private final boolean depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        private final int readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        private final int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        private final int activeTexture = GlStateManager._getActiveTexture();
        private final int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        private final int[] viewport = new int[4];
        private final int[] textures = new int[2];

        private RenderState() {
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            for (int i = 0; i < textures.length; i++) {
                RenderSystem.activeTexture(GL13.GL_TEXTURE0 + i);
                textures[i] = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
            }
            RenderSystem.activeTexture(activeTexture);
        }

        private void restore() {
            if (depthTest) RenderSystem.enableDepthTest();
            else RenderSystem.disableDepthTest();
            if (blend) RenderSystem.enableBlend();
            else RenderSystem.disableBlend();
            if (cull) RenderSystem.enableCull();
            else RenderSystem.disableCull();
            RenderSystem.depthMask(depthMask);
            for (int i = 0; i < textures.length; i++) {
                RenderSystem.activeTexture(GL13.GL_TEXTURE0 + i);
                RenderSystem.bindTexture(textures[i]);
            }
            RenderSystem.activeTexture(activeTexture);
            GlStateManager._glUseProgram(program);
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        }
    }
}
