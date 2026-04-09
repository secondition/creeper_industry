package com.secondition.creeperindustry.content.logistics.storage;

import java.util.List;

import com.secondition.creeperindustry.CIDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.gameevent.GameEvent;

import net.minecraft.util.Mth;

public class StorageDiscItem extends Item {
    public static final int DEFAULT_CAPACITY = 4096;
    private static final int BAR_COLOR = Mth.color(0.24F, 0.72F, 0.47F);

    public StorageDiscItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static StorageDiscContents getContents(ItemStack stack) {
        return stack.getOrDefault(CIDataComponents.STORAGE_DISC_CONTENTS.get(), StorageDiscContents.EMPTY);
    }

    public static void setContents(ItemStack stack, StorageDiscContents contents) {
        stack.set(CIDataComponents.STORAGE_DISC_CONTENTS.get(), contents.isEmpty() ? StorageDiscContents.EMPTY : contents);
    }

    public static boolean canBurn(ItemStack discStack, ItemStack inputStack) {
        if (discStack.isEmpty() || inputStack.isEmpty()) {
            return false;
        }
        StorageDiscContents contents = getContents(discStack);
        return contents.isEmpty() || contents.matches(inputStack);
    }

    public static boolean isFull(ItemStack discStack) {
        return getContents(discStack).storedCount() >= DEFAULT_CAPACITY;
    }

    @Override
    public boolean canFitInsideContainerItems(ItemStack stack) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        StorageDiscContents contents = getContents(stack);
        if (contents.isEmpty()) {
            tooltipComponents.add(Component.translatable("tooltip.creeper_industry.storage_disc.empty").withStyle(ChatFormatting.GRAY));
        } else {
            tooltipComponents.add(Component.translatable("tooltip.creeper_industry.storage_disc.stored_item", contents.storedItem().getHoverName()).withStyle(ChatFormatting.GRAY));
        }

        tooltipComponents.add(Component.translatable("tooltip.creeper_industry.storage_disc.amount", contents.storedCount(), DEFAULT_CAPACITY).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.creeper_industry.storage_disc.usage").withStyle(ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return !getContents(stack).isEmpty();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        StorageDiscContents contents = getContents(stack);
        if (contents.isEmpty()) {
            return 0;
        }
        return Math.min(1 + Mth.floor((float) contents.storedCount() * 12.0F / DEFAULT_CAPACITY), 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return BAR_COLOR;
    }

    @Override
    public void onDestroyed(ItemEntity itemEntity, DamageSource damageSource) {
        StorageDiscContents contents = getContents(itemEntity.getItem());
        if (contents.isEmpty()) {
            return;
        }

        setContents(itemEntity.getItem(), StorageDiscContents.EMPTY);
        ItemUtils.onContainerDestroyed(itemEntity, contents.asItemStacks());
    }

    public InteractionResultHolder<ItemStack> open(Level level, Player player, InteractionHand hand) {
        ItemStack disc = player.getItemInHand(hand);
        StorageDiscContents contents = getContents(disc);
        if (contents.isEmpty()) {
            return InteractionResultHolder.fail(disc);
        }

        if (!level.isClientSide()) {
            for (ItemStack entry : contents.asItemStacks()) {
                player.getInventory().placeItemBackInInventory(entry.copy());
            }
        }

        disc.shrink(1);
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (state.is(BlockTags.RAILS)) {
            ItemStack stack = context.getItemInHand();
            if (level instanceof ServerLevel serverLevel) {
                RailShape railShape = state.getBlock() instanceof BaseRailBlock railBlock
                        ? railBlock.getRailDirection(state, level, pos, null)
                        : RailShape.NORTH_SOUTH;
                double yOffset = railShape.isAscending() ? 0.5D : 0.0D;
                StorageDiscMinecartEntity minecart = new StorageDiscMinecartEntity(
                        serverLevel,
                        pos.getX() + 0.5D,
                        pos.getY() + 0.0625D + yOffset,
                        pos.getZ() + 0.5D,
                        stack.copyWithCount(1)
                );
                serverLevel.addFreshEntity(minecart);
                serverLevel.gameEvent(GameEvent.ENTITY_PLACE, pos, GameEvent.Context.of(context.getPlayer(), state));
            }

            stack.shrink(1);
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) {
            return open(context.getLevel(), player, context.getHand()).getResult();
        }
        return super.useOn(context);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) {
            return open(level, player, hand);
        }
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }
}
