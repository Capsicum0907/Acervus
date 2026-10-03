package io.github.capsicum0907.acervus.gas;

import io.github.capsicum0907.acervus.AcervusRegistry;
import io.github.capsicum0907.acervus.CreativeBlockItem;
import io.github.capsicum0907.acervus.Mods;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
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

    public static final DeferredBlock<CreativeChemicalHeapBlock> CREATIVE_BLOCK = AcervusRegistry.BLOCKS.register(
            "creative_chemical_heap",
            () -> new CreativeChemicalHeapBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.METAL)
                    .noOcclusion()));

    public static final DeferredItem<BlockItem> CREATIVE_ITEM = AcervusRegistry.ITEMS.register(
            "creative_chemical_heap",
            () -> new CreativeBlockItem(CREATIVE_BLOCK.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChemicalHeapBlockEntity>>
            BLOCK_ENTITY = AcervusRegistry.BLOCK_ENTITIES.register("chemical_heap",
                    () -> BlockEntityType.Builder.of(ChemicalHeapBlockEntity::new,
                            AcervusRegistry.all(BLOCK, CREATIVE_BLOCK)).build(null));

    private GasHeap() {
    }

    public static void register(IEventBus modEventBus) {
        if (Mods.mekanism()) {
            modEventBus.addListener(io.github.capsicum0907.acervus.gas.mek.MekanismChemistry::registerCapabilities);
        }
    }
}
