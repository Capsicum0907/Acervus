package io.github.capsicum0907.acervus;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.Slot;

/**
 * A window onto a heap, shaped like a slot.
 *
 * <p>The point is that nothing new has to be learnt. Clicking takes a stack,
 * right-clicking takes half, clicking with something in hand puts it in,
 * shift-clicking moves as much as fits — all of that is the game's own handling of
 * a slot, and none of it is written here. What is written here is only the
 * translation between "a slot holding up to a stack" and "a heap holding billions".
 *
 * <p>That translation is one rule: <b>the slot shows a window, and setting it means
 * changing the total by the difference.</b> Every path the game has into a slot
 * eventually says "the slot now holds this"; reading that as a delta rather than as
 * an assignment is what makes paths nobody enumerated behave correctly anyway.
 *
 * <p>The two size limits are deliberately different, because the game asks them for
 * different reasons. Asked with no argument it is deciding how much to take out, and
 * the answer is a stack, so taking behaves as it does everywhere. Asked about a
 * particular stack it is deciding how much will fit, and the answer is the room the
 * heap has — a plain stack there is what makes a heap of five thousand look full.
 */
public class HeapSlot extends Slot {
    /** The parent constructor needs a container; nothing ever reads it. */
    private static final SimpleContainer UNUSED = new SimpleContainer(0);

    private final HeapBlockEntity heap;

    public HeapSlot(HeapBlockEntity heap, int x, int y) {
        super(UNUSED, 0, x, y);
        this.heap = heap;
    }

    @Override
    public ItemStack getItem() {
        return heap.stack();
    }

    @Override
    public boolean hasItem() {
        return !heap.isEmpty();
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return heap.accepts(stack) && heap.room() > 0;
    }

    @Override
    public boolean mayPickup(Player player) {
        return !heap.isEmpty();
    }

    /** How much comes out at once: a stack, whatever a stack of that item is. */
    @Override
    public int getMaxStackSize() {
        ItemStack sample = heap.sample();
        return sample.isEmpty() ? Item.ABSOLUTE_MAX_STACK_SIZE : sample.getMaxStackSize();
    }

    /** How much will go in: everything the heap has room for. */
    @Override
    public int getMaxStackSize(ItemStack stack) {
        return clamped(getItem().getCount() + heap.room());
    }

    @Override
    public ItemStack remove(int amount) {
        return heap.extract(amount, false);
    }

    /**
     * The delta rule. An empty stack means the caller believes the slot is now empty,
     * which it already is — the heap was changed by whatever emptied it.
     */
    @Override
    public void set(ItemStack stack) {
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

    /**
     * Written out rather than inherited, because the inherited version works out the
     * room as {@code getMaxStackSize(stack) - getItem().getCount()} and then assigns
     * the sum — which is the delta rule taking a longer path to the same place, and
     * one more place for the two limits above to be read in the wrong order.
     */
    @Override
    public ItemStack safeInsert(ItemStack stack, int increment) {
        if (stack.isEmpty() || !mayPlace(stack)) {
            return stack;
        }
        int taken = heap.insert(stack.copyWithCount(Math.min(increment, stack.getCount())), false);
        stack.shrink(taken);
        return stack;
    }

    @Override
    public void setChanged() {
        heap.setChanged();
    }

    /** Nothing to count: this slot is not part of any crafting. */
    @Override
    public void onQuickCraft(ItemStack oldStack, ItemStack newStack) {
    }

    private static int clamped(long value) {
        return (int) Math.min(value, Integer.MAX_VALUE);
    }
}
