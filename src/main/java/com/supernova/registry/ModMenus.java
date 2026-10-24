package com.supernova.registry;

import com.supernova.SupernovaMod;
import com.supernova.menu.ShipMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, SupernovaMod.MODID);

    public static final RegistryObject<MenuType<ShipMenu>> SHIP = MENUS.register("ship",
            () -> IForgeMenuType.create(ShipMenu::new));
}
