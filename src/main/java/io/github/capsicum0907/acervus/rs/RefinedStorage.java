package io.github.capsicum0907.acervus.rs;

import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;

import io.github.capsicum0907.acervus.Acervus;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

public final class RefinedStorage {
    private RefinedStorage() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(RefinedStorage::setup);
        Acervus.LOGGER.info("Refined Storage found; heaps will be read in longs.");
    }

    private static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> RefinedStorageApi.INSTANCE.addExternalStorageProviderFactory(
                (level, pos, side) -> new HeapStorage(level, pos)));
    }
}
