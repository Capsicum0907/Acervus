package io.github.capsicum0907.acervus.client;

import io.github.capsicum0907.acervus.CarriedHeap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;

public class HeapItemDecorator implements IItemDecorator {
    private static final float SCALE = 0.5F;
    private static final int ICON = 16;
    private static final float ABOVE = 100.0F;

    @Override
    public boolean render(GuiGraphics graphics, Font font, ItemStack stack, int x, int y) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return false;
        }
        ItemStack sample = CarriedHeap.of(minecraft.level.registryAccess(), stack).sample();
        if (sample.isEmpty()) {
            return false;
        }
        float inset = ICON * (1.0F - SCALE) / 2.0F;
        graphics.pose().pushPose();
        graphics.pose().translate(x + inset, y + inset, ABOVE);
        graphics.pose().scale(SCALE, SCALE, 1.0F);
        graphics.renderItem(sample, 0, 0);
        graphics.pose().popPose();
        return true;
    }
}
