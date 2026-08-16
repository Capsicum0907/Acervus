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
public class HeapBlockEntity extends BlockEntity implements Pile {
    private static final String SAMPLE = "Sample";

    /**
     * The same name every heap uses for how much it holds.
     *
     * <p>It was {@code Count} here, {@code Amount} on two others and {@code Stored} on
     * the fourth — three names for one idea, which meant that {@code /data merge block}
     * worked on one heap and silently did nothing on the rest. The old name is still
     * read so that heaps saved before this keep their contents.
     */
    private static final String AMOUNT = "Amount";
    private static final String LEGACY_COUNT = "Count";

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

    /** One stack of what is inside, at most. What a person sees in the screen. */
    public ItemStack stack() {
        return portion(sample.isEmpty() ? 0 : sample.getMaxStackSize());
    }

    /**
     * Everything inside, as one stack. What automation is shown, because an item
     * handler is allowed to report more than a stack and a heap that did not would be
     * telling every pipe and every storage network that it holds sixty-four.
     */
    public ItemStack contents() {
        return portion(Integer.MAX_VALUE);
    }

    private ItemStack portion(long most) {
        if (isEmpty() || most <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack shown = sample.copy();
        // An item stack counts in an int however wide the total is.
        shown.setCount((int) Math.min(Math.min(count, most), Integer.MAX_VALUE));
        return shown;
    }

    /**
     * Whether this could go in. An empty heap accepts anything, which is what lets a
     * pipe or a hopper decide what it is for.
     */
    public boolean accepts(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return isEmpty() || ItemStack.isSameItemSameComponents(sample, stack);
    }

    /**
     * Whether this is <em>already</em> what the heap holds — a stricter question than
     * {@link #accepts}, and the one a person should be asked.
     *
     * <p>The difference is the empty heap. Automation deciding what an empty heap is
     * for is the point of automation; a player who right-clicked to look inside and
     * silently committed the block to whatever happened to be in their hand has been
     * caught by it instead.
     */
    public boolean holds(ItemStack stack) {
        return !isEmpty() && !stack.isEmpty() && ItemStack.isSameItemSameComponents(sample, stack);
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
     * @return what was taken out: as much as was asked for, if there is that much.
     *         Not clamped to a stack — see {@link HeapItemHandler} for why not, and
     *         call it with a stack's worth when a stack is what is wanted.
     */
    public ItemStack extract(int amount, boolean simulate) {
        if (isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        int taken = (int) Math.min(Math.min(amount, count), Integer.MAX_VALUE);
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
        tag.putLong(AMOUNT, count);
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
        // the total widened comes back without a migration step. The old key is read
        // when the new one is absent, for the same reason.
        count = sample.isEmpty() ? 0L
                : tag.contains(AMOUNT) ? tag.getLong(AMOUNT) : tag.getLong(LEGACY_COUNT);
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
