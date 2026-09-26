package com.secondition.creeperindustry.content.snapshot;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class SnapshotConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue MAX_SNAPSHOTS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        MAX_SNAPSHOTS = builder
                .comment("Maximum number of persistent snapshots")
                .defineInRange("max_snapshots", 4, 1, 64);
        SPEC = builder.build();
    }

    private SnapshotConfig() {}
}
