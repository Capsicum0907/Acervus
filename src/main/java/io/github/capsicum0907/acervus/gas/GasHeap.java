package io.github.capsicum0907.acervus.gas;

import java.util.function.Supplier;

import io.github.capsicum0907.acervus.Acervus;
import io.github.capsicum0907.acervus.AcervusRegistry;

import mekanism.api.chemical.IChemicalHandler;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The gas heap, and the whole of what Acervus knows about Mekanism.
 *
 * <p>Everything here is registered only when Mekanism is installed. That is not
 * caution about the dependency being optional — it is what makes it optional: a
 * block whose block entity holds a {@code ChemicalStack} cannot be registered
 * without the class that defines one, so the block has to not exist instead.
 *
 * <p>The capability is built by name rather than read off Mekanism's own class.
 * Capabilities are interned by name and type, so
 * {@code mekanism:chemical_handler} resolves to the very object Mekanism registers,
 * and this mod compiles against the published API alone rather than against the
 * mod's internals.
 */
public final class GasHeap {
    private static final String MEKANISM = "mekanism";

    /** The same capability object Mekanism registers, reached without its class. */
    public static final BlockCapability<IChemicalHandler, Direction> CHEMICAL_HANDLER =
            BlockCapability.createSided(
                    ResourceLocation.fromNamespaceAndPath(MEKANISM, "chemical_handler"),
                    IChemicalHandler.class);

    public static final DeferredBlock<ChemicalHeapBlock> BLOCK = AcervusRegistry.BLOCKS.register(
            "chemical_heap",
            () -> new ChemicalHeapBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredItem<BlockItem> ITEM = AcervusRegistry.ITEMS.register("chemical_heap",
            () -> new ChemicalHeapBlockItem(BLOCK.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChemicalHeapBlockEntity>>
            BLOCK_ENTITY = AcervusRegistry.BLOCK_ENTITIES.register("chemical_heap",
                    type(ChemicalHeapBlockEntity::new, BLOCK));

    private GasHeap() {
    }

    /** Whether the gas heap exists at all in this game. */
    public static boolean present() {
        return ModList.get().isLoaded(MEKANISM);
    }

    /**
     * Loading this class is what registers everything in it, so the caller has to
     * have checked first — and the check cannot live inside, because reaching inside
     * is what loads it.
     */
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(GasHeap::registerCapability);
        Acervus.LOGGER.info("Mekanism found; the gas heap is registered.");
    }

    private static void registerCapability(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(CHEMICAL_HANDLER, BLOCK_ENTITY.get(), (heap, side) -> heap.handler());
    }

    @SuppressWarnings("DataFlowIssue") // the vanilla builder wants a data fixer type it never uses
    private static Supplier<BlockEntityType<ChemicalHeapBlockEntity>> type(
            BlockEntityType.BlockEntitySupplier<ChemicalHeapBlockEntity> factory,
            Supplier<? extends Block> block) {
        return () -> BlockEntityType.Builder.of(factory, block.get()).build(null);
    }
}
