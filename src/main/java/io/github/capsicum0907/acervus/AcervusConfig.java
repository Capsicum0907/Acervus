package io.github.capsicum0907.acervus;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Every tunable value lives here. Nothing else in the mod may hold a literal.
 *
 * <p>SERVER, not COMMON: what a heap holds is world state. A client gets the host's
 * values.
 *
 * <p>Grouped by resource, one section each, and the comments are kept to a line or
 * two. A config file is read while looking for one setting, not read through — the
 * reasoning behind these numbers belongs in the README, and putting it here instead
 * turned the file into a wall nobody could find anything in.
 */
public final class AcervusConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.LongValue CAPACITY = BUILDER
            .comment("Items in one heap. The default is two billion; the ceiling is what a long counts.")
            .push("item")
            .defineInRange("capacity", 2_000_000_000L, 64L, Long.MAX_VALUE);

    public static final ModConfigSpec.LongValue FLUID_CAPACITY = pop()
            .comment("Millibuckets in one heap. The default is a billion buckets.")
            .push("fluid")
            .defineInRange("capacity", 1_000_000_000_000L, 1_000L, Long.MAX_VALUE);

    public static final ModConfigSpec.LongValue ENERGY_CAPACITY = pop()
            .comment("Forge Energy in one heap. The default is a trillion.")
            .push("energy")
            .defineInRange("capacity", 1_000_000_000_000L, 1_000L, Long.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue ENERGY_PUSHES = BUILDER
            .comment("Whether a heap offers energy to the blocks touching it.",
                    "On, because energy is pushed rather than fetched. Items and fluids are the",
                    "other way round, which is why only this one has the setting.")
            .define("pushes", true);

    public static final ModConfigSpec.LongValue ENERGY_PUSH_RATE = BUILDER
            .comment("How much it offers each neighbour per tick.",
                    "Capped at about two billion regardless: that is what one call can carry.")
            .defineInRange("pushRate", 1_000_000_000L, 1L, Long.MAX_VALUE);

    public static final ModConfigSpec.LongValue CHEMICAL_CAPACITY = pop()
            .comment("Mekanism chemicals in one heap. Only used when Mekanism is installed.",
                    "The one resource with no ceiling at the edge: Mekanism counts in longs too.")
            .push("chemical")
            .defineInRange("capacity", 1_000_000_000_000L, 1_000L, Long.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue SHOWS_CONTENTS = pop()
            .comment("How a heap looks. Nothing here changes what it does.")
            .push("display")
            .define("showsContents", true);

    public static final ModConfigSpec SPEC = pop().build();

    private AcervusConfig() {
    }

    /** Closes the section just written, so each block above reads as one section. */
    private static ModConfigSpec.Builder pop() {
        return BUILDER.pop();
    }
}
