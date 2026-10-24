package com.supernova.client;

import com.supernova.entity.ShipEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.joml.Vector3f;

/** Capa de pantalla: gris, resplandor, destello, filtro rojo, oscuridad, rayo que cruza, tunel, lineas de velocidad y HUD. */
public class FxOverlay {
    public static void render(ForgeGui gui, GuiGraphics g, float pt, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        SceneFx.update(pt);
        float anim = mc.level.getGameTime() + pt;

        if (SceneFx.red > 0) {
            Gfx2D.fill(g, 0, 0, w, h, 170, 0, 0, SceneFx.red);
            g.fillGradient(0, h / 2, w, h, Gfx2D.argb(0f, 60, 0, 0), Gfx2D.argb(SceneFx.red * 1.2f, 50, 0, 0));
        }
        if (SceneFx.gray > 0) Gfx2D.fill(g, 0, 0, w, h, 115, 115, 120, SceneFx.gray);
        if (SceneFx.bloom > 0) bloom(g, mc, w, h, SceneFx.bloom);
        if (SceneFx.beam > 0) sweep(g, w, h, ClientSceneState.time(pt), SceneFx.beam, SceneFx.vivid);
        if (SceneFx.dim > 0) Gfx2D.fill(g, 0, 0, w, h, 0, 0, 4, SceneFx.dim);
        if (SceneFx.tint > 0) {
            Gfx2D.fill(g, 0, 0, w, h, 130, 0, 10, SceneFx.tint);
            g.fillGradient(0, 0, w, h / 2, Gfx2D.argb(SceneFx.tint * 0.8f, 40, 0, 0), Gfx2D.argb(0f, 40, 0, 0));
        }
        if (SceneFx.dark > 0) Gfx2D.fill(g, 0, 0, w, h, 0, 0, 6, SceneFx.dark);
        if (SceneFx.entry > 0) Gfx2D.fill(g, 0, 0, w, h, 255, 90, 90, SceneFx.entry);

        if (SceneFx.tunnel > 0) tunnel(g, w, h, anim, SceneFx.tunnel);
        else if (SceneFx.speed > 0) Gfx2D.streaks(g, w / 2f, h / 2f, anim, Math.max(w, h) * 0.7f, 70, SceneFx.speed * 0.7f);
        if (mc.player.getVehicle() instanceof ShipEntity ship) ShipHud.render(g, w, h, pt, ship);
        if (SceneFx.tunnelFadeOut > 0) Gfx2D.fill(g, 0, 0, w, h, 0, 0, 0, SceneFx.tunnelFadeOut);

        if (SceneFx.white > 0) Gfx2D.fill(g, 0, 0, w, h, 255, 255, 255, SceneFx.white);
    }

    /** Posicion en pantalla de la estrella, o null si no esta a la vista. */
    private static float[] starScreen(Minecraft mc, int w, int h) {
        Camera cam = mc.gameRenderer.getMainCamera();
        if (!cam.isInitialized()) return null;
        Vector3f look = cam.getLookVector(), up = cam.getUpVector(), left = cam.getLeftVector();
        Vec3 d = SkyRenderer.STAR_DIR;
        float z = (float) (d.x * look.x() + d.y * look.y() + d.z * look.z());
        if (z <= 0.05f) return null;
        float xl = (float) (d.x * left.x() + d.y * left.y() + d.z * left.z());
        float yu = (float) (d.x * up.x() + d.y * up.y() + d.z * up.z());
        float th = (float) Math.tan(Math.toRadians(mc.options.fov().get()) / 2.0);
        float aspect = (float) w / h;
        float ndcX = -xl / (z * th * aspect), ndcY = yu / (z * th);
        return new float[]{(ndcX * 0.5f + 0.5f) * w, (0.5f - ndcY * 0.5f) * h};
    }

    /** Resplandor blanco alrededor de la estrella justo antes de explotar (la escena sigue visible). */
    private static void bloom(GuiGraphics g, Minecraft mc, int w, int h, float k) {
        Gfx2D.fill(g, 0, 0, w, h, 235, 225, 255, 0.10f * k);
        float[] p = starScreen(mc, w, h);
        if (p == null) return;
        Gfx2D.radial(g, p[0], p[1], h * 0.75f * (0.5f + 0.5f * k), 1f, 0.95f, 1f, 0.55f * k);
        Gfx2D.radial(g, p[0], p[1], h * 0.22f, 1f, 1f, 1f, 0.85f * k);
        int ly = (int) p[1];
        Gfx2D.fill(g, 0, ly, w, ly + 1, 235, 235, 255, 0.55f * k);
        Gfx2D.fill(g, 0, ly - 2, w, ly + 3, 200, 200, 255, 0.10f * k);
    }

    /** El rayo blanco que se mueve de izquierda a derecha. */
    private static void sweep(GuiGraphics g, int w, int h, float t, float alpha, boolean vivid) {
        float speed = vivid ? 0.012f : 0.007f;
        float frac = (t * speed) % 1.4f - 0.2f;
        int x = (int) (frac * w);
        int ly = (int) (h * 0.20f);
        Gfx2D.fill(g, 0, ly, w, ly + 1, 235, 235, 255, alpha * 0.55f);
        Gfx2D.fill(g, 0, ly - 1, w, ly + 2, 200, 200, 255, alpha * 0.12f);
        for (int k = 0; k < 8; k++) {
            int bw = 2 + k * 6;
            Gfx2D.fill(g, x - bw, 0, x + bw, h, 220, 225, 255, alpha * 0.14f * (1 - k / 8f) * (vivid ? 1.3f : 1f));
        }
        for (int k = 0; k < 6; k++)
            Gfx2D.fill(g, x - 20 - k * 12, ly - 1 - k, x + 20 + k * 12, ly + 2 + k, 255, 255, 255, alpha * 0.14f);
    }

    /** Salto temporal: aros neon cian/morados y rayas de estrellas sobre fondo negro. */
    private static void tunnel(GuiGraphics g, int w, int h, float anim, float a) {
        Gfx2D.fill(g, 0, 0, w, h, 0, 0, 0, a);
        float cx = w / 2f, cy = h / 2f, maxR = Math.min(w, h) * 0.55f;
        Gfx2D.streaks(g, cx, cy, anim, Math.max(w, h) * 0.7f, 90, a);
        float[] cyan = {0.25f, 0.9f, 1f}, purple = {0.7f, 0.4f, 1f};
        for (int k = 0; k < 7; k++) {
            float phase = (anim * 0.012f + k / 7f) % 1f;
            float r = 18 + (float) Math.pow(phase, 1.6) * maxR;
            float thick = 3 + phase * 9;
            Gfx2D.ring(g, cx, cy, r, r + thick, 64, cyan, purple, a * Mth.sin((float) Math.PI * phase));
        }
        ShipHud.text(g, "SALTO TEMPORAL · EN CURSO", cx, h * 0.86f, 0xFFFF4F7A, 0.9f, 1);
    }
}
