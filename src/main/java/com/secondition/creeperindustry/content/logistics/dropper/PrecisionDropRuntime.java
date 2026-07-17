package com.secondition.creeperindustry.content.logistics.dropper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.secondition.creeperindustry.CIChunkTickets;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public final class PrecisionDropRuntime {
    private static final int MAX_HORIZONTAL_COORDINATE = 29_999_984;
    private static final double TRANSFER_Y = 500.0D;
    private static final long MAX_ASCENT_TICKS = 400L;
    private static final long MAX_SESSION_TICKS = 1_200L;
    private static final long CHUNK_TICKET_GRACE_TICKS = 40L;
    private static final float EXPLOSION_POWER = 4.0F;
    private static final double MIN_HORIZONTAL_SPEED = 1.25D;
    private static final double MAX_HORIZONTAL_SPEED = 8.0D;
    private static final double HORIZONTAL_DISTANCE_SCALE = 32.0D;
    private static final double DISTANCE_VERTICAL_BONUS_FACTOR = 0.35D;
    private static final double MIN_INITIAL_VERTICAL_SPEED = 8.0D;
    private static final double MAX_INITIAL_VERTICAL_SPEED = 30.0D;
    private static final double TRANSFER_HEIGHT_MARGIN = 12.0D;
    private static final double EXPLOSION_PROTECTION_RADIUS_SQUARED = 36.0D;

    private final Map<UUID, PrecisionDropLaunchSession> sessions = new HashMap<>();

    public PrecisionDropLaunchResult start(
            ServerLevel level,
            ServerPlayer player,
            BlockPos dropperPos,
            int targetX,
            int targetZ
    ) {
        if (!isValidTargetCoordinate(targetX) || !isValidTargetCoordinate(targetZ)) {
            return PrecisionDropLaunchResult.INVALID_TARGET;
        }
        BlockPos targetPos = BlockPos.containing(targetX + 0.5D, TRANSFER_Y, targetZ + 0.5D);
        if (!level.getWorldBorder().isWithinBounds(targetPos)) {
            return PrecisionDropLaunchResult.OUTSIDE_WORLD_BORDER;
        }
        if (!isPlayerAvailable(player)) {
            return PrecisionDropLaunchResult.PLAYER_UNAVAILABLE;
        }
        if (!isStandingOnDropper(player, dropperPos)) {
            return PrecisionDropLaunchResult.NOT_ON_PLATFORM;
        }
        if (sessions.containsKey(player.getUUID())) {
            return PrecisionDropLaunchResult.PLAYER_BUSY;
        }
        if (sessions.values().stream().anyMatch(session -> session.dropperPos().equals(dropperPos))) {
            return PrecisionDropLaunchResult.DROPPER_BUSY;
        }

        PrecisionDropLaunchSession session = new PrecisionDropLaunchSession(
                dropperPos,
                targetX,
                targetZ,
                level.getGameTime()
        );
        boolean ticketAdded = CIChunkTickets.PRECISION_DROPPER.forceChunk(
                level,
                player.getUUID(),
                session.targetChunk().x,
                session.targetChunk().z,
                true,
                false
        );
        if (!ticketAdded && !level.hasChunk(session.targetChunk().x, session.targetChunk().z)) {
            return PrecisionDropLaunchResult.CHUNK_LOAD_FAILED;
        }

        sessions.put(player.getUUID(), session);
        launch(level, player, session);
        return PrecisionDropLaunchResult.SUCCESS;
    }

    public void tick(ServerLevel level) {
        long gameTime = level.getGameTime();
        Iterator<Map.Entry<UUID, PrecisionDropLaunchSession>> iterator = sessions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, PrecisionDropLaunchSession> entry = iterator.next();
            ServerPlayer player = level.getServer().getPlayerList().getPlayer(entry.getKey());
            PrecisionDropLaunchSession session = entry.getValue();
            if (player == null || player.level() != level || !player.isAlive() || player.isSpectator()) {
                finish(level, entry.getKey(), session, iterator);
                continue;
            }
            if (gameTime - session.startedGameTime() > MAX_SESSION_TICKS) {
                finish(level, entry.getKey(), session, iterator);
                continue;
            }

            if (session.phase() == PrecisionDropLaunchSession.Phase.ASCENDING) {
                tickAscending(level, player, session, gameTime);
            } else {
                tickFallProtection(level, player, session, gameTime, iterator);
            }
        }
    }

    public boolean shouldCancelDamage(ServerLevel level, ServerPlayer player, DamageSource source) {
        PrecisionDropLaunchSession session = sessions.get(player.getUUID());
        if (session == null) {
            return false;
        }
        if (session.phase() == PrecisionDropLaunchSession.Phase.ASCENDING
                && level.getGameTime() <= session.explosionProtectionUntil()
                && source.is(DamageTypeTags.IS_EXPLOSION)
                && isLaunchExplosion(source, session)) {
            return true;
        }
        if (source.is(DamageTypes.FALL)) {
            player.fallDistance = 0.0F;
            finish(level, player.getUUID(), session);
            return true;
        }
        return false;
    }

    public void cancel(ServerLevel level, UUID playerId) {
        PrecisionDropLaunchSession session = sessions.get(playerId);
        if (session != null) {
            finish(level, playerId, session);
        }
    }

    public void close(ServerLevel level) {
        for (Map.Entry<UUID, PrecisionDropLaunchSession> entry : sessions.entrySet()) {
            releaseChunkTicket(level, entry.getKey(), entry.getValue());
        }
        sessions.clear();
    }

    private void launch(ServerLevel level, ServerPlayer player, PrecisionDropLaunchSession session) {
        Vec3 explosionCenter = Vec3.atCenterOf(session.dropperPos()).add(0.0D, 0.75D, 0.0D);
        level.explode(
                null,
                explosionCenter.x,
                explosionCenter.y,
                explosionCenter.z,
                EXPLOSION_POWER,
                Level.ExplosionInteraction.NONE
        );

        double deltaX = session.targetX() + 0.5D - player.getX();
        double deltaZ = session.targetZ() + 0.5D - player.getZ();
        double horizontalLength = Math.hypot(deltaX, deltaZ);
        double directionX = horizontalLength > 1.0E-6D ? deltaX / horizontalLength : player.getLookAngle().x;
        double directionZ = horizontalLength > 1.0E-6D ? deltaZ / horizontalLength : player.getLookAngle().z;
        double horizontalSpeed = calculateHorizontalSpeed(horizontalLength);
        double verticalSpeed = calculateInitialVerticalSpeed(player.getY(), horizontalSpeed);
        Vec3 launchVelocity = new Vec3(
                directionX * horizontalSpeed,
                verticalSpeed,
                directionZ * horizontalSpeed
        );
        player.setDeltaMovement(launchVelocity);
        player.fallDistance = 0.0F;
        PacketDistributor.sendToPlayer(player, new PrecisionDropLaunchPayload(
                launchVelocity.x,
                launchVelocity.y,
                launchVelocity.z
        ));
        player.hurtMarked = false;
    }

    private void tickAscending(
            ServerLevel level,
            ServerPlayer player,
            PrecisionDropLaunchSession session,
            long gameTime
    ) {
        if (player.getY() >= TRANSFER_Y) {
            boolean teleported = player.teleportTo(
                    level,
                    session.targetX() + 0.5D,
                    TRANSFER_Y,
                    session.targetZ() + 0.5D,
                    Set.of(),
                    player.getYRot(),
                    player.getXRot()
            );
            if (!teleported) {
                releaseChunkTicket(level, player.getUUID(), session);
                session.beginFallProtection(gameTime);
                return;
            }
            player.setDeltaMovement(0.0D, -0.1D, 0.0D);
            player.hurtMarked = true;
            player.fallDistance = 0.0F;
            session.beginFallProtection(gameTime);
            return;
        }
        if (gameTime - session.startedGameTime() > MAX_ASCENT_TICKS
                || gameTime - session.startedGameTime() > 2L
                        && (player.verticalCollision || player.getDeltaMovement().y <= 0.0D)) {
            releaseChunkTicket(level, player.getUUID(), session);
            session.beginFallProtection(gameTime);
        }
    }

    private void tickFallProtection(
            ServerLevel level,
            ServerPlayer player,
            PrecisionDropLaunchSession session,
            long gameTime,
            Iterator<Map.Entry<UUID, PrecisionDropLaunchSession>> iterator
    ) {
        long protectedTicks = gameTime - session.fallProtectionStartedGameTime();
        if (protectedTicks >= CHUNK_TICKET_GRACE_TICKS) {
            releaseChunkTicket(level, player.getUUID(), session);
        }
        if (protectedTicks > 2L && (player.onGround() || player.isInWater() || player.isFallFlying() || player.getAbilities().flying)) {
            player.fallDistance = 0.0F;
            finish(level, player.getUUID(), session, iterator);
        }
    }

    private void finish(
            ServerLevel level,
            UUID playerId,
            PrecisionDropLaunchSession session,
            Iterator<Map.Entry<UUID, PrecisionDropLaunchSession>> iterator
    ) {
        releaseChunkTicket(level, playerId, session);
        iterator.remove();
    }

    private void finish(ServerLevel level, UUID playerId, PrecisionDropLaunchSession session) {
        releaseChunkTicket(level, playerId, session);
        sessions.remove(playerId);
    }

    private void releaseChunkTicket(ServerLevel level, UUID playerId, PrecisionDropLaunchSession session) {
        if (!session.chunkTicketHeld()) {
            return;
        }
        CIChunkTickets.PRECISION_DROPPER.forceChunk(
                level,
                playerId,
                session.targetChunk().x,
                session.targetChunk().z,
                false,
                false
        );
        session.markChunkTicketReleased();
    }

    static boolean isValidTargetCoordinate(int coordinate) {
        return coordinate >= -MAX_HORIZONTAL_COORDINATE && coordinate <= MAX_HORIZONTAL_COORDINATE;
    }

    private static boolean isPlayerAvailable(ServerPlayer player) {
        return player.isAlive()
                && !player.isSpectator()
                && !player.isPassenger()
                && !player.isSleeping();
    }

    private static boolean isStandingOnDropper(ServerPlayer player, BlockPos dropperPos) {
        double centerX = dropperPos.getX() + 0.5D;
        double centerZ = dropperPos.getZ() + 0.5D;
        double expectedY = dropperPos.getY() + 1.0D;
        return Math.abs(player.getX() - centerX) <= 0.7D
                && Math.abs(player.getZ() - centerZ) <= 0.7D
                && player.getY() >= expectedY - 0.15D
                && player.getY() <= expectedY + 0.75D;
    }

    private static boolean isLaunchExplosion(DamageSource source, PrecisionDropLaunchSession session) {
        Vec3 sourcePosition = source.getSourcePosition();
        if (sourcePosition == null) {
            return false;
        }
        Vec3 launchCenter = Vec3.atCenterOf(session.dropperPos()).add(0.0D, 0.75D, 0.0D);
        return sourcePosition.distanceToSqr(launchCenter) <= EXPLOSION_PROTECTION_RADIUS_SQUARED;
    }

    private static double calculateHorizontalSpeed(double horizontalDistance) {
        return Math.clamp(
                MIN_HORIZONTAL_SPEED + Math.sqrt(horizontalDistance) / HORIZONTAL_DISTANCE_SCALE,
                MIN_HORIZONTAL_SPEED,
                MAX_HORIZONTAL_SPEED
        );
    }

    private static double calculateInitialVerticalSpeed(double startY, double horizontalSpeed) {
        double low = MIN_INITIAL_VERTICAL_SPEED;
        double high = MAX_INITIAL_VERTICAL_SPEED;
        for (int i = 0; i < 24; i++) {
            double middle = (low + high) * 0.5D;
            if (simulateMaximumY(startY, middle) >= TRANSFER_Y + TRANSFER_HEIGHT_MARGIN) {
                high = middle;
            } else {
                low = middle;
            }
        }
        double distanceBonus = (horizontalSpeed - MIN_HORIZONTAL_SPEED) * DISTANCE_VERTICAL_BONUS_FACTOR;
        return Math.min(MAX_INITIAL_VERTICAL_SPEED, high + distanceBonus);
    }

    private static double simulateMaximumY(double startY, double initialVelocity) {
        double y = startY;
        double velocity = initialVelocity;
        for (int tick = 0; tick < MAX_ASCENT_TICKS && velocity > 0.0D; tick++) {
            y += velocity;
            velocity = (velocity - 0.08D) * 0.98D;
        }
        return y;
    }

}
