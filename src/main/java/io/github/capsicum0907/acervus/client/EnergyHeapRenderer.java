package io.github.capsicum0907.acervus.client;

import com.mojang.blaze3d.vertex.PoseStack;

import io.github.capsicum0907.acervus.AcervusConfig;
import io.github.capsicum0907.acervus.Counts;
import io.github.capsicum0907.acervus.EnergyHeapBlock;
import io.github.capsicum0907.acervus.EnergyHeapBlockEntity;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

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

        BlockState state = heap.getBlockState();
        if (!state.hasProperty(EnergyHeapBlock.FACING)) {
            return;
        }
        BlockSurface.onSide(pose, heap.getLevel(), heap.getBlockPos(), state.getValue(EnergyHeapBlock.FACING),
                packedLight, (front, light) -> {
                    front.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
                    Matrix4f matrix = front.last().pose();
                    font.drawInBatch(text, -font.width(text) / 2.0F, -font.lineHeight / 2.0F, 0xFFFFFFFF, false,
                            matrix, buffers, Font.DisplayMode.NORMAL, 0, light);
                });
    }

    @Override
    public int getViewDistance() {
        return 48;
    }
}
