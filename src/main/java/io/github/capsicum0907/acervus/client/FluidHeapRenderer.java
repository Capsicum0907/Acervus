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
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;

public class FluidHeapRenderer implements BlockEntityRenderer<FluidHeapBlockEntity> {
    private static final float TEXT_SCALE = 0.01F;

    private final Font font;

    public FluidHeapRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(FluidHeapBlockEntity heap, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int packedLight, int packedOverlay) {
        if (!AcervusConfig.SHOWS_CONTENTS.get() || heap.isEmpty()) {
            return;
        }

        if (heap.unreadable()) {
            BlockSurface.fillWindows(pose, buffers, heap.getLevel(), heap.getBlockPos(), Missing.sprite(),
                    0xFFFFFFFF, packedLight);
        } else {
            FluidStack sample = heap.sample();
            IClientFluidTypeExtensions look = IClientFluidTypeExtensions.of(sample.getFluid());
            TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(look.getStillTexture(sample));
            BlockSurface.fillWindows(pose, buffers, heap.getLevel(), heap.getBlockPos(), sprite,
                    0xFF000000 | look.getTintColor(sample), packedLight);
        }

        BlockSurface.onEachSide(pose, heap.getLevel(), heap.getBlockPos(), packedLight,
                (side, light) -> draw(heap.infinite() ? Counts.INFINITE : Counts.buckets(heap.amount()), side, buffers, light));
    }

    private void draw(String text, PoseStack pose, MultiBufferSource buffers, int packedLight) {
        pose.pushPose();
        pose.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);

        Matrix4f matrix = pose.last().pose();
        font.drawInBatch(text, -font.width(text) / 2.0F, -font.lineHeight / 2.0F, 0xFFFFFFFF, false, matrix, buffers,
                Font.DisplayMode.NORMAL, 0, packedLight);
        pose.popPose();
    }

    @Override
    public int getViewDistance() {
        return 48;
    }
}
