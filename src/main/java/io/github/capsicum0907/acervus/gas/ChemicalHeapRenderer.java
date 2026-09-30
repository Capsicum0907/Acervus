package io.github.capsicum0907.acervus.gas;

import com.mojang.blaze3d.vertex.PoseStack;

import io.github.capsicum0907.acervus.AcervusConfig;
import io.github.capsicum0907.acervus.Counts;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.joml.Matrix4f;

public class ChemicalHeapRenderer implements BlockEntityRenderer<ChemicalHeapBlockEntity> {
    private static final float TEXT_SCALE = 0.012F;
    private static final float LINE_GAP = 1.2F;

    private final Font font;

    public static io.github.capsicum0907.acervus.client.HeapItemRenderer.Window window(
            net.minecraft.core.HolderLookup.Provider registries, net.minecraft.world.item.ItemStack stack) {
        HeldChemicalHeap heap = HeldChemicalHeap.of(registries, stack);
        return heap.isEmpty() ? null
                : new io.github.capsicum0907.acervus.client.HeapItemRenderer.Window(heap.contentTexture(),
                        heap.contentTint());
    }

    public ChemicalHeapRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(ChemicalHeapBlockEntity heap, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int packedLight, int packedOverlay) {
        if (!AcervusConfig.SHOWS_CONTENTS.get() || heap.isEmpty()) {
            return;
        }

        String name = heap.sample().getChemical().getTextComponent().getString();
        String amount = Counts.brief(heap.amount());

        net.minecraft.resources.ResourceLocation texture = heap.contentTexture();
        if (texture != null) {
            net.minecraft.client.renderer.texture.TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(texture);
            io.github.capsicum0907.acervus.client.BlockSurface.fillWindows(pose, buffers, heap.getLevel(),
                    heap.getBlockPos(), sprite, heap.contentTint(), packedLight);
        }

        io.github.capsicum0907.acervus.client.BlockSurface.onEachSide(pose, heap.getLevel(), heap.getBlockPos(),
                packedLight, (side, light) -> {
                    side.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
                    Matrix4f matrix = side.last().pose();
                    line(name, -font.lineHeight * LINE_GAP, matrix, buffers, light);
                    line(amount, font.lineHeight * 0.2F, matrix, buffers, light);
                });
    }

    private void line(String text, float y, Matrix4f matrix, MultiBufferSource buffers, int packedLight) {
        font.drawInBatch(text, -font.width(text) / 2.0F, y, 0xFFFFFFFF, false, matrix, buffers,
                Font.DisplayMode.NORMAL, 0, packedLight);
    }

    @Override
    public int getViewDistance() {
        return 48;
    }
}
