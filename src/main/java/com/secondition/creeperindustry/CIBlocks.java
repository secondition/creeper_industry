package com.secondition.creeperindustry;

import java.util.function.Supplier;

import com.secondition.creeperindustry.content.energy.transmission.BlastproofDuctBlock;
import com.secondition.creeperindustry.content.energy.transmission.BlastproofFrameBlock;
import com.secondition.creeperindustry.content.energy.transmission.BlastproofGlassBlock;
import com.secondition.creeperindustry.content.energy.signal.CreativeSignalSourceBlock;
import com.secondition.creeperindustry.content.energy.signal.SignalUpdateDetectorBlock;
import com.secondition.creeperindustry.content.logistics.dropper.PrecisionDropperBlock;
import com.secondition.creeperindustry.content.logistics.launcher.RocketLauncherBlock;
import com.secondition.creeperindustry.content.logistics.storage.DiscBurnerBlock;
import com.secondition.creeperindustry.content.production.biosphere.BotanicalBiosphereBlock;
import com.secondition.creeperindustry.content.production.biosphere.MonsterBiosphereBlock;
import com.secondition.creeperindustry.content.production.biosphere.ZoologicalBiosphereBlock;
import com.secondition.creeperindustry.content.production.printer.ThreeDPrinterBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CIBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CreeperIndustry.MODID);
    private static final int BOTANICAL_BIOSPHERE_GLOW_LEVEL = 12;

    public static final Supplier<Block> BLASTPROOF_DUCT = BLOCKS.register("blastproof_duct",
            () -> new BlastproofDuctBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(6.0F, 12.0F).sound(SoundType.METAL)));
    public static final Supplier<Block> BLASTPROOF_FRAME = BLOCKS.register("blastproof_frame",
            () -> new BlastproofFrameBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(8.0F, 16.0F).sound(SoundType.NETHERITE_BLOCK)));
    public static final Supplier<Block> BLASTPROOF_GLASS = BLOCKS.register("blastproof_glass",
            () -> new BlastproofGlassBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).strength(5.0F, 10.0F).sound(SoundType.GLASS)));
    public static final Supplier<Block> CREATIVE_SIGNAL_SOURCE = BLOCKS.register("creative_signal_source",
            () -> new CreativeSignalSourceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F, 6.0F).sound(SoundType.METAL)));
    public static final Supplier<Block> SIGNAL_UPDATE_DETECTOR = BLOCKS.register("signal_update_detector",
            () -> new SignalUpdateDetectorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F, 6.0F).sound(SoundType.METAL)));

    public static final Supplier<Block> PRECISION_DROPPER = BLOCKS.register("precision_dropper",
            () -> new PrecisionDropperBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DISPENSER).strength(3.5F)));
    public static final Supplier<Block> DISC_BURNER = BLOCKS.register("disc_burner",
            () -> new DiscBurnerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.JUKEBOX).strength(2.5F).sound(SoundType.WOOD)));

    public static final Supplier<Block> BOTANICAL_BIOSPHERE = BLOCKS.register("botanical_biosphere",
            () -> new BotanicalBiosphereBlock(botanicalBiosphereProperties()));
    public static final Supplier<Block> ZOOLOGICAL_BIOSPHERE = BLOCKS.register("zoological_biosphere",
            () -> new ZoologicalBiosphereBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).strength(2.5F).sound(SoundType.GLASS).requiresCorrectToolForDrops()));
    public static final Supplier<Block> MONSTER_BIOSPHERE = BLOCKS.register("monster_biosphere",
            () -> new MonsterBiosphereBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).strength(2.5F).sound(SoundType.GLASS).requiresCorrectToolForDrops()));

    public static final Supplier<Block> THREE_D_PRINTER = BLOCKS.register("three_d_printer",
            () -> new ThreeDPrinterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(4.0F).sound(SoundType.METAL)));
    public static final Supplier<Block> ROCKET_LAUNCHER = BLOCKS.register("rocket_launcher",
            () -> new RocketLauncherBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DISPENSER).strength(4.0F).sound(SoundType.METAL)));

    private static BlockBehaviour.Properties botanicalBiosphereProperties() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS)
                .strength(2.5F)
                .sound(SoundType.GLASS)
                .requiresCorrectToolForDrops()
                .lightLevel(state -> BOTANICAL_BIOSPHERE_GLOW_LEVEL);
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
