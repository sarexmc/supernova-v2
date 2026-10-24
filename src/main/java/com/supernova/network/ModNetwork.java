package com.supernova.network;

import com.supernova.SupernovaMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String PROTOCOL = "1";
    public static SimpleChannel CHANNEL;

    public static void register() {
        CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(SupernovaMod.MODID, "main"),
                () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);
        int id = 0;
        CHANNEL.registerMessage(id++, ScenePacket.class, ScenePacket::encode, ScenePacket::decode, ScenePacket::handle);
        CHANNEL.registerMessage(id++, TakeoffPacket.class, TakeoffPacket::encode, TakeoffPacket::decode, TakeoffPacket::handle);
        CHANNEL.registerMessage(id++, HoldPacket.class, HoldPacket::encode, HoldPacket::decode, HoldPacket::handle);
    }
}
