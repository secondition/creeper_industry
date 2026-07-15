package com.secondition.creeperindustry.content.production.biosphere.recipe;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.secondition.creeperindustry.content.production.biosphere.BiosphereType;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public final class BiosphereCultivationRecipeSerializer implements RecipeSerializer<BiosphereCultivationRecipe> {
    private static final Codec<List<ItemStack>> OUTPUTS_CODEC = ItemStack.STRICT_CODEC.listOf().comapFlatMap(
            outputs -> outputs.isEmpty()
                    ? DataResult.error(() -> "Biosphere cultivation recipe must define at least one output")
                    : DataResult.success(outputs),
            outputs -> outputs
    );
    private static final MapCodec<BiosphereCultivationRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiosphereType.CODEC.fieldOf("biosphere").forGetter(BiosphereCultivationRecipe::biosphereType),
            Ingredient.CODEC_NONEMPTY.fieldOf("template").forGetter(BiosphereCultivationRecipe::template),
            BiosphereCatalyst.CODEC.optionalFieldOf("catalyst").forGetter(BiosphereCultivationRecipe::catalyst),
            OUTPUTS_CODEC.fieldOf("outputs").forGetter(BiosphereCultivationRecipe::outputs),
            BiosphereSignalRequirement.CODEC.optionalFieldOf("signal", BiosphereSignalRequirement.DEFAULT)
                    .forGetter(BiosphereCultivationRecipe::signalRequirement)
    ).apply(instance, BiosphereCultivationRecipe::new));
    private static final StreamCodec<RegistryFriendlyByteBuf, BiosphereCultivationRecipe> STREAM_CODEC = StreamCodec.of(
            BiosphereCultivationRecipeSerializer::toNetwork,
            BiosphereCultivationRecipeSerializer::fromNetwork
    );

    @Override
    public MapCodec<BiosphereCultivationRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, BiosphereCultivationRecipe> streamCodec() {
        return STREAM_CODEC;
    }

    private static BiosphereCultivationRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
        BiosphereType biosphereType = buffer.readEnum(BiosphereType.class);
        Ingredient template = Ingredient.CONTENTS_STREAM_CODEC.decode(buffer);
        Optional<BiosphereCatalyst> catalyst = buffer.readBoolean()
                ? Optional.of(new BiosphereCatalyst(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer), buffer.readVarInt()))
                : Optional.empty();
        List<ItemStack> outputs = ItemStack.LIST_STREAM_CODEC.decode(buffer);
        BiosphereSignalRequirement signalRequirement = new BiosphereSignalRequirement(
                buffer.readVarInt(),
                buffer.readEnum(BiosphereInstantaneousRequirement.class)
        );
        return new BiosphereCultivationRecipe(biosphereType, template, catalyst, outputs, signalRequirement);
    }

    private static void toNetwork(RegistryFriendlyByteBuf buffer, BiosphereCultivationRecipe recipe) {
        buffer.writeEnum(recipe.biosphereType());
        Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.template());
        buffer.writeBoolean(recipe.catalyst().isPresent());
        recipe.catalyst().ifPresent(catalyst -> {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, catalyst.ingredient());
            buffer.writeVarInt(catalyst.count());
        });
        ItemStack.LIST_STREAM_CODEC.encode(buffer, recipe.outputs());
        buffer.writeVarInt(recipe.signalRequirement().minimumAmplitude());
        buffer.writeEnum(recipe.signalRequirement().instantaneous());
    }
}
