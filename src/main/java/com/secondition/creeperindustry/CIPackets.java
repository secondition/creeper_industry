package com.secondition.creeperindustry;

import com.secondition.creeperindustry.content.explosion.wave.network.PulseWaveSpawnPacket;
import com.secondition.creeperindustry.content.logistics.rocket.RocketTargetingPacket;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class CIPackets {
    private static final String NETWORK_VERSION = "1";

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        registrar.playToServer(RocketTargetingPacket.TYPE, RocketTargetingPacket.STREAM_CODEC, (payload, context) -> {
        });
        registrar.playToClient(PulseWaveSpawnPacket.TYPE, PulseWaveSpawnPacket.STREAM_CODEC, PulseWaveSpawnPacket::handle);
    }
}
