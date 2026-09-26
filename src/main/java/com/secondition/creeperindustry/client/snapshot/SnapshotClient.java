package com.secondition.creeperindustry.client.snapshot;

import com.secondition.creeperindustry.content.snapshot.SnapshotDimensionPacket;
import net.minecraft.client.Minecraft;

public final class SnapshotClient {
    private SnapshotClient() {}

    public static void addDimension(SnapshotDimensionPacket packet) {
        var player = Minecraft.getInstance().player;
        if (player != null) player.connection.levels().add(packet.dimension());
    }
}
