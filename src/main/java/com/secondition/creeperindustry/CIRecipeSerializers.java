package com.secondition.creeperindustry;

import java.util.function.Supplier;

import com.secondition.creeperindustry.content.production.biosphere.recipe.BiosphereCultivationRecipe;
import com.secondition.creeperindustry.content.production.biosphere.recipe.BiosphereCultivationRecipeSerializer;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CIRecipeSerializers {
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(
            Registries.RECIPE_SERIALIZER,
            CreeperIndustry.MODID
    );

    public static final Supplier<RecipeSerializer<BiosphereCultivationRecipe>> BIOSPHERE_CULTIVATION = RECIPE_SERIALIZERS.register(
            "biosphere_cultivation",
            BiosphereCultivationRecipeSerializer::new
    );

    private CIRecipeSerializers() {
    }

    public static void register(IEventBus modEventBus) {
        RECIPE_SERIALIZERS.register(modEventBus);
    }
}
