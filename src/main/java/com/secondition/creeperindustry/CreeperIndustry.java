package com.secondition.creeperindustry;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(CreeperIndustry.MODID)
public class CreeperIndustry {
    public static final String MODID = "creeper_industry";
    public static final String NAME = "Creeper Industry";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreeperIndustry(IEventBus modEventBus, ModContainer modContainer) {
        CICreativeModeTabs.register(modEventBus);
        CIBlocks.register(modEventBus);
        CIItems.register(modEventBus);
        CIBlockEntityTypes.register(modEventBus);
        CIEntityTypes.register(modEventBus);
        CIMenuTypes.register(modEventBus);
        CIRecipeTypes.register(modEventBus);
        CIRecipeSerializers.register(modEventBus);
        CIAttachmentTypes.register(modEventBus);
        CIChunkTickets.register(modEventBus);
        CIDataComponents.register(modEventBus);
        CICapabilities.register(modEventBus);

        modEventBus.addListener(CIDatagen::gatherData);
        modEventBus.addListener(CIPackets::register);
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
