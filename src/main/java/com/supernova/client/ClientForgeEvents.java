package com.supernova.client;

import com.supernova.SupernovaMod;
import com.supernova.entity.ShipEntity;
import com.supernova.registry.ModDimensions;
import com.supernova.world.GlebaChunkGenerator;
import com.mojang.blaze3d.platform.InputConstants;
import com.supernova.network.HoldPacket;
import com.supernova.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.ViewportEvent;
import org.lwjgl.glfw.GLFW;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SupernovaMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientForgeEvents {
    private static boolean blurOn = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        ClientSceneState.tick();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            blurOn = false;
            return;
        }
        // mantener CTRL para cargar los motores
        if (mc.player != null && mc.screen == null && mc.player.getVehicle() instanceof ShipEntity ship
                && ship.stage() == ShipEntity.Stage.CHARGING) {
            long win = mc.getWindow().getWindow();
            if (InputConstants.isKeyDown(win, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(win, GLFW.GLFW_KEY_RIGHT_CONTROL))
                ModNetwork.CHANNEL.sendToServer(new HoldPacket());
        }
        SceneFx.update(0f);
        boolean want = SceneFx.blur > 0.2f;
        if (want && !blurOn) {
            // desenfoque con el post-shader "blur" de vanilla (efecto de aturdimiento tipo flashbang)
            mc.gameRenderer.loadEffect(new ResourceLocation("shaders/post/blur.json"));
            blurOn = true;
        } else if (!want && blurOn) {
            mc.gameRenderer.shutdownEffect();
            blurOn = false;
        }
    }

    /** Sacudida de camara en las explosiones y en la onda expansiva. */
    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        SceneFx.update((float) e.getPartialTick());
        float s = SceneFx.shake;
        if (s < 0.001f) return;
        float t = mc.level.getGameTime() + (float) e.getPartialTick();
        e.setPitch(e.getPitch() + Mth.sin(t * 1.7f) * s * 1.5f);
        e.setYaw(e.getYaw() + Mth.sin(t * 2.1f + 1f) * s * 1.2f);
        e.setRoll(e.getRoll() + Mth.sin(t * 2.3f) * s * 2f);
    }

    /** Los pasajeros van dentro de la cabina: no se dibujan (asi su skin no queda por fuera). */
    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre e) {
        if (e.getEntity().getVehicle() instanceof ShipEntity) e.setCanceled(true);
    }

    /** Al acelerar (salto temporal) se abre el campo de vision. */
    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov e) {
        SceneFx.update((float) e.getPartialTick());
        if (SceneFx.speed > 0.001f) e.setFOV(e.getFOV() * (1.0 + 0.35 * SceneFx.speed));
    }

    /** Niebla roja (parte 2), rosa (Gleba) o negra (espacio). */
    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor e) {
        SceneFx.update((float) e.getPartialTick());
        if (SceneFx.domeRed > 0) {
            float k = SceneFx.domeRed * 0.85f;
            e.setRed(Mth.lerp(k, e.getRed(), 0.55f));
            e.setGreen(Mth.lerp(k, e.getGreen(), 0.04f));
            e.setBlue(Mth.lerp(k, e.getBlue(), 0.05f));
        }
        if (SceneFx.domeGleba > 0) {
            float k = SceneFx.domeGleba * 0.8f;
            e.setRed(Mth.lerp(k, e.getRed(), 0.80f));
            e.setGreen(Mth.lerp(k, e.getGreen(), 0.36f));
            e.setBlue(Mth.lerp(k, e.getBlue(), 0.46f));
        }
        if (SceneFx.domeBlack > 0) {
            float k = SceneFx.domeBlack;
            e.setRed(Mth.lerp(k, e.getRed(), 0.01f));
            e.setGreen(Mth.lerp(k, e.getGreen(), 0.0f));
            e.setBlue(Mth.lerp(k, e.getBlue(), 0.02f));
        }
    }

    /** Desde la nave en Gleba, esconde el terreno lejano para que se vea el planeta. */
    @SubscribeEvent
    public static void onFog(ViewportEvent.RenderFog e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (!mc.level.dimension().equals(ModDimensions.GLEBA) || !(mc.player.getVehicle() instanceof ShipEntity)) return;
        float alt = (float) (mc.player.getY() - GlebaChunkGenerator.heightAt(0, 0));
        if (alt < 60) return;
        float k = Mth.clamp((alt - 60f) / 140f, 0f, 1f);
        e.setNearPlaneDistance(0f);
        e.setFarPlaneDistance(Mth.lerp(k, e.getFarPlaneDistance(), 170f));
        e.setCanceled(true);
    }

    /** Parte 2: tras la explosion los sonidos suenan "borrosos" (mas graves y bajos). */
    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent e) {
        if (ClientSceneState.scene != 2) return;
        SceneFx.update(0f);
        if (SceneFx.blur < 0.25f) return;
        SoundInstance s = e.getSound();
        if (s == null || s.getLocation().getNamespace().equals(SupernovaMod.MODID)) return;
        e.setSound(new SimpleSoundInstance(s.getLocation(), s.getSource(), s.getVolume() * 0.3f, 0.75f,
                SoundInstance.createUnseededRandom(), s.isLooping(), 0, s.getAttenuation(), s.getX(), s.getY(), s.getZ(), s.isRelative()));
    }
}
