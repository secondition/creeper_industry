package com.secondition.creeperindustry.client.explosion.wave;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Camera response and sound when a wave reaches the player. */
final class PulseWaveFeedback {
    private static final double DURATION_TICKS = 10.0;
    private double startedAt = Double.NEGATIVE_INFINITY;
    private double recoveryTicks = 12.0;
    private float intensity;
    private float polarity = 1.0F;
    private float direction = 1.0F;

    void hit(
            ClientLevel level,
            LocalPlayer player,
            ClientPulseWave wave,
            double distance,
            double gameTime) {
        float strength = wave.strengthAt(distance);
        double amplitude = Math.min(64.0, wave.amplitudeAt(distance)) / 16.0;
        float impact = (float) (amplitude * amplitude);
        double feedbackTime = Math.max(startedAt, gameTime);
        float remaining = visualStrength(feedbackTime);
        recoveryTicks =
                Math.max(
                        Math.min(400.0, Math.max(12.0, impact * 100.0)),
                        Math.max(0.0, recoveryTicks - (feedbackTime - startedAt)));
        intensity =
                Math.min(16.0F, Math.max(impact, remaining) + Math.min(impact, remaining) * 0.25F);
        startedAt = feedbackTime;
        polarity = wave.polarity();
        direction = (wave.id().getLeastSignificantBits() & 1) == 0 ? 1.0F : -1.0F;

        // Play the air burst at the listener after the wave has travelled here.
        level.playLocalSound(
                player.getX(),
                player.getEyeY(),
                player.getZ(),
                SoundEvents.WIND_CHARGE_BURST.value(),
                SoundSource.BLOCKS,
                0.65F * strength,
                polarity > 0 ? 0.65F : 1.0F,
                false);
    }

    void apply(ViewportEvent.ComputeCameraAngles event, double gameTime) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || minecraft.player.isSpectator()
                || minecraft.getCameraEntity() != minecraft.player) {
            return;
        }
        float strength = Math.min(3.0F, intensity * envelope(gameTime));
        if (strength <= 0.0F) {
            return;
        }
        float age = (float) Math.max(0.0, gameTime - startedAt);
        event.setPitch(event.getPitch() - Mth.cos(age * 2.4F) * 4.5F * strength);
        event.setYaw(event.getYaw() + Mth.sin(age * 2.0F + 0.5F) * 2.8F * strength * direction);
        event.setRoll(event.getRoll() + Mth.sin(age * 2.7F) * 1.8F * strength * direction);
    }

    float visualPolarity() {
        return polarity;
    }

    float visualStrength(double gameTime) {
        float fade = recovery(gameTime);
        return intensity * fade;
    }

    private float recovery(double gameTime) {
        double age = gameTime - startedAt;
        return age < 0.0 || age >= recoveryTicks ? 0.0F : (float) (1.0 - age / recoveryTicks);
    }

    float visualAge(double gameTime) {
        return visualStrength(gameTime) > 0.0F ? (float) Math.max(0.0, gameTime - startedAt) : 0.0F;
    }

    private float envelope(double gameTime) {
        double age = gameTime - startedAt;
        if (age < 0.0 || age >= DURATION_TICKS) {
            return 0.0F;
        }
        return (float) (Math.exp(-age * 0.38) * (1.0 - age / DURATION_TICKS));
    }

    void clear() {
        startedAt = Double.NEGATIVE_INFINITY;
        recoveryTicks = 12.0;
        intensity = 0.0F;
        polarity = 1.0F;
    }
}
