package io.github.capsicum0907.acervus.jade;

import io.github.capsicum0907.acervus.Icon;
import io.github.capsicum0907.acervus.client.Icons;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec2;
import snownee.jade.api.ui.Element;

public class IconElement extends Element {
    private static final Vec2 SIZE = new Vec2(Icons.SIZE, Icons.SIZE);

    private final Icon icon;

    public IconElement(Icon icon) {
        this.icon = icon;
    }

    @Override
    public Vec2 getSize() {
        return SIZE;
    }

    @Override
    public void render(GuiGraphics graphics, float x, float y, float maxX, float maxY) {
        Icons.draw(graphics, icon, Math.round(x), Math.round(y));
    }
}
