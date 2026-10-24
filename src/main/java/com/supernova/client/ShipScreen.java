package com.supernova.client;

import com.supernova.menu.ShipMenu;
import com.supernova.network.ModNetwork;
import com.supernova.network.TakeoffPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Panel de control de la nave: ranura de combustible, medidor y boton DESPEGAR. */
public class ShipScreen extends AbstractContainerScreen<ShipMenu> {
    private Button launch;

    public ShipScreen(ShipMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 176;
    }

    @Override
    protected void init() {
        super.init();
        launch = addRenderableWidget(Button.builder(Component.literal("ENCENDER MOTORES"), b -> {
            ModNetwork.CHANNEL.sendToServer(new TakeoffPacket(menu.pos));
            onClose();
        }).bounds(leftPos + 88, topPos + 36, 76, 24).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        launch.active = menu.fuel() >= 50;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        super.render(g, mx, my, pt);
        renderTooltip(g, mx, my);
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        int x = leftPos, y = topPos, w = imageWidth, h = imageHeight;
        g.fill(x, y, x + w, y + h, 0xF00B0B14);
        g.fill(x, y, x + w, y + 1, 0xFFFF4F7A);
        g.fill(x, y + h - 1, x + w, y + h, 0xFF7FE8FF);
        g.fill(x, y, x + 1, y + h, 0xFF7FE8FF);
        g.fill(x + w - 1, y, x + w, y + h, 0xFFFF4F7A);
        for (Slot s : menu.slots) g.fill(x + s.x - 1, y + s.y - 1, x + s.x + 17, y + s.y + 17, 0xFF2A2A3C);
        // medidor de combustible
        int fuel = menu.fuel();
        for (int i = 0; i < 10; i++) {
            boolean on = fuel >= (10 - i) * 10;
            g.fill(x + 56, y + 22 + i * 6, x + 68, y + 22 + i * 6 + 5, on ? 0xFFFF4F7A : 0x50FF4F7A);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mx, int my) {
        ShipHud.text(g, "PANEL DE CONTROL", 8, 7, ShipHud.CYAN, 0.9f, 0);
        ShipHud.text(g, "TANQUE DE DILITIO", 8, 26, ShipHud.CYAN_DIM, 0.6f, 0);
        ShipHud.text(g, menu.fuel() + "%", 54, 85, ShipHud.PINK, 0.9f, 0);
        boolean ok = menu.fuel() >= 50;
        ShipHud.text(g, ok ? "LISTA: ENCIENDE LOS MOTORES" : "REQUIERE 50% DE COMBUSTIBLE", 88, 66, ok ? 0xFF7CFF9A : ShipHud.PINK, 0.55f, 0);
    }
}
