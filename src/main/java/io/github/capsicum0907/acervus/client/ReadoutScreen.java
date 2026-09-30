package io.github.capsicum0907.acervus.client;

import java.util.ArrayList;
import java.util.List;

import io.github.capsicum0907.acervus.Acervus;
import io.github.capsicum0907.acervus.AcervusClientConfig;
import io.github.capsicum0907.acervus.BarScale;
import io.github.capsicum0907.acervus.FluidHeapBlockEntity;
import io.github.capsicum0907.acervus.Heaped;
import io.github.capsicum0907.acervus.ReadoutMenu;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * One screen for the fluid, energy and gas heaps.
 *
 * <p>Every abbreviated number can be pointed at for the whole of it, the same as on
 * the item heap — a number that has been shortened should always be able to say what
 * it stands for.
 *
 * <p>Every measurement here has a twin in {@code tools/make_textures.py}.
 */
public class ReadoutScreen extends AbstractContainerScreen<ReadoutMenu> {
    private static final ResourceLocation PANEL =
            ResourceLocation.fromNamespaceAndPath(Acervus.MODID, "textures/gui/readout.png");

    private static final ResourceLocation BOLT =
            ResourceLocation.fromNamespaceAndPath(Acervus.MODID, "textures/gui/energy_icon.png");

    private static final int PANEL_W = 176;
    private static final int PANEL_H = 166;

    private static final int IN_X = 8;
    private static final int OUT_X = 150;
    private static final int SLOT_Y = 38;
    private static final int LABEL_Y = 57;

    private static final int CONTENT_X = 30;
    private static final int CONTENT_Y = 38;
    private static final int CONTENT_SIZE = 16;

    private static final int COLUMN_X = 52;
    private static final int NAME_Y = 31;
    private static final int BAR_X = 52;
    private static final int BAR_Y = 41;
    private static final int BAR_W = 94;
    private static final int BAR_H = 6;
    private static final int BAR_INSET = 1;
    private static final int TICK = 0xFF373737;
    private static final int CAPTION_Y = 50;

    /**
     * On the title line, against the right edge.
     *
     * <p>It is a fact about the window rather than about the contents, and the title
     * line is where the window describes itself.
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
        graphics.blit(PANEL, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        Heaped heap = heap();
        if (!heap.hasKinds()) {
            graphics.blit(BOLT, leftPos + CONTENT_X, topPos + CONTENT_Y, 0, 0,
                    CONTENT_SIZE, CONTENT_SIZE, CONTENT_SIZE, CONTENT_SIZE);
        }
        if (!heap.isEmpty()) {
            drawContents(graphics, heap);
        }

        BarScale scale = AcervusClientConfig.BAR_SCALE.get();
        int left = leftPos + BAR_X + BAR_INSET;
        int top = topPos + BAR_Y + BAR_INSET;
        int bottom = top + BAR_H - 2 * BAR_INSET;
        int filled = barWidth(scale.fraction(heap.amount(), heap.capacity()));
        if (filled > 0) {
            graphics.fill(left, top, left + filled, bottom, heap.tint());
        }
        for (double tick : scale.ticks(heap.capacity())) {
            int x = left + barWidth(tick);
            graphics.fill(x, bottom - 1, x + 1, bottom, TICK);
        }
    }

    /**
     * The contents themselves, in the box beside the name — the same thing the item
     * heap does by putting the item in a slot. A fluid and a gas are not items, so what
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

        drawUnder(graphics, Component.translatable("gui.acervus.flow.in"), IN_X);
        drawUnder(graphics, Component.translatable("gui.acervus.flow.out"), OUT_X);

        Heaped heap = heap();
        // Said whenever it is true, empty or not: a slot that will not give anything
        // back looks broken unless the screen says that is what it is.
        if (!heap.gives()) {
            Component note = Component.translatable("gui.acervus.intake_only");
            graphics.drawString(font, note, NOTE_RIGHT - font.width(note), NOTE_Y, NOTE, false);
        }

        graphics.drawString(font, name(heap), COLUMN_X, NAME_Y, TEXT, false);
        graphics.drawString(font, caption(heap), COLUMN_X, CAPTION_Y, TEXT, false);
    }

    private void drawUnder(GuiGraphics graphics, Component label, int slotX) {
        graphics.drawString(font, label, slotX + CONTENT_SIZE / 2 - font.width(label) / 2, LABEL_Y, TEXT, false);
    }

    private static Component name(Heaped heap) {
        if (heap.isEmpty()) {
            return Component.translatable("gui.acervus.empty");
        }
        return heap.hasKinds() ? heap.contentName() : Component.translatable("gui.acervus.energy");
    }

    private static Component caption(Heaped heap) {
        return Component.translatable("gui.acervus.of", heap.brief(heap.amount()), heap.brief(heap.capacity()));
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        Heaped heap = heap();
        if (!menu.getCarried().isEmpty()) {
            super.renderTooltip(graphics, mouseX, mouseY);
            return;
        }

        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        if (within(x, y, COLUMN_X, CAPTION_Y, font.width(caption(heap)), font.lineHeight)) {
            graphics.renderComponentTooltip(font, List.of(
                    Component.literal(heap.exact(heap.amount())),
                    Component.literal(heap.exact(heap.capacity())).withStyle(ChatFormatting.DARK_GRAY)),
                    mouseX, mouseY);
            return;
        }
        if (!heap.isEmpty() && within(x, y, CONTENT_X, CONTENT_Y, CONTENT_SIZE, CONTENT_SIZE)) {
            List<Component> lines = new ArrayList<>();
            lines.add(name(heap));
            lines.add(Component.literal(heap.exact(heap.amount())).withStyle(ChatFormatting.GRAY));
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            return;
        }
        if (within(x, y, BAR_X, BAR_Y, BAR_W, BAR_H)) {
            BarScale scale = AcervusClientConfig.BAR_SCALE.get();
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal(heap.exact(heap.amount())));
            long amount = heap.amount();
            long capacity = heap.capacity();
            if (scale == BarScale.DECADE && amount > 0L && amount < capacity) {
                lines.add(Component.literal(heap.exact(BarScale.decadeFloor(amount))
                        + " - " + heap.exact(BarScale.decadeCeiling(amount, capacity)))
                        .withStyle(ChatFormatting.GRAY));
            }
            lines.add(Component.literal(heap.exact(capacity)).withStyle(ChatFormatting.DARK_GRAY));
            lines.add(Component.translatable("gui.acervus.scale",
                    Component.translatable("gui.acervus.scale." + scale.name().toLowerCase(java.util.Locale.ROOT)))
                    .withStyle(ChatFormatting.GRAY));
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (int) mouseX - leftPos;
        int y = (int) mouseY - topPos;
        if (button == 0 && menu.getCarried().isEmpty() && within(x, y, BAR_X, BAR_Y, BAR_W, BAR_H)) {
            AcervusClientConfig.BAR_SCALE.set(AcervusClientConfig.BAR_SCALE.get().next());
            AcervusClientConfig.BAR_SCALE.save();
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    static int barWidth(double fraction) {
        int inner = BAR_W - 2 * BAR_INSET;
        if (fraction <= 0.0) {
            return 0;
        }
        return Math.max(1, Math.min(inner, (int) Math.round(inner * fraction)));
    }

    private boolean within(int x, int y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    /** Never null - see {@link Heaped#NONE}, which a heap that has gone answers with. */
    private Heaped heap() {
        return menu.heap();
    }
}
