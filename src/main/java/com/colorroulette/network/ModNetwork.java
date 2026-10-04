package com.colorroulette.network;

import com.colorroulette.ColorRouletteMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ColorRouletteMod.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    public static void register() {
        CHANNEL.registerMessage(0, RouletteStartPacket.class,
                RouletteStartPacket::encode,
                RouletteStartPacket::decode,
                RouletteStartPacket::handle);
    }

    private ModNetwork() {}
}
