package io.github.capsicum0907.acervus.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.github.capsicum0907.acervus.Acervus;
import io.github.capsicum0907.acervus.AcervusClientConfig;
import io.github.capsicum0907.acervus.BarScale;
import io.github.capsicum0907.acervus.Heaped;
import io.github.capsicum0907.acervus.MenuButtons;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public abstract class ReadoutPanel<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {
    private static final ResourceLocation PANEL =
            ResourceLocation.fromNamespaceAndPath(Acervus.MODID, "textures/gui/readout.png");

    private static final int PANEL_W = 176;
    private static final int PANEL_H = 166;
    private static final int INVENTORY_LABEL_Y = 72;

    private static final int IN_X = 8;
    private static final int OUT_X = 150;
    private static final int LABEL_Y = 57;

    protected static final int CONTENT_X = 30;
    protected static final int CONTENT_Y = 38;
    protected static final int CONTENT_SIZE = 16;

    private static final int COLUMN_X = 52;
    private static final int NAME_Y = 31;
    private static final int BAR_X = 52;
    private static final int BAR_Y = 41;
    private static final int BAR_W = 94;
    private static final int BAR_H = 6;
    private static final int BAR_INSET = 1;
    private static final int CAPTION_Y = 50;
    private static final int TICK = 0xFF373737;

    private static final int NOTE_Y = 6;
    private static final int NOTE_RIGHT = 168;
    private static final int LOCK_X = 158;
    private static final int LOCK_Y = 4;
    private static final int LOCK_GAP = 3;

    private static final int TEXT = 0x404040;
    private static final int NOTE = 0x808080;

    protected ReadoutPanel(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
        this.inventoryLabelY = INVENTORY_LABEL_Y;
    }

    private LockToggle lock;

    protected abstract Heaped heap();

    @Override
    protected void init() {
        super.init();
        lock = addRenderableWidget(new LockToggle(leftPos + LOCK_X, topPos + LOCK_Y, this::heap, () -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, MenuButtons.LOCK);
            }
        }));
        lock.refresh();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (lock != null) {
            lock.refresh();
        }
    }

    protected abstract void drawContents(GuiGraphics graphics, Heaped heap);

    protected boolean slotTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        return false;
    }

    protected boolean outSwitches() {
        return false;
    }

    protected void switchOut() {
    }

    private boolean overOut(int x, int y) {
        Component label = Component.translatable("gui.acervus.flow.out");
        int left = OUT_X + CONTENT_SIZE / 2 - font.width(label) / 2;
        return within(x, y, left, LABEL_Y, font.width(label), font.lineHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(PANEL, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        Heaped heap = heap();
        drawContents(graphics, heap);

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

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);

        drawUnder(graphics, Component.translatable("gui.acervus.flow.in"), IN_X);
        drawUnder(graphics, Component.translatable("gui.acervus.flow.out"), OUT_X);

        Heaped heap = heap();
        if (!heap.gives()) {
            Component note = Component.translatable("gui.acervus.intake_only");
            int right = heap.hasKinds() ? LOCK_X - LOCK_GAP : NOTE_RIGHT;
            graphics.drawString(font, note, right - font.width(note), NOTE_Y, NOTE, false);
        }

        graphics.drawString(font, name(heap), COLUMN_X, NAME_Y, TEXT, false);
        graphics.drawString(font, caption(heap), COLUMN_X, CAPTION_Y, TEXT, false);
    }

    private void drawUnder(GuiGraphics graphics, Component label, int slotX) {
        graphics.drawString(font, label, slotX + CONTENT_SIZE / 2 - font.width(label) / 2, LABEL_Y, TEXT, false);
    }

    protected static Component name(Heaped heap) {
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
        if (slotTooltip(graphics, mouseX, mouseY)) {
            return;
        }

        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        if (outSwitches() && overOut(x, y)) {
            graphics.renderTooltip(font, Component.translatable("gui.acervus.switch_form"), mouseX, mouseY);
            return;
        }
        if (within(x, y, COLUMN_X, CAPTION_Y, font.width(caption(heap)), font.lineHeight)) {
            graphics.renderComponentTooltip(font, List.of(
                    Component.literal(heap.exact(heap.amount())),
                    Component.literal(heap.exact(heap.capacity())).withStyle(ChatFormatting.DARK_GRAY)),
                    mouseX, mouseY);
            return;
        }
        if (!heap.isEmpty() && within(x, y, CONTENT_X, CONTENT_Y, CONTENT_SIZE, CONTENT_SIZE)) {
            graphics.renderComponentTooltip(font, List.of(
                    name(heap),
                    Component.literal(heap.exact(heap.amount())).withStyle(ChatFormatting.GRAY)),
                    mouseX, mouseY);
            return;
        }
        if (within(x, y, BAR_X, BAR_Y, BAR_W, BAR_H)) {
            graphics.renderComponentTooltip(font, barLines(heap), mouseX, mouseY);
            return;
        }
        super.renderTooltip(graphics, mouseX, mouseY);
    }

    private static List<Component> barLines(Heaped heap) {
        BarScale scale = AcervusClientConfig.BAR_SCALE.get();
        long amount = heap.amount();
        long capacity = heap.capacity();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(heap.exact(amount)));
        if (scale == BarScale.DECADE && amount > 0L && amount < capacity) {
            lines.add(Component.literal(heap.power(BarScale.decadeFloor(amount))
                    + " - " + heap.power(BarScale.decadeCeiling(amount, capacity)))
                    .withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.literal(heap.exact(capacity)).withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.translatable("gui.acervus.scale",
                Component.translatable("gui.acervus.scale." + scale.name().toLowerCase(Locale.ROOT)))
                .withStyle(ChatFormatting.GRAY));
        return lines;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (int) mouseX - leftPos;
        int y = (int) mouseY - topPos;
        if (button == 0 && menu.getCarried().isEmpty() && outSwitches() && overOut(x, y)) {
            switchOut();
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        if (button == 0 && menu.getCarried().isEmpty() && within(x, y, BAR_X, BAR_Y, BAR_W, BAR_H)) {
            AcervusClientConfig.BAR_SCALE.set(AcervusClientConfig.BAR_SCALE.get().next());
            AcervusClientConfig.BAR_SCALE.save();
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static int barWidth(double fraction) {
        int inner = BAR_W - 2 * BAR_INSET;
        if (fraction <= 0.0) {
            return 0;
        }
        return Math.max(1, Math.min(inner, (int) Math.round(inner * fraction)));
    }

    private static boolean within(int x, int y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
}
