package com.secondition.creeperindustry.content.production.biosphere.recipe;

import java.util.List;
import java.util.Optional;

import com.secondition.creeperindustry.CIRecipeSerializers;
import com.secondition.creeperindustry.CIRecipeTypes;
import com.secondition.creeperindustry.content.production.biosphere.BiosphereType;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public final class BiosphereCultivationRecipe implements Recipe<BiosphereRecipeInput> {
    private final BiosphereType biosphereType;
    private final Ingredient template;
    private final Optional<BiosphereCatalyst> catalyst;
    private final List<ItemStack> outputs;
    private final BiosphereSignalRequirement signalRequirement;

    public BiosphereCultivationRecipe(
            BiosphereType biosphereType,
            Ingredient template,
            Optional<BiosphereCatalyst> catalyst,
            List<ItemStack> outputs,
            BiosphereSignalRequirement signalRequirement
    ) {
        this.biosphereType = biosphereType;
        this.template = template;
        this.catalyst = catalyst;
        this.outputs = outputs.stream().map(ItemStack::copy).toList();
        this.signalRequirement = signalRequirement;
    }

    @Override
    public boolean matches(BiosphereRecipeInput input, Level level) {
        if (input.biosphereType() != biosphereType || !template.test(input.template())) {
            return false;
        }
        return catalyst.map(value -> value.matches(input.catalyst())).orElseGet(input.catalyst()::isEmpty);
    }

    @Override
    public ItemStack assemble(BiosphereRecipeInput input, HolderLookup.Provider registries) {
        return outputs.getFirst().copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return outputs.getFirst().copy();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(template);
        catalyst.ifPresent(value -> ingredients.add(value.ingredient()));
        return ingredients;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CIRecipeSerializers.BIOSPHERE_CULTIVATION.get();
    }

    @Override
    public RecipeType<?> getType() {
        return CIRecipeTypes.BIOSPHERE_CULTIVATION.get();
    }

    public BiosphereType biosphereType() {
        return biosphereType;
    }

    public Ingredient template() {
        return template;
    }

    public Optional<BiosphereCatalyst> catalyst() {
        return catalyst;
    }

    public List<ItemStack> outputs() {
        return outputs.stream().map(ItemStack::copy).toList();
    }

    public BiosphereSignalRequirement signalRequirement() {
        return signalRequirement;
    }

    public ItemStack selectedOutput(int selectedOutputIndex) {
        return outputs.get(Math.floorMod(selectedOutputIndex, outputs.size())).copy();
    }
}
