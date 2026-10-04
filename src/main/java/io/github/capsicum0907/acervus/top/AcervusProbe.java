package io.github.capsicum0907.acervus.top;

import java.util.function.Function;

import io.github.capsicum0907.acervus.Acervus;
import io.github.capsicum0907.acervus.Readout;

import mcjty.theoneprobe.api.IProbeConfig;
import mcjty.theoneprobe.api.IProbeConfigProvider;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeHitEntityData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.api.ProbeMode;
import mcjty.theoneprobe.api.ElementAlignment;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class AcervusProbe implements Function<ITheOneProbe, Void> {
    private static final ResourceLocation CONTENTS = ResourceLocation.fromNamespaceAndPath(Acervus.MODID, "contents");
    private static final int HIDDEN = 0;
    private static final int SPACING = 4;

    @Override
    public Void apply(ITheOneProbe probe) {
        probe.registerElementFactory(new IconElement.Factory());
        probe.registerProvider(new Contents());
        probe.registerProbeConfigProvider(new Defaults());
        return null;
    }

    private static BlockEntity covered(Level level, IProbeHitData data) {
        BlockEntity entity = level.getBlockEntity(data.getPos());
        return entity != null && Readout.covers(entity) ? entity : null;
    }

    private static final class Contents implements IProbeInfoProvider {
        @Override
        public ResourceLocation getID() {
            return CONTENTS;
        }

        @Override
        public void addProbeInfo(ProbeMode mode, IProbeInfo info, Player player, Level level, BlockState state,
                IProbeHitData data) {
            BlockEntity entity = covered(level, data);
            if (entity == null) {
                return;
            }
            for (Readout.Line line : Readout.of(entity)) {
                info.horizontal(info.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER).spacing(SPACING))
                        .element(new IconElement(line.icon()))
                        .text(line.text());
            }
        }
    }

    private static final class Defaults implements IProbeConfigProvider {
        @Override
        public void getProbeConfig(IProbeConfig config, Player player, Level level, Entity entity,
                IProbeHitEntityData data) {
        }

        @Override
        public void getProbeConfig(IProbeConfig config, Player player, Level level, BlockState state,
                IProbeHitData data) {
            if (covered(level, data) == null) {
                return;
            }
            config.setRFMode(HIDDEN);
            config.setTankMode(HIDDEN);
            config.showTankSetting(IProbeConfig.ConfigMode.NOT);
            config.showChestContents(IProbeConfig.ConfigMode.NOT);
            config.showChestContentsDetailed(IProbeConfig.ConfigMode.NOT);
        }
    }
}
