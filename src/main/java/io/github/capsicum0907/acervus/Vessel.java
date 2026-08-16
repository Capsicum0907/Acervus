package io.github.capsicum0907.acervus;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * The one slot a heap keeps for a container the player puts in, and the direction
 * that container is going.
 *
 * <p>Three of the four heaps hold something a person cannot carry in their hands.
 * Energy and gas have no gesture at all — they arrive and leave through cables and
 * tubes — so without somewhere to put a battery or a tank, a player can fill one of
 * these and then have no way of getting anything back out by hand.
 *
 * <p><b>The direction is decided once, when the container is put in.</b> Deciding it
 * every tick from the container's own state is what makes a heap fill a tank and then
 * immediately take it back: the moment the tank is full, "full" is the answer to
 * "which way?" again, and it reverses. Whether the player meant to fill or to empty
 * is a fact about the moment they put it there, not about how full it is now.
 */
public final class Vessel {
    private static final String HELD = "Vessel";
    private static final String FLOW = "VesselFlow";

    /** Which way the thing in the slot is going. */
    public enum Flow {
        /** Nothing left to do, or nothing to do it with. */
        NONE,
        /** Out of the container and into the heap. */
        IN,
        /** Out of the heap and into the container. */
        OUT
    }

    private ItemStack held = ItemStack.EMPTY;
    private Flow flow = Flow.NONE;
    private boolean decided;

    public ItemStack held() {
        return held;
    }

    public boolean isEmpty() {
        return held.isEmpty();
    }

    /** The player put something in, or took it out: whatever was decided no longer applies. */
    public void hold(ItemStack stack) {
        held = stack;
        flow = Flow.NONE;
        decided = false;
    }

    /** The heap swapped the container for what it became — a bucket for a full one. */
    public void replace(ItemStack stack) {
        held = stack;
    }

    public boolean undecided() {
        return !decided && !held.isEmpty();
    }

    public void decide(Flow direction) {
        flow = direction;
        decided = true;
    }

    /** Nothing more can move; the container stays as it is until somebody takes it. */
    public void done() {
        flow = Flow.NONE;
    }

    public Flow flow() {
        return flow;
    }

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        if (!held.isEmpty()) {
            tag.put(HELD, held.save(registries));
            tag.putString(FLOW, flow.name());
        }
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        held = tag.contains(HELD)
                ? ItemStack.parse(registries, tag.getCompound(HELD)).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        flow = held.isEmpty() ? Flow.NONE : read(tag.getString(FLOW));
        decided = flow != Flow.NONE;
    }

    private static Flow read(String name) {
        try {
            return Flow.valueOf(name);
        } catch (IllegalArgumentException absent) {
            return Flow.NONE;
        }
    }
}
