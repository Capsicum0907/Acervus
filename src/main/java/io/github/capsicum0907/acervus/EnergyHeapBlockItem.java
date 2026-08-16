package io.github.capsicum0907.acervus;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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

    /** Sneak and right-click the air to look inside the one you are holding. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return Carried.open(player, hand,
                (id, inventory, viewer) -> ReadoutMenu.inHand(id, inventory, hand));
    }
    /**
     * The contents as a picture rather than as a line of text; see {@link HeapContents}.
     * Offered for anything at all, and the client decides there is nothing to draw when
     * the heap is empty.
     */
    @Override
    public java.util.Optional<net.minecraft.world.inventory.tooltip.TooltipComponent> getTooltipImage(
            ItemStack stack) {
        return java.util.Optional.of(new HeapContents(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);

        CompoundTag tag = stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).copyTag();
        long stored = tag.contains("Amount") ? tag.getLong("Amount") : tag.getLong("Stored");
        if (stored <= 0) {
            lines.add(Component.translatable("gui.acervus.empty").withStyle(ChatFormatting.GRAY));
            return;
        }
    }
}
