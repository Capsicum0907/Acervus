package io.github.capsicum0907.acervus.client;

import io.github.capsicum0907.acervus.AcervusRegistry;
import io.github.capsicum0907.acervus.Mods;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** The client half: what draws the block, and what draws its screen. */
public final class AcervusClient {
    private AcervusClient() {
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(AcervusRegistry.HEAP_ENTITY.get(), HeapRenderer::new);
        event.registerBlockEntityRenderer(AcervusRegistry.FLUID_HEAP_ENTITY.get(), FluidHeapRenderer::new);
        event.registerBlockEntityRenderer(AcervusRegistry.ENERGY_HEAP_ENTITY.get(), EnergyHeapRenderer::new);
        if (Mods.mekanism()) {
            event.registerBlockEntityRenderer(io.github.capsicum0907.acervus.gas.GasHeap.BLOCK_ENTITY.get(),
                    io.github.capsicum0907.acervus.gas.ChemicalHeapRenderer::new);
        }
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(AcervusRegistry.HEAP_MENU.get(), HeapScreen::new);
        event.register(AcervusRegistry.CARRIED_HEAP_MENU.get(), HeapScreen::new);
        event.register(AcervusRegistry.READOUT_MENU.get(), ReadoutScreen::new);
        event.register(AcervusRegistry.CARRIED_READOUT_MENU.get(), ReadoutScreen::new);
        event.register(AcervusRegistry.HORREUM_MENU.get(), HorreumScreen::new);
    }
}
