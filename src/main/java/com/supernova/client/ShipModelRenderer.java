package com.supernova.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.supernova.SupernovaMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.model.data.ModelData;

/** Dibuja el modelo grande de la nave (block/ship.json) tanto para el bloque como para la entidad en vuelo. */
public class ShipModelRenderer {
    public static final ResourceLocation MODEL = new ResourceLocation(SupernovaMod.MODID, "block/ship");

    public static void render(PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(MODEL);
        VertexConsumer vc = buf.getBuffer(RenderType.cutout());
        RandomSource rnd = RandomSource.create(42L);
        for (Direction d : Direction.values())
            for (var q : model.getQuads(null, d, rnd, ModelData.EMPTY, null))
                vc.putBulkData(ps.last(), q, 1f, 1f, 1f, light, overlay);
        for (var q : model.getQuads(null, null, rnd, ModelData.EMPTY, null))
            vc.putBulkData(ps.last(), q, 1f, 1f, 1f, light, overlay);
    }
}
