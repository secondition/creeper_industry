package com.secondition.creeperindustry.content.energy.signal;

import java.util.Optional;

public class ExplosionSignalService {
    private final ExplosionSignalContextFactory contextFactory;
    private final ExplosionSignalAmplitudeResolver amplitudeResolver;
    private final TransientSignalDispatcher dispatcher;

    public ExplosionSignalService(
            ExplosionSignalContextFactory contextFactory,
            ExplosionSignalAmplitudeResolver amplitudeResolver,
            TransientSignalDispatcher dispatcher
    ) {
        this.contextFactory = contextFactory;
        this.amplitudeResolver = amplitudeResolver;
        this.dispatcher = dispatcher;
    }

    public Optional<ExplosionSignalSource> capture(net.minecraft.world.level.Level level, net.minecraft.world.level.Explosion explosion) {
        Optional<ExplosionSignalContext> context = contextFactory.create(level, explosion);
        if (context.isEmpty()) {
            return Optional.empty();
        }

        Optional<ExplosionSignalSource> source = ExplosionSignalSource.fromContext(context.get(), amplitudeResolver);
        source.ifPresent(signalSource -> dispatcher.dispatch(level, signalSource));
        return source;
    }
}
