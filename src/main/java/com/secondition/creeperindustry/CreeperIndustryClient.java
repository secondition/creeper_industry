package com.secondition.creeperindustry;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(value = CreeperIndustry.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = CreeperIndustry.MODID, value = Dist.CLIENT)
public class CreeperIndustryClient {
    public CreeperIndustryClient() {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
    }
}
