package com.secondition.creeperindustry;

import com.secondition.creeperindustry.content.logistics.rocket.RocketTargetingPacket;
import com.secondition.creeperindustry.content.logistics.dropper.PrecisionDropLaunchPayload;
import com.secondition.creeperindustry.content.logistics.dropper.PrecisionDropperPayload;

import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class CIPackets {
    private static final String NETWORK_VERSION = "1";

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        registrar.playToServer(RocketTargetingPacket.TYPE, RocketTargetingPacket.STREAM_CODEC, (payload, context) -> {
        });
        registrar.playToServer(
                PrecisionDropperPayload.TYPE,
                PrecisionDropperPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        payload.handle(player);
                    }
                })
        );
        registrar.playToClient(
                PrecisionDropLaunchPayload.TYPE,
                PrecisionDropLaunchPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> payload.apply(context.player()))
        );
    }
}
