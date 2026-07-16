package com.secondition.creeperindustry;

import java.util.function.Supplier;

import com.secondition.creeperindustry.content.automation.breaker.SignalRangeBreakerMenu;
import com.secondition.creeperindustry.content.logistics.storage.DiscBurnerMenu;
import com.secondition.creeperindustry.content.energy.signal.CreativeSignalSourceMenu;
import com.secondition.creeperindustry.content.logistics.dropper.PrecisionDropperMenu;
import com.secondition.creeperindustry.content.logistics.launcher.RocketLauncherMenu;
import com.secondition.creeperindustry.content.production.biosphere.BiosphereMenu;
import com.secondition.creeperindustry.content.production.printer.ThreeDPrinterMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CIMenuTypes {
    private static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, CreeperIndustry.MODID);

    public static final Supplier<MenuType<PrecisionDropperMenu>> PRECISION_DROPPER = MENU_TYPES.register("precision_dropper",
            () -> IMenuTypeExtension.create(PrecisionDropperMenu::new));
    public static final Supplier<MenuType<BiosphereMenu>> BIOSPHERE = MENU_TYPES.register("biosphere",
            () -> IMenuTypeExtension.create(BiosphereMenu::new));
    public static final Supplier<MenuType<DiscBurnerMenu>> DISC_BURNER = MENU_TYPES.register("disc_burner",
            () -> IMenuTypeExtension.create(DiscBurnerMenu::new));
    public static final Supplier<MenuType<CreativeSignalSourceMenu>> CREATIVE_SIGNAL_SOURCE = MENU_TYPES.register("creative_signal_source",
            () -> IMenuTypeExtension.create(CreativeSignalSourceMenu::new));
    public static final Supplier<MenuType<SignalRangeBreakerMenu>> SIGNAL_RANGE_BREAKER = MENU_TYPES.register("signal_range_breaker",
            () -> IMenuTypeExtension.create(SignalRangeBreakerMenu::new));
    public static final Supplier<MenuType<ThreeDPrinterMenu>> THREE_D_PRINTER = MENU_TYPES.register("three_d_printer",
            () -> IMenuTypeExtension.create(ThreeDPrinterMenu::new));
    public static final Supplier<MenuType<RocketLauncherMenu>> ROCKET_LAUNCHER = MENU_TYPES.register("rocket_launcher",
            () -> IMenuTypeExtension.create(RocketLauncherMenu::new));

    public static void register(IEventBus modEventBus) {
        MENU_TYPES.register(modEventBus);
    }
}
