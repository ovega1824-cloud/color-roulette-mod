package com.colorroulette;

import com.colorroulette.network.ModNetwork;
import com.colorroulette.registry.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(ColorRouletteMod.MODID)
public class ColorRouletteMod {
    public static final String MODID = "colorroulette";

    public ColorRouletteMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.ITEMS.register(modBus);
        ModItems.TABS.register(modBus);
        ModNetwork.register();
    }
}
