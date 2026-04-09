package com.secondition.creeperindustry.content.logistics.storage;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record StorageDiscContents(ItemStack storedItem, int storedCount) {
    public static final StorageDiscContents EMPTY = new StorageDiscContents(ItemStack.EMPTY, 0);
    public static final Codec<StorageDiscContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("item", ItemStack.EMPTY).forGetter(StorageDiscContents::storedItem),
            Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("count", 0).forGetter(StorageDiscContents::storedCount)
    ).apply(instance, StorageDiscContents::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, StorageDiscContents> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC, StorageDiscContents::storedItem,
            ByteBufCodecs.VAR_INT, StorageDiscContents::storedCount,
            StorageDiscContents::new
    );

    public StorageDiscContents {
        if (storedItem == null || storedItem.isEmpty() || storedCount <= 0) {
            storedItem = ItemStack.EMPTY;
            storedCount = 0;
        } else {
            storedItem = storedItem.copyWithCount(1);
        }
    }

    public boolean isEmpty() {
        return storedItem.isEmpty() || storedCount <= 0;
    }

    public boolean matches(ItemStack stack) {
        return !isEmpty() && !stack.isEmpty() && ItemStack.isSameItemSameComponents(storedItem, stack);
    }

    public StorageDiscContents withStoredItem(ItemStack stack) {
        return stack.isEmpty() ? EMPTY : new StorageDiscContents(stack, storedCount);
    }

    public StorageDiscContents withCount(int newCount) {
        return newCount <= 0 ? EMPTY : new StorageDiscContents(storedItem, newCount);
    }

    public List<ItemStack> asItemStacks() {
        if (isEmpty()) {
            return List.of();
        }

        List<ItemStack> drops = new ArrayList<>();
        int remaining = storedCount;
        int maxStackSize = storedItem.getMaxStackSize();
        while (remaining > 0) {
            int split = Math.min(remaining, maxStackSize);
            drops.add(storedItem.copyWithCount(split));
            remaining -= split;
        }
        return drops;
    }
}
