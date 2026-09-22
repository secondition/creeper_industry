package com.secondition.creeperindustry;

import com.secondition.creeperindustry.content.explosion.wave.network.PulseWaveSpawnPacket;
import com.secondition.creeperindustry.content.logistics.rocket.RocketTargetingPacket;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class CIPackets {
    private static final String NETWORK_VERSION = "2";

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        registrar.playToServer(
                RocketTargetingPacket.TYPE,
                RocketTargetingPacket.STREAM_CODEC,
                (payload, context) -> {});
        registrar.playToClient(
                com.secondition.creeperindustry.content.explosion.wave.network.PeriodicWavePacket
                        .TYPE,
                com.secondition.creeperindustry.content.explosion.wave.network.PeriodicWavePacket
                        .STREAM_CODEC,
                com.secondition.creeperindustry.content.explosion.wave.network.PeriodicWavePacket
                        ::handle);
        registrar.playToClient(
                PulseWaveSpawnPacket.TYPE,
                PulseWaveSpawnPacket.STREAM_CODEC,
                PulseWaveSpawnPacket::handle);
    }
}
