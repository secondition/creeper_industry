package com.secondition.creeperindustry.content.energy.transmission;

import com.secondition.creeperindustry.CISignalSourceTypes;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.BlockHitResult;
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
    public static final BooleanProperty INTERFACE_NORTH = BooleanProperty.create("interface_north");
    public static final BooleanProperty INTERFACE_SOUTH = BooleanProperty.create("interface_south");
    public static final BooleanProperty INTERFACE_EAST = BooleanProperty.create("interface_east");
    public static final BooleanProperty INTERFACE_WEST = BooleanProperty.create("interface_west");
    public static final BooleanProperty INTERFACE_UP = BooleanProperty.create("interface_up");
    public static final BooleanProperty INTERFACE_DOWN = BooleanProperty.create("interface_down");

    private static final VoxelShape CORE_SHAPE = Block.box(5.0D, 5.0D, 5.0D, 11.0D, 11.0D, 11.0D);
    private static final VoxelShape NORTH_SHAPE = Block.box(5.0D, 5.0D, 0.0D, 11.0D, 11.0D, 5.0D);
    private static final VoxelShape SOUTH_SHAPE = Block.box(5.0D, 5.0D, 11.0D, 11.0D, 11.0D, 16.0D);
    private static final VoxelShape EAST_SHAPE = Block.box(11.0D, 5.0D, 5.0D, 16.0D, 11.0D, 11.0D);
    private static final VoxelShape WEST_SHAPE = Block.box(0.0D, 5.0D, 5.0D, 5.0D, 11.0D, 11.0D);
    private static final VoxelShape UP_SHAPE = Block.box(5.0D, 11.0D, 5.0D, 11.0D, 16.0D, 11.0D);
    private static final VoxelShape DOWN_SHAPE = Block.box(5.0D, 0.0D, 5.0D, 11.0D, 5.0D, 11.0D);
    private static final VoxelShape INTERFACE_NORTH_SHAPE = Block.box(4.0D, 4.0D, 0.0D, 12.0D, 12.0D, 5.0D);
    private static final VoxelShape INTERFACE_SOUTH_SHAPE = Block.box(4.0D, 4.0D, 11.0D, 12.0D, 12.0D, 16.0D);
    private static final VoxelShape INTERFACE_EAST_SHAPE = Block.box(11.0D, 4.0D, 4.0D, 16.0D, 12.0D, 12.0D);
    private static final VoxelShape INTERFACE_WEST_SHAPE = Block.box(0.0D, 4.0D, 4.0D, 5.0D, 12.0D, 12.0D);
    private static final VoxelShape INTERFACE_UP_SHAPE = Block.box(4.0D, 11.0D, 4.0D, 12.0D, 16.0D, 12.0D);
    private static final VoxelShape INTERFACE_DOWN_SHAPE = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 5.0D, 12.0D);

    public BlastproofDuctBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(EAST, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false)
                .setValue(INTERFACE_NORTH, false)
                .setValue(INTERFACE_SOUTH, false)
                .setValue(INTERFACE_EAST, false)
                .setValue(INTERFACE_WEST, false)
                .setValue(INTERFACE_UP, false)
                .setValue(INTERFACE_DOWN, false));
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
        return state.setValue(propertyFor(direction), connectsTo(direction, neighborState));
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        return defaultBlockState()
                .setValue(NORTH, connectsTo(Direction.NORTH, level.getBlockState(pos.north())))
                .setValue(SOUTH, connectsTo(Direction.SOUTH, level.getBlockState(pos.south())))
                .setValue(EAST, connectsTo(Direction.EAST, level.getBlockState(pos.east())))
                .setValue(WEST, connectsTo(Direction.WEST, level.getBlockState(pos.west())))
                .setValue(UP, connectsTo(Direction.UP, level.getBlockState(pos.above())))
                .setValue(DOWN, connectsTo(Direction.DOWN, level.getBlockState(pos.below())));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN,
                INTERFACE_NORTH, INTERFACE_SOUTH, INTERFACE_EAST, INTERFACE_WEST, INTERFACE_UP, INTERFACE_DOWN);
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

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide() || !player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        Direction face = hitResult.getDirection();
        BooleanProperty interfaceProperty = DuctTransmissionHelper.interfaceProperty(face);
        if (!state.getValue(interfaceProperty)) {
            return InteractionResult.PASS;
        }

        BlockState updatedState = state.setValue(interfaceProperty, false);
        level.setBlock(pos, updatedState, Block.UPDATE_ALL);
        level.sendBlockUpdated(pos, state, updatedState, Block.UPDATE_ALL);
        level.updateNeighborsAt(pos, updatedState.getBlock());
        level.addFreshEntity(new ItemEntity(level,
                pos.getX() + 0.5D + face.getStepX() * 0.35D,
                pos.getY() + 0.5D + face.getStepY() * 0.35D,
                pos.getZ() + 0.5D + face.getStepZ() * 0.35D,
                new ItemStack(com.secondition.creeperindustry.CIItems.BLASTPROOF_DUCT_INTERFACE.get())));
        refreshAffectedReceivers(level, CISignalSourceTypes.ductNetworkManager().getAffectedReceiversForInterfaceChange(
                level,
                pos,
                CISignalSourceTypes.signalReceiverIndex(),
                CISignalSourceTypes.continuousSignalSourceRepository()
        ));
        return InteractionResult.CONSUME;
    }

    private void refreshAffectedReceivers(Level level, java.util.Collection<BlockPos> receiverPositions) {
        if (!receiverPositions.isEmpty()) {
            CISignalSourceTypes.deferredSignalRefreshQueue().enqueue(level.dimension(), receiverPositions);
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

    private static boolean connectsTo(Direction directionToNeighbor, BlockState state) {
        return DuctTransmissionHelper.canDuctConnect(directionToNeighbor, state);
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
        if (state.getValue(INTERFACE_NORTH)) {
            shape = Shapes.or(shape, INTERFACE_NORTH_SHAPE);
        }
        if (state.getValue(INTERFACE_SOUTH)) {
            shape = Shapes.or(shape, INTERFACE_SOUTH_SHAPE);
        }
        if (state.getValue(INTERFACE_EAST)) {
            shape = Shapes.or(shape, INTERFACE_EAST_SHAPE);
        }
        if (state.getValue(INTERFACE_WEST)) {
            shape = Shapes.or(shape, INTERFACE_WEST_SHAPE);
        }
        if (state.getValue(INTERFACE_UP)) {
            shape = Shapes.or(shape, INTERFACE_UP_SHAPE);
        }
        if (state.getValue(INTERFACE_DOWN)) {
            shape = Shapes.or(shape, INTERFACE_DOWN_SHAPE);
        }
        return shape;
    }
}
