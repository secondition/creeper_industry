package com.secondition.creeperindustry.content.logistics.rocket;

import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public record RocketDestination(ResourceKey<Level> dimension, BlockPos receiverPos, UUID receiverId) {
    public static final Codec<RocketDestination> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("dimension").forGetter(destination -> destination.dimension().location()),
            BlockPos.CODEC.fieldOf("receiver_pos").forGetter(RocketDestination::receiverPos),
            UUIDUtil.CODEC.fieldOf("receiver_id").forGetter(RocketDestination::receiverId)
    ).apply(instance, (dimension, pos, receiverId) -> new RocketDestination(
            ResourceKey.create(Registries.DIMENSION, dimension),
            pos,
            receiverId
    )));

    public static final StreamCodec<RegistryFriendlyByteBuf, RocketDestination> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public RocketDestination decode(RegistryFriendlyByteBuf buffer) {
            return new RocketDestination(
                    ResourceKey.create(Registries.DIMENSION, buffer.readResourceLocation()),
                    buffer.readBlockPos(),
                    buffer.readUUID()
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, RocketDestination destination) {
            buffer.writeResourceLocation(destination.dimension().location());
            buffer.writeBlockPos(destination.receiverPos());
            buffer.writeUUID(destination.receiverId());
        }
    };
}
