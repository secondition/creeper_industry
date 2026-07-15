package com.secondition.creeperindustry;

import java.util.function.Supplier;

import com.secondition.creeperindustry.content.production.biosphere.recipe.BiosphereCultivationRecipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CIRecipeTypes {
    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, CreeperIndustry.MODID);

    public static final Supplier<RecipeType<BiosphereCultivationRecipe>> BIOSPHERE_CULTIVATION = RECIPE_TYPES.register(
            "biosphere_cultivation",
            () -> simple("biosphere_cultivation")
    );
    public static final Supplier<RecipeType<?>> SIGNAL_TRANSFORMATION = RECIPE_TYPES.register("signal_transformation", () -> simple("signal_transformation"));
    public static final Supplier<RecipeType<?>> THREE_D_PRINTING = RECIPE_TYPES.register("three_d_printing", () -> simple("three_d_printing"));

    private static <T extends net.minecraft.world.item.crafting.Recipe<?>> RecipeType<T> simple(String path) {
        return new RecipeType<>() {
            @Override
            public String toString() {
                return CreeperIndustry.MODID + ":" + path;
            }
        };
    }

    public static void register(IEventBus modEventBus) {
        RECIPE_TYPES.register(modEventBus);
    }
}
