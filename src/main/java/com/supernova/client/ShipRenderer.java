package com.supernova.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.supernova.entity.ShipEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** Dibuja la nave en vuelo: modelo grande, giros en el espacio y anillo de condensacion en la punta. */
public class ShipRenderer extends EntityRenderer<ShipEntity> {
    public ShipRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0f;
    }

    @Override
    public void render(ShipEntity ship, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        ShipEntity.Stage st = ship.stage();
        float t = ship.stageTicksClient() + pt;
        float rotY = 0, rotX = 0;
        switch (st) {
            case EXIT -> rotY = 90f * SceneFx.ss(650, 700, ship.meters());
            case TIME_JUMP -> { rotY = 90f; rotX = 55f * SceneFx.ss(0, 40, t); }
            case STABILIZE -> { rotY = 90f; rotX = 55f * (1f - SceneFx.ss(0, 60, t)); }
            case APPROACH -> rotY = 90f * (1f - SceneFx.ss(0, 50, t));
            default -> { }
        }
        ps.pushPose();
        float shake = st == ShipEntity.Stage.COUNTDOWN ? 0.03f * Math.min(1f, t / 60f) : (st == ShipEntity.Stage.LIFTOFF ? 0.025f : 0f);
        if (shake > 0) ps.translate((Math.random() - 0.5) * shake, 0, (Math.random() - 0.5) * shake);
        float cy = 2.2f;
        ps.translate(0, cy, 0);
        ps.mulPose(Axis.YP.rotationDegrees(rotY));
        ps.mulPose(Axis.XP.rotationDegrees(rotX));
        ps.translate(0, -cy, 0);

        ps.pushPose();
        ps.scale(ShipEntity.SCALE, ShipEntity.SCALE, ShipEntity.SCALE);
        ps.translate(-0.5, 0, -0.5);
        ShipModelRenderer.render(ps, buf, light, OverlayTexture.NO_OVERLAY);
        ps.popPose();

        // anillo de condensacion en la punta (a partir de 450 m)
        int m = ship.meters();
        if (m >= 450 && (st == ShipEntity.Stage.ATMOSPHERE || st == ShipEntity.Stage.EXIT)) {
            float a = SceneFx.ss(450, 520, m) * (1f - SceneFx.ss(640, 700, m));
            ring(ps, buf, 4.7f, 1.4f + 0.25f * Mth.sin(t * 0.4f), 4.6f + 0.4f * Mth.sin(t * 0.3f), a * 0.55f);
            ring(ps, buf, 4.9f, 0.8f, 3.0f, a * 0.35f);
        }
        ps.popPose();
        super.render(ship, yaw, pt, ps, buf, light);
    }

    private static void ring(PoseStack ps, MultiBufferSource buf, float y, float r0, float r1, float alpha) {
        if (alpha <= 0.01f) return;
        VertexConsumer vc = buf.getBuffer(RenderType.lightning());
        Matrix4f m = ps.last().pose();
        int segs = 40;
        for (int i = 0; i < segs; i++) {
            float a0 = (float) (i * Math.PI * 2 / segs), a1 = (float) ((i + 1) * Math.PI * 2 / segs);
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            for (int side = 0; side < 2; side++) {
                float yy = y + (side == 0 ? 0f : 0.02f);
                vc.vertex(m, c0 * r0, yy, s0 * r0).color(1f, 0.72f, 0.82f, 0f).endVertex();
                vc.vertex(m, c0 * r1, yy, s0 * r1).color(1f, 0.82f, 0.9f, alpha).endVertex();
                vc.vertex(m, c1 * r1, yy, s1 * r1).color(1f, 0.82f, 0.9f, alpha).endVertex();
                vc.vertex(m, c1 * r0, yy, s1 * r0).color(1f, 0.72f, 0.82f, 0f).endVertex();
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(ShipEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
