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

import snownee.jade.api.ui.IElementHelper;
import io.github.capsicum0907.acervus.client.Icons;
import net.minecraft.world.phys.Vec2;
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

    private static final java.util.List<ResourceLocation> OTHERS = java.util.List.of(
            ResourceLocation.fromNamespaceAndPath("mekanism", "tooltip_renderer"),
            ResourceLocation.fromNamespaceAndPath("mekanism", "chemical"),
            ResourceLocation.fromNamespaceAndPath("mekanism", "fluid"),
            ResourceLocation.fromNamespaceAndPath("mekanism", "energy"));

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
        registration.addTooltipCollectedCallback((box, accessor) -> {
            if (accessor instanceof BlockAccessor block && block.getBlockEntity() != null
                    && Readout.covers(block.getBlockEntity())) {
                OTHERS.forEach(box.getTooltip()::remove);
            }
        });
    }

    private enum Contents implements IBlockComponentProvider {
        INSTANCE;

        private static final int GAP = 4;
        private static final Vec2 TEXT_DROP = new Vec2(0, 4);

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            BlockEntity entity = accessor.getBlockEntity();
            if (entity == null || !Readout.covers(entity)) {
                return;
            }
            for (Readout.Line line : Readout.of(entity)) {
                tooltip.add(new IconElement(line.icon()));
                tooltip.append(IElementHelper.get().spacer(GAP, Icons.SIZE));
                tooltip.append(IElementHelper.get().text(line.text()).translate(TEXT_DROP));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return CONTENTS;
        }
    }
}
