package com.secondition.creeperindustry.content.production.biosphere;

import com.secondition.creeperindustry.CIBlocks;

import net.minecraft.core.*;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/** One definition shared by manual structure validation and the built-in construction templates. */
public final class BiosphereStructure {
    public static final List<Integer> SIZES = List.of(3, 5, 7);

    public record Cell(BlockPos pos, BlockState state) {}

    private BiosphereStructure() {}

    public static BlockPos position(
            BlockPos controller, Direction facing, int size, int x, int y, int z) {
        return controller
                .relative(facing.getClockWise(), x - size / 2)
                .above(y - 1)
                .relative(facing.getOpposite(), z);
    }

    public static BlockPos receiver(BlockPos controller, Direction facing, int size) {
        return position(controller, facing, size, size / 2, 1, size - 1);
    }

    public static List<Cell> cells(BlockPos controller, BlockState controllerState, int size) {
        if (!SIZES.contains(size)) throw new IllegalArgumentException("Structure size");
        Direction facing = controllerState.getValue(BiosphereBlock.FACING);
        List<Cell> cells = new ArrayList<>(size * size * size);
        for (int y = 0; y < size; y++)
            for (int z = 0; z < size; z++)
                for (int x = 0; x < size; x++) {
                    BlockPos pos = position(controller, facing, size, x, y, z);
                    BlockState state;
                    if (x == size / 2 && y == 1 && z == 0) state = controllerState;
                    else if (x == size / 2 && y == 1 && z == size - 1)
                        state = CIBlocks.WAVE_RECEIVER.get().defaultBlockState();
                    else {
                        int borders =
                                (x == 0 || x == size - 1 ? 1 : 0)
                                        + (y == 0 || y == size - 1 ? 1 : 0)
                                        + (z == 0 || z == size - 1 ? 1 : 0);
                        state =
                                borders == 0
                                        ? net.minecraft.world.level.block.Blocks.AIR
                                                .defaultBlockState()
                                        : y == 0 || y == size - 1 || borders >= 2
                                                ? CIBlocks.BLASTPROOF_FRAME
                                                        .get()
                                                        .defaultBlockState()
                                                : CIBlocks.BLASTPROOF_GLASS
                                                        .get()
                                                        .defaultBlockState();
                    }
                    cells.add(new Cell(pos.immutable(), state));
                }
        return List.copyOf(cells);
    }
}
