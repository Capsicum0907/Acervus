package io.github.capsicum0907.acervus.client;

import java.util.List;

import io.github.capsicum0907.acervus.HeapContents;
import io.github.capsicum0907.acervus.Readout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import org.joml.Matrix4f;

public class HeapContentsTooltip implements ClientTooltipComponent {
    private static final int GAP = 4;
    private static final int ROW = 18;

    private static final int AMOUNT_COLOUR = 0xFFAAAAAA;
    private static final int NAME_COLOUR = 0xFFFFFFFF;

    private final List<Readout.Line> rows;

    public HeapContentsTooltip(HeapContents contents) {
        this.rows = read(contents.of());
    }

    public static boolean anything(ItemStack stack) {
        return !read(stack).isEmpty();
    }

    private static List<Readout.Line> read(ItemStack stack) {
        Minecraft client = Minecraft.getInstance();
        return client.level == null ? List.of() : Readout.of(stack, client.level.registryAccess());
    }

    @Override
    public int getHeight() {
        return rows.size() * ROW;
    }

    @Override
    public int getWidth(Font font) {
        int widest = 0;
        for (Readout.Line row : rows) {
            widest = Math.max(widest,
                    Icons.SIZE + GAP + font.width(row.name()) + GAP + font.width(row.amount()));
        }
        return widest;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics graphics) {
        int top = y;
        for (Readout.Line row : rows) {
            Icons.draw(graphics, row.icon(), x, top);
            top += ROW;
        }
    }

    @Override
    public void renderText(Font font, int x, int y, Matrix4f matrix,
            MultiBufferSource.BufferSource buffer) {
        int top = y;
        for (Readout.Line row : rows) {
            float baseline = top + (Icons.SIZE - font.lineHeight) / 2.0F + 1;
            int text = x + Icons.SIZE + GAP;
            font.drawInBatch(row.name(), text, baseline, NAME_COLOUR, true,
                    matrix, buffer, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            font.drawInBatch(Component.literal(row.amount()),
                    text + font.width(row.name()) + GAP, baseline, AMOUNT_COLOUR, true,
                    matrix, buffer, Font.DisplayMode.NORMAL, 0, 0xF000F0);
            top += ROW;
        }
    }
}
