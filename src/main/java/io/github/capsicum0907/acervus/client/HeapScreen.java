package io.github.capsicum0907.acervus.client;

import java.util.ArrayList;
import java.util.List;

import io.github.capsicum0907.acervus.Acervus;
import io.github.capsicum0907.acervus.Counts;
import io.github.capsicum0907.acervus.HeapMenu;
import io.github.capsicum0907.acervus.HeapSlot;
import io.github.capsicum0907.acervus.Pile;

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

    /**
     * On the title line, against the right edge — the same place the readout screen
     * puts it. It is a fact about the window rather than about the contents.
     */
    private static final int NOTE_Y = 6;
    private static final int NOTE_RIGHT = 168;

    private static final int TEXT = 0x404040;

    /** Quieter than the readings: it is a standing fact, not a number that changes. */
    private static final int NOTE = 0x808080;

    public HeapScreen(HeapMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
        this.inventoryLabelY = 72;
    }

    /**
     * {@code renderTooltip} has to be called from here.
     *
     * <p>{@code AbstractContainerScreen} defines it and never calls it — every
     * concrete screen in the game calls it from its own {@code render}, vanilla's
     * chest included. Leaving it out costs not only anything this screen wanted to
     * say, but the item names on every slot in it, which is how it was noticed.
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);

        Pile heap = heap();
        // Said whenever it is true, empty or not: a slot that will not give anything
        // back looks broken unless the screen says that is what it is.
        if (!heap.gives()) {
            Component note = Component.translatable("gui.acervus.intake_only");
            graphics.drawString(font, note, NOTE_RIGHT - font.width(note), NOTE_Y, NOTE, false);
        }
        if (heap.isEmpty()) {
            graphics.drawString(font, Component.translatable("block.acervus.heap.empty"),
                    TEXT_X, NAME_Y, TEXT, false);
            return;
        }

        graphics.drawString(font, heap.sample().getHoverName(), TEXT_X, NAME_Y, TEXT, false);
        // Short, because these are read at a glance. The whole number is a hover away.
        graphics.drawString(font, held(heap), TEXT_X, COUNT_Y, TEXT, false);
        graphics.drawString(font, free(heap), TEXT_X, ROOM_Y, TEXT, false);
    }

    /**
     * Everything shortened can be asked about by pointing at it: the count, the room
     * left, and the slot. A number that has been abbreviated should always be able to
     * say what it stands for, or the abbreviation is a loss rather than a summary.
     *
     * <p>Over the slot the exact count is <em>added</em> to the item's own tooltip
     * rather than replacing it — what is in there is still an item, and everything an
     * item usually says about itself still applies.
     */
    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        Pile heap = heap();
        if (minecraft == null || heap.isEmpty() || !menu.getCarried().isEmpty()) {
            super.renderTooltip(graphics, mouseX, mouseY);
            return;
        }

        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        if (over(x, y, held(heap), COUNT_Y)) {
            graphics.renderTooltip(font, Component.literal(Counts.exact(heap.count())), mouseX, mouseY);
            return;
        }
        if (over(x, y, free(heap), ROOM_Y)) {
            graphics.renderTooltip(font, Component.literal(Counts.exact(heap.room())), mouseX, mouseY);
            return;
        }
        if (hoveredSlot instanceof HeapSlot) {
            List<Component> lines = new ArrayList<>(getTooltipFromItem(minecraft, heap.sample()));
            lines.add(Component.translatable("gui.acervus.exact", Counts.exact(heap.count()))
                    .withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("gui.acervus.room", Counts.exact(heap.room()))
                    .withStyle(ChatFormatting.DARK_GRAY));
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    private Component held(Pile heap) {
        return Component.literal(Counts.brief(heap.count()));
    }

    private Component free(Pile heap) {
        return Component.translatable("gui.acervus.room", Counts.brief(heap.room()));
    }

    /** Measured from what is drawn, so the reachable area is the visible one. */
    private boolean over(int x, int y, Component line, int top) {
        return x >= TEXT_X && x < TEXT_X + font.width(line)
                && y >= top && y < top + font.lineHeight;
    }

    /** Never null — see {@link Pile#NONE}, which a heap that has gone answers with. */
    private Pile heap() {
        return menu.pile();
    }
}
