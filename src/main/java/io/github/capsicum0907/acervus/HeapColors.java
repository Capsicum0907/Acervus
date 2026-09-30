package io.github.capsicum0907.acervus;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class HeapColors {
    public static final int ITEM = 0xFFD4D8DC;
    public static final int FLUID = 0xFF6A9CE0;
    public static final int ENERGY = 0xFFD8A24A;
    public static final int CHEMICAL = 0xFF8FCF8A;

    private HeapColors() {
    }

    public static int of(ItemStack heap) {
        Item kind = heap.getItem();
        if (kind == AcervusRegistry.FLUID_HEAP_ITEM.get()) {
            return FLUID;
        }
        if (kind == AcervusRegistry.ENERGY_HEAP_ITEM.get()) {
            return ENERGY;
        }
        if (Mods.mekanism() && kind == io.github.capsicum0907.acervus.gas.GasHeap.ITEM.get()) {
            return CHEMICAL;
        }
        return ITEM;
    }
}
