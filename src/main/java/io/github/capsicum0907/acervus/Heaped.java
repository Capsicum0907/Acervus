package io.github.capsicum0907.acervus;

import net.minecraft.network.chat.Component;

/**
 * What every heap has in common, which turned out to be less than it looked.
 *
 * <p>Four of them are written now, and the parts that are genuinely shared are: an
 * amount, a capacity, a name for what is being held, and a way of writing the amount
 * down. Everything else — how it is stored, what a "kind" even means, which API the
 * window speaks, whether it pushes or waits — differs at every one, and pulling those
 * together would have produced a base class made of branches.
 *
 * <p>So this is only what a readout needs. It exists because three of the four have
 * no items in them and therefore no slots to build a screen out of, and one screen
 * that can read any of them is worth more than three that each read one.
 */
public interface Heaped {
    /** How much is in it, in that resource's own unit. */
    long amount();

    /** How much would fit, in the same unit. */
    long capacity();

    boolean isEmpty();

    /** What is being held — a fluid, a chemical — or empty when the resource has no kinds. */
    Component contentName();

    /** The short form of an amount, including any unit it should carry. */
    String brief(long value);

    /** Every digit of an amount, for when the detail was asked for. */
    String exact(long value);

    /** The colour of the fill on a gauge, which is also the colour of the block's glass. */
    int tint();

    /**
     * A sprite standing for the contents, or null when the screen must work it out
     * another way. Chemicals carry their own icon; fluids only have one on the client,
     * so a fluid heap leaves this null and the screen looks it up there.
     */
    default net.minecraft.resources.ResourceLocation contentTexture() {
        return null;
    }

    /** The colour that sprite is drawn in. */
    default int contentTint() {
        return 0xFFFFFFFF;
    }

    default long room() {
        return Math.max(0L, capacity() - amount());
    }
}
