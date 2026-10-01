package io.github.capsicum0907.acervus;

import com.mojang.logging.LogUtils;

import io.github.capsicum0907.acervus.client.AcervusClient;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

import org.slf4j.Logger;

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

        if (Mods.mekanism()) {
            io.github.capsicum0907.acervus.gas.GasHeap.register(modEventBus);
        }
        if (Mods.refinedStorage()) {
            io.github.capsicum0907.acervus.rs.RefinedStorage.register(modEventBus);
        }

        modEventBus.addListener(Acervus::registerCapabilities);
        modEventBus.addListener(Acervus::addToCreativeTab);

        NeoForge.EVENT_BUS.addListener(Carried::onPickup);
        NeoForge.EVENT_BUS.addListener(Acervus::onServerStarted);
        NeoForge.EVENT_BUS.addListener(Acervus::onDatapackSync);
        NeoForge.EVENT_BUS.addListener(Carried::onTick);

        LOGGER.info("Acervus {} loaded.", modContainer.getModInfo().getVersion());
    }

    private static void onServerStarted(ServerStartedEvent event) {
        Compression.rebuild(event.getServer().getRecipeManager(), event.getServer().overworld());
    }

    private static void onDatapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() == null) {
            Compression.rebuild(event.getPlayerList().getServer().getRecipeManager(),
                    event.getPlayerList().getServer().overworld());
        }
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, AcervusRegistry.HEAP_ENTITY.get(),
                (heap, side) -> heap.handler());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, AcervusRegistry.FLUID_HEAP_ENTITY.get(),
                (heap, side) -> heap.handler());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, AcervusRegistry.ENERGY_HEAP_ENTITY.get(),
                (heap, side) -> heap.handler());

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

    @Mod(value = MODID, dist = Dist.CLIENT)
    public static class Client {
        public Client(IEventBus modEventBus, ModContainer modContainer) {
            modEventBus.addListener(AcervusClient::registerRenderers);
            modEventBus.addListener(AcervusClient::registerScreens);
            modEventBus.addListener(AcervusClient::registerTooltips);
            modEventBus.addListener(AcervusClient::registerItemProperties);
            modEventBus.addListener(AcervusClient::registerItemExtensions);
            modEventBus.addListener(AcervusClient::registerItemDecorations);
        }
    }
}
