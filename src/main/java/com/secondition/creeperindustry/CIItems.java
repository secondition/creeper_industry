package com.secondition.creeperindustry;

import java.util.function.Supplier;

import com.secondition.creeperindustry.content.logistics.rocket.GuidedFireworkRocketItem;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CIItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreeperIndustry.MODID);

    public static final Supplier<Item> CATNIP = ITEMS.register("catnip", () -> new Item(new Item.Properties()));
    public static final Supplier<Item> GUIDED_FIREWORK_ROCKET = ITEMS.register("guided_firework_rocket", GuidedFireworkRocketItem::new);

    public static final Supplier<Item> BLASTPROOF_DUCT = ITEMS.register("blastproof_duct", () -> new BlockItem(CIBlocks.BLASTPROOF_DUCT.get(), new Item.Properties()));
    public static final Supplier<Item> BLASTPROOF_FRAME = ITEMS.register("blastproof_frame", () -> new BlockItem(CIBlocks.BLASTPROOF_FRAME.get(), new Item.Properties()));
    public static final Supplier<Item> BLASTPROOF_GLASS = ITEMS.register("blastproof_glass", () -> new BlockItem(CIBlocks.BLASTPROOF_GLASS.get(), new Item.Properties()));
    public static final Supplier<Item> CONTINUOUS_SIGNAL_EMITTER = ITEMS.register("continuous_signal_emitter", () -> new BlockItem(CIBlocks.CONTINUOUS_SIGNAL_EMITTER.get(), new Item.Properties()));
    public static final Supplier<Item> SIGNAL_UPDATE_DETECTOR = ITEMS.register("signal_update_detector", () -> new BlockItem(CIBlocks.SIGNAL_UPDATE_DETECTOR.get(), new Item.Properties()));
    public static final Supplier<Item> PRECISION_DROPPER = ITEMS.register("precision_dropper", () -> new BlockItem(CIBlocks.PRECISION_DROPPER.get(), new Item.Properties()));
    public static final Supplier<Item> BOTANICAL_BIOSPHERE = ITEMS.register("botanical_biosphere", () -> new BlockItem(CIBlocks.BOTANICAL_BIOSPHERE.get(), new Item.Properties()));
    public static final Supplier<Item> ZOOLOGICAL_BIOSPHERE = ITEMS.register("zoological_biosphere", () -> new BlockItem(CIBlocks.ZOOLOGICAL_BIOSPHERE.get(), new Item.Properties()));
    public static final Supplier<Item> MONSTER_BIOSPHERE = ITEMS.register("monster_biosphere", () -> new BlockItem(CIBlocks.MONSTER_BIOSPHERE.get(), new Item.Properties()));
    public static final Supplier<Item> THREE_D_PRINTER = ITEMS.register("three_d_printer", () -> new BlockItem(CIBlocks.THREE_D_PRINTER.get(), new Item.Properties()));
    public static final Supplier<Item> ROCKET_LAUNCHER = ITEMS.register("rocket_launcher", () -> new BlockItem(CIBlocks.ROCKET_LAUNCHER.get(), new Item.Properties()));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
