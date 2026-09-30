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

public final class AcervusRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Acervus.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Acervus.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Acervus.MODID);

    public static Item.Properties carriesItsOwnContents() {
        return new Item.Properties();
    }

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

    public static final DeferredBlock<EnergyHeapBlock> ENERGY_HEAP = BLOCKS.register("energy_heap",
            () -> new EnergyHeapBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.METAL)));

    public static final DeferredItem<BlockItem> ENERGY_HEAP_ITEM = ITEMS.register("energy_heap",
            () -> new EnergyHeapBlockItem(ENERGY_HEAP.get(), carriesItsOwnContents()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyHeapBlockEntity>>
            ENERGY_HEAP_ENTITY = BLOCK_ENTITIES.register("energy_heap",
                    energyHeapType(EnergyHeapBlockEntity::new, ENERGY_HEAP));

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

    public static final DeferredHolder<MenuType<?>, MenuType<HorreumMenu>> CARRIED_HORREUM_MENU =
            MENUS.register("carried_horreum", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> HorreumMenu.inHand(id, inventory,
                            buffer.readEnum(net.minecraft.world.InteractionHand.class))));

    public static final DeferredHolder<MenuType<?>, MenuType<HeapMenu>> HEAP_MENU =
            MENUS.register("item_heap", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> HeapMenu.at(id, inventory, buffer.readBlockPos())));

    public static final DeferredHolder<MenuType<?>, MenuType<HeapMenu>> CARRIED_HEAP_MENU =
            MENUS.register("carried_item_heap", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> HeapMenu.inHand(id, inventory,
                            buffer.readEnum(net.minecraft.world.InteractionHand.class))));

    public static final DeferredHolder<MenuType<?>, MenuType<ReadoutMenu>> READOUT_MENU =
            MENUS.register("readout", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> ReadoutMenu.at(id, inventory, buffer.readBlockPos())));

    public static final DeferredHolder<MenuType<?>, MenuType<ReadoutMenu>> CARRIED_READOUT_MENU =
            MENUS.register("carried_readout", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> ReadoutMenu.inHand(id, inventory,
                            buffer.readEnum(net.minecraft.world.InteractionHand.class))));

    private AcervusRegistry() {
    }

    @SuppressWarnings("DataFlowIssue")
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
