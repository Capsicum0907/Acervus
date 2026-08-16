package io.github.capsicum0907.acervus;

import net.neoforged.fml.ModList;

/**
 * Which optional mods are here.
 *
 * <p>A class that <b>must never name anything belonging to them</b>, which is the
 * entire reason it exists rather than being a method on the thing it guards.
 *
 * <p>This was learnt the hard way. The check used to be {@code GasHeap.present()},
 * which reads as exactly the right thing and cannot work: calling a static method
 * initialises the class it is on, and {@code GasHeap} holds a
 * {@code BlockCapability<IChemicalHandler, …>} in a static field. Asking "is Mekanism
 * here?" therefore loaded a Mekanism class to find out, and without Mekanism the mod
 * failed during construction with {@code NoClassDefFoundError}. <b>The guard cannot
 * live behind the door it is guarding.</b>
 *
 * <p>Every use is a short-circuit: {@code Mods.mekanism() && GasHeap.ITEM.get() == …}.
 * The right-hand side is only reached when the answer is yes, and a method body that
 * mentions a missing class is fine as long as it is never run.
 */
public final class Mods {
    private static final String MEKANISM = "mekanism";
    private static final String REFINED_STORAGE = "refinedstorage";

    private Mods() {
    }

    /** Whether the gas heap exists at all in this game. */
    public static boolean mekanism() {
        return ModList.get().isLoaded(MEKANISM);
    }

    /** Whether there is a storage network here that can be told a heap's real total. */
    public static boolean refinedStorage() {
        return ModList.get().isLoaded(REFINED_STORAGE);
    }
}
