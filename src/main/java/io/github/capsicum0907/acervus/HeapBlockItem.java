package io.github.capsicum0907.acervus;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/**
 * The heap as an item.
 *
 * <p>Its whole reason to exist is the tooltip. A heap carries its contents when it
 * is picked up, so one sitting in an inventory can be holding two billion of
 * something and look exactly like an empty one. Saying what is inside is not a
 * nicety here; without it the item lies.
 */
public class HeapBlockItem extends BlockItem {
    public HeapBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);

        HolderLookup.Provider registries = context.registries();
        CompoundTag tag = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        if (registries == null || !tag.contains("Sample")) {
            lines.add(Component.translatable("block.acervus.heap.empty").withStyle(ChatFormatting.GRAY));
            return;
        }

        ItemStack sample = ItemStack.parse(registries, tag.getCompound("Sample")).orElse(ItemStack.EMPTY);
        int count = tag.getInt("Count");
        if (sample.isEmpty() || count <= 0) {
            lines.add(Component.translatable("block.acervus.heap.empty").withStyle(ChatFormatting.GRAY));
            return;
        }

        lines.add(Component.translatable("block.acervus.heap.holding",
                        sample.getHoverName(), Component.literal(String.format("%,d", count)))
                .withStyle(ChatFormatting.GRAY));
    }
}
