package com.secondition.creeperindustry.content.production.biosphere;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import com.secondition.creeperindustry.CIBlockEntityTypes;
import com.secondition.creeperindustry.foundation.blockEntity.SimpleEntityBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public abstract class BiosphereBlock extends SimpleEntityBlock {
    public static final int WIDTH_X = 3;
    public static final int HEIGHT_Y = 7;
    public static final int DEPTH_Z = 2;

    public static final IntegerProperty X_PART = IntegerProperty.create("x_part", 0, WIDTH_X - 1);
    public static final IntegerProperty Y_PART = IntegerProperty.create("y_part", 0, HEIGHT_Y - 1);
    public static final IntegerProperty Z_PART = IntegerProperty.create("z_part", 0, DEPTH_Z - 1);

    private static final ThreadLocal<Set<BlockPos>> REMOVING_STRUCTURES = ThreadLocal.withInitial(HashSet::new);

    private final BiosphereType type;

    protected BiosphereBlock(BiosphereType type, BlockBehaviour.Properties properties) {
        super(properties);
        this.type = type;
        registerDefaultState(defaultBlockState()
                .setValue(X_PART, 0)
                .setValue(Y_PART, 0)
                .setValue(Z_PART, 0));
    }

    public BiosphereType getType() {
        return type;
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos origin = context.getClickedPos();
        for (BlockPos partPos : iterateStructure(origin)) {
            if (!level.isInWorldBounds(partPos) || !level.getWorldBorder().isWithinBounds(partPos)) {
                return null;
            }
            if (!level.getBlockState(partPos).canBeReplaced(context)) {
                return null;
            }
        }
        return defaultBlockState();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) {
            return;
        }

        for (int x = 0; x < WIDTH_X; x++) {
            for (int y = 0; y < HEIGHT_Y; y++) {
                for (int z = 0; z < DEPTH_Z; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }
                    level.setBlock(pos.offset(x, y, z), state
                            .setValue(X_PART, x)
                            .setValue(Y_PART, y)
                            .setValue(Z_PART, z), Block.UPDATE_ALL);
                }
            }
        }
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BiosphereBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(blockEntityType, CIBlockEntityTypes.BIOSPHERE.get(), BiosphereBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
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
            BlockHitResult hitResult
    ) {
        if (openMenu(level, pos, state, player)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (state.is(newState.getBlock())) {
            super.onRemove(state, level, pos, newState, movedByPiston);
            return;
        }

        if (!level.isClientSide()) {
            BlockPos controllerPos = getControllerPos(pos, state);
            Set<BlockPos> removing = REMOVING_STRUCTURES.get();
            boolean rootRemoval = removing.add(controllerPos.immutable());
            if (rootRemoval) {
                try {
                    if (level.getBlockEntity(controllerPos) instanceof BiosphereBlockEntity biosphere) {
                        Containers.dropContents(level, controllerPos, biosphere);
                    }
                    removeOtherParts(level, controllerPos, pos);
                } finally {
                    removing.remove(controllerPos);
                    if (removing.isEmpty()) {
                        REMOVING_STRUCTURES.remove();
                    }
                }
            }
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(X_PART, Y_PART, Z_PART);
    }

    public static boolean isController(BlockState state) {
        return state.getValue(X_PART) == 0
                && state.getValue(Y_PART) == 0
                && state.getValue(Z_PART) == 0;
    }

    public static BlockPos getControllerPos(BlockPos pos, BlockState state) {
        return pos.offset(-state.getValue(X_PART), -state.getValue(Y_PART), -state.getValue(Z_PART));
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

    private void removeOtherParts(Level level, BlockPos controllerPos, BlockPos removedPos) {
        for (BlockPos partPos : iterateStructure(controllerPos)) {
            if (partPos.equals(removedPos)) {
                continue;
            }
            BlockState partState = level.getBlockState(partPos);
            if (partState.getBlock() == this) {
                level.setBlock(partPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    private static Iterable<BlockPos> iterateStructure(BlockPos origin) {
        Set<BlockPos> positions = new HashSet<>();
        for (int x = 0; x < WIDTH_X; x++) {
            for (int y = 0; y < HEIGHT_Y; y++) {
                for (int z = 0; z < DEPTH_Z; z++) {
                    positions.add(origin.offset(x, y, z).immutable());
                }
            }
        }
        return positions;
    }
}
