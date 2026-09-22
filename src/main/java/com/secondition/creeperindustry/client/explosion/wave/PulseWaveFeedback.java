package com.secondition.creeperindustry.client.explosion.wave;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.ViewportEvent;

/** Short, bounded camera recoil and an arrival sound, separate from the explosion sound. */
final class PulseWaveFeedback {
    private static final double DURATION_TICKS = 10.0;
    private static final double VISUAL_RECOVERY_TICKS = 12.0;
    private double startedAt = Double.NEGATIVE_INFINITY;
    private float intensity;
    private float direction = 1.0F;

    void hit(
            ClientLevel level,
            LocalPlayer player,
            ClientPulseWave wave,
            double distance,
            double gameTime) {
        float strength = wave.strengthAt(distance);
        double feedbackTime = Math.max(startedAt, gameTime);
        float remaining = intensity * envelope(feedbackTime);
        intensity =
                Math.min(
                        1.25F,
                        Math.max(strength, remaining) + Math.min(strength, remaining) * 0.25F);
        startedAt = feedbackTime;
        direction = (wave.id().getLeastSignificantBits() & 1) == 0 ? 1.0F : -1.0F;

        // The original explosion still supplies the blast sound. A quiet air burst at the
        // listener supplies arrival feedback without playing GENERIC_EXPLODE twice or
        // applying distance attenuation a second time after the wave has travelled here.
        level.playLocalSound(
                player.getX(),
                player.getEyeY(),
                player.getZ(),
                SoundEvents.WIND_CHARGE_BURST.value(),
                SoundSource.BLOCKS,
                0.65F * strength,
                0.65F,
                false);
    }

    void apply(ViewportEvent.ComputeCameraAngles event, double gameTime) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || minecraft.player.isSpectator()
                || minecraft.getCameraEntity() != minecraft.player) {
            return;
        }
        float strength = intensity * envelope(gameTime);
        if (strength <= 0.0F) {
            return;
        }
        float age = (float) Math.max(0.0, gameTime - startedAt);
        event.setPitch(event.getPitch() - Mth.cos(age * 2.4F) * 4.5F * strength);
        event.setYaw(event.getYaw() + Mth.sin(age * 2.0F + 0.5F) * 2.8F * strength * direction);
        event.setRoll(event.getRoll() + Mth.sin(age * 2.7F) * 1.8F * strength * direction);
    }

    float visualStrength(double gameTime) {
        double age = gameTime - startedAt;
        if (age < 0.0 || age >= VISUAL_RECOVERY_TICKS) {
            return 0.0F;
        }
        return Math.min(1.0F, intensity * 1.35F)
                * (float) (Math.exp(-age * 0.24) * (1.0 - age / VISUAL_RECOVERY_TICKS));
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
        intensity = 0.0F;
    }
}
