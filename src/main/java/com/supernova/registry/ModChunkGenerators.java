package com.supernova.registry;

import com.mojang.serialization.Codec;
import com.supernova.SupernovaMod;
import com.supernova.world.GlebaChunkGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.registries.DeferredRegister;

public class ModChunkGenerators {
    public static final DeferredRegister<Codec<? extends ChunkGenerator>> GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, SupernovaMod.MODID);

    static {
        GENERATORS.register("gleba", () -> GlebaChunkGenerator.CODEC);
    }
}
