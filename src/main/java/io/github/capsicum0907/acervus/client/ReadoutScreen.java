package io.github.capsicum0907.acervus.client;

import java.util.ArrayList;
import java.util.List;

import io.github.capsicum0907.acervus.Acervus;
import io.github.capsicum0907.acervus.FluidHeapBlockEntity;
import io.github.capsicum0907.acervus.Heaped;
import io.github.capsicum0907.acervus.ReadoutMenu;
import io.github.capsicum0907.acervus.Vessel;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

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
    private static final ResourceLocation WITH_CONTENTS =
            ResourceLocation.fromNamespaceAndPath(Acervus.MODID, "textures/gui/readout.png");

    /** The same panel without the contents box, for the resource that has no kinds. */
    private static final ResourceLocation PLAIN =
            ResourceLocation.fromNamespaceAndPath(Acervus.MODID, "textures/gui/readout_plain.png");

    private static final int PANEL_W = 176;
    private static final int PANEL_H = 166;

    private static final int BAR_X = 8;
    private static final int BAR_Y = 20;
    private static final int BAR_W = 160;
    private static final int BAR_H = 10;

    /** Where the contents themselves are drawn, in the box on the left. */
    private static final int CONTENT_X = 8;
    private static final int CONTENT_Y = 38;
    private static final int CONTENT_SIZE = 16;

    private static final int NAME_Y = 34;
    private static final int AMOUNT_Y = 46;
    private static final int ROOM_Y = 58;

    /** Clear of the contents box when there is one, and against the edge when there is not. */
    private static final int TEXT_X = 30;
    private static final int TEXT_X_PLAIN = 8;

    /** Under the container slot, saying which way what is in it is going. */
    private static final int FLOW_Y = 56;

    /** Matches the slot position in ReadoutMenu. */
    private static final int VESSEL_X = 150;

    /**
     * On the title line, against the right edge.
     *
     * <p>Not under the readings, where it would have to sit between the last of them
     * and the inventory label with four pixels to spare. It is a fact about the window
     * rather than about the contents, and the title line is where the window describes
     * itself.
     */
    private static final int NOTE_Y = 6;
    private static final int NOTE_RIGHT = 168;

    private static final int TEXT = 0x404040;

    /** Quieter than the readings: it is a standing fact, not a number that changes. */
    private static final int NOTE = 0x808080;

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
        Heaped shown = heap();
        graphics.blit(shown.hasKinds() ? WITH_CONTENTS : PLAIN,
                leftPos, topPos, 0, 0, imageWidth, imageHeight);

        Heaped heap = shown;
        if (heap.isEmpty()) {
            return;
        }
        // Drawn rather than blitted, so the colour can be the resource's own and the
        // texture does not need a variant per heap.
        int filled = (int) (BAR_W * Math.min(1.0, (double) heap.amount() / Math.max(1L, heap.capacity())));
        if (filled > 0) {
            graphics.fill(leftPos + BAR_X, topPos + BAR_Y,
                    leftPos + BAR_X + filled, topPos + BAR_Y + BAR_H, heap.tint());
        }

        drawContents(graphics, heap);
    }

    /**
     * The contents themselves, in the box on the left — the same thing the item heap
     * does by putting the item in a slot. A fluid and a gas are not items, so what
     * stands for them is their own sprite off the block atlas.
     *
     * <p>A chemical carries its icon and its tint on itself. A fluid's are only
     * reachable from the client, so the fluid heap leaves them unanswered and they are
     * looked up here instead — which it can do because the fluid heap, unlike the gas
     * one, exists in every game.
     */
    private void drawContents(GuiGraphics graphics, Heaped heap) {
        ResourceLocation texture = heap.contentTexture();
        int tint = heap.contentTint();

        if (!heap.hasKinds()) {
            return;
        }
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

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);

        drawFlow(graphics);

        Heaped heap = heap();
        int textX = textX();
        // Said whenever it is true, empty or not: a slot that will not give anything
        // back looks broken unless the screen says that is what it is.
        if (!heap.gives()) {
            Component note = Component.translatable("gui.acervus.intake_only");
            graphics.drawString(font, note, NOTE_RIGHT - font.width(note), NOTE_Y, NOTE, false);
        }
        if (heap.isEmpty()) {
            graphics.drawString(font, Component.translatable("gui.acervus.empty"),
                    textX, NAME_Y, TEXT, false);
            return;
        }

        if (!heap.contentName().getString().isEmpty()) {
            graphics.drawString(font, heap.contentName(), textX, NAME_Y, TEXT, false);
        }
        graphics.drawString(font, heap.brief(heap.amount()), textX, AMOUNT_Y, TEXT, false);
        graphics.drawString(font, Component.translatable("gui.acervus.room", heap.brief(heap.room())),
                textX, ROOM_Y, TEXT, false);
    }

    /**
     * Which way what is in the slot is going, written under it.
     *
     * <p>The direction is decided when the container is put in and does not change
     * afterwards, so a player who cannot see it has no way of telling why a tank is
     * filling rather than emptying. Saying it is the difference between a rule and a
     * mystery.
     */
    private void drawFlow(GuiGraphics graphics) {
        Vessel.Flow flow = menu.flow();
        if (flow == Vessel.Flow.NONE) {
            return;
        }
        Component label = Component.translatable(
                flow == Vessel.Flow.IN ? "gui.acervus.flow.in" : "gui.acervus.flow.out");
        graphics.drawString(font, label,
                VESSEL_X + CONTENT_SIZE / 2 - font.width(label) / 2, FLOW_Y, TEXT, false);
    }

    /** The text starts clear of the contents box, when there is one. */
    private int textX() {
        Heaped heap = heap();
        return heap.hasKinds() ? TEXT_X : TEXT_X_PLAIN;
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        Heaped heap = heap();
        if (heap.isEmpty() || !menu.getCarried().isEmpty()) {
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
        if (within(x, y, CONTENT_X, CONTENT_Y, CONTENT_SIZE, CONTENT_SIZE)) {
            // The contents box answers the same question the item heap's slot does:
            // what is this, and how much of it.
            List<Component> lines = new ArrayList<>();
            lines.add(heap.contentName());
            lines.add(Component.literal(heap.exact(heap.amount())).withStyle(ChatFormatting.GRAY));
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            return;
        }
        if (within(x, y, BAR_X, BAR_Y, BAR_W, BAR_H)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal(heap.exact(heap.amount())));
            lines.add(Component.literal(heap.exact(heap.capacity())).withStyle(ChatFormatting.DARK_GRAY));
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    private boolean within(int x, int y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    private boolean over(int x, int y, String line, int top) {
        int textX = textX();
        return x >= textX && x < textX + font.width(line) && y >= top && y < top + font.lineHeight;
    }

    /** Never null - see {@link Heaped#NONE}, which a heap that has gone answers with. */
    private Heaped heap() {
        return menu.heap();
    }
}
