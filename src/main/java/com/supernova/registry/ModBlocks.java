package com.supernova.registry;

import com.supernova.SupernovaMod;
import com.supernova.block.ShipBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SupernovaMod.MODID);

    public static final RegistryObject<Block> LAUNCH_PAD = BLOCKS.register("launch_pad",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4f, 12f)
                    .sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> SHIP = BLOCKS.register("ship",
            () -> new ShipBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4f, 12f)
                    .sound(SoundType.METAL).noOcclusion().requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> GLEBA_STONE = BLOCKS.register("gleba_stone",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(1.8f, 6f)
                    .sound(SoundType.DEEPSLATE).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> GLEBA_SOIL = BLOCKS.register("gleba_soil",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.6f)
                    .sound(SoundType.GRAVEL)));
    public static final RegistryObject<Block> GLEBA_MOSS = BLOCKS.register("gleba_moss",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(0.6f)
                    .sound(SoundType.MOSS)));
    public static final RegistryObject<Block> GLEBA_SAND = BLOCKS.register("gleba_sand",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.5f)
                    .sound(SoundType.SAND)));
    public static final RegistryObject<Block> GLEBA_CRYSTAL = BLOCKS.register("gleba_crystal",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(1.5f)
                    .sound(SoundType.AMETHYST).lightLevel(s -> 12).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> GLEBA_STALK = BLOCKS.register("gleba_stalk",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(1.2f)
                    .sound(SoundType.STEM)));
    public static final RegistryObject<Block> GLEBA_CAP = BLOCKS.register("gleba_cap",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.8f)
                    .sound(SoundType.SHROOMLIGHT).lightLevel(s -> 9)));
}
