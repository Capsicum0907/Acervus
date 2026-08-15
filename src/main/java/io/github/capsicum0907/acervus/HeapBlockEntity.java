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
 */
public class HeapBlockEntity extends BlockEntity {
    private static final String SAMPLE = "Sample";
    private static final String COUNT = "Count";

    private ItemStack sample = ItemStack.EMPTY;
    private int count;

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

    public int count() {
        return count;
    }

    public int capacity() {
        return AcervusConfig.CAPACITY.get();
    }

    /** What a heap will show the outside world at once: a stack of it, at most. */
    public ItemStack stack() {
        if (isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack shown = sample.copy();
        shown.setCount(Math.min(count, sample.getMaxStackSize()));
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
        int room = capacity() - count;
        int taken = Math.min(room, stack.getCount());
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
        int taken = Math.min(Math.min(amount, count), sample.getMaxStackSize());
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

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!sample.isEmpty()) {
            tag.put(SAMPLE, sample.save(registries));
            tag.putInt(COUNT, count);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        sample = tag.contains(SAMPLE)
                ? ItemStack.parse(registries, tag.getCompound(SAMPLE)).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        count = sample.isEmpty() ? 0 : tag.getInt(COUNT);
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
