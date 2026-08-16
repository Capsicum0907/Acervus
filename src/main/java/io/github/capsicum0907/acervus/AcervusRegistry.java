package io.github.capsicum0907.acervus;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registration. One block, its item, and its block entity. */
public final class AcervusRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Acervus.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Acervus.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Acervus.MODID);

    /**
     * {@code noOcclusion} because the contents are drawn inside it, and a block that
     * declares itself solid has its neighbours' faces culled against it — including
     * the ones this block wants to be seen through.
     *
     * <p><b>No {@code requiresCorrectToolForDrops}.</b> It is the natural thing to
     * write for a block of metal and glass, and it is wrong here: a heap that fails
     * to drop is a heap whose entire contents are gone, and no amount of "you should
     * have brought a pickaxe" makes losing two billion items a reasonable outcome. A
     * pickaxe is still the right tool — the block is in {@code mineable/pickaxe}, so
     * it is what breaks one quickly — but being without one costs time, not the
     * contents.
     */
    public static final DeferredBlock<HeapBlock> HEAP = BLOCKS.register("heap",
            () -> new HeapBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredItem<BlockItem> HEAP_ITEM = ITEMS.register("heap",
            () -> new HeapBlockItem(HEAP.get(), new net.minecraft.world.item.Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeapBlockEntity>> HEAP_ENTITY =
            BLOCK_ENTITIES.register("heap", blockEntityType(HeapBlockEntity::new, HEAP));

    /** The same block, for a resource that is measured rather than counted. */
    public static final DeferredBlock<FluidHeapBlock> FLUID_HEAP = BLOCKS.register("fluid_heap",
            () -> new FluidHeapBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredItem<BlockItem> FLUID_HEAP_ITEM = ITEMS.register("fluid_heap",
            () -> new FluidHeapBlockItem(FLUID_HEAP.get(), new net.minecraft.world.item.Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidHeapBlockEntity>>
            FLUID_HEAP_ENTITY = BLOCK_ENTITIES.register("fluid_heap",
                    fluidHeapType(FluidHeapBlockEntity::new, FLUID_HEAP));

    /** The same block again, for the resource that has no identity at all. */
    public static final DeferredBlock<EnergyHeapBlock> ENERGY_HEAP = BLOCKS.register("energy_heap",
            () -> new EnergyHeapBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredItem<BlockItem> ENERGY_HEAP_ITEM = ITEMS.register("energy_heap",
            () -> new EnergyHeapBlockItem(ENERGY_HEAP.get(), new net.minecraft.world.item.Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyHeapBlockEntity>>
            ENERGY_HEAP_ENTITY = BLOCK_ENTITIES.register("energy_heap",
                    energyHeapType(EnergyHeapBlockEntity::new, ENERGY_HEAP));

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Acervus.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<HeapMenu>> HEAP_MENU =
            MENUS.register("heap", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> new HeapMenu(id, inventory, buffer.readBlockPos())));

    /** One menu for the three heaps whose contents are not items. */
    public static final DeferredHolder<MenuType<?>, MenuType<ReadoutMenu>> READOUT_MENU =
            MENUS.register("readout", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> new ReadoutMenu(id, inventory, buffer.readBlockPos())));

    private AcervusRegistry() {
    }

    @SuppressWarnings("DataFlowIssue") // the vanilla builder wants a data fixer type it never uses
    private static Supplier<BlockEntityType<HeapBlockEntity>> blockEntityType(
            BlockEntityType.BlockEntitySupplier<HeapBlockEntity> factory, Supplier<? extends Block> block) {
        return () -> BlockEntityType.Builder.of(factory, block.get()).build(null);
    }

    @SuppressWarnings("DataFlowIssue")
    private static Supplier<BlockEntityType<FluidHeapBlockEntity>> fluidHeapType(
            BlockEntityType.BlockEntitySupplier<FluidHeapBlockEntity> factory, Supplier<? extends Block> block) {
        return () -> BlockEntityType.Builder.of(factory, block.get()).build(null);
    }

    @SuppressWarnings("DataFlowIssue")
    private static Supplier<BlockEntityType<EnergyHeapBlockEntity>> energyHeapType(
            BlockEntityType.BlockEntitySupplier<EnergyHeapBlockEntity> factory, Supplier<? extends Block> block) {
        return () -> BlockEntityType.Builder.of(factory, block.get()).build(null);
    }
}
