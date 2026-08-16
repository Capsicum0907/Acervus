package io.github.capsicum0907.acervus;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

/**
 * "Draw what is in this" — a heap, or a rack of them.
 *
 * <p>A line of text says everything a picture does, and says it slower. A rack of
 * twelve heaps says it twelve times, at which point nobody reads any of it. So each
 * kind of contents gets its own small icon beside its name and its amount, and a rack
 * becomes a list that can be taken in without opening it.
 *
 * <p><b>It carries the item itself and nothing worked out from it.</b> That is not
 * laziness: {@code Item#getTooltipImage} is handed a stack and nothing else, and
 * reading a heap needs the registries — which are reachable from the client, where
 * tooltips are drawn, and not from here. So the reading happens in
 * {@link io.github.capsicum0907.acervus.client.HeapContentsTooltip} along with the
 * drawing, and this stays the one thing it is safe for a class in every game to know:
 * which stack is being asked about.
 */
public record HeapContents(ItemStack of) implements TooltipComponent {
}
