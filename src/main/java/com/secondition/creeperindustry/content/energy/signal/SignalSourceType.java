package com.secondition.creeperindustry.content.energy.signal;

import net.minecraft.resources.ResourceLocation;

public record SignalSourceType<T extends SignalSource>(ResourceLocation id, Class<T> sourceClass) {
}
