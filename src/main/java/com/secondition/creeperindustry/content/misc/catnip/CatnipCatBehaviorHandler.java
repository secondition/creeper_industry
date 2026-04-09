package com.secondition.creeperindustry.content.misc.catnip;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nullable;

import com.secondition.creeperindustry.CIItems;
import com.secondition.creeperindustry.CreeperIndustry;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

@EventBusSubscriber(modid = CreeperIndustry.MODID)
public final class CatnipCatBehaviorHandler {
    private static final int GOAL_PRIORITY = 3;

    private CatnipCatBehaviorHandler() {
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Cat cat)) {
            return;
        }

        boolean installed = cat.goalSelector.getAvailableGoals()
                .stream()
                .anyMatch(goal -> goal.getGoal() instanceof CatnipPouncePlayerGoal);
        if (!installed) {
            cat.goalSelector.addGoal(GOAL_PRIORITY, new CatnipPouncePlayerGoal(cat));
        }
    }

    private static final class CatnipPouncePlayerGoal extends Goal {
        private static final double SEARCH_RANGE = 12.0D;
        private static final double SEARCH_RANGE_SQR = SEARCH_RANGE * SEARCH_RANGE;
        private static final double APPROACH_SPEED = 0.6D;
        private static final double CHASE_SPEED = 1.33D;
        private static final double POUNCE_MIN_DISTANCE_SQR = 2.25D;
        private static final double POUNCE_MAX_DISTANCE_SQR = 16.0D;
        private static final double STEAL_DISTANCE_SQR = 2.0D;
        private static final double POUNCE_HORIZONTAL_SPEED = 0.85D;
        private static final double POUNCE_VERTICAL_SPEED = 0.55D;
        private static final int STEAL_COOLDOWN_TICKS = 100;
        private static final int CELEBRATION_TICKS = 60;
        private static final int CELEBRATION_POSE_INTERVAL = 6;
        private static final int CELEBRATION_JUMP_INTERVAL = 10;
        private static final double CELEBRATION_JUMP_VERTICAL_SPEED = 0.3D;
        private static final float CELEBRATION_SPIN_DEGREES = 24.0F;

        private final Cat cat;
        @Nullable
        private Player targetPlayer;
        private int nextPounceTick;
        private int celebrationTicksRemaining;
        private boolean pouncing;

        private CatnipPouncePlayerGoal(Cat cat) {
            this.cat = cat;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (!canCatInteract()) {
                return false;
            }

            targetPlayer = findNearestCatnipHolder();
            return targetPlayer != null;
        }

        @Override
        public boolean canContinueToUse() {
            if (!canCatInteract()) {
                return false;
            }

            if (isCelebrating()) {
                return true;
            }

            if (targetPlayer == null || !targetPlayer.isAlive() || targetPlayer.isSpectator()) {
                return false;
            }

            if (pouncing && !cat.onGround()) {
                return true;
            }

            return isHoldingCatnip(targetPlayer) && cat.distanceToSqr(targetPlayer) <= SEARCH_RANGE_SQR * 4.0D;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            cat.setInSittingPose(false);
            cat.setPose(Pose.STANDING);
            pouncing = false;
        }

        @Override
        public void stop() {
            cat.getNavigation().stop();
            cat.setInSittingPose(false);
            cat.setPose(Pose.STANDING);
            targetPlayer = null;
            celebrationTicksRemaining = 0;
            pouncing = false;
        }

        @Override
        public void tick() {
            if (targetPlayer == null || !targetPlayer.isAlive() || targetPlayer.isSpectator() || !isHoldingCatnip(targetPlayer)) {
                targetPlayer = findNearestCatnipHolder();
            }

            if (targetPlayer != null) {
                cat.setInSittingPose(false);
                cat.setPose(Pose.STANDING);
                cat.getLookControl().setLookAt(targetPlayer, 30.0F, 30.0F);

                double distanceSqr = cat.distanceToSqr(targetPlayer);
                if (!pouncing) {
                    double speed = distanceSqr <= POUNCE_MAX_DISTANCE_SQR ? CHASE_SPEED : APPROACH_SPEED;
                    cat.getNavigation().moveTo(targetPlayer, speed);

                    if (cat.onGround()
                            && cat.tickCount >= nextPounceTick
                            && distanceSqr >= POUNCE_MIN_DISTANCE_SQR
                            && distanceSqr <= POUNCE_MAX_DISTANCE_SQR
                            && cat.hasLineOfSight(targetPlayer)) {
                        pounceAtTarget();
                    }
                } else if (cat.onGround()) {
                    pouncing = false;
                }

                if (distanceSqr <= STEAL_DISTANCE_SQR && stealCatnip(targetPlayer)) {
                    nextPounceTick = cat.tickCount + STEAL_COOLDOWN_TICKS;
                    celebrationTicksRemaining = CELEBRATION_TICKS;
                    pouncing = false;
                    cat.getNavigation().stop();
                }
            } else if (cat.onGround()) {
                pouncing = false;
            }

            if (isCelebrating()) {
                tickCelebration();
            }
        }

        private boolean canCatInteract() {
            return cat.isAlive()
                    && !cat.isOrderedToSit()
                    && !cat.isPassenger()
                    && !cat.isLeashed();
        }

        @Nullable
        private Player findNearestCatnipHolder() {
            List<Player> candidates = cat.level()
                    .getEntitiesOfClass(Player.class, cat.getBoundingBox().inflate(SEARCH_RANGE), this::isValidTargetPlayer);
            return candidates.stream()
                    .min(Comparator.comparingDouble(cat::distanceToSqr))
                    .orElse(null);
        }

        private boolean isValidTargetPlayer(Player player) {
            return player.isAlive()
                    && !player.isSpectator()
                    && isHoldingCatnip(player);
        }

        private boolean isHoldingCatnip(Player player) {
            return !getHeldCatnip(player).isEmpty();
        }

        private boolean isCelebrating() {
            return celebrationTicksRemaining > 0;
        }

        private ItemStack getHeldCatnip(Player player) {
            ItemStack mainHand = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (mainHand.is(CIItems.CATNIP.get())) {
                return mainHand;
            }

            ItemStack offHand = player.getItemInHand(InteractionHand.OFF_HAND);
            if (offHand.is(CIItems.CATNIP.get())) {
                return offHand;
            }

            return ItemStack.EMPTY;
        }

        private void pounceAtTarget() {
            if (targetPlayer == null) {
                return;
            }

            Vec3 horizontal = new Vec3(targetPlayer.getX() - cat.getX(), 0.0D, targetPlayer.getZ() - cat.getZ());
            if (horizontal.lengthSqr() < 1.0E-6D) {
                return;
            }

            Vec3 direction = horizontal.normalize();
            cat.setJumping(true);
            cat.getNavigation().stop();
            cat.setDeltaMovement(cat.getDeltaMovement().add(
                    direction.x * POUNCE_HORIZONTAL_SPEED,
                    POUNCE_VERTICAL_SPEED,
                    direction.z * POUNCE_HORIZONTAL_SPEED
            ));
            pouncing = true;
        }

        private void tickCelebration() {
            boolean crouchingBeat = ((CELEBRATION_TICKS - celebrationTicksRemaining) / CELEBRATION_POSE_INTERVAL) % 2 == 0;
            cat.setInSittingPose(false);
            cat.setPose(crouchingBeat ? Pose.CROUCHING : Pose.STANDING);

            if (cat.onGround() && celebrationTicksRemaining % CELEBRATION_JUMP_INTERVAL == 0) {
                Vec3 movement = cat.getDeltaMovement();
                cat.setJumping(true);
                cat.setDeltaMovement(movement.x, CELEBRATION_JUMP_VERTICAL_SPEED, movement.z);
            } else {
                cat.setJumping(false);
            }

            float spinYRot = cat.getYRot() + CELEBRATION_SPIN_DEGREES;
            cat.setYRot(spinYRot);
            cat.setYHeadRot(spinYRot);
            cat.setYBodyRot(spinYRot);

            celebrationTicksRemaining--;
            if (!isCelebrating()) {
                cat.setInSittingPose(false);
                cat.setPose(Pose.STANDING);
            }
        }

        private boolean stealCatnip(Player player) {
            ItemStack stack = getHeldCatnip(player);
            if (stack.isEmpty()) {
                return false;
            }

            stack.shrink(1);
            player.getInventory().setChanged();
            cat.playSound(SoundEvents.GENERIC_DRINK, 1.0F, 1.0F);
            return true;
        }
    }
}
