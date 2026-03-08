package com.secondition.creeperindustry.content.logistics.dropper;

import com.secondition.creeperindustry.CIMenuTypes;
import com.secondition.creeperindustry.foundation.menu.PlaceholderMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class PrecisionDropperMenu extends PlaceholderMenu {
    public PrecisionDropperMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        super(CIMenuTypes.PRECISION_DROPPER.get(), containerId);
    }
}
