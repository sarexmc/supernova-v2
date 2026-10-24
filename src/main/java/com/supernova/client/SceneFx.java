package com.supernova.client;

import com.supernova.Timeline;
import com.supernova.entity.ShipEntity;
import com.supernova.registry.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/** Calcula, para el fotograma actual, la intensidad de cada efecto visual. Lo leen el cielo, el overlay y los eventos. */
public final class SceneFx {
    public static float gray, white, blur, red, tint, dim, dark, beam, bloom, domeRed, domeBlack, domeGleba, shake;
    public static boolean starOn, vivid, twoRings;
    public static float starR, starWhite, starBreath, ring1, ring2, ringT, ringR;
    public static float earthAlpha, earthGap, glebaSmall, planetAlpha, planetGap, smallEarth;
    public static float tunnel, tunnelFadeOut, speed, entry;

    private SceneFx() {}

    public static float ss(float a, float b, float x) {
        float t = Mth.clamp((x - a) / (b - a), 0f, 1f);
        return t * t * (3 - 2 * t);
    }

    public static float ease(float x) {
        float t = 1f - Mth.clamp(x, 0f, 1f);
        return 1f - t * t * t;
    }

    public static boolean anySky() {
        return domeRed > 0 || domeBlack > 0 || domeGleba > 0 || planetAlpha > 0 || earthAlpha > 0 || glebaSmall > 0 || smallEarth > 0 || starOn || ring1 > 0;
    }

    public static void update(float pt) {
        gray = white = blur = red = tint = dim = dark = beam = bloom = domeRed = domeBlack = domeGleba = shake = 0;
        starOn = vivid = twoRings = false;
        starR = starWhite = starBreath = ring1 = ring2 = ringT = ringR = 0;
        earthAlpha = earthGap = glebaSmall = planetAlpha = planetGap = smallEarth = 0;
        tunnel = tunnelFadeOut = speed = entry = 0;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        boolean gleba = mc.level.dimension().equals(ModDimensions.GLEBA);
        boolean overworld = mc.level.dimension() == Level.OVERWORLD;
        boolean sheltered = !mc.level.canSeeSky(mc.player.blockPosition().above());
        int sc = ClientSceneState.scene;
        float t = ClientSceneState.time(pt);

        if (overworld && sc == 1) part1(t, sheltered);
        else if (overworld && sc == 2) part2(t, sheltered);
        if (gleba) domeGleba = 1f;
        ship(mc, gleba);
    }

    private static void part1(float t, boolean sheltered) {
        float blast = Timeline.P1_BLAST, m60 = Timeline.P1_MSG60;
        if (t < blast) {
            starOn = true;
            float p = Mth.clamp((t - m60) / 1200f, 0f, 1f);
            starR = t < m60 ? Mth.lerp(t / m60, 6f, 8f) : 8f + 24f * p * p;
            starBreath = 0.92f + 0.08f * Mth.sin(t * 0.05f);
            if (t > m60) gray = 0.62f * (float) Math.pow(p, 1.3);
            bloom = ss(blast - 100, blast, t) * (sheltered ? 0f : 1f);   // resplandor alrededor de la estrella, la escena sigue visible
            starWhite = 0.7f * ss(blast - 100, blast, t);
        } else {
            float dt = t - blast;
            float end = Timeline.P1_END - blast;
            float fadeOut = 1f - ss(end - 300, end, dt);
            white = 0.75f * Math.max(0f, 1f - dt / 60f);                  // destello corto
            dim = 0.42f * ss(10, 90, dt) * fadeOut;                       // luego el mundo queda "mas o menos oscuro"
            ring1 = ss(0, 60, dt) * fadeOut;
            ringT = dt;
            ringR = 6f + 52f * ease(dt / 160f);
            beam = ss(60, 140, dt) * fadeOut * (sheltered ? 0.15f : 1f);
            if (dt < 80) shake = 1f - dt / 80f;
            float w1 = Timeline.P1_WAVE - blast;
            if (dt >= w1 && dt < w1 + 100) shake = 1.2f * (1f - (dt - w1) / 100f);
            if (sheltered) dark = 0.8f * ss(120, 220, dt) * fadeOut;
        }
    }

    private static void part2(float t, boolean sheltered) {
        float blast = Timeline.P2_BLAST;
        vivid = true;
        domeRed = ss(0, 100, t);
        if (t < blast) {
            red = 0.30f * ss(0, 100, t);
            starOn = true;
            starR = Mth.lerp(ss(0, blast, t), 16f, 28f) * (1f + 0.08f * Mth.sin(t * 0.075f));
            starBreath = 0.8f + 0.2f * Mth.sin(t * 0.075f + 0.8f);
            starWhite = ss(blast - 300, blast - 20, t);
            bloom = ss(blast - 100, blast, t) * (sheltered ? 0f : 1f);
        } else {
            float dt = t - blast;
            white = 0.5f * Math.max(0f, 1f - dt / 70f);                   // flash suave
            blur = white;
            tint = 0.30f * ss(0, 40, dt);                                 // rojo transparente
            dim = 0.22f * ss(10, 80, dt);
            ring1 = ss(0, 50, dt);
            ring2 = ss(30, 90, dt);
            twoRings = true;
            ringT = dt;
            ringR = 8f + 44f * ease(dt / 140f);
            beam = ss(40, 120, dt);
            if (dt < 100) shake = 1.5f * (1f - dt / 100f);
        }
    }

    private static void ship(Minecraft mc, boolean gleba) {
        Entity v = mc.player.getVehicle();
        if (!(v instanceof ShipEntity ship)) return;
        ShipEntity.Stage st = ship.stage();
        int ticks = ship.stageTicksClient();
        int m = ship.meters();
        switch (st) {
            case CHARGING -> shake = Math.max(shake, 0.10f * ship.charge() / 100f);
            case COUNTDOWN -> shake = Math.max(shake, 0.25f + 0.4f * ticks / 60f);
            case LIFTOFF, ATMOSPHERE -> shake = Math.max(shake, 0.35f);
            case EXIT -> shake = Math.max(shake, 0.2f);
            case TIME_JUMP -> shake = Math.max(shake, 0.9f);
            default -> { }
        }
        if (!gleba) {
            domeBlack = Math.max(domeBlack, Mth.clamp((m - 60) / 400f, 0f, 1f));
            earthAlpha = ss(580, 640, m);
            earthGap = 60f + Math.min(40f, Math.max(0, m - 600) * 0.1f);
            glebaSmall = ss(650, 700, m);
            speed = ss(600, 700, m);
        } else {
            float alt = (float) (ship.getY() - ship.groundY());
            domeBlack = Mth.clamp((alt - 120f) / 300f, 0f, 1f);
            domeGleba = 1f - domeBlack;
            planetAlpha = ss(200, 450, alt);
            planetGap = Mth.lerp(Mth.clamp((alt - 100f) / 900f, 0f, 1f), 6f, 110f);
            smallEarth = ss(300, 450, alt);
            entry = 0.4f * Mth.sin((float) Math.PI * Mth.clamp((520f - alt) / 270f, 0f, 1f));
            if (st == ShipEntity.Stage.APPROACH) speed = 0.5f * ss(0, 40, ticks);
        }
        if (st == ShipEntity.Stage.TIME_JUMP) {
            tunnel = ss(0, 25, ticks);
            speed = 1f;
        }
        if (st == ShipEntity.Stage.STABILIZE) tunnelFadeOut = 1f - ss(0, 40, ticks);
    }
}
