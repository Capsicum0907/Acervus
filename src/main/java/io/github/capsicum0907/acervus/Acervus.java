package io.github.capsicum0907.acervus;

import com.mojang.logging.LogUtils;

import io.github.capsicum0907.acervus.client.AcervusClient;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
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

    private static final Logger LOGGER = LogUtils.getLogger();

    public Acervus(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, AcervusConfig.SPEC);

        AcervusRegistry.BLOCKS.register(modEventBus);
        AcervusRegistry.ITEMS.register(modEventBus);
        AcervusRegistry.BLOCK_ENTITIES.register(modEventBus);

        modEventBus.addListener(Acervus::registerCapabilities);
        modEventBus.addListener(Acervus::addToCreativeTab);

        LOGGER.info("Acervus {} loaded.", modContainer.getModInfo().getVersion());
    }

    /**
     * The one thing that makes hoppers, droppers and every mod's pipes work: the item
     * handler capability. They all ask a block for this and none of them ask for
     * anything else, so getting it right once is the whole of the integration.
     *
     * <p>Registered without regard to side — a heap offers the same window in every
     * direction.
     */
    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, AcervusRegistry.HEAP_ENTITY.get(),
                (heap, side) -> new HeapItemHandler(heap));
    }

    private static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(AcervusRegistry.HEAP_ITEM);
        }
    }

    /** Drawing is a client concern, and this is the only place that knows it exists. */
    @Mod(value = MODID, dist = Dist.CLIENT)
    public static class Client {
        public Client(IEventBus modEventBus, ModContainer modContainer) {
            modEventBus.addListener(AcervusClient::registerRenderers);
        }
    }
}
