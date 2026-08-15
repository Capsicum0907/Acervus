package io.github.capsicum0907.acervus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What a heap holds: one kind of item, and how many.
 *
 * <p>The count is a plain int rather than a list of slots. A slot is a place to put
 * up to a stack, and this block's entire reason for existing is that it does not
 * work that way — dividing two billion items into slots would be thirty-one million
 * of them.
 *
 * <p>The sample always has a count of one. It is the identity of what is stored —
 * item and components — and nothing else; how many there are is the other field.
 * Keeping the two apart is what stops a stack size from leaking into the total.
 *
 * <p><b>The count is a long, and that costs nothing.</b> An item stack counts with
 * an int, but no item stack ever carries this number: what leaves a heap is a stack
 * of at most a stack, worked out from the total rather than being it. The total is
 * therefore free to be as wide as it likes, and the only care needed is at the three
 * places the outside world asks in ints, where the answer is clamped on the way out.
 *
 * <p><b>This must never implement {@link net.minecraft.world.Container}.</b> A
 * hopper prefers the container path over the item handler when a block offers both,
 * and a container that tries to describe billions in slots of sixty-four is exactly
 * the shape that goes wrong. InfChest had to add a mixin to force hoppers off that
 * path; not being a container at all is the same fix, made earlier.
 */
public class HeapBlockEntity extends BlockEntity {
    private static final String SAMPLE = "Sample";
    private static final String COUNT = "Count";

    private ItemStack sample = ItemStack.EMPTY;
    private long count;

    private final HeapItemHandler handler = new HeapItemHandler(this);

    public HeapBlockEntity(BlockPos pos, BlockState state) {
        super(AcervusRegistry.HEAP_ENTITY.get(), pos, state);
    }

    public boolean isEmpty() {
        return sample.isEmpty() || count <= 0;
    }

    /** The identity of what is stored, with a count of one. Never handed out to be mutated. */
    public ItemStack sample() {
        return sample.copy();
    }

    public long count() {
        return count;
    }

    public long capacity() {
        return AcervusConfig.CAPACITY.get();
    }

    public long room() {
        return Math.max(0L, capacity() - count);
    }

    /**
     * One handler for the whole block, handed to every side.
     *
     * <p>Not one per query. Two handler objects for one heap are two places a stale
     * answer could live, and telling two askers different things about the same
     * contents is how a storage block ends up creating items out of nothing.
     */
    public HeapItemHandler handler() {
        return handler;
    }

    /** What a heap will show the outside world at once: a stack of it, at most. */
    public ItemStack stack() {
        if (isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack shown = sample.copy();
        // Clamped on the way out, which is the only place the width of the total matters.
        shown.setCount((int) Math.min(count, sample.getMaxStackSize()));
        return shown;
    }

    public boolean accepts(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return isEmpty() || ItemStack.isSameItemSameComponents(sample, stack);
    }

    /**
     * @return how many of the stack were taken, which may be none
     */
    public int insert(ItemStack stack, boolean simulate) {
        if (!accepts(stack)) {
            return 0;
        }
        // The offered stack counts in an int, so what is taken always fits in one
        // however wide the total is.
        int taken = (int) Math.min(room(), stack.getCount());
        if (taken <= 0 || simulate) {
            return Math.max(taken, 0);
        }

        if (isEmpty()) {
            sample = stack.copyWithCount(1);
            count = 0;
        }
        count += taken;
        changed();
        return taken;
    }

    /**
     * @return what was taken out, never more than one stack of it
     */
    public ItemStack extract(int amount, boolean simulate) {
        if (isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        int taken = (int) Math.min(Math.min(amount, count), sample.getMaxStackSize());
        ItemStack out = sample.copyWithCount(taken);
        if (simulate) {
            return out;
        }

        count -= taken;
        if (count <= 0) {
            // The sample is dropped with the last item: a heap that remembers what it
            // used to hold would refuse the next thing put into it for no visible reason.
            sample = ItemStack.EMPTY;
            count = 0;
        }
        changed();
        return out;
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            // The contents are drawn, so a client that does not hear about a change
            // keeps drawing the old one.
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    /**
     * The count is written even when it is zero, and that is not a formality.
     *
     * <p>An update packet carrying an empty tag is discarded before it reaches the
     * block entity — {@code IBlockEntityExtension#onDataPacket} guards on
     * {@code !tag.isEmpty()}. A heap that had just been emptied would therefore save
     * nothing, send nothing the client would accept, and go on being drawn holding
     * whatever it held a moment ago. Writing one field always is what makes emptying
     * a thing the client is told about.
     */
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong(COUNT, count);
        if (!sample.isEmpty()) {
            tag.put(SAMPLE, sample.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        sample = tag.contains(SAMPLE)
                ? ItemStack.parse(registries, tag.getCompound(SAMPLE)).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        // getLong reads a tag that was written as an int too, so a heap saved before
        // the total widened comes back without a migration step.
        count = sample.isEmpty() ? 0L : tag.getLong(COUNT);
    }

    /** The client is sent the same fields, because it draws them. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
