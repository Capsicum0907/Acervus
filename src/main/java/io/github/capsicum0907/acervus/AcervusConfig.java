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

    public static final ModConfigSpec.IntValue WINDOW_SLOTS = BUILDER
            .comment("How many slots a heap shows to hoppers and pipes.",
                    "This is the throughput dial, and the reason it exists is a rule that is not",
                    "ours: an item handler may never hand out more than one stack per extraction.",
                    "A pipe that asks each slot once per tick therefore moves one stack per slot",
                    "per tick, and nothing a heap does can raise that except having more slots.",
                    "The contents are divided evenly between them, so what a slot says is still",
                    "true and the slots still add up to the total.")
            .defineInRange("windowSlots", 27, 1, 64);

    public static final ModConfigSpec.BooleanValue SHOWS_CONTENTS = BUILDER
            .comment("Whether a heap draws what it holds, and how many.",
                    "Off is for servers that would rather not pay for the drawing.")
            .define("showsContents", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private AcervusConfig() {
    }
}
