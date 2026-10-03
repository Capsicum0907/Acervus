package io.github.capsicum0907.acervus;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.BaseEntityBlock;

public class CreativeEnergyHeapBlock extends EnergyHeapBlock implements Creative {
    public static final MapCodec<CreativeEnergyHeapBlock> CODEC = simpleCodec(CreativeEnergyHeapBlock::new);

    public CreativeEnergyHeapBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
