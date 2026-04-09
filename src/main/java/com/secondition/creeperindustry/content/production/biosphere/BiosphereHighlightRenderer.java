package com.secondition.creeperindustry.content.production.biosphere;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;

public final class BiosphereHighlightRenderer {
    private static final float OUTLINE_RED = 0.0F;
    private static final float OUTLINE_GREEN = 0.0F;
    private static final float OUTLINE_BLUE = 0.0F;
    private static final float OUTLINE_ALPHA = 0.4F;

    private BiosphereHighlightRenderer() {
    }

    public static void onRenderBlockHighlight(RenderHighlightEvent.Block event) {
        Level level = event.getCamera().getEntity() != null ? event.getCamera().getEntity().level() : null;
        if (level == null) {
            return;
        }

        BlockPos hitPos = event.getTarget().getBlockPos();
        BlockState state = level.getBlockState(hitPos);
        if (!(state.getBlock() instanceof BiosphereBlock biosphereBlock)) {
            return;
        }

        BlockPos controllerPos = BiosphereBlock.getControllerPos(hitPos, state);
        BlockState controllerState = level.getBlockState(controllerPos);
        if (!(controllerState.getBlock() instanceof BiosphereBlock)) {
            return;
        }

        VoxelShape outlineShape = BiosphereApproximateCollisionShapes.getStructureShape(
                biosphereBlock.getType(),
                controllerState.getValue(BiosphereBlock.FACING)
        );
        if (outlineShape.isEmpty()) {
            return;
        }

        event.setCanceled(true);

        Vec3 cameraPos = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource bufferSource = event.getMultiBufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());

        renderShape(
                poseStack,
                consumer,
                outlineShape,
                controllerPos.getX() - cameraPos.x,
                controllerPos.getY() - cameraPos.y,
                controllerPos.getZ() - cameraPos.z,
                OUTLINE_RED,
                OUTLINE_GREEN,
                OUTLINE_BLUE,
                OUTLINE_ALPHA
        );
    }

    private static void renderShape(
            PoseStack poseStack,
            VertexConsumer consumer,
            VoxelShape shape,
            double x,
            double y,
            double z,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        PoseStack.Pose pose = poseStack.last();
        shape.forAllEdges((minX, minY, minZ, maxX, maxY, maxZ) -> {
            float normalX = (float) (maxX - minX);
            float normalY = (float) (maxY - minY);
            float normalZ = (float) (maxZ - minZ);
            float length = Mth.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
            if (length <= 1.0E-6F) {
                return;
            }

            normalX /= length;
            normalY /= length;
            normalZ /= length;
            consumer.addVertex(pose, (float) (minX + x), (float) (minY + y), (float) (minZ + z))
                    .setColor(red, green, blue, alpha)
                    .setNormal(pose, normalX, normalY, normalZ);
            consumer.addVertex(pose, (float) (maxX + x), (float) (maxY + y), (float) (maxZ + z))
                    .setColor(red, green, blue, alpha)
                    .setNormal(pose, normalX, normalY, normalZ);
        });
    }
}
