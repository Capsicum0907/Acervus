package io.github.capsicum0907.acervus;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

public class HorreumItemHandler implements IItemHandler {
    private final HorreumBlockEntity rack;

    public HorreumItemHandler(HorreumBlockEntity rack) {
        this.rack = rack;
    }

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
        return heap != null && heap.roomFor(stack) > 0;
    }
}
