package io.github.capsicum0907.acervus.client;

import io.github.capsicum0907.acervus.Acervus;
import io.github.capsicum0907.acervus.HeapBlockEntity;
import io.github.capsicum0907.acervus.HeapMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * What a heap looks like from the inside.
 *
 * <p>Every measurement here has a twin in {@code tools/make_textures.py}, which draws
 * the panel these numbers sit on. Changing one without the other moves the picture
 * off the frame.
 */
public class HeapScreen extends AbstractContainerScreen<HeapMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Acervus.MODID, "textures/gui/heap.png");

    private static final int PANEL_W = 176;
    private static final int PANEL_H = 184;

    private static final int WELL_X = 7;
    private static final int WELL_Y = 18;

    private static final int BUTTON_Y = 63;
    private static final int BUTTON_W = 78;
    private static final int BUTTON_H = 20;

    private static final int TEXT = 0x404040;

    public HeapScreen(HeapMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
        this.inventoryLabelY = 90;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.acervus.take_stack"),
                        button -> press(HeapMenu.TAKE_STACK))
                .bounds(leftPos + 8, topPos + BUTTON_Y, BUTTON_W, BUTTON_H).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.acervus.take_one"),
                        button -> press(HeapMenu.TAKE_ONE))
                .bounds(leftPos + 90, topPos + BUTTON_Y, BUTTON_W, BUTTON_H).build());
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);

        HeapBlockEntity heap = minecraft == null ? null : menu.heap(minecraft.player);
        if (heap == null || heap.isEmpty()) {
            graphics.drawString(font, Component.translatable("block.acervus.heap.empty"),
                    WELL_X + 7, WELL_Y + 16, TEXT, false);
            return;
        }

        ItemStack sample = heap.sample();
        graphics.renderItem(sample, WELL_X + 7, WELL_Y + 12);
        graphics.drawString(font, sample.getHoverName(), WELL_X + 31, WELL_Y + 8, TEXT, false);
        // Grouped, so nobody has to count zeros to tell a million from ten million.
        graphics.drawString(font, String.format("%,d", heap.count()), WELL_X + 31, WELL_Y + 20, TEXT, false);
        graphics.drawString(font, Component.translatable("gui.acervus.room",
                        String.format("%,d", heap.room())),
                WELL_X + 31, WELL_Y + 31, TEXT, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
