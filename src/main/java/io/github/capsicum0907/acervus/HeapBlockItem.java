package io.github.capsicum0907.acervus;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class HeapBlockItem extends ContentsBlockItem {
    public HeapBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return Carried.open(player, hand, (id, inventory, viewer) -> HeapMenu.inHand(id, inventory, hand));
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
        CarriedHeap heap = CarriedHeap.of(registries, stack);
        long count = heap.count();
        if (heap.isEmpty()) {
            lines.add(Component.translatable("gui.acervus.empty").withStyle(ChatFormatting.GRAY));
            return;
        }

        if (stack.getCount() == 1 && AcervusConfig.SPEC.isLoaded()
                && AcervusConfig.ABSORBS_WHEN_CARRIED.get() && count < AcervusConfig.CAPACITY.get()) {
            lines.add(Component.translatable("block.acervus.item_heap.absorbing")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
