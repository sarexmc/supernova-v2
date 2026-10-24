package com.supernova.world;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import com.supernova.SupernovaMod;
import com.supernova.registry.ModDimensions;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.LoggerChunkProgressListener;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Crea la dimension Gleba POR CODIGO al iniciar el servidor (como hacen los mods de dimensiones dinamicas).
 * No usa datapack ni modifica level.dat: no sale el aviso de "ajustes experimentales" y funciona en mundos ya existentes.
 */
@Mod.EventBusSubscriber(modid = SupernovaMod.MODID)
public class GlebaDimension {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent e) {
        get(e.getServer());
    }

    /** @return el nivel de Gleba (lo crea si todavia no existe) o null si fallo. */
    public static synchronized ServerLevel get(MinecraftServer server) {
        ServerLevel existing = server.getLevel(ModDimensions.GLEBA);
        if (existing != null) return existing;
        try {
            return create(server);
        } catch (Throwable t) {
            LOGGER.error("[Supernova] No se pudo crear la dimension Gleba", t);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static ServerLevel create(MinecraftServer server) throws Exception {
        Object access = null;
        Map<ResourceKey<Level>, ServerLevel> levels = null;
        for (Field f : MinecraftServer.class.getDeclaredFields()) {
            f.setAccessible(true);
            if (LevelStorageSource.LevelStorageAccess.class.isAssignableFrom(f.getType())) {
                access = f.get(server);
            } else if (Map.class.isAssignableFrom(f.getType())) {
                Object v = f.get(server);
                if (v instanceof Map<?, ?> m && m.containsKey(Level.OVERWORLD)) levels = (Map<ResourceKey<Level>, ServerLevel>) v;
            }
        }
        if (access == null || levels == null) throw new IllegalStateException("No se encontraron los campos del servidor");

        RegistryAccess ra = server.registryAccess();
        Holder<DimensionType> type = ra.registryOrThrow(Registries.DIMENSION_TYPE).getHolderOrThrow(BuiltinDimensionTypes.OVERWORLD);
        Holder<Biome> biome = ra.registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.CHERRY_GROVE);
        LevelStem stem = new LevelStem(type, new GlebaChunkGenerator(new FixedBiomeSource(biome)));
        DerivedLevelData data = new DerivedLevelData(server.getWorldData(), server.getWorldData().overworldData());
        long seed = server.overworld().getSeed();
        ChunkProgressListener listener = new LoggerChunkProgressListener(0);

        Constructor<?> best = null;
        for (Constructor<?> c : ServerLevel.class.getDeclaredConstructors())
            if (best == null || c.getParameterCount() > best.getParameterCount()) best = c;
        best.setAccessible(true);
        Class<?>[] pt = best.getParameterTypes();
        Object[] args = new Object[pt.length];
        for (int i = 0; i < pt.length; i++) {
            Class<?> t = pt[i];
            if (t == boolean.class) args[i] = false;
            else if (t == long.class) args[i] = BiomeManager.obfuscateSeed(seed);
            else if (t == Executor.class) args[i] = Util.backgroundExecutor();
            else if (t == MinecraftServer.class) args[i] = server;
            else if (t == LevelStorageSource.LevelStorageAccess.class) args[i] = access;
            else if (t == ServerLevelData.class) args[i] = data;
            else if (t == ResourceKey.class) args[i] = ModDimensions.GLEBA;
            else if (t == LevelStem.class) args[i] = stem;
            else if (t == ChunkProgressListener.class) args[i] = listener;
            else if (t == List.class) args[i] = ImmutableList.of();
            else args[i] = null;
        }
        ServerLevel level = (ServerLevel) best.newInstance(args);
        levels.put(ModDimensions.GLEBA, level);
        MinecraftForge.EVENT_BUS.post(new LevelEvent.Load(level));
        LOGGER.info("[Supernova] Dimension Gleba creada");
        return level;
    }
}
