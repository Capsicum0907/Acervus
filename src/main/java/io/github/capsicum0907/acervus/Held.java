package io.github.capsicum0907.acervus;

import java.util.function.Supplier;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * A fluid, energy or gas heap that is being carried rather than placed.
 *
 * <p>The contents ride on the item — that is how a heap survives being mined — so a
 * heap in a pocket is a full heap with nothing reading it. This is the reading, in
 * the same terms a readout already speaks, so one screen serves both.
 *
 * <p><b>It takes and does not give.</b> {@link #gives()} is false, which is the same
 * rule the item heap follows for the same reason: reaching a trillion of anything
 * from an inventory slot, with no block to place and nothing to stand next to, ends
 * every reason to build a storage room. What a carried one can still do is
 * {@link #draw} — empty a bucket, a battery or a tank into itself — because pouring
 * something in is the direction that costs nothing.
 *
 * <p>The stack is fetched afresh every time rather than held: the item can be moved,
 * swapped or dropped while a screen is open on it, and writing into a stack that is
 * no longer anywhere would lose whatever was written.
 */
public abstract class Held implements Heaped {
    protected static final String SAMPLE = "Sample";
    protected static final String AMOUNT = "Amount";

    protected final HolderLookup.Provider registries;
    private final Supplier<ItemStack> where;

    protected Held(HolderLookup.Provider registries, Supplier<ItemStack> where) {
        this.registries = registries;
        this.where = where;
    }

    @Override
    public final boolean gives() {
        return false;
    }

    /**
     * Empties what is in the vessel into the heap, as far as it will go, and says so
     * through the vessel's own direction — which is always {@link Vessel.Flow#IN} here,
     * so none of the latching the blocks need applies.
     */
    public abstract void draw(Vessel vessel);

    protected final ItemStack item() {
        return where.get();
    }

    protected final CompoundTag tag() {
        return item().getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).copyTag();
    }

    /**
     * Writes the contents back, dropping the component altogether once there is nothing
     * left, so that emptied heaps stack with the other empty ones instead of looking
     * different for carrying an empty tag.
     */
    protected final void write(CompoundTag tag, boolean empty) {
        if (empty) {
            item().remove(DataComponents.BLOCK_ENTITY_DATA);
        } else {
            item().set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
        }
    }
}
