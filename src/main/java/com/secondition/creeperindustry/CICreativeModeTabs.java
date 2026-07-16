package com.secondition.creeperindustry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CICreativeModeTabs {
    private static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreeperIndustry.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.creeper_industry.main"))
            .icon(() -> new ItemStack(CIBlocks.THREE_D_PRINTER.get()))
            .displayItems((parameters, output) -> {
                output.accept(CIItems.CATNIP.get());
                output.accept(CIItems.GUIDED_FIREWORK_ROCKET.get());
                output.accept(CIItems.STORAGE_DISC.get());
                output.accept(CIBlocks.BLASTPROOF_DUCT.get());
                output.accept(CIItems.BLASTPROOF_DUCT_INTERFACE.get());
                output.accept(CIBlocks.BLASTPROOF_FRAME.get());
                output.accept(CIBlocks.BLASTPROOF_GLASS.get());
                output.accept(CIBlocks.CREATIVE_SIGNAL_SOURCE.get());
                output.accept(CIBlocks.SIGNAL_UPDATE_DETECTOR.get());
                output.accept(CIBlocks.SIGNAL_RANGE_BREAKER.get());
                output.accept(CIBlocks.PRECISION_DROPPER.get());
                output.accept(CIBlocks.DISC_BURNER.get());
                output.accept(CIBlocks.BOTANICAL_BIOSPHERE.get());
                output.accept(CIBlocks.ZOOLOGICAL_BIOSPHERE.get());
                output.accept(CIBlocks.MONSTER_BIOSPHERE.get());
                output.accept(CIBlocks.THREE_D_PRINTER.get());
                output.accept(CIBlocks.ROCKET_LAUNCHER.get());
            })
            .build());

    public static void register(IEventBus modEventBus) {
        CREATIVE_MODE_TABS.register(modEventBus);
    }
}
