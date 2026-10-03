package io.github.capsicum0907.acervus.top;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;

public final class TheOneProbe {
    private static final String MODID = "theoneprobe";
    private static final String HANDSHAKE = "getTheOneProbe";

    private TheOneProbe() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener((InterModEnqueueEvent event) ->
                InterModComms.sendTo(MODID, HANDSHAKE, AcervusProbe::new));
    }
}
