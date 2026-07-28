package com.secondition.creeperindustry.content.logistics.launcher;

public enum GuidedFireworkReceiverMode {
    STORAGE,
    UNLOADING;

    public GuidedFireworkReceiverMode toggle() {
        return this == STORAGE ? UNLOADING : STORAGE;
    }

    public static GuidedFireworkReceiverMode fromId(int id) {
        return id == UNLOADING.ordinal() ? UNLOADING : STORAGE;
    }
}
