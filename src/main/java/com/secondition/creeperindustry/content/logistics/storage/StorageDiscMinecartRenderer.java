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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StorageDiscMinecartRenderer extends EntityRenderer<StorageDiscMinecartEntity> {
    private static final float MODEL_SCALE = 1.35F;
    private static final float BODY_BASE_HEIGHT = 0.03125F;
    private static final float TOP_LAYER_OFFSET = 0.03125F;
    private static final ItemStack GLASS_PANE_STACK = new ItemStack(Items.GLASS_PANE);

    private final ItemRenderer itemRenderer;

    public StorageDiscMinecartRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.0F;
        this.shadowStrength = 0.0F;
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

        poseStack.translate(0.0F, BODY_BASE_HEIGHT, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(270.0F - entityYaw));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-pitch));
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));

        float hurtTime = entity.getHurtTime() - partialTicks;
        float damage = entity.getDamage() - partialTicks;
        if (damage < 0.0F) {
            damage = 0.0F;
        }
        if (hurtTime > 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.sin(hurtTime) * hurtTime * damage / 10.0F * entity.getHurtDir()));
        }

        poseStack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
        renderLayer(GLASS_PANE_STACK, 0.0F, poseStack, buffer, packedLight, entity.getId());
        renderLayer(entity.getDiscStack(), TOP_LAYER_OFFSET, poseStack, buffer, packedLight, entity.getId() + 1);
        poseStack.popPose();
    }

    private void renderLayer(ItemStack stack, float heightOffset, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int seed) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, heightOffset);
        this.itemRenderer.renderStatic(
                stack,
                ItemDisplayContext.GROUND,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                null,
                seed
        );
        poseStack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(StorageDiscMinecartEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
