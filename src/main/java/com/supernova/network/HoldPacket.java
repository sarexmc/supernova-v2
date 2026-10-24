package com.supernova.network;

import com.supernova.entity.ShipEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: "estoy manteniendo CTRL" (se envia cada tick mientras se cargan los motores). */
public class HoldPacket {
    public static void encode(HoldPacket p, FriendlyByteBuf buf) { }

    public static HoldPacket decode(FriendlyByteBuf buf) {
        return new HoldPacket();
    }

    public static void handle(HoldPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.getVehicle() instanceof ShipEntity ship) ship.markHeld(player);
        });
        ctx.get().setPacketHandled(true);
    }
}
