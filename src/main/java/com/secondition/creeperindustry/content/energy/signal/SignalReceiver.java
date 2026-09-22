package com.secondition.creeperindustry.content.energy.signal;

public interface SignalReceiver {
    default void scheduleSignalChange(long arrivalUnits) {}

    void receiveSignal(AggregatedSignal signal);

    default void clearSignal() {}
}
