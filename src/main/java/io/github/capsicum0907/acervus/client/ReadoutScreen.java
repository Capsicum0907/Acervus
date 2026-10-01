package io.github.capsicum0907.acervus.client;

import io.github.capsicum0907.acervus.Acervus;
import io.github.capsicum0907.acervus.FluidHeapBlockEntity;
import io.github.capsicum0907.acervus.Heaped;
import io.github.capsicum0907.acervus.ReadoutMenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

public class ReadoutScreen extends ReadoutPanel<ReadoutMenu> {
    private static final ResourceLocation BOLT =
            ResourceLocation.fromNamespaceAndPath(Acervus.MODID, "textures/gui/energy_icon.png");

    public ReadoutScreen(ReadoutMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected Heaped heap() {
        return menu.heap();
    }

    @Override
    protected void drawContents(GuiGraphics graphics, Heaped heap) {
        if (!heap.hasKinds()) {
            graphics.blit(BOLT, leftPos + CONTENT_X, topPos + CONTENT_Y, 0, 0,
                    CONTENT_SIZE, CONTENT_SIZE, CONTENT_SIZE, CONTENT_SIZE);
            return;
        }
        if (heap.unreadable()) {
            graphics.blit(leftPos + CONTENT_X, topPos + CONTENT_Y, 0, CONTENT_SIZE, CONTENT_SIZE, Missing.sprite());
            return;
        }
        if (heap.isEmpty()) {
            return;
        }
        ResourceLocation texture = heap.contentTexture();
        int tint = heap.contentTint();
        if (heap instanceof FluidHeapBlockEntity fluid) {
            FluidStack held = fluid.sample();
            if (held.isEmpty()) {
                return;
            }
            IClientFluidTypeExtensions look = IClientFluidTypeExtensions.of(held.getFluid());
            texture = look.getStillTexture(held);
            tint = 0xFF000000 | look.getTintColor(held);
        }
        if (texture == null) {
            return;
        }

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
        graphics.setColor(
                (tint >> 16 & 0xFF) / 255.0F,
                (tint >> 8 & 0xFF) / 255.0F,
                (tint & 0xFF) / 255.0F,
                1.0F);
        graphics.blit(leftPos + CONTENT_X, topPos + CONTENT_Y, 0, CONTENT_SIZE, CONTENT_SIZE, sprite);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
