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

    public static final ModConfigSpec.IntValue CAPACITY = BUILDER
            .comment("How many of one item a heap holds.",
                    "The ceiling is what an int can count, because that is what an item stack",
                    "counts with; asking for more would be asking for a number the game cannot",
                    "hand back.")
            .defineInRange("capacity", 2_000_000_000, 64, Integer.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue SHOWS_CONTENTS = BUILDER
            .comment("Whether a heap draws what it holds, and how many.",
                    "Off is for servers that would rather not pay for the drawing.")
            .define("showsContents", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private AcervusConfig() {
    }
}
