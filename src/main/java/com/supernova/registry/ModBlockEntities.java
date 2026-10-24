package com.supernova.registry;

import com.supernova.SupernovaMod;
import com.supernova.block.ShipBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, SupernovaMod.MODID);

    public static final RegistryObject<BlockEntityType<ShipBlockEntity>> SHIP = BLOCK_ENTITIES.register("ship",
            () -> BlockEntityType.Builder.of(ShipBlockEntity::new, ModBlocks.SHIP.get()).build(null));
}
