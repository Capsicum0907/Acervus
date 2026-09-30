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
        BlockItem.setBlockEntityData(item(), type(), empty ? new CompoundTag() : tag);
    }
}
