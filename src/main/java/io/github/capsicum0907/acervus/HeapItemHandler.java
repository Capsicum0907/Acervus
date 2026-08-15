package io.github.capsicum0907.acervus;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The window a heap shows to hoppers, pipes and anything else that moves items.
 *
 * <p><b>Two slots, and they are not symmetric.</b> One is what a heap gives out and
 * the other is what it takes in, because the two questions have different honest
 * answers.
 *
 * <ul>
 * <li><b>Taking out</b> must never show a count past a stack. Code on the far side
 *     of this boundary was written for sixty-four — vanilla's and every mod's alike
 *     — and hands anything larger to something that rounds it down. That is where
 *     blocks of this kind lose items.
 * <li><b>Putting in</b> must show the real room left. A great many pipes work out
 *     how much fits as {@code limit - count} rather than by asking, and a slot that
 *     answered "sixty-four, and sixty-four are already there" would look full to
 *     them while holding a thousand. The room is reported on a slot that is always
 *     empty, so a large number is never attached to an item stack.
 * </ul>
 *
 * <p><b>Nothing is remembered here.</b> Every method reads the block entity when it
 * is called. A handler that cached even one field would be a second copy of the
 * truth, and two of them — one per side, say — would be two copies that disagree.
 * That is the shape most duplication bugs in storage blocks actually have.
 *
 * <p>Both slots face every side. A heap has no front.
 */
public class HeapItemHandler implements IItemHandler {
    /** What a heap gives out: at most one stack of what is inside. */
    public static final int TAKE = 0;

    /** What a heap takes in: always shown empty, and reporting the room that is left. */
    public static final int PUT = 1;

    private final HeapBlockEntity heap;

    public HeapItemHandler(HeapBlockEntity heap) {
        this.heap = heap;
    }

    @Override
    public int getSlots() {
        return 2;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot == TAKE ? heap.stack() : ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (slot != PUT) {
            return stack;
        }
        int taken = heap.insert(stack, simulate);
        return taken >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - taken);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return slot == TAKE ? heap.extract(amount, simulate) : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        if (slot == TAKE) {
            ItemStack sample = heap.sample();
            return sample.isEmpty() ? net.minecraft.world.item.Item.ABSOLUTE_MAX_STACK_SIZE
                    : sample.getMaxStackSize();
        }
        // The room left can be wider than an int. Clamping here is safe because a
        // caller only ever uses this to decide whether a stack fits, and a stack
        // always does when the answer is this large.
        return (int) Math.min(heap.room(), Integer.MAX_VALUE);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot == PUT && heap.accepts(stack);
    }
}
