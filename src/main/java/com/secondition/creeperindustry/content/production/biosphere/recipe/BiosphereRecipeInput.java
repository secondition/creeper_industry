package com.secondition.creeperindustry.content.production.biosphere.recipe;

import com.secondition.creeperindustry.content.production.biosphere.BiosphereType;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record BiosphereRecipeInput(BiosphereType biosphereType, ItemStack template, ItemStack catalyst) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> template;
            case 1 -> catalyst;
            default -> throw new IllegalArgumentException("Unsupported biosphere recipe input index: " + index);
        };
    }

    @Override
    public int size() {
        return 2;
    }
}
