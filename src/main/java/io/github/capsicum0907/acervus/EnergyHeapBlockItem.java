package io.github.capsicum0907.acervus;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;

/** The energy heap as an item, saying what it is carrying so that it is not lying. */
public class EnergyHeapBlockItem extends BlockItem {
    public EnergyHeapBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);

        CompoundTag tag = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).copyTag();
        long stored = tag.contains("Amount") ? tag.getLong("Amount") : tag.getLong("Stored");
        if (stored <= 0) {
            lines.add(Component.translatable("block.acervus.heap.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
        lines.add(Component.translatable("block.acervus.energy_heap.holding", Counts.brief(stored))
                .withStyle(ChatFormatting.GRAY));
    }
}
