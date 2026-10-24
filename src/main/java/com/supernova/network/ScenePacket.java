package com.supernova.network;

import com.supernova.client.ClientSceneState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> cliente: escena activa (0 ninguna, 1 parte 1, 2 parte 2) y ticks transcurridos. */
public class ScenePacket {
    private final int scene;
    private final int elapsed;

    public ScenePacket(int scene, int elapsed) {
        this.scene = scene;
        this.elapsed = elapsed;
    }

    public static void encode(ScenePacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.scene);
        buf.writeVarInt(p.elapsed);
    }

    public static ScenePacket decode(FriendlyByteBuf buf) {
        return new ScenePacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(ScenePacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSceneState.set(p.scene, p.elapsed)));
        ctx.get().setPacketHandled(true);
    }
}
