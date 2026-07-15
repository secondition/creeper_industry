package com.secondition.creeperindustry.content.production.biosphere.recipe;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.secondition.creeperindustry.CIRecipeTypes;
import com.secondition.creeperindustry.content.energy.signal.AggregatedSignal;
import com.secondition.creeperindustry.content.production.biosphere.BiosphereType;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

public final class BiosphereRecipeService {
    private BiosphereRecipeService() {
    }

    public static Optional<RecipeHolder<BiosphereCultivationRecipe>> findRecipe(
            Level level,
            BiosphereType biosphereType,
            ItemStack template,
            ItemStack catalyst
    ) {
        BiosphereRecipeInput input = new BiosphereRecipeInput(biosphereType, template, catalyst);
        return recipesFor(level, biosphereType).stream()
                .filter(holder -> holder.value().matches(input, level))
                .min(Comparator.comparing(holder -> holder.id().toString()));
    }

    public static Optional<RecipeHolder<BiosphereCultivationRecipe>> findRecipeForTemplate(
            Level level,
            BiosphereType biosphereType,
            ItemStack template
    ) {
        return recipesFor(level, biosphereType).stream()
                .filter(holder -> holder.value().template().test(template))
                .min(Comparator.comparing(holder -> holder.id().toString()));
    }

    public static boolean isValidTemplate(Level level, BiosphereType biosphereType, ItemStack stack) {
        return !stack.isEmpty() && findRecipeForTemplate(level, biosphereType, stack).isPresent();
    }

    public static boolean isValidCatalyst(Level level, BiosphereType biosphereType, ItemStack stack) {
        return !stack.isEmpty() && recipesFor(level, biosphereType).stream()
                .map(holder -> holder.value().catalyst())
                .flatMap(Optional::stream)
                .anyMatch(catalyst -> catalyst.ingredient().test(stack));
    }

    public static boolean hasMatchingSignal(
            Level level,
            BiosphereType biosphereType,
            ItemStack template,
            AggregatedSignal signal
    ) {
        return findRecipeForTemplate(level, biosphereType, template)
                .map(holder -> holder.value().signalRequirement().matches(signal))
                .orElse(false);
    }

    private static List<RecipeHolder<BiosphereCultivationRecipe>> recipesFor(Level level, BiosphereType biosphereType) {
        return level.getRecipeManager().getAllRecipesFor(CIRecipeTypes.BIOSPHERE_CULTIVATION.get()).stream()
                .filter(holder -> holder.value().biosphereType() == biosphereType)
                .toList();
    }
}
