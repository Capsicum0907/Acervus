package io.github.capsicum0907.acervus;

import java.util.function.Supplier;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntityType;

public abstract class Held implements Heaped {
    protected static final String SAMPLE = "Sample";
    protected static final String AMOUNT = "Amount";
    protected static final String LOCKED = "Locked";

    protected final HolderLookup.Provider registries;
    private final Supplier<ItemStack> where;
    private final boolean gives;

    protected Held(HolderLookup.Provider registries, Supplier<ItemStack> where, boolean gives) {
        this.registries = registries;
        this.where = where;
        this.gives = gives;
    }

    @Override
    public final boolean gives() {
        return gives;
    }

    public abstract void draw(Vessel vessel);

    public abstract boolean emptyContainer(ItemStack stack);

    protected abstract BlockEntityType<?> type();

    protected final ItemStack item() {
        return where.get();
    }

    protected final CompoundTag tag() {
        return item().getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).copyTag();
    }

    protected final void write(CompoundTag tag, boolean empty) {
        BlockItem.setBlockEntityData(item(), type(), empty && !tag.getBoolean(LOCKED) ? new CompoundTag() : tag);
    }

    protected final void emptied(CompoundTag tag) {
        if (!tag.getBoolean(LOCKED)) {
            tag.remove(SAMPLE);
        }
    }

    @Override
    public boolean locked() {
        return tag().getBoolean(LOCKED);
    }

    @Override
    public boolean canLock() {
        return hasKinds() && !isEmpty();
    }

    @Override
    public void lock(boolean on) {
        if (on == locked() || (on && !canLock())) {
            return;
        }
        CompoundTag tag = tag();
        if (on) {
            tag.putBoolean(LOCKED, true);
            write(tag, false);
            return;
        }
        tag.remove(LOCKED);
        boolean empty = amount() <= 0;
        if (empty) {
            tag.remove(SAMPLE);
        }
        write(tag, empty);
    }
}
