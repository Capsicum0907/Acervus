package io.github.capsicum0907.acervus.gas;

import java.util.function.Supplier;

import io.github.capsicum0907.acervus.AcervusRegistry;
import io.github.capsicum0907.acervus.Mods;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

public final class GasHeap {
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
        if (Mods.mekanism()) {
            modEventBus.addListener(io.github.capsicum0907.acervus.gas.mek.MekanismChemistry::registerCapabilities);
        }
    }

    @SuppressWarnings("DataFlowIssue")
    private static Supplier<BlockEntityType<ChemicalHeapBlockEntity>> type(
            BlockEntityType.BlockEntitySupplier<ChemicalHeapBlockEntity> factory,
            Supplier<? extends Block> block) {
        return () -> BlockEntityType.Builder.of(factory, block.get()).build(null);
    }
}
