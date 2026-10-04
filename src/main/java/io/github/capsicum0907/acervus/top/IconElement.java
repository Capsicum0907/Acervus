package io.github.capsicum0907.acervus.top;

import io.github.capsicum0907.acervus.Acervus;
import io.github.capsicum0907.acervus.Icon;
import io.github.capsicum0907.acervus.client.Icons;

import mcjty.theoneprobe.api.IElement;
import mcjty.theoneprobe.api.IElementFactory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record IconElement(Icon icon) implements IElement {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Acervus.MODID, "icon");
    private static final int SIZE = 16;

    @Override
    public void render(GuiGraphics graphics, int x, int y) {
        Icons.draw(graphics, icon, x, y);
    }

    @Override
    public int getWidth() {
        return SIZE;
    }

    @Override
    public int getHeight() {
        return SIZE;
    }

    @Override
    public void toBytes(RegistryFriendlyByteBuf buffer) {
        Icon.STREAM_CODEC.encode(buffer, icon);
    }

    @Override
    public ResourceLocation getID() {
        return ID;
    }

    public static final class Factory implements IElementFactory {
        @Override
        public IElement createElement(RegistryFriendlyByteBuf buffer) {
            return new IconElement(Icon.STREAM_CODEC.decode(buffer));
        }

        @Override
        public ResourceLocation getId() {
            return ID;
        }
    }
}
