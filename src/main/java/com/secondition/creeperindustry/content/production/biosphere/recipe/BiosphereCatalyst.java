package com.secondition.creeperindustry.content.production.biosphere.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public record BiosphereCatalyst(Ingredient ingredient, int count) {
    public static final Codec<BiosphereCatalyst> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(BiosphereCatalyst::ingredient),
            ExtraCodecs.intRange(1, 64).optionalFieldOf("count", 1).forGetter(BiosphereCatalyst::count)
    ).apply(instance, BiosphereCatalyst::new));

    public boolean matches(ItemStack stack) {
        return ingredient.test(stack) && stack.getCount() >= count;
    }
}
