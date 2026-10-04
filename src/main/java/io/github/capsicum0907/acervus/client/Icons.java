package io.github.capsicum0907.acervus.client;

import io.github.capsicum0907.acervus.Acervus;
import io.github.capsicum0907.acervus.Icon;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

public final class Icons {
    public static final int SIZE = 16;

    private static final ResourceLocation BOLT =
            ResourceLocation.fromNamespaceAndPath(Acervus.MODID, "textures/gui/energy_icon.png");

    private Icons() {
    }

    public static void draw(GuiGraphics graphics, Icon icon, int x, int y) {
        switch (icon.kind()) {
            case ITEM -> graphics.renderItem(icon.item(), x, y);
            case ENERGY -> graphics.blit(BOLT, x, y, 0, 0, SIZE, SIZE, SIZE, SIZE);
            case MISSING -> graphics.blit(x, y, 0, SIZE, SIZE, Missing.sprite());
            case FLUID -> {
                IClientFluidTypeExtensions look = IClientFluidTypeExtensions.of(icon.fluid().getFluid());
                atlas(graphics, look.getStillTexture(icon.fluid()), 0xFF000000 | look.getTintColor(icon.fluid()), x, y);
            }
            case SPRITE -> icon.sprite().ifPresent(sprite -> atlas(graphics, sprite, icon.tint(), x, y));
        }
    }

    private static void atlas(GuiGraphics graphics, ResourceLocation texture, int tint, int x, int y) {
        if (texture == null) {
            return;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(texture);
        graphics.setColor((tint >> 16 & 0xFF) / 255.0F, (tint >> 8 & 0xFF) / 255.0F, (tint & 0xFF) / 255.0F, 1.0F);
        graphics.blit(x, y, 0, SIZE, SIZE, sprite);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
