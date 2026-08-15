package io.github.capsicum0907.acervus.client;

import java.util.ArrayList;
import java.util.List;

import io.github.capsicum0907.acervus.Acervus;
import io.github.capsicum0907.acervus.Counts;
import io.github.capsicum0907.acervus.HeapBlockEntity;
import io.github.capsicum0907.acervus.HeapMenu;
import io.github.capsicum0907.acervus.HeapSlot;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * What a heap looks like from the inside: a slot, and beside it what would not fit
 * in one.
 *
 * <p>Every measurement here has a twin in {@code tools/make_textures.py}, which
 * draws the panel these numbers sit on. Changing one without the other moves the
 * picture off the frame.
 */
public class HeapScreen extends AbstractContainerScreen<HeapMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Acervus.MODID, "textures/gui/heap.png");

    private static final int PANEL_W = 176;
    private static final int PANEL_H = 166;

    private static final int TEXT_X = 42;
    private static final int NAME_Y = 26;
    private static final int COUNT_Y = 38;
    private static final int ROOM_Y = 50;

    private static final int TEXT = 0x404040;

    public HeapScreen(HeapMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);

        HeapBlockEntity heap = heap();
        if (heap == null || heap.isEmpty()) {
            graphics.drawString(font, Component.translatable("block.acervus.heap.empty"),
                    TEXT_X, NAME_Y, TEXT, false);
            return;
        }

        graphics.drawString(font, heap.sample().getHoverName(), TEXT_X, NAME_Y, TEXT, false);
        // Short, because this is read at a glance. The whole number is a hover away.
        graphics.drawString(font, Counts.brief(heap.count()), TEXT_X, COUNT_Y, TEXT, false);
        graphics.drawString(font, Component.translatable("gui.acervus.room", Counts.brief(heap.room())),
                TEXT_X, ROOM_Y, TEXT, false);
    }

    /**
     * Hovering the heap asks for the detail, so the exact count is added to the item's
     * own tooltip rather than replacing it — what is in there is still an item, and
     * everything an item usually says about itself still applies.
     */
    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        HeapBlockEntity heap = heap();
        if (minecraft == null || heap == null || heap.isEmpty()
                || !(hoveredSlot instanceof HeapSlot) || !menu.getCarried().isEmpty()) {
            super.renderTooltip(graphics, mouseX, mouseY);
            return;
        }

        ItemStack sample = heap.sample();
        List<Component> lines = new ArrayList<>(getTooltipFromItem(minecraft, sample));
        lines.add(Component.translatable("gui.acervus.exact", Counts.exact(heap.count()))
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("gui.acervus.room", Counts.exact(heap.room()))
                .withStyle(ChatFormatting.DARK_GRAY));
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
    }

    private HeapBlockEntity heap() {
        return minecraft == null || minecraft.player == null ? null : menu.heap(minecraft.player);
    }
}
