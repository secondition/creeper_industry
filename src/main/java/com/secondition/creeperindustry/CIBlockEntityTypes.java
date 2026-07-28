package com.secondition.creeperindustry;

import java.util.function.Supplier;

import com.secondition.creeperindustry.content.automation.breaker.SignalRangeBreakerBlockEntity;
import com.secondition.creeperindustry.content.logistics.dropper.PrecisionDropperBlockEntity;
import com.secondition.creeperindustry.content.logistics.launcher.RocketLauncherBlockEntity;
import com.secondition.creeperindustry.content.logistics.launcher.GuidedFireworkReceiverBlockEntity;
import com.secondition.creeperindustry.content.logistics.storage.DiscBurnerBlockEntity;
import com.secondition.creeperindustry.content.energy.signal.CreativeSignalSourceBlockEntity;
import com.secondition.creeperindustry.content.energy.signal.SignalUpdateDetectorBlockEntity;
import com.secondition.creeperindustry.content.production.biosphere.BiosphereBlockEntity;
import com.secondition.creeperindustry.content.production.printer.ThreeDPrinterBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CIBlockEntityTypes {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CreeperIndustry.MODID);

    public static final Supplier<BlockEntityType<PrecisionDropperBlockEntity>> PRECISION_DROPPER = BLOCK_ENTITY_TYPES.register("precision_dropper",
            () -> BlockEntityType.Builder.of(PrecisionDropperBlockEntity::new, CIBlocks.PRECISION_DROPPER.get()).build(null));
    public static final Supplier<BlockEntityType<CreativeSignalSourceBlockEntity>> CREATIVE_SIGNAL_SOURCE = BLOCK_ENTITY_TYPES.register("creative_signal_source",
            () -> BlockEntityType.Builder.of(CreativeSignalSourceBlockEntity::new, CIBlocks.CREATIVE_SIGNAL_SOURCE.get()).build(null));
    public static final Supplier<BlockEntityType<SignalUpdateDetectorBlockEntity>> SIGNAL_UPDATE_DETECTOR = BLOCK_ENTITY_TYPES.register("signal_update_detector",
            () -> BlockEntityType.Builder.of(SignalUpdateDetectorBlockEntity::new, CIBlocks.SIGNAL_UPDATE_DETECTOR.get()).build(null));
    public static final Supplier<BlockEntityType<SignalRangeBreakerBlockEntity>> SIGNAL_RANGE_BREAKER = BLOCK_ENTITY_TYPES.register("signal_range_breaker",
            () -> BlockEntityType.Builder.of(SignalRangeBreakerBlockEntity::new, CIBlocks.SIGNAL_RANGE_BREAKER.get()).build(null));
    public static final Supplier<BlockEntityType<BiosphereBlockEntity>> BIOSPHERE = BLOCK_ENTITY_TYPES.register("biosphere",
            () -> BlockEntityType.Builder.of(BiosphereBlockEntity::new,
                    CIBlocks.BOTANICAL_BIOSPHERE.get(),
                    CIBlocks.ZOOLOGICAL_BIOSPHERE.get(),
                    CIBlocks.MONSTER_BIOSPHERE.get()).build(null));
    public static final Supplier<BlockEntityType<DiscBurnerBlockEntity>> DISC_BURNER = BLOCK_ENTITY_TYPES.register("disc_burner",
            () -> BlockEntityType.Builder.of(DiscBurnerBlockEntity::new, CIBlocks.DISC_BURNER.get()).build(null));
    public static final Supplier<BlockEntityType<ThreeDPrinterBlockEntity>> THREE_D_PRINTER = BLOCK_ENTITY_TYPES.register("three_d_printer",
            () -> BlockEntityType.Builder.of(ThreeDPrinterBlockEntity::new, CIBlocks.THREE_D_PRINTER.get()).build(null));
    public static final Supplier<BlockEntityType<RocketLauncherBlockEntity>> ROCKET_LAUNCHER = BLOCK_ENTITY_TYPES.register("rocket_launcher",
            () -> BlockEntityType.Builder.of(RocketLauncherBlockEntity::new, CIBlocks.ROCKET_LAUNCHER.get()).build(null));
    public static final Supplier<BlockEntityType<GuidedFireworkReceiverBlockEntity>> GUIDED_FIREWORK_RECEIVER = BLOCK_ENTITY_TYPES.register("guided_firework_receiver",
            () -> BlockEntityType.Builder.of(GuidedFireworkReceiverBlockEntity::new, CIBlocks.GUIDED_FIREWORK_RECEIVER.get()).build(null));

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITY_TYPES.register(modEventBus);
    }
}
