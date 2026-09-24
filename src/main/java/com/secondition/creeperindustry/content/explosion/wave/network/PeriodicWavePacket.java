package com.secondition.creeperindustry.content.explosion.wave.network;

import com.secondition.creeperindustry.CreeperIndustry;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;
import java.util.function.Consumer;

public record PeriodicWavePacket(
        UUID version,
        long start,
        long end,
        double x,
        double y,
        double z,
        int amplitude,
        int wavelength,
        int frequencyNumerator,
        int frequencyDenominator,
        long anchorTick,
        double phase)
        implements CustomPacketPayload {
    public static final Type<PeriodicWavePacket> TYPE =
            new Type<>(CreeperIndustry.asResource("periodic_wave"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PeriodicWavePacket> STREAM_CODEC =
            StreamCodec.of(
                    (b, p) -> {
                        b.writeUUID(p.version);
                        b.writeLong(p.start);
                        b.writeLong(p.end);
                        b.writeDouble(p.x);
                        b.writeDouble(p.y);
                        b.writeDouble(p.z);
                        b.writeInt(p.amplitude);
                        b.writeInt(p.wavelength);
                        b.writeInt(p.frequencyNumerator);
                        b.writeInt(p.frequencyDenominator);
                        b.writeLong(p.anchorTick);
                        b.writeDouble(p.phase);
                    },
                    b -> new PeriodicWavePacket(b.readUUID(), b.readLong(), b.readLong(),
                            b.readDouble(), b.readDouble(), b.readDouble(), b.readInt(),
                            b.readInt(), b.readInt(), b.readInt(), b.readLong(), b.readDouble()));
    private static Consumer<PeriodicWavePacket> client = p -> {};

    public static void setClientHandler(Consumer<PeriodicWavePacket> handler) {
        client = handler;
    }

    public static void handle(PeriodicWavePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> client.accept(packet));
    }

    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
