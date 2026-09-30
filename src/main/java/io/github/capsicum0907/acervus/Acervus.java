package io.github.capsicum0907.acervus;

import com.mojang.logging.LogUtils;

import io.github.capsicum0907.acervus.client.AcervusClient;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

import org.slf4j.Logger;

/**
 * Entry point. {@link #MODID} must match {@code mod_id} in gradle.properties,
 * which is what the generated neoforge.mods.toml is filled from.
 */
@Mod(Acervus.MODID)
public class Acervus {
    public static final String MODID = "acervus";

    public static final Logger LOGGER = LogUtils.getLogger();

    public Acervus(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, AcervusConfig.SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, AcervusClientConfig.SPEC);

        AcervusRegistry.BLOCKS.register(modEventBus);
        AcervusRegistry.ITEMS.register(modEventBus);
        AcervusRegistry.BLOCK_ENTITIES.register(modEventBus);
        AcervusRegistry.MENUS.register(modEventBus);

        // Loading the class is what registers it, so the check has to come first.
        if (Mods.mekanism()) {
            io.github.capsicum0907.acervus.gas.GasHeap.register(modEventBus);
        }
        if (Mods.refinedStorage()) {
            io.github.capsicum0907.acervus.rs.RefinedStorage.register(modEventBus);
        }

        modEventBus.addListener(Acervus::registerCapabilities);
        modEventBus.addListener(Acervus::addToCreativeTab);

        // The game bus, not the mod bus: this one happens while playing rather than
        // while loading. A carried heap has no block entity to tick, so the only moment
        // it can act is the moment something is walked over.
        NeoForge.EVENT_BUS.addListener(Carried::onPickup);
        NeoForge.EVENT_BUS.addListener(Carried::onTick);

        LOGGER.info("Acervus {} loaded.", modContainer.getModInfo().getVersion());
    }

    /**
     * The one thing that makes hoppers, droppers and every mod's pipes work: the item
     * handler capability. They all ask a block for this and none of them ask for
     * anything else, so getting it right once is the whole of the integration.
     *
     * <p>Registered without regard to side — a heap offers the same window in every
     * direction — and handing back the block's one handler rather than a fresh one
     * per ask, so six sides cannot become six opinions about the same contents.
     */
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, AcervusRegistry.HEAP_ENTITY.get(),
                (heap, side) -> heap.handler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, AcervusRegistry.FLUID_HEAP_ENTITY.get(),
                (heap, side) -> heap.handler());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, AcervusRegistry.ENERGY_HEAP_ENTITY.get(),
                (heap, side) -> heap.handler());

        // The rack offers all of them at once: what a pipe finds depends on what is in
        // its slots, not on which block it is talking to.
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, AcervusRegistry.HORREUM_ENTITY.get(),
                (rack, side) -> rack.items());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, AcervusRegistry.HORREUM_ENTITY.get(),
                (rack, side) -> rack.fluids());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, AcervusRegistry.HORREUM_ENTITY.get(),
                (rack, side) -> rack.energy());
        if (Mods.mekanism()) {
            io.github.capsicum0907.acervus.gas.GasHeap.registerRackCapability(event);
        }
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(AcervusRegistry.HEAP_ITEM);
            event.accept(AcervusRegistry.FLUID_HEAP_ITEM);
            event.accept(AcervusRegistry.ENERGY_HEAP_ITEM);
            event.accept(AcervusRegistry.HORREUM_ITEM);
            if (Mods.mekanism()) {
                event.accept(io.github.capsicum0907.acervus.gas.GasHeap.ITEM);
            }
        }
    }

    /** Drawing is a client concern, and this is the only place that knows it exists. */
    @Mod(value = MODID, dist = Dist.CLIENT)
    public static class Client {
        public Client(IEventBus modEventBus, ModContainer modContainer) {
            modEventBus.addListener(AcervusClient::registerRenderers);
            modEventBus.addListener(AcervusClient::registerScreens);
            modEventBus.addListener(AcervusClient::registerTooltips);
            modEventBus.addListener(AcervusClient::registerItemProperties);
            modEventBus.addListener(AcervusClient::registerItemExtensions);
        }
    }
}
