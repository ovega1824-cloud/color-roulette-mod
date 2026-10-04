package com.colorroulette.registry;

import com.colorroulette.ColorRouletteMod;
import com.colorroulette.RouletteColor;
import com.colorroulette.item.ColorChipItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.EnumMap;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ColorRouletteMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ColorRouletteMod.MODID);

    /** Una ficha por cada color: red_chip, blue_chip, etc. */
    public static final Map<RouletteColor, RegistryObject<Item>> CHIPS = new EnumMap<>(RouletteColor.class);

    static {
        for (RouletteColor color : RouletteColor.values()) {
            CHIPS.put(color, ITEMS.register(color.id + "_chip",
                    () -> new ColorChipItem(color, new Item.Properties().stacksTo(1))));
        }
    }

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.colorroulette"))
                    .icon(() -> new ItemStack(CHIPS.get(RouletteColor.RED).get()))
                    .displayItems((params, output) -> {
                        for (RouletteColor color : RouletteColor.values()) {
                            output.accept(CHIPS.get(color).get());
                        }
                    })
                    .build());

    private ModItems() {}
}
