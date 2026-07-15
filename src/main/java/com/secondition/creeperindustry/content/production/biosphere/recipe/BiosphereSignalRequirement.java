package com.secondition.creeperindustry.content.production.biosphere.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.secondition.creeperindustry.content.energy.signal.AggregatedSignal;

import net.minecraft.util.ExtraCodecs;

public record BiosphereSignalRequirement(
        int minimumAmplitude,
        BiosphereInstantaneousRequirement instantaneous
) {
    public static final BiosphereSignalRequirement DEFAULT = new BiosphereSignalRequirement(
            9,
            BiosphereInstantaneousRequirement.POSITIVE
    );
    public static final Codec<BiosphereSignalRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("minimum_amplitude", 9)
                    .forGetter(BiosphereSignalRequirement::minimumAmplitude),
            BiosphereInstantaneousRequirement.CODEC.optionalFieldOf(
                    "instantaneous",
                    BiosphereInstantaneousRequirement.POSITIVE
            ).forGetter(BiosphereSignalRequirement::instantaneous)
    ).apply(instance, BiosphereSignalRequirement::new));

    public boolean matches(AggregatedSignal signal) {
        return signal.signal().amplitude() >= minimumAmplitude
                && instantaneous.matches(signal.instantaneousValue());
    }
}
