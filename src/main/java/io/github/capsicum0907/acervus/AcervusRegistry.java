package io.github.capsicum0907.acervus;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
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
     * What every item in this mod is made with.
     *
     * <p>Deliberately plain: a heap stacks to sixty four <em>while it is empty</em>, and
     * the rule that takes that away the moment it holds something is in
     * {@link ContentsBlockItem}, where it can be asked of the item rather than fixed
     * here for all of them at once.
     *
     * <p>The method stays even though it adds nothing, because every item here has to
     * go through {@link ContentsBlockItem} and this is the one line that says so.
     */
    public static Item.Properties carriesItsOwnContents() {
        return new Item.Properties();
    }

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
    public static final DeferredBlock<HeapBlock> HEAP = BLOCKS.register("item_heap",
            () -> new HeapBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredItem<BlockItem> HEAP_ITEM = ITEMS.register("item_heap",
            () -> new HeapBlockItem(HEAP.get(), carriesItsOwnContents()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeapBlockEntity>> HEAP_ENTITY =
            BLOCK_ENTITIES.register("item_heap", blockEntityType(HeapBlockEntity::new, HEAP));

    /** The same block, for a resource that is measured rather than counted. */
    public static final DeferredBlock<FluidHeapBlock> FLUID_HEAP = BLOCKS.register("fluid_heap",
            () -> new FluidHeapBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredItem<BlockItem> FLUID_HEAP_ITEM = ITEMS.register("fluid_heap",
            () -> new FluidHeapBlockItem(FLUID_HEAP.get(), carriesItsOwnContents()));

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
            () -> new EnergyHeapBlockItem(ENERGY_HEAP.get(), carriesItsOwnContents()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyHeapBlockEntity>>
            ENERGY_HEAP_ENTITY = BLOCK_ENTITIES.register("energy_heap",
                    energyHeapType(EnergyHeapBlockEntity::new, ENERGY_HEAP));

    /**
     * The rack: a block that holds heaps and offers one place to reach all of them.
     *
     * <p>Solid, unlike the heaps — there is nothing to see through, because what it
     * holds is heaps rather than contents, and each of those says what is in it on its
     * own tooltip. No {@code requiresCorrectToolForDrops}, for the reason every block
     * in this mod goes without it: failing to drop would take everything inside with it.
     */
    public static final DeferredBlock<HorreumBlock> HORREUM = BLOCKS.register("horreum",
            () -> new HorreumBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.METAL)));

    public static final DeferredItem<BlockItem> HORREUM_ITEM = ITEMS.register("horreum",
            () -> new HorreumBlockItem(HORREUM.get(), carriesItsOwnContents()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HorreumBlockEntity>>
            HORREUM_ENTITY = BLOCK_ENTITIES.register("horreum",
                    horreumType(HorreumBlockEntity::new, HORREUM));

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Acervus.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<HorreumMenu>> HORREUM_MENU =
            MENUS.register("horreum", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> HorreumMenu.at(id, inventory, buffer.readBlockPos())));

    /** The same rack in a hand; see {@link #CARRIED_HEAP_MENU}. */
    public static final DeferredHolder<MenuType<?>, MenuType<HorreumMenu>> CARRIED_HORREUM_MENU =
            MENUS.register("carried_horreum", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> HorreumMenu.inHand(id, inventory,
                            buffer.readEnum(net.minecraft.world.InteractionHand.class))));

    public static final DeferredHolder<MenuType<?>, MenuType<HeapMenu>> HEAP_MENU =
            MENUS.register("item_heap", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> HeapMenu.at(id, inventory, buffer.readBlockPos())));

    /**
     * The same menu and the same screen, over the heap in a hand rather than the one in
     * the world. Two types because the two carry different things across the wire — a
     * position, or which hand — and one type cannot read both.
     */
    public static final DeferredHolder<MenuType<?>, MenuType<HeapMenu>> CARRIED_HEAP_MENU =
            MENUS.register("carried_item_heap", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> HeapMenu.inHand(id, inventory,
                            buffer.readEnum(net.minecraft.world.InteractionHand.class))));

    /** One menu for the three heaps whose contents are not items. */
    public static final DeferredHolder<MenuType<?>, MenuType<ReadoutMenu>> READOUT_MENU =
            MENUS.register("readout", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> ReadoutMenu.at(id, inventory, buffer.readBlockPos())));

    /** The same readout over the one in a hand; see {@link #CARRIED_HEAP_MENU}. */
    public static final DeferredHolder<MenuType<?>, MenuType<ReadoutMenu>> CARRIED_READOUT_MENU =
            MENUS.register("carried_readout", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> ReadoutMenu.inHand(id, inventory,
                            buffer.readEnum(net.minecraft.world.InteractionHand.class))));

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

    @SuppressWarnings("DataFlowIssue")
    private static Supplier<BlockEntityType<HorreumBlockEntity>> horreumType(
            BlockEntityType.BlockEntitySupplier<HorreumBlockEntity> factory, Supplier<? extends Block> block) {
        return () -> BlockEntityType.Builder.of(factory, block.get()).build(null);
    }
}
