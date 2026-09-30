package io.github.capsicum0907.acervus;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class HorreumBlockItem extends ContentsBlockItem {
    public HorreumBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return Carried.open(player, hand,
                (id, inventory, viewer) -> HorreumMenu.inHand(id, inventory, hand));
    }

    @Override
    public java.util.Optional<net.minecraft.world.inventory.tooltip.TooltipComponent> getTooltipImage(
            ItemStack stack) {
        return java.util.Optional.of(new HeapContents(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);

        HolderLookup.Provider registries = context.registries();
        if (registries == null) {
            return;
        }
        NonNullList<ItemStack> heaps = HorreumBlockEntity.readHeaps(stack, registries);

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
