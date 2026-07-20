package com.secondition.creeperindustry.content.explosion.wave.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.secondition.creeperindustry.content.explosion.wave.WavePropagationMath;
import com.secondition.creeperindustry.content.explosion.wave.network.PulseWaveSpawnPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = com.secondition.creeperindustry.CreeperIndustry.MODID, value = Dist.CLIENT)
public final class ClientPulseWavePresentation {
    private static final List<ClientPulseWave> ACTIVE = new ArrayList<>();
    private static float shakeIntensity;
    private static int shakeTicks;

    private ClientPulseWavePresentation() {
    }

    public static void spawn(PulseWaveSpawnPacket packet) {
        ACTIVE.add(new ClientPulseWave(
                packet.waveId(),
                new Vec3(packet.originX(), packet.originY(), packet.originZ()),
                packet.emissionGameTime(),
                packet.speedBlocksPerTick(),
                packet.maxRadius(),
                packet.amplitude()
        ));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            ACTIVE.clear();
            shakeIntensity = 0.0F;
            shakeTicks = 0;
            return;
        }

        long gameTime = level.getGameTime();
        Iterator<ClientPulseWave> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            ClientPulseWave wave = iterator.next();
            double previousRadius = wave.radiusAt(gameTime - 1);
            double currentRadius = wave.radiusAt(gameTime);
            spawnShellParticles(level, wave, currentRadius);

            double playerDistance = wave.origin.distanceTo(player.getBoundingBox().getCenter());
            boolean shellHit = WavePropagationMath.isShellCrossing(playerDistance, previousRadius, currentRadius);
            boolean lateCatchUp = playerDistance <= currentRadius && (currentRadius > 0.0 || playerDistance == 0.0);
            if (!wave.hitLocalPlayer && (shellHit || lateCatchUp)) {
                applyLocalPlayerHit(level, wave, playerDistance);
            }

            if (currentRadius > wave.maxRadius + wave.speedBlocksPerTick) {
                iterator.remove();
            }
        }

        if (shakeTicks > 0) {
            shakeTicks--;
            if (shakeTicks <= 0) {
                shakeIntensity = 0.0F;
            } else {
                shakeIntensity *= 0.85F;
            }
        }
    }

    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (shakeTicks <= 0 || shakeIntensity <= 0.0F) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        float time = (float) (minecraft.player.tickCount + event.getPartialTick()) * 1.7F;
        float yawJitter = Mth.sin(time * 1.3F) * shakeIntensity * 2.5F;
        float pitchJitter = Mth.cos(time * 1.7F) * shakeIntensity * 1.8F;
        event.setYaw(event.getYaw() + yawJitter);
        event.setPitch(event.getPitch() + pitchJitter);
    }

    private static void spawnShellParticles(Level level, ClientPulseWave wave, double radius) {
        if (radius <= 0.0 || radius > wave.maxRadius) {
            return;
        }

        int points = Mth.clamp((int) (radius * 4.0), 8, 48);
        for (int i = 0; i < points; i++) {
            double theta = level.random.nextDouble() * Math.PI * 2.0;
            double phi = Math.acos(2.0 * level.random.nextDouble() - 1.0);
            double sinPhi = Math.sin(phi);
            double x = wave.origin.x + radius * sinPhi * Math.cos(theta);
            double y = wave.origin.y + radius * Math.cos(phi);
            double z = wave.origin.z + radius * sinPhi * Math.sin(theta);
            level.addParticle(ParticleTypes.CLOUD, x, y, z, 0.0, 0.0, 0.0);
            if (i % 4 == 0) {
                level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
            }
        }
    }

    private static void applyLocalPlayerHit(Level level, ClientPulseWave wave, double playerDistance) {
        wave.hitLocalPlayer = true;
        float strength = strengthAt(wave, playerDistance);
        level.playLocalSound(
                wave.origin.x,
                wave.origin.y,
                wave.origin.z,
                SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.BLOCKS,
                4.0F * strength,
                (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F,
                false
        );
        shakeIntensity = Math.max(shakeIntensity, 0.4F * strength);
        shakeTicks = Math.max(shakeTicks, 8 + Mth.ceil(6.0F * strength));
    }

    private static float strengthAt(ClientPulseWave wave, double distance) {
        double remaining = Math.max(0.0, wave.amplitude - distance);
        return (float) Mth.clamp(remaining / Math.max(1, wave.amplitude), 0.05, 1.0);
    }

    private static final class ClientPulseWave {
        private final UUID id;
        private final Vec3 origin;
        private final long emissionGameTime;
        private final double speedBlocksPerTick;
        private final double maxRadius;
        private final int amplitude;
        private boolean hitLocalPlayer;

        private ClientPulseWave(
                UUID id,
                Vec3 origin,
                long emissionGameTime,
                double speedBlocksPerTick,
                double maxRadius,
                int amplitude
        ) {
            this.id = id;
            this.origin = origin;
            this.emissionGameTime = emissionGameTime;
            this.speedBlocksPerTick = speedBlocksPerTick;
            this.maxRadius = maxRadius;
            this.amplitude = amplitude;
        }

        private double radiusAt(long gameTime) {
            long age = Math.max(0, gameTime - emissionGameTime);
            return speedBlocksPerTick * age;
        }
    }
}
