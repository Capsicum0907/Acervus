package io.github.capsicum0907.acervus;

import net.minecraft.world.item.ItemStack;

/**
 * A heap of one kind of item, wherever it happens to be kept.
 *
 * <p>There are two places: a block in the world, and an item in an inventory. They
 * store the same thing and answer the same questions, and until now only the block
 * could be asked — the screen, the slot and the menu were all written against
 * {@link HeapBlockEntity} directly. This is that dependency named, so the same slot
 * and the same screen work over either.
 *
 * <p>It is deliberately not a common base class. The two differ entirely in how they
 * persist, how they reach the client and what may be taken out of them; what they
 * share is the arithmetic, and that is all this says.
 */
public interface Pile {
    boolean isEmpty();

    /** The identity of what is stored, with a count of one. */
    ItemStack sample();

    /** How many, which is wider than any stack can carry. */
    long count();

    long room();

    /** One stack of it at most: what a slot can show. */
    ItemStack stack();

    boolean accepts(ItemStack stack);

    int insert(ItemStack stack, boolean simulate);

    ItemStack extract(int amount, boolean simulate);

    void setChanged();

    /**
     * Whether anything may come out of it.
     *
     * <p>A heap in the world gives; a heap in a pocket does not. Reaching two billion
     * of anything from an inventory slot, with no block to place and nothing to stand
     * next to, makes every other kind of storage pointless — so putting it down is the
     * price of drawing from it. See {@link CarriedHeap}.
     */
    default boolean gives() {
        return true;
    }

    /**
     * A heap that is not there: the block was broken while its screen was open, or the
     * item left the hand holding it.
     *
     * <p>Answering with this rather than with null is what lets the screen and the menu
     * be written without a null check at every reading — an empty heap and a missing
     * one look the same to a reader, and should.
     */
    Pile NONE = new Pile() {
        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public ItemStack sample() {
            return ItemStack.EMPTY;
        }

        @Override
        public long count() {
            return 0L;
        }

        @Override
        public long room() {
            return 0L;
        }

        @Override
        public ItemStack stack() {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean accepts(ItemStack stack) {
            return false;
        }

        @Override
        public int insert(ItemStack stack, boolean simulate) {
            return 0;
        }

        @Override
        public ItemStack extract(int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public void setChanged() {
        }

        @Override
        public boolean gives() {
            return false;
        }
    };
}
