package com.secondition.creeperindustry.content.logistics.dropper;

import com.secondition.creeperindustry.CreeperIndustry;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public record PrecisionDropperPayload(
        BlockPos dropperPos,
        int containerId,
        int targetX,
        int targetZ,
        boolean launch
) implements CustomPacketPayload {
    public static final Type<PrecisionDropperPayload> TYPE = new Type<>(CreeperIndustry.asResource("precision_dropper_configure"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PrecisionDropperPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PrecisionDropperPayload decode(RegistryFriendlyByteBuf buffer) {
            return new PrecisionDropperPayload(
                    buffer.readBlockPos(),
                    buffer.readVarInt(),
                    buffer.readInt(),
                    buffer.readInt(),
                    buffer.readBoolean()
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, PrecisionDropperPayload payload) {
            buffer.writeBlockPos(payload.dropperPos());
            buffer.writeVarInt(payload.containerId());
            buffer.writeInt(payload.targetX());
            buffer.writeInt(payload.targetZ());
            buffer.writeBoolean(payload.launch());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(ServerPlayer player) {
        if (!(player.containerMenu instanceof PrecisionDropperMenu menu)
                || menu.containerId != containerId
                || !menu.dropperPos().equals(dropperPos)
                || !menu.stillValid(player)
                || menu.dropper() == null
                || !(player.level() instanceof ServerLevel level)) {
            player.displayClientMessage(
                    Component.translatable(PrecisionDropLaunchResult.INVALID_MENU.translationKey()),
                    false
            );
            return;
        }

        PrecisionDropperBlockEntity dropper = menu.dropper();
        if (!PrecisionDropRuntime.isValidTargetCoordinate(targetX)
                || !PrecisionDropRuntime.isValidTargetCoordinate(targetZ)) {
            player.displayClientMessage(
                    Component.translatable(PrecisionDropLaunchResult.INVALID_TARGET.translationKey()),
                    false
            );
            return;
        }
        dropper.setTarget(targetX, targetZ);
        if (!launch) {
            player.displayClientMessage(Component.translatable("message.creeper_industry.precision_dropper.saved"), false);
            return;
        }

        PrecisionDropLaunchResult result = PrecisionDropRuntimeAccess.get(level).start(
                level,
                player,
                dropper.getBlockPos(),
                targetX,
                targetZ
        );
        player.displayClientMessage(Component.translatable(result.translationKey()), false);
        if (result.successful()) {
            player.closeContainer();
        }
    }
}
