package com.secondition.creeperindustry.content.energy.signal;

import java.util.Collection;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class DefaultSignalContributionAggregator implements SignalContributionAggregator {
    @Override
    @Nullable
    public AggregatedSignal aggregate(ResourceKey<Level> level, BlockPos targetPos, long gameTime, Collection<DeliveredSignal> contributions) {
        if (contributions.isEmpty()) {
            throw new IllegalArgumentException("Cannot aggregate an empty contribution set");
        }

        boolean containsTransientSignal = contributions.stream()
                .map(DeliveredSignal::source)
                .anyMatch(source -> !(source instanceof ContinuousSignalSource));
        if (containsTransientSignal) {
            return aggregateCurrentTick(level, targetPos, gameTime, contributions);
        }

        int commonPeriod = 1;
        for (DeliveredSignal contribution : contributions) {
            commonPeriod = leastCommonMultiple(commonPeriod, contribution.source().signal().periodTicks());
        }

        int currentTickValue = 0;
        for (DeliveredSignal contribution : contributions) {
            currentTickValue += sampleSignal(contribution, gameTime);
        }

        long absoluteValueSum = 0L;
        int strongestCost = Integer.MAX_VALUE;
        for (int sample = 0; sample < commonPeriod; sample++) {
            int sampleValue = 0;
            int sampleStrongestCost = Integer.MAX_VALUE;
            for (DeliveredSignal contribution : contributions) {
                int value = sampleSignal(contribution, gameTime + sample);
                sampleValue += value;
                if (value != 0) {
                    sampleStrongestCost = Math.min(sampleStrongestCost, contribution.propagationCost());
                }
            }

            int absValue = Math.abs(sampleValue);
            absoluteValueSum += absValue;
            if (absValue > 0) {
                strongestCost = Math.min(strongestCost, sampleStrongestCost == Integer.MAX_VALUE ? 0 : sampleStrongestCost);
            }
        }

        int averageAmplitude = Math.toIntExact((absoluteValueSum + commonPeriod - 1L) / commonPeriod);
        if (averageAmplitude <= 0) {
            return null;
        }
        SignalDefinition signal = new SignalDefinition(averageAmplitude, commonPeriod, 0, SignalWaveform.SQUARE);
        return new AggregatedSignal(level, targetPos, gameTime, signal, currentTickValue, contributions.size(), strongestCost == Integer.MAX_VALUE ? 0 : strongestCost);
    }

    @Nullable
    private AggregatedSignal aggregateCurrentTick(ResourceKey<Level> level, BlockPos targetPos, long gameTime, Collection<DeliveredSignal> contributions) {
        int summedValue = 0;
        int strongestCost = Integer.MAX_VALUE;
        for (DeliveredSignal contribution : contributions) {
            int value = sampleSignal(contribution, gameTime);
            summedValue += value;
            if (value != 0) {
                strongestCost = Math.min(strongestCost, contribution.propagationCost());
            }
        }

        int amplitude = Math.abs(summedValue);
        if (amplitude <= 0) {
            return null;
        }

        SignalDefinition signal = new SignalDefinition(amplitude, 1, 0, SignalWaveform.SQUARE);
        return new AggregatedSignal(level, targetPos, gameTime, signal, summedValue, contributions.size(), strongestCost == Integer.MAX_VALUE ? 0 : strongestCost);
    }

    private int sampleSignal(DeliveredSignal contribution, long absoluteGameTime) {
        return SignalSampling.sampleAtGameTime(
                contribution.source().signal(),
                contribution.effectiveAmplitude(),
                absoluteGameTime
        );
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
