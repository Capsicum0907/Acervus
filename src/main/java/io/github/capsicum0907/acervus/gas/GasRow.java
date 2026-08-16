package io.github.capsicum0907.acervus.gas;

import io.github.capsicum0907.acervus.client.HeapContentsTooltip;

import mekanism.api.chemical.ChemicalStack;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * A gas heap read as a tooltip row.
 *
 * <p>It is here rather than beside the other three because it is the only one that
 * names a chemical, and nothing outside this package may. The caller reaches it only
 * after {@code Mods.mekanism()}, so a game without Mekanism never loads it.
 *
 * <p>A chemical carries its own icon and its own tint, unlike a fluid, so the sprite
 * is settled here and the drawing side has nothing to look up.
 */
public final class GasRow {
    private GasRow() {
    }

    /** Null when the heap is empty, as with every other kind. */
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
