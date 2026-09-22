package com.secondition.creeperindustry.content.production.biosphere.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.secondition.creeperindustry.content.energy.signal.AggregatedSignal;

/** Both polarities drive production; the threshold is strictly exceeded after superposition. */
public record BiosphereSignalRequirement(int minimumAmplitude) {
    public static final BiosphereSignalRequirement DEFAULT = new BiosphereSignalRequirement(9);
    public static final Codec<BiosphereSignalRequirement> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.intRange(0, 1000000)
                                                    .optionalFieldOf("minimum_amplitude", 9)
                                                    .forGetter(
                                                            BiosphereSignalRequirement
                                                                    ::minimumAmplitude))
                                    .apply(i, BiosphereSignalRequirement::new));

    public boolean matches(AggregatedSignal signal) {
        return signal.amplitude() > minimumAmplitude;
    }
}
