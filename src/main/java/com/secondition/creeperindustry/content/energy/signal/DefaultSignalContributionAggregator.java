package com.secondition.creeperindustry.content.energy.signal;

import java.util.Collection;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class DefaultSignalContributionAggregator implements SignalContributionAggregator {
    @Override
    public AggregatedSignal aggregate(ResourceKey<Level> level, BlockPos targetPos, long gameTime, Collection<DeliveredSignal> contributions) {
        if (contributions.isEmpty()) {
            throw new IllegalArgumentException("Cannot aggregate an empty contribution set");
        }

        int commonPeriod = 1;
        for (DeliveredSignal contribution : contributions) {
            commonPeriod = leastCommonMultiple(commonPeriod, contribution.source().signal().periodTicks());
        }

        int strongestAmplitude = 0;
        int strongestCost = Integer.MAX_VALUE;
        for (int sample = 0; sample < commonPeriod; sample++) {
            int sampleValue = 0;
            int sampleStrongestCost = Integer.MAX_VALUE;
            for (DeliveredSignal contribution : contributions) {
                int value = sampleSignal(contribution, sample);
                sampleValue += value;
                if (value != 0) {
                    sampleStrongestCost = Math.min(sampleStrongestCost, contribution.propagationCost());
                }
            }

            int absValue = Math.abs(sampleValue);
            if (absValue > strongestAmplitude) {
                strongestAmplitude = absValue;
                strongestCost = sampleStrongestCost == Integer.MAX_VALUE ? 0 : sampleStrongestCost;
            }
        }

        SignalDefinition signal = new SignalDefinition(strongestAmplitude, commonPeriod, 0, SignalWaveform.SQUARE);
        return new AggregatedSignal(level, targetPos, gameTime, signal, contributions.size(), strongestCost == Integer.MAX_VALUE ? 0 : strongestCost);
    }

    private int sampleSignal(DeliveredSignal contribution, int sampleTick) {
        SignalDefinition signal = contribution.source().signal();
        int period = signal.periodTicks();
        int phase = Math.floorMod(sampleTick + signal.phaseTicks(), period);
        return switch (signal.waveform()) {
            case SQUARE -> phase * 2 < period ? contribution.effectiveAmplitude() : -contribution.effectiveAmplitude();
        };
    }

    private int leastCommonMultiple(int left, int right) {
        return left / greatestCommonDivisor(left, right) * right;
    }

    private int greatestCommonDivisor(int left, int right) {
        int a = Math.abs(left);
        int b = Math.abs(right);
        while (b != 0) {
            int next = a % b;
            a = b;
            b = next;
        }
        return Math.max(a, 1);
    }
}
