package io.github.capsicum0907.acervus.client;

import java.util.ArrayList;
import java.util.List;

import io.github.capsicum0907.acervus.Counts;
import io.github.capsicum0907.acervus.HeapMenu;
import io.github.capsicum0907.acervus.HeapSlot;
import io.github.capsicum0907.acervus.Heaped;
import io.github.capsicum0907.acervus.Pile;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class HeapScreen extends ReadoutPanel<HeapMenu> {
    private static final int ITEM_TINT = 0xFFA8D4E0;

    public HeapScreen(HeapMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected Heaped heap() {
        return new Reading(menu.pile());
    }

    @Override
    protected void drawContents(GuiGraphics graphics, Heaped heap) {
        Pile pile = menu.pile();
        if (!pile.isEmpty()) {
            graphics.renderItem(pile.sample(), leftPos + CONTENT_X, topPos + CONTENT_Y);
        }
    }

    @Override
    protected boolean slotTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        Pile pile = menu.pile();
        if (minecraft == null || pile.isEmpty() || !(hoveredSlot instanceof HeapSlot)) {
            return false;
        }
        List<Component> lines = new ArrayList<>(getTooltipFromItem(minecraft, pile.sample()));
        lines.add(Component.translatable("gui.acervus.exact", Counts.exact(pile.count()))
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("gui.acervus.room", Counts.exact(pile.room()))
                .withStyle(ChatFormatting.DARK_GRAY));
        graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        return true;
    }

    private record Reading(Pile pile) implements Heaped {
        @Override
        public long amount() {
            return pile.count();
        }

        @Override
        public long capacity() {
            return pile.count() + pile.room();
        }

        @Override
        public boolean isEmpty() {
            return pile.isEmpty();
        }

        @Override
        public Component contentName() {
            return pile.isEmpty() ? Component.empty() : pile.sample().getHoverName();
        }

        @Override
        public String brief(long value) {
            return Counts.brief(value);
        }

        @Override
        public String exact(long value) {
            return Counts.exact(value);
        }

        @Override
        public int tint() {
            return ITEM_TINT;
        }

        @Override
        public boolean gives() {
            return pile.gives();
        }
    }
}
