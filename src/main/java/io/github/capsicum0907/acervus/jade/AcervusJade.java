package io.github.capsicum0907.acervus.jade;

import io.github.capsicum0907.acervus.Acervus;
import io.github.capsicum0907.acervus.EnergyHeapBlock;
import io.github.capsicum0907.acervus.EnergyHeapBlockEntity;
import io.github.capsicum0907.acervus.FluidHeapBlock;
import io.github.capsicum0907.acervus.FluidHeapBlockEntity;
import io.github.capsicum0907.acervus.HeapBlock;
import io.github.capsicum0907.acervus.HeapBlockEntity;
import io.github.capsicum0907.acervus.HorreumBlock;
import io.github.capsicum0907.acervus.HorreumBlockEntity;
import io.github.capsicum0907.acervus.Readout;
import io.github.capsicum0907.acervus.gas.ChemicalHeapBlock;
import io.github.capsicum0907.acervus.gas.ChemicalHeapBlockEntity;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.view.HideThingsExtensionProvider;

@WailaPlugin
public class AcervusJade implements IWailaPlugin {
    private static final ResourceLocation CONTENTS = ResourceLocation.fromNamespaceAndPath(Acervus.MODID, "contents");

    private static final java.util.List<Class<? extends Block>> BLOCKS = java.util.List.of(
            HeapBlock.class, FluidHeapBlock.class, EnergyHeapBlock.class, ChemicalHeapBlock.class, HorreumBlock.class);

    private static final java.util.List<Class<? extends BlockEntity>> ENTITIES = java.util.List.of(
            HeapBlockEntity.class, FluidHeapBlockEntity.class, EnergyHeapBlockEntity.class,
            ChemicalHeapBlockEntity.class, HorreumBlockEntity.class);

    @Override
    public void register(IWailaCommonRegistration registration) {
        for (Class<? extends BlockEntity> entity : ENTITIES) {
            registration.registerItemStorage(HideThingsExtensionProvider.instance(), entity);
            registration.registerFluidStorage(HideThingsExtensionProvider.instance(), entity);
            registration.registerEnergyStorage(HideThingsExtensionProvider.instance(), entity);
        }
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        for (Class<? extends Block> block : BLOCKS) {
            registration.registerBlockComponent(Contents.INSTANCE, block);
        }
    }

    private enum Contents implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            BlockEntity entity = accessor.getBlockEntity();
            if (entity == null || !Readout.covers(entity)) {
                return;
            }
            for (Readout.Line line : Readout.of(entity)) {
                tooltip.add(line.text());
            }
        }

        @Override
        public ResourceLocation getUid() {
            return CONTENTS;
        }
    }
}
