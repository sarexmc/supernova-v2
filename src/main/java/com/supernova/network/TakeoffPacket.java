package com.supernova.network;

import com.supernova.server.ShipLauncher;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> servidor: pulsar el boton DESPEGAR. */
public class TakeoffPacket {
    private final BlockPos pos;

    public TakeoffPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(TakeoffPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
    }

    public static TakeoffPacket decode(FriendlyByteBuf buf) {
        return new TakeoffPacket(buf.readBlockPos());
    }

    public static void handle(TakeoffPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            if (player.blockPosition().distSqr(p.pos) > 100) return;
            ShipLauncher.tryLaunch(player.serverLevel(), p.pos, player);
        });
        ctx.get().setPacketHandled(true);
    }
}
