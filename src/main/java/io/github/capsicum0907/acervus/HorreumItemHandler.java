package io.github.capsicum0907.acervus;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The window a hopper, a pipe or a storage network sees onto a rack of item heaps.
 *
 * <p><b>One handler slot per rack slot, always twelve, whatever is in them.</b> Slot
 * indices are the one thing a pipe remembers between ticks, so they must not move
 * when a heap is taken out or a fluid heap is put in beside it. A rack slot holding
 * anything but an item heap reads as an empty handler slot that refuses everything —
 * which is what an empty slot is.
 *
 * <p>Everything else is {@link HeapItemHandler} widened by one index, including the
 * two departures from the {@code IItemHandler} javadoc it documents: the true count is
 * reported, and extraction is bounded by what was asked for rather than by a stack.
 */
public class HorreumItemHandler implements IItemHandler {
    private final HorreumBlockEntity rack;

    public HorreumItemHandler(HorreumBlockEntity rack) {
        this.rack = rack;
    }

    /** The heap in that slot, ready to give, or null if there is not one. */
    private CarriedHeap at(int slot) {
        return rack.read(slot, AcervusRegistry.HEAP_ITEM.get(), CarriedHeap::stored);
    }

    @Override
    public int getSlots() {
        return HorreumBlockEntity.SLOTS;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        CarriedHeap heap = at(slot);
        if (heap == null || heap.isEmpty()) {
            return ItemStack.EMPTY;
        }
        // The whole count, saturated at what a stack can carry. Rounding it down to
        // sixty-four is what makes an external storage misreport a heap.
        return heap.sample().copyWithCount((int) Math.min(heap.count(), Integer.MAX_VALUE));
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        CarriedHeap heap = at(slot);
        if (heap == null || stack.isEmpty()) {
            return stack;
        }
        int taken = heap.insert(stack, simulate);
        if (taken <= 0) {
            return stack;
        }
        if (!simulate) {
            rack.changed();
        }
        // The offered stack is never modified; only what is left over is handed back.
        return taken >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - taken);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        CarriedHeap heap = at(slot);
        if (heap == null || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack out = heap.extract(amount, simulate);
        if (!out.isEmpty() && !simulate) {
            rack.changed();
        }
        return out;
    }

    /**
     * How much would fit if the slot were empty, which is what a great many pipes work
     * the room out from — see {@link HeapItemHandler} for why answering with a stack
     * makes a heap look full.
     */
    @Override
    public int getSlotLimit(int slot) {
        CarriedHeap heap = at(slot);
        if (heap == null) {
            return 0;
        }
        return (int) Math.min(heap.capacity(), Integer.MAX_VALUE);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        CarriedHeap heap = at(slot);
        return heap != null && heap.accepts(stack) && heap.room() > 0;
    }
}
