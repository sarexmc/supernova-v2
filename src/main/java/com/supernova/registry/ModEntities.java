package com.supernova.registry;

import com.supernova.SupernovaMod;
import com.supernova.entity.ShipEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SupernovaMod.MODID);

    public static final RegistryObject<EntityType<ShipEntity>> SHIP = ENTITIES.register("ship",
            () -> EntityType.Builder.<ShipEntity>of(ShipEntity::new, MobCategory.MISC)
                    .sized(2.0f, 3.0f).clientTrackingRange(16).updateInterval(1).fireImmune().build("ship"));
}
