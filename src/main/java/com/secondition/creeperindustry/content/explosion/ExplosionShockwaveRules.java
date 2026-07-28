package com.secondition.creeperindustry.content.explosion;

import net.minecraft.util.Mth;

public final class ExplosionShockwaveRules {
    public static final float MIN_VISUAL_RADIUS = 24.0F;
    public static final float MAX_VISUAL_RADIUS = 64.0F;
    public static final float WAVE_SPEED_BLOCKS_PER_TICK = 3.25F;
    public static final float WAVE_THICKNESS_BLOCKS = 2.5F;
    public static final float IMPACT_LINGER_TICKS = 5.0F;

    private static final float BASE_VISUAL_RADIUS = 16.0F;
    private static final float RADIUS_PER_POWER = 6.0F;

    private ExplosionShockwaveRules() {
    }

    public static float visualRadius(float explosionPower) {
        return Mth.clamp(
                BASE_VISUAL_RADIUS + Math.max(0.0F, explosionPower) * RADIUS_PER_POWER,
                MIN_VISUAL_RADIUS,
                MAX_VISUAL_RADIUS
        );
    }

    public static float lifetimeTicks(float explosionPower) {
        return visualRadius(explosionPower) / WAVE_SPEED_BLOCKS_PER_TICK + IMPACT_LINGER_TICKS;
    }
}
