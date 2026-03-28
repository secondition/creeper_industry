package com.secondition.creeperindustry.content.energy.transmission;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public final class DuctTransmissionHelper {
    private DuctTransmissionHelper() {
    }

    public static boolean isDuct(BlockState state) {
        return state.getBlock() instanceof BlastproofDuctBlock;
    }

    public static boolean isInterface(BlockState state) {
        return state.getBlock() instanceof BlastproofDuctBlock && hasAnyInterface(state);
    }

    public static boolean canDuctConnect(Direction directionToNeighbor, BlockState neighborState) {
        return isDuct(neighborState);
    }

    public static boolean hasInterface(BlockState state, Direction direction) {
        return state.getBlock() instanceof BlastproofDuctBlock && state.getValue(interfaceProperty(direction));
    }

    public static boolean canInstallInterface(BlockState state, Direction direction) {
        return state.getBlock() instanceof BlastproofDuctBlock
                && !state.getValue(connectionProperty(direction))
                && !state.getValue(interfaceProperty(direction));
    }

    public static BooleanProperty interfaceProperty(Direction direction) {
        return switch (direction) {
            case NORTH -> BlastproofDuctBlock.INTERFACE_NORTH;
            case SOUTH -> BlastproofDuctBlock.INTERFACE_SOUTH;
            case EAST -> BlastproofDuctBlock.INTERFACE_EAST;
            case WEST -> BlastproofDuctBlock.INTERFACE_WEST;
            case UP -> BlastproofDuctBlock.INTERFACE_UP;
            case DOWN -> BlastproofDuctBlock.INTERFACE_DOWN;
        };
    }

    public static BooleanProperty connectionProperty(Direction direction) {
        return switch (direction) {
            case NORTH -> BlastproofDuctBlock.NORTH;
            case SOUTH -> BlastproofDuctBlock.SOUTH;
            case EAST -> BlastproofDuctBlock.EAST;
            case WEST -> BlastproofDuctBlock.WEST;
            case UP -> BlastproofDuctBlock.UP;
            case DOWN -> BlastproofDuctBlock.DOWN;
        };
    }
    private static boolean hasAnyInterface(BlockState state) {
        return hasInterface(state, Direction.NORTH)
                || hasInterface(state, Direction.SOUTH)
                || hasInterface(state, Direction.EAST)
                || hasInterface(state, Direction.WEST)
                || hasInterface(state, Direction.UP)
                || hasInterface(state, Direction.DOWN);
    }
}
