package io.github.capsicum0907.acervus;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class AcervusConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.LongValue CAPACITY = BUILDER
            .push("item")
            .defineInRange("capacity", 2_000_000_000L, 64L, Long.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue ABSORBS_WHEN_CARRIED = BUILDER
            .define("absorbsWhenCarried", true);

    public static final ModConfigSpec.LongValue FLUID_CAPACITY = pop()
            .push("fluid")
            .defineInRange("capacity", 1_000_000_000_000L, 1_000L, Long.MAX_VALUE);

    public static final ModConfigSpec.LongValue ENERGY_CAPACITY = pop()
            .push("energy")
            .defineInRange("capacity", 1_000_000_000_000L, 1_000L, Long.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue ENERGY_PUSHES = BUILDER
            .define("pushes", true);

    public static final ModConfigSpec.LongValue ENERGY_PUSH_RATE = BUILDER
            .defineInRange("pushRate", 1_000_000_000L, 1L, Long.MAX_VALUE);

    public static final ModConfigSpec.LongValue CHEMICAL_CAPACITY = pop()
            .push("chemical")
            .defineInRange("capacity", 1_000_000_000_000L, 1_000L, Long.MAX_VALUE);

    public static final ModConfigSpec.BooleanValue SHOWS_CONTENTS = pop()
            .push("display")
            .define("showsContents", true);

    public static final ModConfigSpec SPEC = pop().build();

    private AcervusConfig() {
    }

    private static ModConfigSpec.Builder pop() {
        return BUILDER.pop();
    }
}
