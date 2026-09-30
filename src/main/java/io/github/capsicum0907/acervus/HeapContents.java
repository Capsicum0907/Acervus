package io.github.capsicum0907.acervus;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

public record HeapContents(ItemStack of) implements TooltipComponent {
}
