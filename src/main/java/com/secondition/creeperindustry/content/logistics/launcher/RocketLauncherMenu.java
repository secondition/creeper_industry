package com.secondition.creeperindustry.content.logistics.launcher;

import com.secondition.creeperindustry.CIMenuTypes;
import com.secondition.creeperindustry.foundation.menu.PlaceholderMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;

public class RocketLauncherMenu extends PlaceholderMenu {
    public RocketLauncherMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf extraData) {
        super(CIMenuTypes.ROCKET_LAUNCHER.get(), containerId);
    }
}
