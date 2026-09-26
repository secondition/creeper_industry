package com.secondition.creeperindustry.content.snapshot;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import java.util.function.Consumer;

public record SnapshotDimensionPacket(ResourceKey<Level> dimension) implements CustomPacketPayload {
    public static final Type<SnapshotDimensionPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("creeper_industry", "snapshot_dimension"));
    public static final StreamCodec<ByteBuf, SnapshotDimensionPacket> STREAM_CODEC =
            ResourceKey.streamCodec(Registries.DIMENSION).map(SnapshotDimensionPacket::new,
                    SnapshotDimensionPacket::dimension);

    private static Consumer<SnapshotDimensionPacket> client = packet -> {};

    public static void setClientHandler(Consumer<SnapshotDimensionPacket> handler) {
        client = handler;
    }

    public static void handle(SnapshotDimensionPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> client.accept(packet));
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
