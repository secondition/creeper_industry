package com.secondition.creeperindustry.content.logistics.rocket;

import com.secondition.creeperindustry.CIItems;
import com.secondition.creeperindustry.content.logistics.launcher.GuidedFireworkReceiverBlockEntity;
import com.secondition.creeperindustry.content.logistics.rocket.runtime.RocketFlightRuntimeAccess;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;

public class GuidedFireworkRocketEntity extends Entity implements ItemSupplier {
    private static final int MAX_FLIGHT_TICKS = 7200;
    private static final double SPEED = 1.25D;
    private RocketDestination destination;
    private ItemStack cargo = ItemStack.EMPTY;
    private BlockPos launchPos = BlockPos.ZERO;
    private int flightTicks;
    private boolean resolved;

    public GuidedFireworkRocketEntity(EntityType<? extends GuidedFireworkRocketEntity> type, Level level) { super(type, level); }

    public void initialize(RocketDestination destination, ItemStack cargo, BlockPos launchPos) {
        this.destination = destination;
        this.cargo = cargo.copyWithCount(1);
        this.launchPos = launchPos.immutable();
    }

    @Override public void tick() {
        super.tick();
        if (level().isClientSide()) {
            if (tickCount % 2 == 0) level().addParticle(ParticleTypes.FIREWORK, getX(), getY(), getZ(), 0.0D, -0.05D, 0.0D);
            return;
        }
        if (!(level() instanceof ServerLevel level) || resolved || destination == null || cargo.isEmpty()) { discard(); return; }
        RocketFlightRuntimeAccess.get(level).claim(level, getUUID(), chunkPosition());
        if (++flightTicks > MAX_FLIGHT_TICKS) { fail(level); return; }

        Vec3 receiver = Vec3.atCenterOf(destination.receiverPos()).add(0.0D, 0.75D, 0.0D);
        double cruiseY = Math.min(level.getMaxBuildHeight() - 8.0D, Math.max(launchPos.getY(), destination.receiverPos().getY()) + 32.0D);
        Vec3 target;
        if (getY() < cruiseY - 1.0D && horizontalDistanceTo(receiver) > 12.0D) {
            target = new Vec3(getX(), cruiseY, getZ());
        } else if (horizontalDistanceTo(receiver) > 8.0D) {
            target = new Vec3(receiver.x, cruiseY, receiver.z);
        } else {
            target = receiver;
        }
        Vec3 delta = target.subtract(position());
        if (position().distanceToSqr(receiver) <= 2.25D) { deliver(level); return; }
        Vec3 velocity = delta.normalize().scale(SPEED);
        ChunkPos next = new ChunkPos(BlockPos.containing(position().add(velocity.scale(4.0D))));
        if (!RocketFlightRuntimeAccess.get(level).ensure(level, getUUID(), next)) { setDeltaMovement(Vec3.ZERO); return; }
        setDeltaMovement(velocity);
        move(MoverType.SELF, velocity);
        if (horizontalCollision || verticalCollision) fail(level);
    }

    private double horizontalDistanceTo(Vec3 target) { return Math.hypot(target.x - getX(), target.z - getZ()); }

    private void deliver(ServerLevel level) {
        if (level.getBlockEntity(destination.receiverPos()) instanceof GuidedFireworkReceiverBlockEntity receiver) {
            GuidedFireworkReceiverBlockEntity.ReceptionResult result = receiver.acceptRocketCargo(destination, cargo);
            if (result == GuidedFireworkReceiverBlockEntity.ReceptionResult.ACCEPTED) {
                cargo = ItemStack.EMPTY;
                finish(level, false);
                return;
            }
            if (result == GuidedFireworkReceiverBlockEntity.ReceptionResult.FULL) {
                cargo = ItemStack.EMPTY;
                finish(level, true);
                return;
            }
        }
        fail(level);
    }

    private void fail(ServerLevel level) {
        if (!cargo.isEmpty()) {
            ItemEntity drop = new ItemEntity(level, getX(), getY(), getZ(), cargo.copy());
            if (level.addFreshEntity(drop)) cargo = ItemStack.EMPTY;
        }
        if (!cargo.isEmpty()) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        finish(level, true);
    }

    private void finish(ServerLevel level, boolean explosion) {
        resolved = true;
        if (explosion) level.broadcastEntityEvent(this, (byte)17);
        RocketFlightRuntimeAccess.get(level).finish(level, getUUID());
        discard();
    }

    @Override public ItemStack getItem() { return new ItemStack(CIItems.GUIDED_FIREWORK_ROCKET.get()); }
    @Override public void handleEntityEvent(byte id) {
        if (id == 17) level().addParticle(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 0.0D, 0.0D, 0.0D);
        else super.handleEntityEvent(id);
    }
    @Override public boolean isPickable() { return true; }
    @Override public boolean hurt(DamageSource source, float amount) {
        if (level() instanceof ServerLevel level && !resolved) fail(level);
        return true;
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override protected void addAdditionalSaveData(CompoundTag tag) {
        if (destination != null) {
            tag.putString("TargetDimension", destination.dimension().location().toString());
            tag.putLong("TargetPos", destination.receiverPos().asLong());
            tag.putUUID("ReceiverId", destination.receiverId());
        }
        tag.put("Cargo", cargo.saveOptional(level().registryAccess()));
        tag.putLong("LaunchPos", launchPos.asLong());
        tag.putInt("FlightTicks", flightTicks);
    }

    @Override protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("TargetDimension") && tag.hasUUID("ReceiverId")) {
            var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                    net.minecraft.resources.ResourceLocation.parse(tag.getString("TargetDimension")));
            destination = new RocketDestination(key, BlockPos.of(tag.getLong("TargetPos")), tag.getUUID("ReceiverId"));
        }
        cargo = ItemStack.parseOptional(level().registryAccess(), tag.getCompound("Cargo"));
        launchPos = BlockPos.of(tag.getLong("LaunchPos"));
        flightTicks = tag.getInt("FlightTicks");
    }
}
