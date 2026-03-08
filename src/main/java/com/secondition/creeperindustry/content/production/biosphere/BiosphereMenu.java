package com.secondition.creeperindustry.content.production.biosphere;

import com.secondition.creeperindustry.CIMenuTypes;
import com.secondition.creeperindustry.foundation.menu.PlaceholderMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class BiosphereMenu extends PlaceholderMenu {
    public BiosphereMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        super(CIMenuTypes.BIOSPHERE.get(), containerId);
    }
}
