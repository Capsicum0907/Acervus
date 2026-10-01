package io.github.capsicum0907.acervus.client;

import com.mojang.blaze3d.vertex.PoseStack;

import io.github.capsicum0907.acervus.AcervusConfig;
import io.github.capsicum0907.acervus.Counts;
import io.github.capsicum0907.acervus.HeapBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public class HeapRenderer implements BlockEntityRenderer<HeapBlockEntity> {
    private static final float ITEM_SCALE = 0.5F;

    private static final float TEXT_SCALE = 0.01F;

    private static final float TEXT_DROP = 0.34F;

    private final ItemRenderer items;
    private final Font font;

    public HeapRenderer(BlockEntityRendererProvider.Context context) {
        this.items = context.getItemRenderer();
        this.font = context.getFont();
    }

    @Override
    public void render(HeapBlockEntity heap, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int packedLight, int packedOverlay) {
        if (!AcervusConfig.SHOWS_CONTENTS.get() || heap.isEmpty()) {
            return;
        }

        if (heap.unreadable()) {
            BlockSurface.fillWindows(pose, buffers, heap.getLevel(), heap.getBlockPos(), Missing.sprite(),
                    0xFFFFFFFF, packedLight);
            BlockSurface.onEachSide(pose, heap.getLevel(), heap.getBlockPos(), packedLight,
                    (side, light) -> drawCount(heap.count(), side, buffers, light));
            return;
        }

        ItemStack sample = heap.sample();
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());

        pose.pushPose();
        pose.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
        items.renderStatic(sample, ItemDisplayContext.GUI, packedLight, OverlayTexture.NO_OVERLAY,
                pose, buffers, heap.getLevel(), 0);
        pose.popPose();
        pose.popPose();

        BlockSurface.onEachSide(pose, heap.getLevel(), heap.getBlockPos(), packedLight,
                (side, light) -> drawCount(heap.count(), side, buffers, light));
    }

    private void drawCount(long count, PoseStack pose, MultiBufferSource buffers, int packedLight) {
        String text = Counts.brief(count);

        pose.pushPose();
        pose.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
        pose.translate(0.0F, TEXT_DROP / TEXT_SCALE, 0.0F);

        Matrix4f matrix = pose.last().pose();
        font.drawInBatch(text, -font.width(text) / 2.0F, 0.0F, 0xFFFFFFFF, false, matrix, buffers,
                Font.DisplayMode.NORMAL, 0, packedLight);
        pose.popPose();
    }

    @Override
    public int getViewDistance() {
        return 48;
    }
}
