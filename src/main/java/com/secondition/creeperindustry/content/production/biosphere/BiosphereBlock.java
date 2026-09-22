package com.secondition.creeperindustry.content.production.biosphere;

import com.secondition.creeperindustry.foundation.blockEntity.SimpleEntityBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

public abstract class BiosphereBlock extends SimpleEntityBlock {
    public static final int WIDTH_X = 3;
    public static final int HEIGHT_Y = 7;
    public static final int DEPTH_Z = 2;

    public static final IntegerProperty X_PART = IntegerProperty.create("x_part", 0, WIDTH_X - 1);
    public static final IntegerProperty Y_PART = IntegerProperty.create("y_part", 0, HEIGHT_Y - 1);
    public static final IntegerProperty Z_PART = IntegerProperty.create("z_part", 0, DEPTH_Z - 1);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final ThreadLocal<Set<BlockPos>> REMOVING_STRUCTURES =
            ThreadLocal.withInitial(HashSet::new);

    private final BiosphereType type;

    protected BiosphereBlock(BiosphereType type, BlockBehaviour.Properties properties) {
        super(properties);
        this.type = type;
        registerDefaultState(
                defaultBlockState()
                        .setValue(X_PART, 0)
                        .setValue(Y_PART, 0)
                        .setValue(Z_PART, 0)
                        .setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    public BiosphereType getType() {
        return type;
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BiosphereBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (tryCycleOutputSelection(level, pos, state, player)) {
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        if (openMenu(level, pos, state, player)) {
            return InteractionResult.sidedSuccess(level.isClientSide());
        }
        return InteractionResult.PASS;
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult) {
        if (openMenu(level, pos, state, player)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onRemove(
            BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!state.is(next.getBlock())
                && !level.isClientSide()
                && isController(state)
                && level.getBlockEntity(pos) instanceof BiosphereBlockEntity machine)
            Containers.dropContents(level, pos, machine);
        super.onRemove(state, level, pos, next, moved);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(X_PART, Y_PART, Z_PART, FACING);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    public static boolean isController(BlockState state) {
        return state.getValue(X_PART) == 0
                && state.getValue(Y_PART) == 0
                && state.getValue(Z_PART) == 0;
    }

    public static BlockPos getControllerPos(BlockPos pos, BlockState state) {
        BlockPos worldOffset =
                rotateOffset(
                        state.getValue(FACING),
                        state.getValue(X_PART),
                        state.getValue(Y_PART),
                        state.getValue(Z_PART));
        return pos.subtract(worldOffset);
    }

    public static BlockPos getPartPos(
            BlockPos controllerPos,
            net.minecraft.core.Direction facing,
            int xPart,
            int yPart,
            int zPart) {
        return controllerPos.offset(rotateOffset(facing, xPart, yPart, zPart));
    }

    public static AABB getStructureBounds(
            BlockPos controllerPos, net.minecraft.core.Direction facing) {
        int minX = controllerPos.getX();
        int minY = controllerPos.getY();
        int minZ = controllerPos.getZ();
        int maxX = controllerPos.getX() + 1;
        int maxY = controllerPos.getY() + 1;
        int maxZ = controllerPos.getZ() + 1;

        for (BlockPos partPos : iterateStructure(controllerPos, facing)) {
            minX = Math.min(minX, partPos.getX());
            minY = Math.min(minY, partPos.getY());
            minZ = Math.min(minZ, partPos.getZ());
            maxX = Math.max(maxX, partPos.getX() + 1);
            maxY = Math.max(maxY, partPos.getY() + 1);
            maxZ = Math.max(maxZ, partPos.getZ() + 1);
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private VoxelShape getApproximateShape(BlockState state) {
        return BiosphereApproximateCollisionShapes.getShape(
                type,
                state.getValue(X_PART),
                state.getValue(Y_PART),
                state.getValue(Z_PART),
                state.getValue(FACING));
    }

    private boolean openMenu(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.isClientSide()) {
            return true;
        }

        BlockEntity blockEntity = level.getBlockEntity(getControllerPos(pos, state));
        if (!(blockEntity instanceof BiosphereBlockEntity biosphere)) {
            return false;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(biosphere);
        }
        return true;
    }

    private boolean tryCycleOutputSelection(
            Level level, BlockPos pos, BlockState state, Player player) {
        if (type == BiosphereType.BOTANICAL || !player.isShiftKeyDown()) {
            return false;
        }
        if (level.isClientSide()) {
            return true;
        }

        BlockEntity blockEntity = level.getBlockEntity(getControllerPos(pos, state));
        return blockEntity instanceof BiosphereBlockEntity biosphere
                && biosphere.cycleOutputSelection(player);
    }

    private void removeOtherParts(Level level, BlockPos controllerPos, BlockPos removedPos) {
        for (BlockPos partPos :
                iterateStructure(controllerPos, getStructureFacing(level, controllerPos))) {
            if (partPos.equals(removedPos)) {
                continue;
            }
            BlockState partState = level.getBlockState(partPos);
            if (partState.getBlock() == this) {
                level.setBlock(partPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    private static Iterable<BlockPos> iterateStructure(
            BlockPos origin, net.minecraft.core.Direction facing) {
        List<BlockPos> positions = new ArrayList<>(WIDTH_X * HEIGHT_Y * DEPTH_Z);
        for (int x = 0; x < WIDTH_X; x++) {
            for (int y = 0; y < HEIGHT_Y; y++) {
                for (int z = 0; z < DEPTH_Z; z++) {
                    positions.add(getPartPos(origin, facing, x, y, z).immutable());
                }
            }
        }
        return positions;
    }

    private static BlockPos rotateOffset(
            net.minecraft.core.Direction facing, int xPart, int yPart, int zPart) {
        return switch (facing) {
            case NORTH -> new BlockPos(xPart, yPart, zPart);
            case EAST -> new BlockPos(-zPart, yPart, xPart);
            case SOUTH -> new BlockPos(-xPart, yPart, -zPart);
            case WEST -> new BlockPos(zPart, yPart, -xPart);
            default ->
                    throw new IllegalArgumentException("Unsupported biosphere facing: " + facing);
        };
    }

    private net.minecraft.core.Direction getStructureFacing(Level level, BlockPos controllerPos) {
        BlockState controllerState = level.getBlockState(controllerPos);
        if (controllerState.getBlock() == this) {
            return controllerState.getValue(FACING);
        }
        return defaultBlockState().getValue(FACING);
    }
}
