package com.secondition.creeperindustry;

import net.neoforged.neoforge.data.event.GatherDataEvent;

public class CIDatagen {
    public static void gatherData(GatherDataEvent event) {
        if (!event.getMods().contains(CreeperIndustry.MODID)) {
            return;
        }
    }
}
