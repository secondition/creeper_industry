package com.secondition.creeperindustry.content.explosion;

import java.util.Objects;
import java.util.function.Consumer;

public final class ExplosionShockwaveClientBridge {
    private static Consumer<ExplosionShockwavePayload> receiver = payload -> {
    };

    private ExplosionShockwaveClientBridge() {
    }

    public static void register(Consumer<ExplosionShockwavePayload> clientReceiver) {
        receiver = Objects.requireNonNull(clientReceiver);
    }

    public static void receive(ExplosionShockwavePayload payload) {
        receiver.accept(payload);
    }
}
