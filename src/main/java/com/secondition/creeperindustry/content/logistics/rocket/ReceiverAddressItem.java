package com.secondition.creeperindustry.content.logistics.rocket;

import java.util.List;

import com.secondition.creeperindustry.CIDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class ReceiverAddressItem extends Item {
    public ReceiverAddressItem(Properties properties) {
        super(properties.stacksTo(64));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        RocketDestination destination = stack.get(CIDataComponents.ROCKET_DESTINATION.get());
        if (destination == null) {
            tooltip.add(Component.translatable("tooltip.creeper_industry.receiver_address.unbound").withStyle(ChatFormatting.RED));
            return;
        }
        tooltip.add(Component.translatable(
                "tooltip.creeper_industry.receiver_address.dimension",
                destination.dimension().location().toString()
        ).withStyle(ChatFormatting.GRAY));
        BlockPosText.append(tooltip, destination.receiverPos());
    }

    private static final class BlockPosText {
        private BlockPosText() {
        }

        private static void append(List<Component> tooltip, net.minecraft.core.BlockPos pos) {
            tooltip.add(Component.translatable(
                    "tooltip.creeper_industry.receiver_address.position",
                    pos.getX(),
                    pos.getY(),
                    pos.getZ()
            ).withStyle(ChatFormatting.GRAY));
        }
    }
}
