package io.github.capsicum0907.acervus;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The window a heap shows to hoppers, pipes and anything else that moves items.
 *
 * <p><b>One slot, holding at most one stack.</b> The count inside a heap runs to
 * billions, but an item stack handed across this boundary is read by code that was
 * written for sixty-four — vanilla's and every other mod's alike — and most of it
 * rounds anything larger back down at the far end. That is where storage blocks of
 * this kind normally break, so the large number never crosses: it stays inside, and
 * what leaves is an ordinary stack that nothing has to be taught about.
 *
 * <p>Insertion is not limited the same way, because the number going in is bounded
 * by whatever the inserter was holding, which is already a stack at most.
 *
 * <p>The same window is offered on every side. A heap has no front, and sides that
 * only take or only give would be a second thing to explain for no gain.
 */
public class HeapItemHandler implements IItemHandler {
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
        return heap.stack();
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        int taken = heap.insert(stack, simulate);
        if (taken >= stack.getCount()) {
            return ItemStack.EMPTY;
        }
        return stack.copyWithCount(stack.getCount() - taken);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return heap.extract(amount, simulate);
    }

    /**
     * A stack of whatever is inside, or a stack of the largest thing that could be,
     * while it is empty. Reporting the heap's real capacity here would be reporting a
     * number the caller is about to try to build an item stack out of.
     */
    @Override
    public int getSlotLimit(int slot) {
        ItemStack sample = heap.sample();
        return sample.isEmpty() ? Item.ABSOLUTE_MAX_STACK_SIZE : sample.getMaxStackSize();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return heap.accepts(stack);
    }
}
