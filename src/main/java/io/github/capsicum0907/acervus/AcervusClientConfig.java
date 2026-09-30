package io.github.capsicum0907.acervus;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class AcervusClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.EnumValue<BarScale> BAR_SCALE = BUILDER
            .push("screen")
            .defineEnum("barScale", BarScale.LINEAR);

    public static final ModConfigSpec SPEC = BUILDER.pop().build();

    private AcervusClientConfig() {
    }
}
