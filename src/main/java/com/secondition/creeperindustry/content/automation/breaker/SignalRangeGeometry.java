package com.secondition.creeperindustry.content.automation.breaker;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public final class SignalRangeGeometry {
    private SignalRangeGeometry() {
    }

    public static List<BlockPos> collectPositions(BlockPos machinePos, Direction facing, SignalRange range) {
        Direction right = localRight(facing);
        Direction up = localUp(facing);
        int halfWidth = range.width() / 2;
        int halfHeight = range.height() / 2;
        List<BlockPos> positions = new ArrayList<>(range.volume());

        for (int depthOffset = 1; depthOffset <= range.depth(); depthOffset++) {
            BlockPos layerCenter = machinePos.relative(facing, depthOffset);
            for (int heightOffset = -halfHeight; heightOffset <= halfHeight; heightOffset++) {
                for (int widthOffset = -halfWidth; widthOffset <= halfWidth; widthOffset++) {
                    positions.add(layerCenter.relative(right, widthOffset).relative(up, heightOffset).immutable());
                }
            }
        }
        return List.copyOf(positions);
    }

    private static Direction localRight(Direction facing) {
        return switch (facing) {
            case UP -> Direction.EAST;
            case DOWN -> Direction.WEST;
            default -> facing.getClockWise();
        };
    }

    private static Direction localUp(Direction facing) {
        return switch (facing) {
            case UP, DOWN -> Direction.NORTH;
            default -> Direction.UP;
        };
    }
}
