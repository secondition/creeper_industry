package com.secondition.creeperindustry;

import com.secondition.creeperindustry.content.logistics.storage.StorageDiscMinecartEntity;
import com.secondition.creeperindustry.content.logistics.storage.StorageDiscMinecartRenderer;
import com.secondition.creeperindustry.content.logistics.storage.DiscBurnerScreen;
import com.secondition.creeperindustry.content.production.biosphere.BiosphereScreen;
import com.secondition.creeperindustry.content.production.biosphere.BiosphereBlockEntityRenderer;
import com.secondition.creeperindustry.content.production.biosphere.BiosphereHighlightRenderer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = CreeperIndustry.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = CreeperIndustry.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class CreeperIndustryClient {
    public CreeperIndustryClient() {
        NeoForge.EVENT_BUS.addListener(BiosphereHighlightRenderer::onRenderBlockHighlight);
    }

    @SubscribeEvent
    static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(CIMenuTypes.BIOSPHERE.get(), BiosphereScreen::new);
        event.register(CIMenuTypes.DISC_BURNER.get(), DiscBurnerScreen::new);
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(CIBlockEntityTypes.BIOSPHERE.get(), BiosphereBlockEntityRenderer::new);
        event.registerEntityRenderer(CIEntityTypes.STORAGE_DISC_MINECART.get(), StorageDiscMinecartRenderer::new);
    }
}
