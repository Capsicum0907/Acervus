package io.github.capsicum0907.acervus;

import net.minecraft.world.level.block.entity.BlockEntity;

public interface Creative {
    static boolean is(BlockEntity entity) {
        return entity.getBlockState().getBlock() instanceof Creative;
    }
}
