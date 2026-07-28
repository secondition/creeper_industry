package com.secondition.creeperindustry.client.explosion;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.secondition.creeperindustry.CreeperIndustry;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

final class ShockwaveRenderer {
    private static final int LATITUDE_SEGMENTS = 10;
    private static final int LONGITUDE_SEGMENTS = 20;
    private static final float TWO_PI = (float) (Math.PI * 2.0);

    private static ShaderInstance shockwaveShader;
    private static final RenderType SHOCKWAVE_RENDER_TYPE = RenderType.create(
            "creeper_industry_explosion_shockwave",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.TRIANGLES,
            RenderType.TRANSIENT_BUFFER_SIZE,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(
                            () -> shockwaveShader != null ? shockwaveShader : GameRenderer.getPositionColorShader()
                    ))
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false)
    );

    static void registerShader(RegisterShadersEvent event) {
        try {
            event.registerShader(
                    new ShaderInstance(
                            event.getResourceProvider(),
                            CreeperIndustry.asResource("explosion_shockwave"),
                            DefaultVertexFormat.POSITION_COLOR
                    ),
                    shader -> shockwaveShader = shader
            );
        } catch (Exception exception) {
            shockwaveShader = null;
            CreeperIndustry.LOGGER.warn(
                    "Unable to load explosion shockwave world shader; using the built-in color shader",
                    exception
            );
        }
    }

    void render(RenderLevelStageEvent event, ShockwaveManager manager) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        long gameTime = minecraft.level.getGameTime();
        Camera camera = event.getCamera();
        Vec3 cameraPosition = camera.getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(SHOCKWAVE_RENDER_TYPE);
        boolean rendered = false;

        for (ClientShockwave shockwave : manager.visibleShockwaves(gameTime, partialTick)) {
            float radius = shockwave.radius(gameTime, partialTick);
            if (radius <= 0.2F || radius >= shockwave.maxRadius()) {
                continue;
            }
            AABB bounds = AABB.ofSize(shockwave.center(), radius * 2.1, radius * 2.1, radius * 2.1);
            if (!event.getFrustum().isVisible(bounds)) {
                continue;
            }

            poseStack.pushPose();
            poseStack.translate(
                    shockwave.center().x - cameraPosition.x,
                    shockwave.center().y - cameraPosition.y,
                    shockwave.center().z - cameraPosition.z
            );
            poseStack.scale(radius, radius, radius);
            renderSphere(
                    poseStack,
                    consumer,
                    shockwave,
                    cameraPosition.subtract(shockwave.center()).scale(1.0 / radius),
                    radius / shockwave.maxRadius(),
                    shockwave.age(gameTime, partialTick)
            );
            poseStack.popPose();
            rendered = true;
        }

        if (rendered) {
            bufferSource.endBatch(SHOCKWAVE_RENDER_TYPE);
        }
    }

    private void renderSphere(
            PoseStack poseStack,
            VertexConsumer consumer,
            ClientShockwave shockwave,
            Vec3 cameraLocal,
            float normalizedRadius,
            float age
    ) {
        float distanceFade = 1.0F - normalizedRadius;
        distanceFade *= distanceFade;
        float baseAlpha = (0.025F + 0.12F * distanceFade) * shockwave.visibility();
        float warmAmount = Mth.clamp(1.0F - age / 5.0F, 0.0F, 1.0F);
        float red = Mth.lerp(warmAmount, 0.70F, 1.0F);
        float green = Mth.lerp(warmAmount, 0.88F, 0.72F);
        float blue = Mth.lerp(warmAmount, 1.0F, 0.36F);
        float seedPhase = (shockwave.visualSeed() & 4095L) * (TWO_PI / 4096.0F);

        for (int latitude = 0; latitude < LATITUDE_SEGMENTS; latitude++) {
            float latitude0 = (float) (-Math.PI / 2.0 + Math.PI * latitude / LATITUDE_SEGMENTS);
            float latitude1 = (float) (-Math.PI / 2.0 + Math.PI * (latitude + 1) / LATITUDE_SEGMENTS);
            for (int longitude = 0; longitude < LONGITUDE_SEGMENTS; longitude++) {
                float longitude0 = TWO_PI * longitude / LONGITUDE_SEGMENTS;
                float longitude1 = TWO_PI * (longitude + 1) / LONGITUDE_SEGMENTS;

                SphereVertex a = vertex(latitude0, longitude0, age, seedPhase, cameraLocal, baseAlpha);
                SphereVertex b = vertex(latitude1, longitude0, age, seedPhase, cameraLocal, baseAlpha);
                SphereVertex c = vertex(latitude1, longitude1, age, seedPhase, cameraLocal, baseAlpha);
                SphereVertex d = vertex(latitude0, longitude1, age, seedPhase, cameraLocal, baseAlpha);
                addTriangle(poseStack, consumer, a, b, c, red, green, blue);
                addTriangle(poseStack, consumer, a, c, d, red, green, blue);
            }
        }
    }

    private SphereVertex vertex(
            float latitude,
            float longitude,
            float age,
            float seedPhase,
            Vec3 cameraLocal,
            float baseAlpha
    ) {
        double cosLatitude = Math.cos(latitude);
        Vec3 normal = new Vec3(
                cosLatitude * Math.cos(longitude),
                Math.sin(latitude),
                cosLatitude * Math.sin(longitude)
        );
        float ripple = Mth.sin(longitude * 3.0F + latitude * 5.0F + age * 0.42F + seedPhase)
                + 0.5F * Mth.sin(longitude * 7.0F - latitude * 4.0F - age * 0.31F + seedPhase * 0.37F);
        Vec3 position = normal.scale(1.0 + ripple * 0.018);
        Vec3 viewDirection = cameraLocal.subtract(position).normalize();
        float facing = (float) Math.abs(normal.dot(viewDirection));
        float fresnel = 1.0F - facing;
        float alpha = baseAlpha * (0.25F + 0.75F * fresnel * fresnel);
        return new SphereVertex(position, alpha);
    }

    private void addTriangle(
            PoseStack poseStack,
            VertexConsumer consumer,
            SphereVertex first,
            SphereVertex second,
            SphereVertex third,
            float red,
            float green,
            float blue
    ) {
        addVertex(poseStack, consumer, first, red, green, blue);
        addVertex(poseStack, consumer, second, red, green, blue);
        addVertex(poseStack, consumer, third, red, green, blue);
    }

    private void addVertex(
            PoseStack poseStack,
            VertexConsumer consumer,
            SphereVertex vertex,
            float red,
            float green,
            float blue
    ) {
        consumer.addVertex(
                poseStack.last(),
                (float) vertex.position().x,
                (float) vertex.position().y,
                (float) vertex.position().z
        ).setColor(red, green, blue, vertex.alpha());
    }

    private record SphereVertex(Vec3 position, float alpha) {
    }
}
