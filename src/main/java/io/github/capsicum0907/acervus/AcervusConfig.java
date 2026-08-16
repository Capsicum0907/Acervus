package io.github.capsicum0907.acervus;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Every tunable value lives here. Nothing else in the mod may hold a literal.
 *
 * <p>SERVER, not COMMON: what a heap holds is world state. A client gets the
 * host's values.
 */
public final class AcervusConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.LongValue CAPACITY = BUILDER
            .comment("How many of one item a heap holds.",
                    "A long, not an int. The count never leaves the block as a number — what",
                    "leaves is an item stack of at most one stack — so nothing outside has to be",
                    "able to hold it, and there is no reason to stop at two billion.",
                    "The default stops there anyway, because a number people can picture is a",
                    "better default than the largest one that fits.")
            .defineInRange("capacity", 2_000_000_000L, 64L, Long.MAX_VALUE);

    public static final ModConfigSpec.LongValue FLUID_CAPACITY = BUILDER
            .comment("How much of one fluid a heap holds, in millibuckets.",
                    "A thousand to the bucket, so the default is a billion buckets.",
                    "Higher than the item default on purpose: fluids are the thing that runs out",
                    "of room first in practice.",
                    "Filling and draining are limited to about two billion millibuckets per call,",
                    "because that is what a fluid stack counts in - but a call is not a lifetime,",
                    "and repeating one fills a heap as far as this says.")
            .defineInRange("fluidCapacity", 1_000_000_000_000L, 1_000L, Long.MAX_VALUE);

    public static final ModConfigSpec.LongValue ENERGY_CAPACITY = BUILDER
            .comment("How much Forge Energy a heap holds.",
                    "The default is a trillion, which is the order of magnitude somebody with a",
                    "working reactor actually reaches - two billion, which is all an int can",
                    "say, is a few minutes of one.",
                    "Moving it is limited to about two billion per call for that same reason,",
                    "and for the same reason as the others that is a limit on one call rather",
                    "than on the block.")
            .defineInRange("energyCapacity", 1_000_000_000_000L, 1_000L, Long.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue SHOWS_CONTENTS = BUILDER
            .comment("Whether a heap draws what it holds, and how many.",
                    "Off is for servers that would rather not pay for the drawing.")
            .define("showsContents", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private AcervusConfig() {
    }
}
