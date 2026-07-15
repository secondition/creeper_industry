package com.secondition.creeperindustry.content.production.biosphere;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

public enum BiosphereType implements StringRepresentable {
    BOTANICAL("botanical"),
    ZOOLOGICAL("zoological"),
    MONSTER("monster");

    public static final Codec<BiosphereType> CODEC = StringRepresentable.fromEnum(BiosphereType::values);

    private final String serializedName;

    BiosphereType(String serializedName) {
        this.serializedName = serializedName;
    }

    @Override
    public String getSerializedName() {
        return serializedName;
    }
}
