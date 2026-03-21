package com.secondition.creeperindustry.content.energy.signal;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.vehicle.MinecartTNT;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ExplosionSignalContextFactory {
    public Optional<ExplosionSignalContext> create(Level level, Explosion explosion) {
        ExplosionSignalKind kind = resolveKind(level, explosion);
        if (kind == ExplosionSignalKind.OTHER) {
            return Optional.empty();
        }

        Vec3 position = explosion.center();
        ResourceLocation sourceId = resolveSourceId(level, explosion, kind, position);
        return Optional.of(new ExplosionSignalContext(
                level.dimension(),
                position,
                level.getGameTime(),
                kind,
                explosion.radius(),
                sourceId
        ));
    }

    private ExplosionSignalKind resolveKind(Level level, Explosion explosion) {
        Entity directSource = explosion.getDirectSourceEntity();
        if (directSource instanceof Creeper) {
            return ExplosionSignalKind.CREEPER;
        }
        if (directSource instanceof PrimedTnt || directSource instanceof MinecartTNT) {
            return ExplosionSignalKind.TNT;
        }
        if (directSource instanceof EndCrystal) {
            return ExplosionSignalKind.END_CRYSTAL;
        }
        if (isBedExplosion(level, explosion.center())) {
            return ExplosionSignalKind.BED;
        }
        return ExplosionSignalKind.OTHER;
    }

    private ResourceLocation resolveSourceId(Level level, Explosion explosion, ExplosionSignalKind kind, Vec3 position) {
        Entity directSource = explosion.getDirectSourceEntity();
        if (directSource != null) {
            return BuiltInRegistries.ENTITY_TYPE.getKey(directSource.getType());
        }
        if (kind == ExplosionSignalKind.BED) {
            BlockPos nearestBedPos = findNearestBed(level, position);
            if (nearestBedPos != null) {
                return BuiltInRegistries.BLOCK.getKey(level.getBlockState(nearestBedPos).getBlock());
            }
        }
        return ResourceLocation.withDefaultNamespace("unknown");
    }

    private boolean isBedExplosion(Level level, Vec3 position) {
        return findNearestBed(level, position) != null;
    }

    private BlockPos findNearestBed(Level level, Vec3 position) {
        BlockPos center = BlockPos.containing(position);
        for (BlockPos candidate : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
            BlockState state = level.getBlockState(candidate);
            if (state.getBlock() instanceof BedBlock) {
                return candidate.immutable();
            }
        }
        return null;
    }
}
