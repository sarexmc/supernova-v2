package com.supernova.registry;

import com.supernova.SupernovaMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

public class ModDimensions {
    public static final ResourceKey<Level> GLEBA = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation(SupernovaMod.MODID, "gleba"));
}
