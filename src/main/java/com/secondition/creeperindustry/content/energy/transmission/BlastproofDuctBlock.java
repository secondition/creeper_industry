package com.secondition.creeperindustry.content.energy.transmission;

import com.secondition.creeperindustry.CISignalSourceTypes;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.LinkedHashSet;

public class BlastproofDuctBlock extends Block {
    public static final MapCodec<BlastproofDuctBlock> CODEC = simpleCodec(BlastproofDuctBlock::new);
    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    private static final VoxelShape CORE_SHAPE = Block.box(5.0D, 5.0D, 5.0D, 11.0D, 11.0D, 11.0D);
    private static final VoxelShape NORTH_SHAPE = Block.box(5.0D, 5.0D, 0.0D, 11.0D, 11.0D, 5.0D);
    private static final VoxelShape SOUTH_SHAPE = Block.box(5.0D, 5.0D, 11.0D, 11.0D, 11.0D, 16.0D);
    private static final VoxelShape EAST_SHAPE = Block.box(11.0D, 5.0D, 5.0D, 16.0D, 11.0D, 11.0D);
    private static final VoxelShape WEST_SHAPE = Block.box(0.0D, 5.0D, 5.0D, 5.0D, 11.0D, 11.0D);
    private static final VoxelShape UP_SHAPE = Block.box(5.0D, 11.0D, 5.0D, 11.0D, 16.0D, 11.0D);
    private static final VoxelShape DOWN_SHAPE = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 5.0D, 11.0D);

    public BlastproofDuctBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(EAST, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !oldState.is(state.getBlock())) {
            refreshAffectedReceivers(level, CISignalSourceTypes.ductNetworkManager().getAffectedReceiversAfterPlacement(
                    level,
                    pos,
                    CISignalSourceTypes.signalReceiverIndex(),
                    CISignalSourceTypes.continuousSignalSourceRepository()
            ));
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        LinkedHashSet<BlockPos> affectedReceivers = new LinkedHashSet<>();
        if (!level.isClientSide() && !state.is(newState.getBlock())) {
            affectedReceivers.addAll(CISignalSourceTypes.ductNetworkManager().getAffectedReceiversBeforeRemoval(
                    level,
                    pos,
                    CISignalSourceTypes.signalReceiverIndex(),
                    CISignalSourceTypes.continuousSignalSourceRepository()
            ));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (!level.isClientSide() && !state.is(newState.getBlock())) {
            affectedReceivers.addAll(CISignalSourceTypes.ductNetworkManager().getAffectedReceiversAfterRemoval(
                    level,
                    pos,
                    CISignalSourceTypes.signalReceiverIndex(),
                    CISignalSourceTypes.continuousSignalSourceRepository()
            ));
            refreshAffectedReceivers(level, affectedReceivers);
        }
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(propertyFor(direction), connectsTo(neighborState));
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        return defaultBlockState()
                .setValue(NORTH, connectsTo(level.getBlockState(pos.north())))
                .setValue(SOUTH, connectsTo(level.getBlockState(pos.south())))
                .setValue(EAST, connectsTo(level.getBlockState(pos.east())))
                .setValue(WEST, connectsTo(level.getBlockState(pos.west())))
                .setValue(UP, connectsTo(level.getBlockState(pos.above())))
                .setValue(DOWN, connectsTo(level.getBlockState(pos.below())));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return buildShape(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return buildShape(state);
    }

    private void refreshAffectedReceivers(Level level, java.util.Collection<BlockPos> receiverPositions) {
        if (!receiverPositions.isEmpty()) {
            CISignalSourceTypes.continuousSignalUpdateService().refreshReceivers(level, receiverPositions);
        }
    }

    private static BooleanProperty propertyFor(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    private static boolean connectsTo(BlockState state) {
        return state.getBlock() instanceof BlastproofDuctBlock;
    }

    private static VoxelShape buildShape(BlockState state) {
        VoxelShape shape = CORE_SHAPE;
        if (state.getValue(NORTH)) {
            shape = Shapes.or(shape, NORTH_SHAPE);
        }
        if (state.getValue(SOUTH)) {
            shape = Shapes.or(shape, SOUTH_SHAPE);
        }
        if (state.getValue(EAST)) {
            shape = Shapes.or(shape, EAST_SHAPE);
        }
        if (state.getValue(WEST)) {
            shape = Shapes.or(shape, WEST_SHAPE);
        }
        if (state.getValue(UP)) {
            shape = Shapes.or(shape, UP_SHAPE);
        }
        if (state.getValue(DOWN)) {
            shape = Shapes.or(shape, DOWN_SHAPE);
        }
        return shape;
    }
}
