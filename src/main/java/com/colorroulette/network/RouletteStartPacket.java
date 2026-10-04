package com.colorroulette.network;

import com.colorroulette.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> todos los clientes: "arranca la ruleta y cae en este color". */
public class RouletteStartPacket {
    public final int colorIndex;
    /** Pequeño desvío dentro del segmento para que no caiga siempre al centro (grados). */
    public final float jitter;
    /** Vueltas completas extra antes de frenar. */
    public final int turns;
    public final String playerName;

    public RouletteStartPacket(int colorIndex, float jitter, int turns, String playerName) {
        this.colorIndex = colorIndex;
        this.jitter = jitter;
        this.turns = turns;
        this.playerName = playerName;
    }

    public static void encode(RouletteStartPacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.colorIndex);
        buf.writeFloat(msg.jitter);
        buf.writeVarInt(msg.turns);
        buf.writeUtf(msg.playerName, 64);
    }

    public static RouletteStartPacket decode(FriendlyByteBuf buf) {
        return new RouletteStartPacket(buf.readVarInt(), buf.readFloat(), buf.readVarInt(), buf.readUtf(64));
    }

    public static void handle(RouletteStartPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientPacketHandler.handle(msg)));
        ctx.get().setPacketHandled(true);
    }
}
