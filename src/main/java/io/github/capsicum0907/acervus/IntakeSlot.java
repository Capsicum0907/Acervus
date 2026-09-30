package io.github.capsicum0907.acervus;

import java.util.function.Supplier;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class IntakeSlot extends Slot {
    private static final SimpleContainer UNUSED = new SimpleContainer(0);

    private final Supplier<Pile> heap;

    public IntakeSlot(Supplier<Pile> heap, int x, int y) {
        super(UNUSED, 0, x, y);
        this.heap = heap;
    }

    @Override
    public ItemStack getItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean hasItem() {
        return false;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        Pile pile = heap.get();
        return pile.accepts(stack) && pile.room() > 0;
    }

    @Override
    public boolean mayPickup(Player player) {
        return false;
    }

    @Override
    public int getMaxStackSize() {
        return Item.ABSOLUTE_MAX_STACK_SIZE;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return (int) Math.min(heap.get().room(), Integer.MAX_VALUE);
    }

    @Override
    public void set(ItemStack stack) {
        if (!stack.isEmpty() && mayPlace(stack)) {
            heap.get().insert(stack, false);
        }
    }

    @Override
    public ItemStack safeInsert(ItemStack stack, int increment) {
        if (stack.isEmpty() || !mayPlace(stack)) {
            return stack;
        }
        int taken = heap.get().insert(stack.copyWithCount(Math.min(increment, stack.getCount())), false);
        stack.shrink(taken);
        return stack;
    }

    @Override
    public ItemStack remove(int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setChanged() {
        heap.get().setChanged();
    }
}
