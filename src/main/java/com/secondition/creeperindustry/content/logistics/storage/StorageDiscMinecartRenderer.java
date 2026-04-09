package com.secondition.creeperindustry.content.logistics.storage;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StorageDiscMinecartRenderer extends EntityRenderer<StorageDiscMinecartEntity> {
    private final ItemRenderer itemRenderer;

    public StorageDiscMinecartRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.35F;
    }

    @Override
    public void render(StorageDiscMinecartEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        poseStack.pushPose();

        double x = Mth.lerp(partialTicks, entity.xOld, entity.getX());
        double y = Mth.lerp(partialTicks, entity.yOld, entity.getY());
        double z = Mth.lerp(partialTicks, entity.zOld, entity.getZ());
        Vec3 railPos = entity.getPos(x, y, z);
        float pitch = Mth.lerp(partialTicks, entity.xRotO, entity.getXRot());

        if (railPos != null) {
            Vec3 ahead = entity.getPosOffs(x, y, z, 0.3F);
            Vec3 behind = entity.getPosOffs(x, y, z, -0.3F);
            if (ahead == null) {
                ahead = railPos;
            }
            if (behind == null) {
                behind = railPos;
            }

            poseStack.translate(railPos.x - x, (ahead.y + behind.y) / 2.0D - y, railPos.z - z);
            Vec3 direction = behind.add(-ahead.x, -ahead.y, -ahead.z);
            if (direction.lengthSqr() > 0.0D) {
                direction = direction.normalize();
                entityYaw = (float) (Math.atan2(direction.z, direction.x) * 180.0D / Math.PI);
                pitch = (float) (Math.atan(direction.y) * 73.0D);
            }
        }

        poseStack.translate(0.0F, 0.1F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-pitch));

        float hurtTime = entity.getHurtTime() - partialTicks;
        float damage = entity.getDamage() - partialTicks;
        if (damage < 0.0F) {
            damage = 0.0F;
        }
        if (hurtTime > 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(hurtTime) * hurtTime * damage / 10.0F * entity.getHurtDir()));
        }

        poseStack.scale(1.35F, 1.35F, 1.35F);
        this.itemRenderer.renderStatic(
                entity.getDiscStack(),
                ItemDisplayContext.GROUND,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.getId()
        );
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(StorageDiscMinecartEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
