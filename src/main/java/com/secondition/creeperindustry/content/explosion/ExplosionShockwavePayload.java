package com.secondition.creeperindustry.content.explosion;

import com.secondition.creeperindustry.CreeperIndustry;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

public record ExplosionShockwavePayload(Vec3 center, float explosionPower, long visualSeed)
        implements CustomPacketPayload {
    public static final Type<ExplosionShockwavePayload> TYPE = new Type<>(
            CreeperIndustry.asResource("explosion_shockwave")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ExplosionShockwavePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ExplosionShockwavePayload decode(RegistryFriendlyByteBuf buffer) {
            return new ExplosionShockwavePayload(
                    new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                    buffer.readFloat(),
                    buffer.readLong()
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, ExplosionShockwavePayload payload) {
            buffer.writeDouble(payload.center().x);
            buffer.writeDouble(payload.center().y);
            buffer.writeDouble(payload.center().z);
            buffer.writeFloat(payload.explosionPower());
            buffer.writeLong(payload.visualSeed());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
