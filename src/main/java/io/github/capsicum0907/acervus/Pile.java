package io.github.capsicum0907.acervus;

import net.minecraft.world.item.ItemStack;

public interface Pile {
    boolean isEmpty();

    ItemStack sample();

    long count();

    long room();

    ItemStack stack();

    boolean accepts(ItemStack stack);

    int insert(ItemStack stack, boolean simulate);

    ItemStack extract(int amount, boolean simulate);

    void setChanged();

    default boolean gives() {
        return true;
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
