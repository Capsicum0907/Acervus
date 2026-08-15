package io.github.capsicum0907.acervus.client;

import com.mojang.blaze3d.vertex.PoseStack;

import io.github.capsicum0907.acervus.AcervusConfig;
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

/**
 * What makes a heap worth looking at: it shows what it holds, and how many.
 *
 * <p>Both are drawn facing the camera rather than fixed to a face. A block with
 * four faces to decorate would mean drawing the contents four times, and a block
 * with a chosen front would mean the answer depends on which way it was placed.
 * Turning the drawing toward whoever is looking costs one rotation and is right
 * from everywhere.
 */
public class HeapRenderer implements BlockEntityRenderer<HeapBlockEntity> {
    /** Big enough to read from across a room, small enough to sit inside the block. */
    private static final float ITEM_SCALE = 0.5F;

    /** Text is authored at sixteen pixels; this brings it down to world scale. */
    private static final float TEXT_SCALE = 0.01F;

    /** Below the item, clear of it at the scale above. */
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

        ItemStack sample = heap.sample();
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());

        pose.pushPose();
        pose.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
        // GUI, not GROUND: the contents should read as a picture of the item rather
        // than as an item lying on a surface inside a box.
        items.renderStatic(sample, ItemDisplayContext.GUI, packedLight, OverlayTexture.NO_OVERLAY,
                pose, buffers, heap.getLevel(), 0);
        pose.popPose();

        drawCount(heap.count(), pose, buffers, packedLight);
        pose.popPose();
    }

    private void drawCount(long count, PoseStack pose, MultiBufferSource buffers, int packedLight) {
        // Grouped, because the difference between 2000000 and 20000000 is not
        // something anybody should have to count zeros to see.
        String text = String.format("%,d", count);

        pose.pushPose();
        // Negative Y: the font draws downward, and the pose is already turned to face
        // the camera, which leaves its Y axis pointing the other way.
        pose.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
        pose.translate(0.0F, TEXT_DROP / TEXT_SCALE, 0.0F);

        Matrix4f matrix = pose.last().pose();
        font.drawInBatch(text, -font.width(text) / 2.0F, 0.0F, 0xFFFFFFFF, false, matrix, buffers,
                Font.DisplayMode.NORMAL, 0, packedLight);
        pose.popPose();
    }

    /** Contents are the reason to look at one, so they should be visible from further than a sign. */
    @Override
    public int getViewDistance() {
        return 48;
    }
}
