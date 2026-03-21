package com.secondition.creeperindustry.content.energy.signal;

import java.util.Optional;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ExplosionSignalContextFactory {
    private final ExplosionSignalFilter filter;

    public ExplosionSignalContextFactory(ExplosionSignalFilter filter) {
        this.filter = filter;
    }

    public Optional<ExplosionSignalContext> create(Level level, Explosion explosion) {
        if (!filter.shouldCreateSignal(level, explosion)) {
            return Optional.empty();
        }

        Vec3 position = explosion.center();
        ResourceLocation sourceId = resolveSourceId(explosion);
        return Optional.of(new ExplosionSignalContext(
                level.dimension(),
                position,
                level.getGameTime(),
                explosion.radius(),
                sourceId
        ));
    }

    private ResourceLocation resolveSourceId(Explosion explosion) {
        Entity directSource = explosion.getDirectSourceEntity();
        if (directSource != null) {
            return BuiltInRegistries.ENTITY_TYPE.getKey(directSource.getType());
        }
        return ResourceLocation.withDefaultNamespace("unknown");
    }
}
