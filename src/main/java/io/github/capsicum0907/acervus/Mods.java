package io.github.capsicum0907.acervus;

import net.neoforged.fml.ModList;

public final class Mods {
    private static final String MEKANISM = "mekanism";
    private static final String REFINED_STORAGE = "refinedstorage";
    private static final String THE_ONE_PROBE = "theoneprobe";

    private Mods() {
    }

    public static boolean mekanism() {
        return ModList.get().isLoaded(MEKANISM);
    }

    public static boolean refinedStorage() {
        return ModList.get().isLoaded(REFINED_STORAGE);
    }

    public static boolean theOneProbe() {
        return ModList.get().isLoaded(THE_ONE_PROBE);
    }
}
