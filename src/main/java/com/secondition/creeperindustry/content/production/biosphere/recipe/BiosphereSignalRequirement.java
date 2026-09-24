package com.secondition.creeperindustry.content.production.biosphere.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Signed threshold for rising absolute-amplitude steps. */
public record BiosphereSignalRequirement(int startAmplitude) {
    public static final BiosphereSignalRequirement DEFAULT = new BiosphereSignalRequirement(9);
    public static final Codec<BiosphereSignalRequirement> CODEC =
            RecordCodecBuilder.create(i -> i.group(
                    Codec.intRange(-1000000, 1000000)
                            .validate(value -> value == 0
                                    ? DataResult.error(() -> "Start amplitude cannot be zero")
                                    : DataResult.success(value))
                            .optionalFieldOf("start_amplitude", 9)
                            .forGetter(BiosphereSignalRequirement::startAmplitude))
                    .apply(i, BiosphereSignalRequirement::new));
}
