package io.github.capsicum0907.acervus;

import java.util.function.Supplier;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class CarriedHeap implements Pile {
    private static final String SAMPLE = "Sample";
    private static final String AMOUNT = "Amount";
    private static final String LOCKED = "Locked";
    private static final String UNIT = "Unit";

    private static final String LEGACY_COUNT = "Count";

    private final HolderLookup.Provider registries;
    private final Supplier<ItemStack> where;

    private CustomData read;
    private ItemStack parsed = ItemStack.EMPTY;

    private final boolean gives;

    private CarriedHeap(HolderLookup.Provider registries, Supplier<ItemStack> where, boolean gives) {
        this.registries = registries;
        this.where = where;
        this.gives = gives;
    }

    public static CarriedHeap of(HolderLookup.Provider registries, ItemStack stack) {
        return new CarriedHeap(registries, () -> stack, false);
    }

    public static CarriedHeap of(Player player, ItemStack stack) {
        return of(player.level().registryAccess(), stack);
    }

    public static CarriedHeap inHand(Player player, InteractionHand hand) {
        return new CarriedHeap(player.level().registryAccess(), () -> player.getItemInHand(hand), false);
    }

    public static CarriedHeap stored(HolderLookup.Provider registries, ItemStack stack) {
        return new CarriedHeap(registries, () -> stack, true);
    }

    @Override
    public boolean gives() {
        return gives;
    }

    @Override
    public boolean isEmpty() {
        return (sample().isEmpty() && !unreadable()) || amount(tag()) <= 0;
    }

    @Override
    public boolean unreadable() {
        return tag().contains(SAMPLE) && sample().isEmpty();
    }

    @Override
    public String unreadableId() {
        return unreadable() ? tag().getCompound(SAMPLE).getString("id") : "";
    }

    @Override
    public ItemStack sample() {
        CustomData data = where.get().getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY);
        if (data.equals(read)) {
            return parsed;
        }
        read = data;
        CompoundTag tag = data.copyTag();
        parsed = tag.contains(SAMPLE)
                ? ItemStack.parse(registries, tag.getCompound(SAMPLE)).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        return parsed;
    }

    @Override
    public long count() {
        CompoundTag tag = tag();
        return amount(tag) / unit(tag);
    }

    private static long amount(CompoundTag tag) {
        return Counts.stored(tag, AMOUNT, LEGACY_COUNT);
    }

    private static long unit(CompoundTag tag) {
        return Math.max(1L, tag.contains(UNIT) ? tag.getLong(UNIT) : 1L);
    }

    private CompoundTag settled() {
        CompoundTag tag = tag();
        ItemStack sample = sample();
        if (Compression.ready() && !sample.isEmpty()) {
            long current = Compression.unitOf(sample.getItem());
            long stored = unit(tag);
            if (current != stored) {
                tag.putLong(AMOUNT, Compression.rebase(amount(tag), stored, current));
                tag.putLong(UNIT, current);
                tag.remove(LEGACY_COUNT);
            }
        }
        return tag;
    }

    public long capacity() {
        return AcervusConfig.CAPACITY.get();
    }

    @Override
    public long room() {
        CompoundTag tag = tag();
        long unit = unit(tag);
        return Math.max(0L, Compression.times(capacity(), unit) - amount(tag)) / unit;
    }

    @Override
    public long limit() {
        return Compression.times(capacity(), unit(tag())) / unit(tag());
    }

    @Override
    public ItemStack stack() {
        ItemStack sample = sample();
        if (sample.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return sample.copyWithCount((int) Math.min(count(), sample.getMaxStackSize()));
    }

    @Override
    public boolean accepts(ItemStack stack) {
        if (stack.isEmpty() || unreadable()) {
            return false;
        }
        return sample().isEmpty() || Compression.sameKind(sample(), stack);
    }

    public boolean holds(ItemStack stack) {
        return !unreadable() && !isEmpty() && !stack.isEmpty() && Compression.sameKind(sample(), stack);
    }

    @Override
    public int insert(ItemStack stack, boolean simulate) {
        if (!accepts(stack)) {
            return 0;
        }
        CompoundTag tag = settled();
        boolean first = sample().isEmpty();
        long registered = first ? Compression.unitOf(stack.getItem()) : unit(tag);
        long each = first || stack.is(sample().getItem()) ? registered : Compression.unitOf(stack.getItem());
        long held = first ? 0L : amount(tag);
        int taken = (int) Math.min(roomFor(stack), stack.getCount());
        if (taken <= 0 || simulate) {
            return Math.max(taken, 0);
        }

        ItemStack heap = where.get();
        if (first) {
            tag.put(SAMPLE, stack.copyWithCount(1).save(registries));
        }
        tag.putLong(AMOUNT, held + taken * each);
        tag.putLong(UNIT, registered);
        tag.remove(LEGACY_COUNT);
        write(heap, tag);
        return taken;
    }

    @Override
    public ItemStack extract(int amount, boolean simulate) {
        if (!gives() || unreadable() || isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        int taken = (int) Math.min(Math.min(amount, count()), Integer.MAX_VALUE);
        ItemStack out = sample().copyWithCount(taken);
        if (simulate) {
            return out;
        }

        ItemStack heap = where.get();
        CompoundTag tag = settled();
        long unit = unit(tag);
        long left = amount(tag) - taken * unit;
        tag.putLong(UNIT, unit);
        tag.remove(LEGACY_COUNT);
        if (left <= 0) {
            if (!tag.getBoolean(LOCKED)) {
                tag.remove(SAMPLE);
            }
            left = 0;
        }
        tag.putLong(AMOUNT, left);
        write(heap, tag);
        return out;
    }

    @Override
    public void setChanged() {
    }

    @Override
    public long roomFor(ItemStack stack) {
        if (!accepts(stack)) {
            return 0L;
        }
        CompoundTag tag = settled();
        boolean first = sample().isEmpty();
        long registered = first ? Compression.unitOf(stack.getItem()) : unit(tag);
        long each = first || stack.is(sample().getItem()) ? registered : Compression.unitOf(stack.getItem());
        long space = Math.max(0L, Compression.times(capacity(), registered) - (first ? 0L : amount(tag)));
        return space / each;
    }

    @Override
    public boolean locked() {
        return tag().getBoolean(LOCKED);
    }

    @Override
    public boolean canLock() {
        return !isEmpty();
    }

    @Override
    public void lock(boolean on) {
        if (unreadable() || on == locked() || (on && !canLock())) {
            return;
        }
        CompoundTag tag = tag();
        if (on) {
            tag.putBoolean(LOCKED, true);
        } else {
            tag.remove(LOCKED);
            if (amount(tag) <= 0) {
                tag.remove(SAMPLE);
            }
        }
        write(where.get(), tag);
    }

    private CompoundTag tag() {
        return where.get().getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).copyTag();
    }

    private void write(ItemStack heap, CompoundTag tag) {
        BlockItem.setBlockEntityData(heap, AcervusRegistry.HEAP_ENTITY.get(),
                tag.contains(SAMPLE) ? tag : new CompoundTag());
    }
}
