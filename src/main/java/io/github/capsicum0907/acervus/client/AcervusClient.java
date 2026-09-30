package io.github.capsicum0907.acervus.client;

import io.github.capsicum0907.acervus.Acervus;
import io.github.capsicum0907.acervus.AcervusConfig;
import io.github.capsicum0907.acervus.AcervusRegistry;
import io.github.capsicum0907.acervus.EnergyHeapBlock;
import io.github.capsicum0907.acervus.HeldEnergyHeap;
import io.github.capsicum0907.acervus.Mods;

import io.github.capsicum0907.acervus.HeapContents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
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

    public static void registerItemProperties(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(AcervusRegistry.ENERGY_HEAP_ITEM.get(),
                ResourceLocation.fromNamespaceAndPath(Acervus.MODID, EnergyHeapBlock.LAMPS_PROPERTY),
                (stack, level, entity, seed) -> EnergyHeapBlock.lampFraction(heldLamps(stack, level))));
    }

    private static int heldLamps(ItemStack stack, Level level) {
        if (!AcervusConfig.SPEC.isLoaded()) {
            return 0;
        }
        Level world = level != null ? level : Minecraft.getInstance().level;
        RegistryAccess registries = world != null ? world.registryAccess() : null;
        if (registries == null) {
            return 0;
        }
        HeldEnergyHeap heap = HeldEnergyHeap.of(registries, stack);
        return EnergyHeapBlock.lamps(heap.amount(), heap.capacity());
    }

    /** What turns {@link HeapContents} into something drawn. */
    public static void registerTooltips(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(HeapContents.class, HeapContentsTooltip::new);
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(AcervusRegistry.HEAP_MENU.get(), HeapScreen::new);
        event.register(AcervusRegistry.CARRIED_HEAP_MENU.get(), HeapScreen::new);
        event.register(AcervusRegistry.READOUT_MENU.get(), ReadoutScreen::new);
        event.register(AcervusRegistry.CARRIED_READOUT_MENU.get(), ReadoutScreen::new);
        event.register(AcervusRegistry.HORREUM_MENU.get(), HorreumScreen::new);
        event.register(AcervusRegistry.CARRIED_HORREUM_MENU.get(), HorreumScreen::new);
    }
}
