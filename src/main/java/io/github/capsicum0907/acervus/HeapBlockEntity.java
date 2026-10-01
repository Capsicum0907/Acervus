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

public class HeapBlockEntity extends BlockEntity implements Pile {
    private static final String SAMPLE = "Sample";
    private static final String LOCKED = "Locked";

    private static final String AMOUNT = "Amount";
    private static final String LEGACY_COUNT = "Count";
    private static final String UNIT = "Unit";

    private ItemStack sample = ItemStack.EMPTY;
    private long amount;
    private long unit = 1L;
    private boolean locked;

    private final HeapItemHandler handler = new HeapItemHandler(this);

    public HeapBlockEntity(BlockPos pos, BlockState state) {
        super(AcervusRegistry.HEAP_ENTITY.get(), pos, state);
    }

    public boolean isEmpty() {
        return sample.isEmpty() || amount <= 0;
    }

    public ItemStack sample() {
        return sample.copy();
    }

    public long count() {
        return amount / unit;
    }

    public long capacity() {
        return AcervusConfig.CAPACITY.get();
    }

    public long room() {
        return Math.max(0L, Compression.times(capacity(), unit) - amount) / unit;
    }

    public HeapItemHandler handler() {
        return handler;
    }

    public ItemStack stack() {
        return portion(sample.isEmpty() ? 0 : sample.getMaxStackSize());
    }

    public ItemStack contents() {
        return portion(Integer.MAX_VALUE);
    }

    private ItemStack portion(long most) {
        if (isEmpty() || most <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack shown = sample.copy();
        shown.setCount((int) Math.min(Math.min(count(), most), Integer.MAX_VALUE));
        return shown;
    }

    public boolean accepts(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return sample.isEmpty() || ItemStack.isSameItemSameComponents(sample, stack);
    }

    public boolean holds(ItemStack stack) {
        return !isEmpty() && !stack.isEmpty() && ItemStack.isSameItemSameComponents(sample, stack);
    }

    public int insert(ItemStack stack, boolean simulate) {
        if (!accepts(stack)) {
            return 0;
        }
        settleUnit();
        int taken = (int) Math.min(room(), stack.getCount());
        if (taken <= 0 || simulate) {
            return Math.max(taken, 0);
        }

        if (sample.isEmpty()) {
            sample = stack.copyWithCount(1);
            amount = 0L;
            unit = Compression.unitOf(sample.getItem());
        }
        amount += taken * unit;
        changed();
        return taken;
    }

    public ItemStack extract(int amount, boolean simulate) {
        if (isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        settleUnit();
        int taken = (int) Math.min(Math.min(amount, count()), Integer.MAX_VALUE);
        ItemStack out = sample.copyWithCount(taken);
        if (simulate) {
            return out;
        }

        this.amount -= taken * unit;
        if (this.amount <= 0) {
            if (!locked) {
                sample = ItemStack.EMPTY;
            }
            this.amount = 0L;
        }
        changed();
        return out;
    }

    @Override
    public boolean locked() {
        return locked;
    }

    @Override
    public boolean canLock() {
        return !isEmpty();
    }

    @Override
    public void lock(boolean on) {
        if (on == locked || (on && !canLock())) {
            return;
        }
        locked = on;
        if (!on && amount <= 0) {
            sample = ItemStack.EMPTY;
        }
        changed();
    }

    private void settleUnit() {
        if (!Compression.ready() || sample.isEmpty()) {
            return;
        }
        long current = Compression.unitOf(sample.getItem());
        if (current != unit) {
            amount = Compression.rebase(amount, unit, current);
            unit = current;
        }
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong(AMOUNT, amount);
        tag.putLong(UNIT, unit);
        tag.putBoolean(LOCKED, locked);
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
        locked = tag.getBoolean(LOCKED) && !sample.isEmpty();
        amount = sample.isEmpty() ? 0L
                : tag.contains(AMOUNT) ? tag.getLong(AMOUNT) : tag.getLong(LEGACY_COUNT);
        unit = Math.max(1L, tag.contains(UNIT) ? tag.getLong(UNIT) : 1L);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
