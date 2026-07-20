package com.secondition.creeperindustry.content.explosion.wave.network;

import java.util.UUID;
import java.util.function.Consumer;

import com.secondition.creeperindustry.CreeperIndustry;
import com.secondition.creeperindustry.content.explosion.wave.PulseWaveEmission;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PulseWaveSpawnPacket(
        UUID waveId,
        double originX,
        double originY,
        double originZ,
        long emissionGameTime,
        double speedBlocksPerTick,
        double maxRadius,
        int amplitude
) implements CustomPacketPayload {
    public static final Type<PulseWaveSpawnPacket> TYPE = new Type<>(CreeperIndustry.asResource("pulse_wave_spawn"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PulseWaveSpawnPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeUUID(packet.waveId());
                buf.writeDouble(packet.originX());
                buf.writeDouble(packet.originY());
                buf.writeDouble(packet.originZ());
                buf.writeVarLong(packet.emissionGameTime());
                buf.writeDouble(packet.speedBlocksPerTick());
                buf.writeDouble(packet.maxRadius());
                buf.writeVarInt(packet.amplitude());
            },
            buf -> new PulseWaveSpawnPacket(
                    buf.readUUID(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readVarLong(),
                    buf.readDouble(),
                    buf.readDouble(),
                    buf.readVarInt()
            )
    );

    private static Consumer<PulseWaveSpawnPacket> clientHandler = packet -> {
    };

    public static void setClientHandler(Consumer<PulseWaveSpawnPacket> handler) {
        clientHandler = handler != null ? handler : packet -> {
        };
    }

    public static PulseWaveSpawnPacket fromEmission(PulseWaveEmission emission) {
        Vec3 origin = emission.origin();
        return new PulseWaveSpawnPacket(
                emission.id(),
                origin.x,
                origin.y,
                origin.z,
                emission.emissionGameTime(),
                emission.profile().propagationSpeedBlocksPerTick(),
                emission.maxEffectiveRadius(),
                emission.sourceAmplitude()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PulseWaveSpawnPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> clientHandler.accept(payload));
    }
}
