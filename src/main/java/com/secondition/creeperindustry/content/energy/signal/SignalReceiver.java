package com.secondition.creeperindustry.content.energy.signal;

public interface SignalReceiver {
    default boolean slowOnly() { return false; }

    default void scheduleSignalChange(double arrivalTick) {}

    void receiveSignal(AggregatedSignal signal);

    default void clearSignal() {}
}
