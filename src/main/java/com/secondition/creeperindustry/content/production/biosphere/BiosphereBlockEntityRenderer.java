package com.secondition.creeperindustry.content.production.biosphere;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

public class BiosphereBlockEntityRenderer implements BlockEntityRenderer<BiosphereBlockEntity> {
    private static final double DISPLAY_X = 1.0D;
    private static final double DISPLAY_Y = 1.35D;
    private static final double DISPLAY_Z = 1.0D;
    private static final float DISPLAY_SCALE = 1.65F;
    private static final float ROTATION_SPEED = 4.0F;

    private final ItemRenderer itemRenderer;

    public BiosphereBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(BiosphereBlockEntity biosphere, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!BiosphereBlock.isController(biosphere.getBlockState()) || biosphere.getBiosphereType() != BiosphereType.BOTANICAL) {
            return;
        }

        ItemStack sapling = biosphere.getDisplayedSapling();
        if (sapling.isEmpty()) {
            return;
        }

        ItemStack renderedStack = sapling.copy();
        renderedStack.setCount(1);

        double gameTime = (biosphere.getLevel() != null ? biosphere.getLevel().getGameTime() : 0L) + partialTick;
        double bobOffset = Math.sin(gameTime * 0.1D) * 0.05D;

        poseStack.pushPose();
        poseStack.translate(DISPLAY_X, DISPLAY_Y + bobOffset, DISPLAY_Z);
        poseStack.mulPose(Axis.YP.rotationDegrees((float)(gameTime * ROTATION_SPEED)));
        poseStack.scale(DISPLAY_SCALE, DISPLAY_SCALE, DISPLAY_SCALE);
        itemRenderer.renderStatic(renderedStack, ItemDisplayContext.GROUND, packedLight, packedOverlay, poseStack, bufferSource, biosphere.getLevel(), 0);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(BiosphereBlockEntity biosphere) {
        if (!BiosphereBlock.isController(biosphere.getBlockState())) {
            return BlockEntityRenderer.super.getRenderBoundingBox(biosphere);
        }

        return new AABB(
                biosphere.getBlockPos().getX(),
                biosphere.getBlockPos().getY(),
                biosphere.getBlockPos().getZ(),
                biosphere.getBlockPos().getX() + BiosphereBlock.WIDTH_X,
                biosphere.getBlockPos().getY() + BiosphereBlock.HEIGHT_Y,
                biosphere.getBlockPos().getZ() + BiosphereBlock.DEPTH_Z);
    }
}
