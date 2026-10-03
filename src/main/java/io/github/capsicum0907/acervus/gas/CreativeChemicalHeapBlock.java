package io.github.capsicum0907.acervus.gas;

import com.mojang.serialization.MapCodec;

import io.github.capsicum0907.acervus.Creative;

import net.minecraft.world.level.block.BaseEntityBlock;

public class CreativeChemicalHeapBlock extends ChemicalHeapBlock implements Creative {
    public static final MapCodec<CreativeChemicalHeapBlock> CODEC = simpleCodec(CreativeChemicalHeapBlock::new);

    public CreativeChemicalHeapBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
}
