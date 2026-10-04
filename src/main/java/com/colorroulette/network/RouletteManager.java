package com.colorroulette.network;

import com.colorroulette.RouletteColor;
import com.colorroulette.RouletteTiming;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraftforge.network.PacketDistributor;

/** Lógica del servidor: decide cuándo se puede girar y avisa a todos los jugadores. */
public final class RouletteManager {
    private static MinecraftServer lastServer;
    private static int busyUntil;

    /**
     * Intenta iniciar la ruleta. Si ya hay una girando devuelve false
     * (así no se encimen animaciones si varios juegan a la vez).
     */
    public static boolean tryStart(ServerPlayer player, RouletteColor color) {
        MinecraftServer server = player.server;
        if (server != lastServer) {
            lastServer = server;
            busyUntil = 0;
        }

        int now = server.getTickCount();
        if (now < busyUntil) {
            player.displayClientMessage(Component.translatable("message.colorroulette.busy"), true);
            return false;
        }
        busyUntil = now + RouletteTiming.TOTAL + 5;

        RandomSource random = player.getRandom();
        float jitter = (random.nextFloat() * 2f - 1f) * 14f;
        int turns = 5 + random.nextInt(3);

        ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(),
                new RouletteStartPacket(color.ordinal(), jitter, turns, player.getGameProfile().getName()));
        return true;
    }

    private RouletteManager() {}
}
