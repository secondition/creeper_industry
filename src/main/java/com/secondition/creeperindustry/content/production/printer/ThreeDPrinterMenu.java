package com.secondition.creeperindustry.content.production.printer;

import com.secondition.creeperindustry.CIMenuTypes;
import com.secondition.creeperindustry.foundation.menu.PlaceholderMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class ThreeDPrinterMenu extends PlaceholderMenu {
    public ThreeDPrinterMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        super(CIMenuTypes.THREE_D_PRINTER.get(), containerId);
    }
}
