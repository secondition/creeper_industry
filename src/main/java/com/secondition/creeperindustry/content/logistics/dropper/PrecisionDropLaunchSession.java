package com.secondition.creeperindustry.content.logistics.dropper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

final class PrecisionDropLaunchSession {
    private static final double DESCENT_CONFIRMATION_DISTANCE = 0.25D;

    enum Phase {
        ASCENDING,
        FALL_PROTECTED
    }

    private final BlockPos dropperPos;
    private final int targetX;
    private final int targetZ;
    private final ChunkPos targetChunk;
    private final long startedGameTime;
    private final long explosionProtectionUntil;
    private Phase phase = Phase.ASCENDING;
    private long fallProtectionStartedGameTime = Long.MIN_VALUE;
    private boolean chunkTicketHeld = true;
    private double highestObservedY = Double.NEGATIVE_INFINITY;

    PrecisionDropLaunchSession(BlockPos dropperPos, int targetX, int targetZ, long startedGameTime) {
        this.dropperPos = dropperPos.immutable();
        this.targetX = targetX;
        this.targetZ = targetZ;
        this.targetChunk = new ChunkPos(targetX >> 4, targetZ >> 4);
        this.startedGameTime = startedGameTime;
        this.explosionProtectionUntil = startedGameTime + 1L;
    }

    BlockPos dropperPos() {
        return dropperPos;
    }

    int targetX() {
        return targetX;
    }

    int targetZ() {
        return targetZ;
    }

    ChunkPos targetChunk() {
        return targetChunk;
    }

    long startedGameTime() {
        return startedGameTime;
    }

    long explosionProtectionUntil() {
        return explosionProtectionUntil;
    }

    Phase phase() {
        return phase;
    }

    long fallProtectionStartedGameTime() {
        return fallProtectionStartedGameTime;
    }

    void beginFallProtection(long gameTime) {
        phase = Phase.FALL_PROTECTED;
        fallProtectionStartedGameTime = gameTime;
    }

    boolean chunkTicketHeld() {
        return chunkTicketHeld;
    }

    void markChunkTicketReleased() {
        chunkTicketHeld = false;
    }

    boolean observeHeightAndCheckDescending(double currentY) {
        if (currentY > highestObservedY) {
            highestObservedY = currentY;
            return false;
        }
        return currentY < highestObservedY - DESCENT_CONFIRMATION_DISTANCE;
    }
}
