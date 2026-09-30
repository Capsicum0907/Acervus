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
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

public final class GasHeap {
    private static final String MEKANISM = "mekanism";

    public static final BlockCapability<IChemicalHandler, Direction> CHEMICAL_HANDLER =
            BlockCapability.createSided(
                    ResourceLocation.fromNamespaceAndPath(MEKANISM, "chemical_handler"),
                    IChemicalHandler.class);

    public static final net.neoforged.neoforge.capabilities.ItemCapability<IChemicalHandler, Void>
            CHEMICAL_ITEM = net.neoforged.neoforge.capabilities.ItemCapability.createVoid(
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
            () -> new ChemicalHeapBlockItem(BLOCK.get(), AcervusRegistry.carriesItsOwnContents()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChemicalHeapBlockEntity>>
            BLOCK_ENTITY = AcervusRegistry.BLOCK_ENTITIES.register("chemical_heap",
                    type(ChemicalHeapBlockEntity::new, BLOCK));

    private GasHeap() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(GasHeap::registerCapability);
        Acervus.LOGGER.info("Mekanism found; the gas heap is registered.");
    }

    private static void registerCapability(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(CHEMICAL_HANDLER, BLOCK_ENTITY.get(), (heap, side) -> heap.handler());
    }

    public static void registerRackCapability(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(CHEMICAL_HANDLER,
                io.github.capsicum0907.acervus.AcervusRegistry.HORREUM_ENTITY.get(),
                (rack, side) -> new HorreumChemicalHandler(rack));
    }

    @SuppressWarnings("DataFlowIssue")
    private static Supplier<BlockEntityType<ChemicalHeapBlockEntity>> type(
            BlockEntityType.BlockEntitySupplier<ChemicalHeapBlockEntity> factory,
            Supplier<? extends Block> block) {
        return () -> BlockEntityType.Builder.of(factory, block.get()).build(null);
    }
}
