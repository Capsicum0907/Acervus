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

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Acervus.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<HeapMenu>> HEAP_MENU =
            MENUS.register("heap", () -> IMenuTypeExtension.create(
                    (id, inventory, buffer) -> new HeapMenu(id, inventory, buffer.readBlockPos())));

    private AcervusRegistry() {
    }

    @SuppressWarnings("DataFlowIssue") // the vanilla builder wants a data fixer type it never uses
    private static Supplier<BlockEntityType<HeapBlockEntity>> blockEntityType(
            BlockEntityType.BlockEntitySupplier<HeapBlockEntity> factory, Supplier<? extends Block> block) {
        return () -> BlockEntityType.Builder.of(factory, block.get()).build(null);
    }
}
