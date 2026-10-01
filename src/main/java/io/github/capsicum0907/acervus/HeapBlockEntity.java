package io.github.capsicum0907.acervus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
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
    private static final String SMALLEST = "Smallest";

    private ItemStack sample = ItemStack.EMPTY;
    private long amount;
    private long unit = 1L;
    private Item smallest;
    private CompoundTag unreadable;
    private String unreadableSmallest;
    private boolean locked;

    private final HeapItemHandler handler = new HeapItemHandler(this);

    public HeapBlockEntity(BlockPos pos, BlockState state) {
        super(AcervusRegistry.HEAP_ENTITY.get(), pos, state);
    }

    public boolean isEmpty() {
        return (sample.isEmpty() && unreadable == null) || amount <= 0;
    }

    @Override
    public boolean unreadable() {
        return unreadable != null;
    }

    @Override
    public String unreadableId() {
        return unreadable == null ? "" : unreadable.getString("id");
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
        if (stack.isEmpty() || unreadable != null) {
            return false;
        }
        return sample.isEmpty() || Compression.sameKind(sample, stack);
    }

    public boolean holds(ItemStack stack) {
        return unreadable == null && !isEmpty() && !stack.isEmpty() && Compression.sameKind(sample, stack);
    }

    public int insert(ItemStack stack, boolean simulate) {
        if (!accepts(stack)) {
            return 0;
        }
        settleUnit();
        long each = each(stack);
        int taken = (int) Math.min(roomFor(stack), stack.getCount());
        if (taken <= 0 || simulate) {
            return Math.max(taken, 0);
        }

        if (sample.isEmpty()) {
            sample = stack.copyWithCount(1);
            amount = 0L;
            unit = Compression.unitOf(sample.getItem());
            settleUnit();
        }
        amount += taken * each;
        changed();
        return taken;
    }

    public ItemStack extract(int amount, boolean simulate) {
        if (unreadable != null || isEmpty() || amount <= 0) {
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
                smallest = null;
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
        if (unreadable != null || on == locked || (on && !canLock())) {
            return;
        }
        locked = on;
        if (!on && amount <= 0) {
            sample = ItemStack.EMPTY;
            smallest = null;
        }
        changed();
    }

    @Override
    public long roomFor(ItemStack stack) {
        if (!accepts(stack)) {
            return 0L;
        }
        boolean first = sample.isEmpty();
        long registered = first ? Compression.unitOf(stack.getItem()) : unit;
        long space = Math.max(0L, Compression.times(capacity(), registered) - (first ? 0L : amount));
        return space / each(stack);
    }

    private long each(ItemStack stack) {
        if (sample.isEmpty() || stack.is(sample.getItem())) {
            return sample.isEmpty() ? Compression.unitOf(stack.getItem()) : unit;
        }
        return Compression.unitOf(stack.getItem());
    }

    private boolean settleUnit() {
        if (!Compression.ready() || sample.isEmpty()) {
            return false;
        }
        boolean changed = false;
        long current = Compression.unitOf(sample.getItem());
        if (current != unit) {
            amount = Compression.rebase(amount, unit, current);
            unit = current;
            changed = true;
        }
        Item least = Compression.chainOf(sample.getItem()).get(0).item();
        Item wanted = least == sample.getItem() ? null : least;
        if (wanted != smallest) {
            smallest = wanted;
            changed = true;
        }
        return changed;
    }

    @Override
    public void settle() {
        if (settleUnit()) {
            changed();
        }
    }

    @Override
    public long count(boolean least) {
        return least && hasSmaller() ? amount : count();
    }

    @Override
    public boolean hasSmaller() {
        return smallest != null && !sample.isEmpty();
    }

    @Override
    public ItemStack stack(boolean least) {
        if (!least || !hasSmaller()) {
            return stack();
        }
        ItemStack shown = new ItemStack(smallest);
        shown.setCount((int) Math.min(Math.min(amount, shown.getMaxStackSize()), Integer.MAX_VALUE));
        return amount <= 0 ? ItemStack.EMPTY : shown;
    }

    @Override
    public ItemStack extract(int wanted, boolean simulate, boolean least) {
        if (!least || !hasSmaller()) {
            return extract(wanted, simulate);
        }
        if (unreadable != null || isEmpty() || wanted <= 0) {
            return ItemStack.EMPTY;
        }
        settleUnit();
        if (!hasSmaller()) {
            return extract(wanted, simulate);
        }
        int taken = (int) Math.min(Math.min(wanted, amount), Integer.MAX_VALUE);
        ItemStack out = new ItemStack(smallest, taken);
        if (simulate) {
            return out;
        }
        amount -= taken;
        if (amount <= 0) {
            if (!locked) {
                sample = ItemStack.EMPTY;
                smallest = null;
            }
            amount = 0L;
        }
        changed();
        return out;
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
        if (smallest != null) {
            tag.putString(SMALLEST, BuiltInRegistries.ITEM.getKey(smallest).toString());
        } else if (unreadableSmallest != null) {
            tag.putString(SMALLEST, unreadableSmallest);
        }
        tag.putBoolean(LOCKED, locked);
        if (!sample.isEmpty()) {
            tag.put(SAMPLE, sample.save(registries));
        } else if (unreadable != null) {
            tag.put(SAMPLE, unreadable.copy());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        sample = tag.contains(SAMPLE)
                ? ItemStack.parse(registries, tag.getCompound(SAMPLE)).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        unreadable = tag.contains(SAMPLE) && sample.isEmpty() ? tag.getCompound(SAMPLE).copy() : null;
        unreadableSmallest = unreadable != null && tag.contains(SMALLEST) ? tag.getString(SMALLEST) : null;
        boolean known = !sample.isEmpty() || unreadable != null;
        locked = tag.getBoolean(LOCKED) && known;
        amount = !known ? 0L
                : tag.contains(AMOUNT) ? tag.getLong(AMOUNT) : tag.getLong(LEGACY_COUNT);
        unit = Math.max(1L, tag.contains(UNIT) ? tag.getLong(UNIT) : 1L);
        ResourceLocation least = tag.contains(SMALLEST) ? ResourceLocation.tryParse(tag.getString(SMALLEST)) : null;
        smallest = least == null ? null : BuiltInRegistries.ITEM.getOptional(least).orElse(null);
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
