package io.github.capsicum0907.acervus.client;

import java.util.function.BiFunction;

import com.mojang.blaze3d.vertex.PoseStack;

import io.github.capsicum0907.acervus.HeldFluidHeap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.fluids.FluidStack;

public class HeapItemRenderer extends BlockEntityWithoutLevelRenderer {
    public record Window(ResourceLocation texture, int tint) {
    }

    private final BiFunction<HolderLookup.Provider, ItemStack, Window> contents;

    public HeapItemRenderer(BiFunction<HolderLookup.Provider, ItemStack, Window> contents) {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.contents = contents;
    }

    public static Window fluid(HolderLookup.Provider registries, ItemStack stack) {
        FluidStack sample = HeldFluidHeap.of(registries, stack).sample();
        if (sample.isEmpty()) {
            return null;
        }
        IClientFluidTypeExtensions look = IClientFluidTypeExtensions.of(sample.getFluid());
        return new Window(look.getStillTexture(sample), 0xFF000000 | look.getTintColor(sample));
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers,
            int light, int overlay) {
        if (!(stack.getItem() instanceof BlockItem block)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getBlockRenderer().renderSingleBlock(block.getBlock().defaultBlockState(), pose, buffers,
                light, overlay, ModelData.EMPTY, null);

        Level level = minecraft.level;
        Window window = level == null ? null : contents.apply(level.registryAccess(), stack);
        if (window == null || window.texture() == null) {
            return;
        }
        TextureAtlasSprite sprite = minecraft.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(window.texture());
        BlockSurface.fillWindows(pose, buffers, null, BlockPos.ZERO, sprite, window.tint(), light);
    }
}
