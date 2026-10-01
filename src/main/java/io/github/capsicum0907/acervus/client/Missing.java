package io.github.capsicum0907.acervus.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

public final class Missing {
    public static final ResourceLocation LOCATION = MissingTextureAtlasSprite.getLocation();

    private Missing() {
    }

    public static TextureAtlasSprite sprite() {
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(LOCATION);
    }
}
