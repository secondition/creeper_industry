package com.secondition.creeperindustry.content.energy.signal;

import java.util.Locale;

public enum CreativeSignalSourceSignalType {
    PULSE(0, "pulse"),
    CONTINUOUS(1, "continuous");

    private final int serializedId;
    private final String serializedName;

    CreativeSignalSourceSignalType(int serializedId, String serializedName) {
        this.serializedId = serializedId;
        this.serializedName = serializedName;
    }

    public int getSerializedId() {
        return serializedId;
    }

    public String getSerializedName() {
        return serializedName;
    }

    public static CreativeSignalSourceSignalType bySerializedId(int serializedId) {
        for (CreativeSignalSourceSignalType type : values()) {
            if (type.serializedId == serializedId) {
                return type;
            }
        }
        return CONTINUOUS;
    }

    public static CreativeSignalSourceSignalType bySerializedName(String serializedName) {
        if (serializedName == null || serializedName.isBlank()) {
            return CONTINUOUS;
        }

        String normalizedName = serializedName.toLowerCase(Locale.ROOT);
        for (CreativeSignalSourceSignalType type : values()) {
            if (type.serializedName.equals(normalizedName)) {
                return type;
            }
        }
        return CONTINUOUS;
    }
}
