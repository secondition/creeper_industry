package com.secondition.creeperindustry.content.energy.signal;

import java.util.List;

import com.secondition.creeperindustry.CISignalSourceTypes;
import net.minecraft.world.level.Level;

public class SimpleTransientSignalDispatcher implements TransientSignalDispatcher {
    @Override
    public void dispatch(Level level, SignalSource source) {
        List<SignalReach> reachableTargets = SignalReachEvaluator.findReachableTargets(
                source,
                CISignalSourceTypes.signalReceiverSelector().getPotentialTargets(level.dimension(), source)
        );
        for (SignalReach reach : reachableTargets) {
            CISignalSourceTypes.signalAggregationService().submitContribution(
                    level,
                    new DeliveredSignal(source, reach.targetPos(), reach.effectiveAmplitude(), reach.propagationCost())
            );
        }

        CISignalSourceTypes.transientSignalImpactTracker().record(
                level.dimension(),
                source.gameTime(),
                reachableTargets.stream().map(SignalReach::targetPos).toList()
        );
    }
}
