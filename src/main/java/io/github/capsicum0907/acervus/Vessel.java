package io.github.capsicum0907.acervus;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * The one slot a heap keeps for a container the player puts in.
 *
 * <p>Three of the four heaps hold something a person cannot carry in their hands.
 * Energy and gas have no gesture at all — they arrive and leave through cables and
 * tubes — so without somewhere to put a battery or a tank, a player can fill one of
 * these and then have no way of ever getting anything back out by hand. This is that
 * somewhere.
 *
 * <p>It is a plain field rather than an item handler because nothing outside is meant
 * to reach it: a pipe has the real window to talk to, and this exists only for the
 * person standing at the screen.
 */
public final class Vessel {
    private static final String KEY = "Vessel";

    private ItemStack held = ItemStack.EMPTY;

    public ItemStack held() {
        return held;
    }

    public void hold(ItemStack stack) {
        held = stack;
    }

    public boolean isEmpty() {
        return held.isEmpty();
    }

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        if (!held.isEmpty()) {
            tag.put(KEY, held.save(registries));
        }
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        held = tag.contains(KEY)
                ? ItemStack.parse(registries, tag.getCompound(KEY)).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
    }
}
