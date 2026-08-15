package io.github.capsicum0907.acervus;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The window a heap shows to hoppers, pipes and anything else that moves items.
 *
 * <p>The shape of it is dictated by the {@link IItemHandler} contract, which says
 * two things that pull in opposite directions and are easy to get the wrong way
 * round:
 *
 * <ul>
 * <li>{@code getStackInSlot} — "the result's stack size <em>may</em> be greater
 *     than the itemstack's max size". So a heap should say how much it really has.
 *     Rounding that down to a stack is not caution, it is a lie, and it is why an
 *     external storage reading one saw sixty-four of something there were a hundred
 *     thousand of.
 * <li>{@code extractItem} — the result "must be less than or equal to {@code amount}
 *     <em>and</em> {@code getMaxStackSize()}". So one extraction is one stack, no
 *     matter what is asked for. This is not a choice a heap gets to make.
 * </ul>
 *
 * <p><b>That second rule is what the slot count is for.</b> A pipe asks each slot
 * once a tick, so one slot means one stack a tick however fast the pipe claims to
 * be. The contents are therefore divided evenly across
 * {@link AcervusConfig#WINDOW_SLOTS} slots — each one telling the truth about its
 * share, all of them adding up to the total — and the pipe gets a stack from each.
 * It is the only lever there is, because the alternative is breaking the contract
 * that keeps everything else honest.
 *
 * <p><b>Nothing is remembered here.</b> Every method reads the block entity when it
 * is called. A handler that cached even one field would be a second copy of the
 * truth, and two of them — one per side, say — would be two copies that disagree.
 * That is the shape most duplication bugs in storage blocks actually have.
 */
public class HeapItemHandler implements IItemHandler {
    private final HeapBlockEntity heap;

    public HeapItemHandler(HeapBlockEntity heap) {
        this.heap = heap;
    }

    @Override
    public int getSlots() {
        return AcervusConfig.WINDOW_SLOTS.get();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        int share = clamped(shareOf(heap.count(), slot));
        if (heap.isEmpty() || share <= 0) {
            return ItemStack.EMPTY;
        }
        return heap.sample().copyWithCount(share);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!inRange(slot)) {
            return stack;
        }
        int taken = heap.insert(stack, simulate);
        return taken >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - taken);
    }

    /**
     * Capped by this slot's share as well as by the contract, so that taking from a
     * slot never takes more than that slot said was there.
     */
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!inRange(slot)) {
            return ItemStack.EMPTY;
        }
        int wanted = (int) Math.min(amount, shareOf(heap.count(), slot));
        return wanted <= 0 ? ItemStack.EMPTY : heap.extract(wanted, simulate);
    }

    /** This slot's share of the whole capacity, so that {@code limit - count} is the room left. */
    @Override
    public int getSlotLimit(int slot) {
        return clamped(shareOf(heap.capacity(), slot));
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return inRange(slot) && heap.accepts(stack);
    }

    /**
     * An even division, with the remainder given to the earliest slots so the shares
     * always add back up to the total exactly.
     */
    private long shareOf(long total, int slot) {
        int slots = getSlots();
        return total / slots + (slot < total % slots ? 1 : 0);
    }

    private boolean inRange(int slot) {
        return slot >= 0 && slot < getSlots();
    }

    private static int clamped(long value) {
        return (int) Math.min(value, Integer.MAX_VALUE);
    }
}
