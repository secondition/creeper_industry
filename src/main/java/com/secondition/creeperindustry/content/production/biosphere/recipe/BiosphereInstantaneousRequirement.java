package com.secondition.creeperindustry.content.production.biosphere.recipe;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

public enum BiosphereInstantaneousRequirement implements StringRepresentable {
    ANY("any"),
    POSITIVE("positive"),
    NEGATIVE("negative");

    public static final Codec<BiosphereInstantaneousRequirement> CODEC = StringRepresentable.fromEnum(
            BiosphereInstantaneousRequirement::values
    );

    private final String serializedName;

    BiosphereInstantaneousRequirement(String serializedName) {
        this.serializedName = serializedName;
    }

    public boolean matches(int instantaneousValue) {
        return switch (this) {
            case ANY -> true;
            case POSITIVE -> instantaneousValue > 0;
            case NEGATIVE -> instantaneousValue < 0;
        };
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
