package io.github.capsicum0907.acervus;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

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
