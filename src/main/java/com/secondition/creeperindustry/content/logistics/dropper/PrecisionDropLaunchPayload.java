package com.secondition.creeperindustry.content.logistics.dropper;

import com.secondition.creeperindustry.CreeperIndustry;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;

public record PrecisionDropLaunchPayload(double velocityX, double velocityY, double velocityZ)
        implements CustomPacketPayload {
    public static final Type<PrecisionDropLaunchPayload> TYPE = new Type<>(
            CreeperIndustry.asResource("precision_dropper_launch")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, PrecisionDropLaunchPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PrecisionDropLaunchPayload decode(RegistryFriendlyByteBuf buffer) {
            return new PrecisionDropLaunchPayload(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, PrecisionDropLaunchPayload payload) {
            buffer.writeDouble(payload.velocityX());
            buffer.writeDouble(payload.velocityY());
            buffer.writeDouble(payload.velocityZ());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void apply(Player player) {
        player.setDeltaMovement(velocityX, velocityY, velocityZ);
        player.fallDistance = 0.0F;
    }
}
