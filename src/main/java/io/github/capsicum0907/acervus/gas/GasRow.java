package io.github.capsicum0907.acervus.gas;

import io.github.capsicum0907.acervus.client.HeapContentsTooltip;

import mekanism.api.chemical.ChemicalStack;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public final class GasRow {
    private GasRow() {
    }

    public static HeapContentsTooltip.Row of(HolderLookup.Provider registries, ItemStack heap) {
        HeldChemicalHeap held = HeldChemicalHeap.of(registries, heap);
        if (held.isEmpty()) {
            return null;
        }
        ChemicalStack sample = held.sample();
        return new HeapContentsTooltip.Row(ItemStack.EMPTY, FluidStack.EMPTY,
                sample.getChemical().getIcon(), true, sample.getChemicalTint(),
                held.contentName(), held.brief(held.amount()));
    }
}
