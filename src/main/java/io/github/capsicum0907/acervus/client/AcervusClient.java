package io.github.capsicum0907.acervus.client;

import io.github.capsicum0907.acervus.AcervusRegistry;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** The client half: one renderer, registered once. */
public final class AcervusClient {
    private AcervusClient() {
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(AcervusRegistry.HEAP_ENTITY.get(), HeapRenderer::new);
    }
}
