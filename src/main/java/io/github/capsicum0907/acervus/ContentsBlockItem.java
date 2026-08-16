package io.github.capsicum0907.acervus;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

/**
 * Every item in this mod: one that carries, on the item, what its block was holding.
 *
 * <p><b>Why they share a class at all.</b> Components belong to the {@code ItemStack},
 * not to each item in it, so two of these in one stack are not two containers — they
 * are one set of contents with a count of two beside it. Filling that stack fills
 * "both"; splitting it copies what was inside; and <em>placing</em> from it copies the
 * contents onto every block placed, without anything being written at all. That last
 * one is the reason a rule about opening screens could not have been enough: placement
 * duplicates in complete silence.
 *
 * <p><b>The rule: it stacks only while it is empty.</b> Not "it never stacks" — sixty
 * four empty heaps are sixty four empty heaps, and carrying them is the whole
 * convenience. The moment one holds something it is alone, and it goes back to
 * stacking when it is emptied again.
 *
 * <p>Derived, never stamped. {@code getMaxStackSize} <em>asks</em> whether the item is
 * carrying anything rather than having an answer written into it by whoever last wrote
 * the contents. There are four places that write contents — the two screens, the block
 * being broken, and middle-click — and a fifth would be added one day without the
 * stamping being noticed. A question cannot be forgotten the way a statement can.
 *
 * <p>The one question is {@code BLOCK_ENTITY_DATA}, which every one of these uses and
 * which is <em>removed</em> rather than emptied when the last of the contents goes —
 * see {@link Held#write}. So "is it carrying anything" and "does it stack" are the same
 * question asked twice, which is why they cannot disagree.
 */
public abstract class ContentsBlockItem extends BlockItem {
    protected ContentsBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    /** Whether this item has anything in it, asked of the item and nothing else. */
    public static boolean holdsSomething(ItemStack stack) {
        return stack.has(DataComponents.BLOCK_ENTITY_DATA);
    }

    /**
     * Whether this is one of these rather than a pile of them.
     *
     * <p>Anything that would put contents <em>into</em> an item asks this first, because
     * a stack cannot be filled without filling all of it. {@link Carried#absorb} and
     * {@link Carried#open} are the two ways in.
     */
    public static boolean alone(ItemStack stack) {
        return stack.getCount() == 1;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return holdsSomething(stack) ? 1 : super.getMaxStackSize(stack);
    }

    /**
     * A stack of these that is somehow carrying something will not be placed.
     *
     * <p>Nothing in normal play can reach this: a stack cannot be filled, and a filled
     * one cannot be stacked. It is here for the case the rule is broken from outside —
     * a command that names both the contents and a count, another mod's inventory code
     * merging without asking {@code getMaxStackSize} — because placing such a stack is
     * where the duplication would actually happen, one full block at a time.
     *
     * <p>Refused out loud rather than quietly placed as an empty one. Losing the
     * contents without saying so would look exactly like the bug this prevents.
     */
    @Override
    public InteractionResult place(BlockPlaceContext context) {
        ItemStack stack = context.getItemInHand();
        if (holdsSomething(stack) && !alone(stack)) {
            Player player = context.getPlayer();
            if (player != null) {
                player.displayClientMessage(Component.translatable("acervus.stacked.place"), true);
            }
            return InteractionResult.FAIL;
        }
        return super.place(context);
    }
}
