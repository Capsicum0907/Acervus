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

    /**
     * Sneak and right-click the air to look inside the one you are holding.
     *
     * <p>The air, because right-clicking a block is how a heap is placed and that must
     * keep working; {@code useOn} runs first and only a miss reaches here. Sneaking,
     * because a plain right-click with a heap in hand already means something on the
     * heap in front of you.
     *
     * <p>The screen it opens is the block's own — same slot, same numbers — with one
     * difference the screen states outright: nothing comes out. See {@link CarriedHeap}.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return Carried.open(player, hand, (id, inventory, viewer) -> HeapMenu.inHand(id, inventory, hand));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);

        HolderLookup.Provider registries = context.registries();
        if (registries == null) {
            return;
        }
        // Read through the same class the slot and the screen read through, rather than
        // off the tag here: three readings of one field is three places for the key to
        // be spelt differently, which is what happened to the count once already.
        CarriedHeap heap = CarriedHeap.of(registries, stack);
        long count = heap.count();
        if (heap.isEmpty()) {
            lines.add(Component.translatable("gui.acervus.empty").withStyle(ChatFormatting.GRAY));
            return;
        }

        lines.add(Component.translatable("block.acervus.item_heap.holding",
                        heap.sample().getHoverName(), Component.literal(String.format("%,d", count)))
                .withStyle(ChatFormatting.GRAY));

        // Said only while it is true, because it is a thing the item is quietly doing
        // to items the player expected to end up in a slot. A heap that is full, or one
        // in a game where the setting is off, says nothing.
        if (stack.getCount() == 1 && AcervusConfig.SPEC.isLoaded()
                && AcervusConfig.ABSORBS_WHEN_CARRIED.get() && count < AcervusConfig.CAPACITY.get()) {
            lines.add(Component.translatable("block.acervus.item_heap.absorbing")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
