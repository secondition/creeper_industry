package com.secondition.creeperindustry.content.logistics.rocket;

import com.secondition.creeperindustry.CreeperIndustry;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record RocketTargetingPacket(BlockPos targetPos) implements CustomPacketPayload {
    public static final Type<RocketTargetingPacket> TYPE = new Type<>(CreeperIndustry.asResource("rocket_targeting"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RocketTargetingPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            RocketTargetingPacket::targetPos,
            RocketTargetingPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
