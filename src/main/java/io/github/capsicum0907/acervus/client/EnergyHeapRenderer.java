package io.github.capsicum0907.acervus.client;

import com.mojang.blaze3d.vertex.PoseStack;

import io.github.capsicum0907.acervus.AcervusConfig;
import io.github.capsicum0907.acervus.Counts;
import io.github.capsicum0907.acervus.EnergyHeapBlockEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.joml.Matrix4f;

/**
 * An energy heap has nothing to draw but the number.
 *
 * <p>The other two show a picture of what they hold because there is one. Energy has
 * no picture: any icon would be a decision about what electricity looks like, and
 * the number is the entire content anyway. So it is drawn larger and alone, in the
 * middle of the block, rather than under an invented symbol.
 */
public class EnergyHeapRenderer implements BlockEntityRenderer<EnergyHeapBlockEntity> {
    private static final float TEXT_SCALE = 0.016F;

    private final Font font;

    public EnergyHeapRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(EnergyHeapBlockEntity heap, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int packedLight, int packedOverlay) {
        if (!AcervusConfig.SHOWS_CONTENTS.get() || heap.isEmpty()) {
            return;
        }

        String text = Counts.brief(heap.stored()) + " FE";

        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        pose.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);

        Matrix4f matrix = pose.last().pose();
        font.drawInBatch(text, -font.width(text) / 2.0F, -font.lineHeight / 2.0F, 0xFFFFFFFF, false,
                matrix, buffers, Font.DisplayMode.NORMAL, 0, packedLight);
        pose.popPose();
    }

    @Override
    public int getViewDistance() {
        return 48;
    }
}
