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
        return sample().isEmpty() || count() <= 0;
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
        return tag.contains(AMOUNT) ? tag.getLong(AMOUNT) : tag.getLong(LEGACY_COUNT);
    }

    public long capacity() {
        return AcervusConfig.CAPACITY.get();
    }

    @Override
    public long room() {
        return Math.max(0L, capacity() - count());
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
        if (stack.isEmpty()) {
            return false;
        }
        return sample().isEmpty() || ItemStack.isSameItemSameComponents(sample(), stack);
    }

    public boolean holds(ItemStack stack) {
        return !isEmpty() && !stack.isEmpty() && ItemStack.isSameItemSameComponents(sample(), stack);
    }

    @Override
    public int insert(ItemStack stack, boolean simulate) {
        if (!accepts(stack)) {
            return 0;
        }
        int taken = (int) Math.min(room(), stack.getCount());
        if (taken <= 0 || simulate) {
            return Math.max(taken, 0);
        }

        ItemStack heap = where.get();
        CompoundTag tag = tag();
        long held = isEmpty() ? 0L : count();
        if (isEmpty()) {
            tag.put(SAMPLE, stack.copyWithCount(1).save(registries));
        }
        tag.putLong(AMOUNT, held + taken);
        tag.remove(LEGACY_COUNT);
        write(heap, tag);
        return taken;
    }

    @Override
    public ItemStack extract(int amount, boolean simulate) {
        if (!gives() || isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        int taken = (int) Math.min(Math.min(amount, count()), Integer.MAX_VALUE);
        ItemStack out = sample().copyWithCount(taken);
        if (simulate) {
            return out;
        }

        ItemStack heap = where.get();
        CompoundTag tag = tag();
        long left = count() - taken;
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
    public boolean locked() {
        return tag().getBoolean(LOCKED);
    }

    @Override
    public boolean canLock() {
        return !isEmpty();
    }

    @Override
    public void lock(boolean on) {
        if (on == locked() || (on && !canLock())) {
            return;
        }
        CompoundTag tag = tag();
        if (on) {
            tag.putBoolean(LOCKED, true);
        } else {
            tag.remove(LOCKED);
            if (count() <= 0) {
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
