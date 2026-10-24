package com.supernova.server;

import com.supernova.block.ShipBlockEntity;
import com.supernova.entity.ShipEntity;
import com.supernova.registry.ModBlocks;
import com.supernova.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class ShipLauncher {
    /** Pulsar el boton: sube a la tripulacion y empieza la fase de encendido de motores (hay que mantener CTRL). */
    public static void tryLaunch(ServerLevel level, BlockPos pos, ServerPlayer starter) {
        if (!level.getBlockState(pos).is(ModBlocks.SHIP.get())) return;
        if (!(level.getBlockEntity(pos) instanceof ShipBlockEntity ship)) return;
        if (!level.getBlockState(pos.below()).is(ModBlocks.LAUNCH_PAD.get())) {
            starter.displayClientMessage(Component.literal("Falta la base de lanzamiento debajo de la nave."), true);
            return;
        }
        if (ship.fuel < ShipBlockEntity.TAKEOFF_FUEL) {
            starter.displayClientMessage(Component.literal("Combustible insuficiente: se necesita al menos "
                    + ShipBlockEntity.TAKEOFF_FUEL + "%. Inserta tanques de dilitio."), true);
            return;
        }
        List<ServerPlayer> crew = level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(6), p -> !p.isSpectator());
        if (!crew.contains(starter)) crew.add(starter);
        if (crew.size() > 4) crew = crew.subList(0, 4);

        ShipEntity entity = new ShipEntity(ModEntities.SHIP.get(), level);
        entity.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        entity.setup(pos, pos.getY(), 70, ship.fuel, ShipEntity.Stage.CHARGING, crew);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3); // suelta los tanques que sobren
        level.addFreshEntity(entity);
        for (ServerPlayer p : crew) p.startRiding(entity, true);
    }
}
