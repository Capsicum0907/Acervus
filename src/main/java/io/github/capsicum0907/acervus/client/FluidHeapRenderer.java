package io.github.capsicum0907.acervus.client;

import com.mojang.blaze3d.vertex.PoseStack;

import io.github.capsicum0907.acervus.AcervusConfig;
import io.github.capsicum0907.acervus.Counts;
import io.github.capsicum0907.acervus.FluidHeapBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;

/**
 * The fluid heap draws what it holds by drawing that fluid's bucket.
 *
 * <p>A fluid has no picture of its own that is any good at this size — the still
 * texture is a flat colour, and the same flat colour for several fluids. Its bucket
 * is a picture the player already reads as "this liquid", and every fluid worth
 * storing has one.
 */
public class FluidHeapRenderer implements BlockEntityRenderer<FluidHeapBlockEntity> {
    private static final float ITEM_SCALE = 0.5F;
    private static final float TEXT_SCALE = 0.01F;
    private static final float TEXT_DROP = 0.34F;

    private final ItemRenderer items;
    private final Font font;

    public FluidHeapRenderer(BlockEntityRendererProvider.Context context) {
        this.items = context.getItemRenderer();
        this.font = context.getFont();
    }

    @Override
    public void render(FluidHeapBlockEntity heap, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int packedLight, int packedOverlay) {
        if (!AcervusConfig.SHOWS_CONTENTS.get() || heap.isEmpty()) {
            return;
        }

        FluidStack sample = heap.sample();
        ItemStack bucket = new ItemStack(sample.getFluid().getBucket());

        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());

        if (!bucket.isEmpty()) {
            pose.pushPose();
            pose.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
            items.renderStatic(bucket, ItemDisplayContext.GUI, packedLight, OverlayTexture.NO_OVERLAY,
                    pose, buffers, heap.getLevel(), 0);
            pose.popPose();
        }

        // Buckets, not millibuckets: three digits of every number go on a unit nobody
        // counts in.
        draw(Counts.buckets(heap.amount()), pose, buffers, packedLight);
        pose.popPose();
    }

    private void draw(String text, PoseStack pose, MultiBufferSource buffers, int packedLight) {
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
