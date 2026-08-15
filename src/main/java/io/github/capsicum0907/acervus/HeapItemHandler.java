package io.github.capsicum0907.acervus;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The window a heap shows to hoppers, pipes and anything else that moves items.
 *
 * <p>One slot. What is in it is what is in the heap, and taking from it takes as
 * much as was asked for.
 *
 * <h2>Why this exceeds a stack, deliberately</h2>
 *
 * The {@link IItemHandler} javadoc says two things about size, and only one of them
 * is kept here:
 *
 * <ul>
 * <li>{@code getStackInSlot} — "the result's stack size <em>may</em> be greater than
 *     the itemstack's max size". Kept, and the reason a heap says how much it really
 *     holds. Rounding it to a stack is not caution but a lie: an external storage
 *     reading one would report sixty-four of something there are a hundred thousand
 *     of, which is what InfChest does and what makes it read wrong in Refined
 *     Storage.
 * <li>{@code extractItem} — the result "must be less than or equal to {@code amount}
 *     <em>and</em> {@code getMaxStackSize()}". <b>Not kept.</b> Only {@code amount}
 *     bounds what comes out.
 * </ul>
 *
 * <p>Breaking the second one is not an oversight. Every mod that moves large amounts
 * expects it broken: InfChest's own handler is {@code totalCount().min(amount)} with
 * no stack clamp, and that is precisely why a pipe with an unlimited upgrade can
 * empty one. Keeping the clause instead caps a heap at one stack per call — one
 * stack per tick against a pipe that asks once — and no amount of extra slots buys
 * that back honestly.
 *
 * <p>What it costs is that a caller which asks for more than a stack must be able to
 * hold what it gets. Callers that cannot ask for 64 and get 64; a caller that asks
 * for two billion has said it can take two billion. The guarantees this mod is
 * responsible for are the ones on the other side of the line, and those are kept
 * exactly: simulating never changes anything, the offered stack is never modified,
 * every side shares one handler, and nothing here remembers anything.
 *
 * <p>Both sides face every direction. A heap has no front.
 */
public class HeapItemHandler implements IItemHandler {
    private static final int SLOT = 0;

    private final HeapBlockEntity heap;

    public HeapItemHandler(HeapBlockEntity heap) {
        this.heap = heap;
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slot == SLOT ? heap.contents() : ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (slot != SLOT) {
            return stack;
        }
        int taken = heap.insert(stack, simulate);
        return taken >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - taken);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return slot == SLOT ? heap.extract(amount, simulate) : ItemStack.EMPTY;
    }

    /** The capacity, so that {@code limit - count} is the room left. */
    @Override
    public int getSlotLimit(int slot) {
        return slot == SLOT ? clamped(heap.capacity()) : 0;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slot == SLOT && heap.accepts(stack);
    }

    private static int clamped(long value) {
        return (int) Math.min(value, Integer.MAX_VALUE);
    }
}
