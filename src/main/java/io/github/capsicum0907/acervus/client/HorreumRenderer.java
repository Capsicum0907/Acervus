package io.github.capsicum0907.acervus.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import io.github.capsicum0907.acervus.Acervus;
import io.github.capsicum0907.acervus.HorreumBlock;
import io.github.capsicum0907.acervus.HorreumBlockEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public class HorreumRenderer implements BlockEntityRenderer<HorreumBlockEntity> {
    private static final ResourceLocation LAMP =
            ResourceLocation.fromNamespaceAndPath(Acervus.MODID, "textures/block/horreum_lamp.png");

    private static final int ACROSS = 3;
    private static final float FACE = 64.0F;
    private static final float START = 10.0F;
    private static final float STEP = 16.0F;
    private static final float SIZE = 12.0F;
    private static final float HALF = 0.5F;

    public HorreumRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(HorreumBlockEntity rack, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int packedLight, int packedOverlay) {
        BlockState state = rack.getBlockState();
        if (!state.hasProperty(HorreumBlock.FACING)) {
            return;
        }
        BlockSurface.onSide(pose, rack.getLevel(), rack.getBlockPos(), state.getValue(HorreumBlock.FACING),
                packedLight, (front, light) -> {
                    VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(LAMP));
                    PoseStack.Pose last = front.last();
                    for (int slot = 0; slot < HorreumBlockEntity.SLOTS; slot++) {
                        if (rack.heap(slot).isEmpty()) {
                            continue;
                        }
                        float left = -HALF + (START + (slot % ACROSS) * STEP) / FACE;
                        float top = HALF - (START + (slot / ACROSS) * STEP) / FACE;
                        float right = left + SIZE / FACE;
                        float bottom = top - SIZE / FACE;
                        corner(consumer, last, left, bottom, 0.0F, 1.0F, light);
                        corner(consumer, last, right, bottom, 1.0F, 1.0F, light);
                        corner(consumer, last, right, top, 1.0F, 0.0F, light);
                        corner(consumer, last, left, top, 0.0F, 0.0F, light);
                    }
                });
    }

    private static void corner(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float u, float v,
            int light) {
        consumer.addVertex(pose, x, y, 0.0F)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, 0.0F, 0.0F, 1.0F);
    }
}
