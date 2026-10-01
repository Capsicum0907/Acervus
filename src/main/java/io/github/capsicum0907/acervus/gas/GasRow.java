package io.github.capsicum0907.acervus.gas;

import io.github.capsicum0907.acervus.client.HeapContentsTooltip;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public final class GasRow {
    private GasRow() {
    }

    public static HeapContentsTooltip.Row of(HolderLookup.Provider registries, ItemStack heap) {
        HeldChemicalHeap held = HeldChemicalHeap.of(registries, heap);
        if (held.unreadable()) {
            return new HeapContentsTooltip.Row(ItemStack.EMPTY, FluidStack.EMPTY,
                    io.github.capsicum0907.acervus.client.Missing.LOCATION, true, 0xFFFFFFFF,
                    held.contentName(), held.brief(held.amount()));
        }
        if (held.isEmpty() || held.contentTexture() == null) {
            return null;
        }
        return new HeapContentsTooltip.Row(ItemStack.EMPTY, FluidStack.EMPTY,
                held.contentTexture(), true, held.contentTint(),
                held.contentName(), held.brief(held.amount()));
    }
}
