package io.github.capsicum0907.acervus;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.BaseEntityBlock;

public class CreativeFluidHeapBlock extends FluidHeapBlock implements Creative {
    public static final MapCodec<CreativeFluidHeapBlock> CODEC = simpleCodec(CreativeFluidHeapBlock::new);

    public CreativeFluidHeapBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
