package io.github.capsicum0907.acervus.client;

import java.util.function.Supplier;

import io.github.capsicum0907.acervus.Acervus;
import io.github.capsicum0907.acervus.Heaped;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class LockToggle extends AbstractWidget {
    public static final int SIZE = 10;
    private static final int ICON = 6;

    private static final ResourceLocation LOCK =
            ResourceLocation.fromNamespaceAndPath(Acervus.MODID, "textures/gui/lock.png");

    private static final int LIGHT = 0xFFC6C6C6;
    private static final int FACE = 0xFF8B8B8B;
    private static final int DARK = 0xFF373737;
    private static final int LIT = 0xFFA0A0A0;
    private static final int LOCKED = 0xFFD04A4A;
    private static final int FREE = 0xFF505050;

    private final Supplier<Heaped> heap;
    private final Runnable pressed;
    private Boolean shown;

    public LockToggle(int x, int y, Supplier<Heaped> heap, Runnable pressed) {
        super(x, y, SIZE, SIZE, Component.empty());
        this.heap = heap;
        this.pressed = pressed;
    }

    public void refresh() {
        Heaped current = heap.get();
        visible = current.hasKinds();
        active = current.locked() || current.canLock();
        boolean locked = current.locked();
        if (shown == null || shown != locked) {
            shown = locked;
            setTooltip(Tooltip.create(Component.translatable(locked ? "gui.acervus.locked" : "gui.acervus.free")));
        }
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
        int x = getX();
        int y = getY();
        graphics.fill(x, y, x + width, y + height, isHovered() && active ? LIT : FACE);
        graphics.fill(x, y, x + width, y + 1, LIGHT);
        graphics.fill(x, y, x + 1, y + height, LIGHT);
        graphics.fill(x, y + height - 1, x + width, y + height, DARK);
        graphics.fill(x + width - 1, y, x + width, y + height, DARK);

        int colour = Boolean.TRUE.equals(shown) ? LOCKED : FREE;
        graphics.setColor((colour >> 16 & 0xFF) / 255.0F, (colour >> 8 & 0xFF) / 255.0F, (colour & 0xFF) / 255.0F,
                1.0F);
        int inset = (SIZE - ICON) / 2;
        graphics.blit(LOCK, x + inset, y + inset, 0, 0, ICON, ICON, ICON, ICON);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        pressed.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }
}
