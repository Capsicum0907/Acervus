package io.github.capsicum0907.acervus.client;

import io.github.capsicum0907.acervus.Acervus;
import io.github.capsicum0907.acervus.HorreumMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * The rack from the inside: twelve slots and the player's own inventory.
 *
 * <p>Deliberately plain. Each heap says what it holds on its own tooltip, so there is
 * nothing this screen could add that would not be a second copy of the same numbers.
 *
 * <p>Every measurement here has a twin in {@code tools/make_textures.py}.
 */
public class HorreumScreen extends AbstractContainerScreen<HorreumMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Acervus.MODID, "textures/gui/horreum.png");

    private static final int PANEL_W = 176;
    private static final int PANEL_H = 166;

    public HorreumScreen(HorreumMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
        this.inventoryLabelY = 72;
    }

    /** See {@link HeapScreen#render}: the screen has to call this itself. */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }
}
