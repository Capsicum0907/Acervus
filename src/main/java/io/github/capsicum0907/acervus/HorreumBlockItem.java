package io.github.capsicum0907.acervus;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;

/**
 * The rack as an item, and the reason it needs its own class is the same as every
 * heap's: it carries what is inside it, so a full one and an empty one look alike.
 *
 * <p>It says how many heaps, not what is in them. Twelve lines of contents would be
 * a tooltip nobody can read past, and each heap says its own when looked at.
 */
public class HorreumBlockItem extends BlockItem {
    public HorreumBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);

        HolderLookup.Provider registries = context.registries();
        if (registries == null) {
            return;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).copyTag();
        NonNullList<ItemStack> heaps =
                NonNullList.withSize(HorreumBlockEntity.SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag.getCompound("Heaps"), heaps, registries);

        int held = 0;
        for (ItemStack heap : heaps) {
            if (!heap.isEmpty()) {
                held++;
            }
        }
        lines.add(held == 0
                ? Component.translatable("gui.acervus.empty").withStyle(ChatFormatting.GRAY)
                : Component.translatable("block.acervus.horreum.holding",
                        held, HorreumBlockEntity.SLOTS).withStyle(ChatFormatting.GRAY));
    }
}
