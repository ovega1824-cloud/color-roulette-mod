package com.colorroulette.client;

import com.colorroulette.RouletteColor;
import com.colorroulette.network.RouletteStartPacket;

public final class ClientPacketHandler {
    public static void handle(RouletteStartPacket msg) {
        RouletteClient.start(RouletteColor.byIndex(msg.colorIndex), msg.jitter, msg.turns, msg.playerName);
    }

    private ClientPacketHandler() {}
}
