package io.github.capsicum0907.acervus;

import java.util.function.Supplier;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;

public class HeapSlot extends Slot {
    private static final SimpleContainer UNUSED = new SimpleContainer(0);

    private final Supplier<Pile> heap;

    public HeapSlot(Supplier<Pile> heap, int x, int y) {
        super(UNUSED, 0, x, y);
        this.heap = heap;
    }

    private Pile heap() {
        return heap.get();
    }

    @Override
    public ItemStack getItem() {
        return heap().stack();
    }

    @Override
    public boolean hasItem() {
        return !heap().isEmpty();
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }

    @Override
    public boolean mayPickup(Player player) {
        Pile heap = heap();
        return heap.gives() && !heap.isEmpty();
    }

    @Override
    public int getMaxStackSize() {
        ItemStack sample = heap().sample();
        return sample.isEmpty() ? Item.ABSOLUTE_MAX_STACK_SIZE : sample.getMaxStackSize();
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return clamped(getItem().getCount() + heap().room());
    }

    @Override
    public ItemStack remove(int amount) {
        return heap().extract(amount, false);
    }

    @Override
    public void set(ItemStack stack) {
        Pile heap = heap();
        if (stack.isEmpty() || !heap.accepts(stack)) {
            return;
        }
        int difference = stack.getCount() - getItem().getCount();
        if (difference > 0) {
            heap.insert(stack.copyWithCount(difference), false);
        } else if (difference < 0) {
            heap.extract(-difference, false);
        }
    }

    @Override
    public ItemStack safeInsert(ItemStack stack, int increment) {
        return stack;
    }

    @Override
    public void setChanged() {
        heap().setChanged();
    }

    @Override
    public void onQuickCraft(ItemStack oldStack, ItemStack newStack) {
    }

    private static int clamped(long value) {
        return (int) Math.min(value, Integer.MAX_VALUE);
    }
}
