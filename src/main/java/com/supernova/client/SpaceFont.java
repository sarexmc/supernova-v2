package com.supernova.client;

import com.supernova.SupernovaMod;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

/**
 * Fuente "espacial". Si existe assets/supernova/font/space.json (con tu .ttf) se usa; si no, la fuente normal.
 * Mira el README para anadir una fuente tipo Orbitron.
 */
public class SpaceFont {
    private static final ResourceLocation ID = new ResourceLocation(SupernovaMod.MODID, "space");
    private static Boolean has;

    public static Style style() {
        if (has == null)
            has = Minecraft.getInstance().getResourceManager().getResource(new ResourceLocation(SupernovaMod.MODID, "font/space.json")).isPresent();
        return has ? Style.EMPTY.withFont(ID) : Style.EMPTY;
    }

    public static Component c(String s) {
        return Component.literal(s).withStyle(style());
    }
}
