package com.secondition.creeperindustry.content.energy.signal;

import java.util.OptionalInt;

import net.minecraft.util.Mth;

public class DefaultExplosionSignalAmplitudeResolver implements ExplosionSignalAmplitudeResolver {
    @Override
    public OptionalInt resolveAmplitude(ExplosionSignalContext context) {
        if (context.explosionPower() <= 0.0F) {
            return OptionalInt.empty();
        }

        return OptionalInt.of(Mth.ceil(context.explosionPower() * 4.0F));
    }
}
