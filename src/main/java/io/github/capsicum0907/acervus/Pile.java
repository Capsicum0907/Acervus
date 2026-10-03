package io.github.capsicum0907.acervus;

import net.minecraft.world.item.ItemStack;

public interface Pile {
    boolean isEmpty();

    ItemStack sample();

    long count();

    long room();

    long limit();

    ItemStack stack();

    boolean accepts(ItemStack stack);

    int insert(ItemStack stack, boolean simulate);

    ItemStack extract(int amount, boolean simulate);

    void setChanged();

    default boolean gives() {
        return true;
    }

    default boolean infinite() {
        return false;
    }

    default int intake(ItemStack stack, boolean simulate) {
        return insert(stack, simulate);
    }

    default boolean unreadable() {
        return false;
    }

    default String unreadableId() {
        return "";
    }

    default long count(boolean smallest) {
        return smallest ? 0L : count();
    }

    default boolean hasSmaller() {
        return false;
    }

    default ItemStack stack(boolean smallest) {
        return smallest ? ItemStack.EMPTY : stack();
    }

    default ItemStack extract(int amount, boolean simulate, boolean smallest) {
        return smallest ? ItemStack.EMPTY : extract(amount, simulate);
    }

    default void settle() {
    }

    default long roomFor(ItemStack stack) {
        return accepts(stack) ? room() : 0L;
    }

    default boolean locked() {
        return false;
    }

    default boolean canLock() {
        return false;
    }

    default void lock(boolean on) {
    }

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
        public long limit() {
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
