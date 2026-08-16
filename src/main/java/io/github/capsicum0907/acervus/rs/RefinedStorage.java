package io.github.capsicum0907.acervus.rs;

import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;

import io.github.capsicum0907.acervus.Acervus;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * The whole of what Acervus knows about Refined Storage: one registration.
 *
 * <p>Everything here is loaded only when Refined Storage is installed, and the check
 * lives in {@code Mods} rather than in this class — see
 * {@link io.github.capsicum0907.acervus.gas.GasHeap#register} for the crash that rule
 * was bought with.
 *
 * <p>Registered in common setup rather than in the mod constructor, because
 * {@code RefinedStorageApi.INSTANCE} is something Refined Storage fills in during its
 * own construction, and mod constructors run in an order nobody should rely on.
 */
public final class RefinedStorage {
    private RefinedStorage() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(RefinedStorage::setup);
        Acervus.LOGGER.info("Refined Storage found; heaps will be read in longs.");
    }

    private static void setup(FMLCommonSetupEvent event) {
        // The factory is asked about every external storage in the world, ours or not,
        // and must answer with something either way. HeapStorage answers "nothing here"
        // when the block is somebody else's.
        event.enqueueWork(() -> RefinedStorageApi.INSTANCE.addExternalStorageProviderFactory(
                (level, pos, side) -> new HeapStorage(level, pos)));
    }
}
