package com.secondition.creeperindustry;

import com.secondition.creeperindustry.client.energy.signal.CreativeSignalSourceScreen;
import com.secondition.creeperindustry.client.explosion.wave.ClientPulseWavePresentation;
import com.secondition.creeperindustry.client.explosion.wave.PulseWaveRenderer;
import com.secondition.creeperindustry.client.production.biosphere.BiosphereScreen;
import com.secondition.creeperindustry.content.explosion.wave.network.PulseWaveSpawnPacket;
import com.secondition.creeperindustry.content.logistics.storage.DiscBurnerScreen;
import com.secondition.creeperindustry.content.logistics.storage.StorageDiscMinecartRenderer;
import com.secondition.creeperindustry.content.snapshot.SnapshotDimensionPacket;
import com.secondition.creeperindustry.content.snapshot.SnapshotTableMenu;
import com.secondition.creeperindustry.client.snapshot.SnapshotClient;
import com.secondition.creeperindustry.client.snapshot.SnapshotTableScreen;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

@Mod(value = CreeperIndustry.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(
        modid = CreeperIndustry.MODID,
        value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD)
public class CreeperIndustryClient {
    public CreeperIndustryClient() {
        com.secondition.creeperindustry.content.explosion.wave.network.PeriodicWavePacket
                .setClientHandler(ClientPulseWavePresentation::periodic);
        PulseWaveSpawnPacket.setClientHandler(ClientPulseWavePresentation::spawn);
        SnapshotDimensionPacket.setClientHandler(SnapshotClient::addDimension);
    }

    @SubscribeEvent
    static void onRegisterShaders(RegisterShadersEvent event) throws java.io.IOException {
        PulseWaveRenderer.registerShader(event);
    }

    @SubscribeEvent
    static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(CIMenuTypes.BIOSPHERE.get(), BiosphereScreen::new);
        event.register(CIMenuTypes.DISC_BURNER.get(), DiscBurnerScreen::new);
        event.register(CIMenuTypes.CREATIVE_SIGNAL_SOURCE.get(), CreativeSignalSourceScreen::new);
        event.register(CIMenuTypes.SNAPSHOT_TABLE.get(), SnapshotTableScreen::new);
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {

        event.registerEntityRenderer(
                CIEntityTypes.STORAGE_DISC_MINECART.get(), StorageDiscMinecartRenderer::new);
    }
}
