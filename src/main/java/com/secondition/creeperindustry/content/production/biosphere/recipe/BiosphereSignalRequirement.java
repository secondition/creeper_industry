package com.secondition.creeperindustry.content.production.biosphere.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Strength required for a slow-wave working edge. */
public record BiosphereSignalRequirement(int startAmplitude) {
    public static final BiosphereSignalRequirement DEFAULT = new BiosphereSignalRequirement(9);
    public static final Codec<BiosphereSignalRequirement> CODEC =
            RecordCodecBuilder.create(i -> i.group(
                    Codec.intRange(1, 1000000).optionalFieldOf("start_amplitude", 9)
                            .forGetter(BiosphereSignalRequirement::startAmplitude))
                    .apply(i, BiosphereSignalRequirement::new));
}
