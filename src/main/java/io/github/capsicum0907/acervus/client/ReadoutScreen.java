package io.github.capsicum0907.acervus.client;

import java.util.ArrayList;
import java.util.List;

import io.github.capsicum0907.acervus.Acervus;
import io.github.capsicum0907.acervus.Heaped;
import io.github.capsicum0907.acervus.ReadoutMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * One screen for the fluid, energy and gas heaps.
 *
 * <p>All three are readouts with a slot: a bar that says how full, the name of what
 * is in it, the amount short, and the room left. Every abbreviated number can be
 * pointed at for the whole of it, the same as on the item heap — a number that has
 * been shortened should always be able to say what it stands for.
 *
 * <p>Every measurement here has a twin in {@code tools/make_textures.py}.
 */
public class ReadoutScreen extends AbstractContainerScreen<ReadoutMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Acervus.MODID, "textures/gui/readout.png");

    private static final int PANEL_W = 176;
    private static final int PANEL_H = 166;

    private static final int BAR_X = 8;
    private static final int BAR_Y = 20;
    private static final int BAR_W = 160;
    private static final int BAR_H = 10;

    private static final int NAME_Y = 34;
    private static final int AMOUNT_Y = 46;
    private static final int ROOM_Y = 58;
    private static final int TEXT_X = 8;

    private static final int TEXT = 0x404040;

    public ReadoutScreen(ReadoutMenu menu, Inventory inventory, Component title) {
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

        Heaped heap = heap();
        if (heap == null || heap.isEmpty()) {
            return;
        }
        // Drawn rather than blitted, so the colour can be the resource's own and the
        // texture does not need a variant per heap.
        int filled = (int) (BAR_W * Math.min(1.0, (double) heap.amount() / Math.max(1L, heap.capacity())));
        if (filled > 0) {
            graphics.fill(leftPos + BAR_X, topPos + BAR_Y,
                    leftPos + BAR_X + filled, topPos + BAR_Y + BAR_H, heap.tint());
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);

        Heaped heap = heap();
        if (heap == null || heap.isEmpty()) {
            graphics.drawString(font, Component.translatable("block.acervus.heap.empty"),
                    TEXT_X, NAME_Y, TEXT, false);
            return;
        }

        if (!heap.contentName().getString().isEmpty()) {
            graphics.drawString(font, heap.contentName(), TEXT_X, NAME_Y, TEXT, false);
        }
        graphics.drawString(font, heap.brief(heap.amount()), TEXT_X, AMOUNT_Y, TEXT, false);
        graphics.drawString(font, Component.translatable("gui.acervus.room", heap.brief(heap.room())),
                TEXT_X, ROOM_Y, TEXT, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        Heaped heap = heap();
        if (heap == null || heap.isEmpty() || !menu.getCarried().isEmpty()) {
            super.renderTooltip(graphics, mouseX, mouseY);
            return;
        }

        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        if (over(x, y, heap.brief(heap.amount()), AMOUNT_Y)) {
            graphics.renderTooltip(font, Component.literal(heap.exact(heap.amount())), mouseX, mouseY);
            return;
        }
        Component room = Component.translatable("gui.acervus.room", heap.brief(heap.room()));
        if (over(x, y, room.getString(), ROOM_Y)) {
            graphics.renderTooltip(font, Component.literal(heap.exact(heap.room())), mouseX, mouseY);
            return;
        }
        if (withinBar(x, y)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal(heap.exact(heap.amount())));
            lines.add(Component.literal(heap.exact(heap.capacity())).withStyle(ChatFormatting.DARK_GRAY));
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    private boolean withinBar(int x, int y) {
        return x >= BAR_X && x < BAR_X + BAR_W && y >= BAR_Y && y < BAR_Y + BAR_H;
    }

    private boolean over(int x, int y, String line, int top) {
        return x >= TEXT_X && x < TEXT_X + font.width(line) && y >= top && y < top + font.lineHeight;
    }

    private Heaped heap() {
        return minecraft == null || minecraft.player == null ? null : menu.heap(minecraft.player);
    }
}
