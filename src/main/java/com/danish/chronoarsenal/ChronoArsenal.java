package com.danish.chronoarsenal;

import com.danish.chronoarsenal.registry.ModEffects;
import com.danish.chronoarsenal.registry.ModItems;
import com.danish.chronoarsenal.registry.ModTabs;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ChronoArsenal.MODID)
public class ChronoArsenal {
    public static final String MODID = "chronoarsenal";

    // No-argument constructor on purpose: NeoForge 1.20.1 (47.1.x) cannot pass
    // FMLJavaModLoadingContext into the constructor like newer Forge builds can.
    public ChronoArsenal() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        ModTabs.TABS.register(modBus);
    }
}
