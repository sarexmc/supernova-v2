package com.supernova.registry;

import com.supernova.SupernovaMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SupernovaMod.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SupernovaMod.MODID);

    public static final RegistryObject<Item> DILITHIUM_TANK = ITEMS.register("dilithium_tank",
            () -> new Item(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> LAUNCH_PAD = blockItem("launch_pad", ModBlocks.LAUNCH_PAD);
    public static final RegistryObject<Item> SHIP = blockItem("ship", ModBlocks.SHIP);
    public static final RegistryObject<Item> GLEBA_STONE = blockItem("gleba_stone", ModBlocks.GLEBA_STONE);
    public static final RegistryObject<Item> GLEBA_SOIL = blockItem("gleba_soil", ModBlocks.GLEBA_SOIL);
    public static final RegistryObject<Item> GLEBA_MOSS = blockItem("gleba_moss", ModBlocks.GLEBA_MOSS);
    public static final RegistryObject<Item> GLEBA_SAND = blockItem("gleba_sand", ModBlocks.GLEBA_SAND);
    public static final RegistryObject<Item> GLEBA_CRYSTAL = blockItem("gleba_crystal", ModBlocks.GLEBA_CRYSTAL);

    public static final RegistryObject<Item> GLEBA_STALK = blockItem("gleba_stalk", ModBlocks.GLEBA_STALK);
    public static final RegistryObject<Item> GLEBA_CAP = blockItem("gleba_cap", ModBlocks.GLEBA_CAP);

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.supernova"))
                    .icon(() -> new ItemStack(DILITHIUM_TANK.get()))
                    .displayItems((params, out) -> ITEMS.getEntries().forEach(e -> out.accept(e.get())))
                    .build());

    private static RegistryObject<Item> blockItem(String name, RegistryObject<Block> block) {
        return ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }
}
