package com.secondition.creeperindustry.content.energy.signal;

import java.util.OptionalInt;

public interface ExplosionSignalAmplitudeResolver {
    OptionalInt resolveAmplitude(ExplosionSignalContext context);
}
