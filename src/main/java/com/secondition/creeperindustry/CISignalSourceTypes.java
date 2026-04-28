package com.secondition.creeperindustry;

import com.secondition.creeperindustry.content.energy.signal.ContinuousSignalSourceRepository;
import com.secondition.creeperindustry.content.energy.signal.ContinuousSignalPropagationService;
import com.secondition.creeperindustry.content.energy.signal.ContinuousSignalUpdateService;
import com.secondition.creeperindustry.content.energy.signal.CreativeSignalPulseSource;
import com.secondition.creeperindustry.content.energy.signal.ExplosionSignalSource;
import com.secondition.creeperindustry.content.energy.signal.InMemorySignalSourceRepository;
import com.secondition.creeperindustry.content.energy.signal.InMemoryContinuousSignalSourceRepository;
import com.secondition.creeperindustry.content.energy.signal.InMemorySignalReceiverIndex;
import com.secondition.creeperindustry.content.energy.signal.InMemoryTransientSignalImpactTracker;
import com.secondition.creeperindustry.content.energy.signal.DefaultSignalContributionAggregator;
import com.secondition.creeperindustry.content.energy.signal.DeferredSignalRefreshQueue;
import com.secondition.creeperindustry.content.energy.signal.DuctNetworkManager;
import com.secondition.creeperindustry.content.energy.signal.InMemorySignalAggregationService;
import com.secondition.creeperindustry.content.energy.signal.MachineSignalSource;
import com.secondition.creeperindustry.content.energy.signal.SignalSourceRepository;
import com.secondition.creeperindustry.content.energy.signal.SignalSourceType;
import com.secondition.creeperindustry.content.energy.signal.SignalAggregationService;
import com.secondition.creeperindustry.content.energy.signal.SignalReceiverIndex;
import com.secondition.creeperindustry.content.energy.signal.SignalReceiverSelector;
import com.secondition.creeperindustry.content.energy.signal.SimpleTransientSignalDispatcher;
import com.secondition.creeperindustry.content.energy.signal.TransientSignalImpactTracker;
import com.secondition.creeperindustry.content.energy.signal.TransientSignalDispatcher;
import com.secondition.creeperindustry.content.energy.signal.UnifiedSignalRefreshService;

public class CISignalSourceTypes {
    private static final SignalSourceRepository REPOSITORY = new InMemorySignalSourceRepository();
    private static final ContinuousSignalSourceRepository CONTINUOUS_SOURCE_REPOSITORY = new InMemoryContinuousSignalSourceRepository();
    private static final SignalReceiverIndex SIGNAL_RECEIVER_INDEX = new InMemorySignalReceiverIndex();
    private static final SignalReceiverSelector SIGNAL_RECEIVER_SELECTOR = new SignalReceiverSelector(SIGNAL_RECEIVER_INDEX);
    private static final DuctNetworkManager DUCT_NETWORK_MANAGER = new DuctNetworkManager();
    private static final DeferredSignalRefreshQueue DEFERRED_SIGNAL_REFRESH_QUEUE = new DeferredSignalRefreshQueue();
    private static final TransientSignalImpactTracker TRANSIENT_SIGNAL_IMPACT_TRACKER = new InMemoryTransientSignalImpactTracker();
    private static final DefaultSignalContributionAggregator SIGNAL_CONTRIBUTION_AGGREGATOR = new DefaultSignalContributionAggregator();
    private static final SignalAggregationService SIGNAL_AGGREGATION_SERVICE = new InMemorySignalAggregationService();
    private static final ContinuousSignalPropagationService CONTINUOUS_SIGNAL_PROPAGATION_SERVICE = new ContinuousSignalPropagationService(
            CONTINUOUS_SOURCE_REPOSITORY,
            SIGNAL_CONTRIBUTION_AGGREGATOR
    );
    private static final UnifiedSignalRefreshService UNIFIED_SIGNAL_REFRESH_SERVICE = new UnifiedSignalRefreshService(
            CONTINUOUS_SIGNAL_PROPAGATION_SERVICE,
            SIGNAL_AGGREGATION_SERVICE,
            SIGNAL_CONTRIBUTION_AGGREGATOR
    );
    private static final ContinuousSignalUpdateService CONTINUOUS_SIGNAL_UPDATE_SERVICE = new ContinuousSignalUpdateService(
            CONTINUOUS_SOURCE_REPOSITORY,
            SIGNAL_RECEIVER_INDEX,
            SIGNAL_RECEIVER_SELECTOR,
            UNIFIED_SIGNAL_REFRESH_SERVICE
    );
    private static final TransientSignalDispatcher TRANSIENT_DISPATCHER = new SimpleTransientSignalDispatcher();

    public static final SignalSourceType<ExplosionSignalSource> EXPLOSION = register("explosion", ExplosionSignalSource.class);
    public static final SignalSourceType<MachineSignalSource> MACHINE = register("machine", MachineSignalSource.class);
    public static final SignalSourceType<CreativeSignalPulseSource> CREATIVE_PULSE = register("creative_pulse", CreativeSignalPulseSource.class);

    private static <T extends com.secondition.creeperindustry.content.energy.signal.SignalSource> SignalSourceType<T> register(String path, Class<T> sourceClass) {
        return REPOSITORY.registerType(new SignalSourceType<>(CreeperIndustry.asResource(path), sourceClass));
    }

    public static SignalSourceRepository repository() {
        return REPOSITORY;
    }

    public static ContinuousSignalSourceRepository continuousSignalSourceRepository() {
        return CONTINUOUS_SOURCE_REPOSITORY;
    }

    public static SignalReceiverIndex signalReceiverIndex() {
        return SIGNAL_RECEIVER_INDEX;
    }

    public static SignalReceiverSelector signalReceiverSelector() {
        return SIGNAL_RECEIVER_SELECTOR;
    }

    public static DuctNetworkManager ductNetworkManager() {
        return DUCT_NETWORK_MANAGER;
    }

    public static DeferredSignalRefreshQueue deferredSignalRefreshQueue() {
        return DEFERRED_SIGNAL_REFRESH_QUEUE;
    }

    public static TransientSignalImpactTracker transientSignalImpactTracker() {
        return TRANSIENT_SIGNAL_IMPACT_TRACKER;
    }

    public static ContinuousSignalPropagationService continuousSignalPropagationService() {
        return CONTINUOUS_SIGNAL_PROPAGATION_SERVICE;
    }

    public static ContinuousSignalUpdateService continuousSignalUpdateService() {
        return CONTINUOUS_SIGNAL_UPDATE_SERVICE;
    }

    public static TransientSignalDispatcher transientDispatcher() {
        return TRANSIENT_DISPATCHER;
    }

    public static SignalAggregationService signalAggregationService() {
        return SIGNAL_AGGREGATION_SERVICE;
    }

    public static UnifiedSignalRefreshService unifiedSignalRefreshService() {
        return UNIFIED_SIGNAL_REFRESH_SERVICE;
    }
}
