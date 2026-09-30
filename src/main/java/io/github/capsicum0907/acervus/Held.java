package io.github.capsicum0907.acervus;

import java.util.function.Supplier;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntityType;

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
    private final boolean gives;

    protected Held(HolderLookup.Provider registries, Supplier<ItemStack> where, boolean gives) {
        this.registries = registries;
        this.where = where;
        this.gives = gives;
    }

    /**
     * Whether anything may come out, decided by <em>where the item is</em> rather than
     * by what it is.
     *
     * <p>A heap in a pocket does not give, and the reason was never the item — it was
     * that reaching a trillion of anything from an inventory slot, with nothing to
     * place and nowhere to stand, ends every reason to build a storage room. The price
     * is putting a block down. A heap slotted into a {@link HorreumBlockEntity} has had
     * that price paid, by the controller, so the same reading code answers the other
     * way. One field, set where the heap is found.
     */
    @Override
    public final boolean gives() {
        return gives;
    }

    public abstract void draw(Vessel vessel);

    public abstract boolean emptyContainer(ItemStack stack);

    /** Which block this is the item of; the written contents have to name it. */
    protected abstract BlockEntityType<?> type();

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
     *
     * <p><b>Through {@code setBlockEntityData}, never {@code CustomData.of}.</b> The
     * component is persisted with {@code CustomData.CODEC_WITH_ID}, which refuses a tag
     * with no {@code id} naming the block entity — and refuses it while the player's
     * inventory is being saved, which takes the world down. Nothing catches this
     * earlier: the network codec does not check, so a heap written by hand looks
     * perfectly well until the first autosave.
     */
    protected final void write(CompoundTag tag, boolean empty) {
        BlockItem.setBlockEntityData(item(), type(), empty ? new CompoundTag() : tag);
    }
}
