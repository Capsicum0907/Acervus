package io.github.capsicum0907.acervus;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

public final class HeldFluidHeap extends Held {
    private HeldFluidHeap(HolderLookup.Provider registries, java.util.function.Supplier<ItemStack> where,
            boolean gives) {
        super(registries, where, gives);
    }

    public static HeldFluidHeap of(HolderLookup.Provider registries, ItemStack stack) {
        return new HeldFluidHeap(registries, () -> stack, false);
    }

    public static HeldFluidHeap inHand(Player player, InteractionHand hand) {
        return new HeldFluidHeap(player.level().registryAccess(), () -> player.getItemInHand(hand), false);
    }

    public static HeldFluidHeap stored(HolderLookup.Provider registries, ItemStack stack) {
        return new HeldFluidHeap(registries, () -> stack, true);
    }

    public FluidStack sample() {
        CompoundTag tag = tag();
        return tag.contains(SAMPLE)
                ? FluidStack.parse(registries, tag.getCompound(SAMPLE)).orElse(FluidStack.EMPTY)
                : FluidStack.EMPTY;
    }

    @Override
    public long amount() {
        return Counts.stored(tag(), AMOUNT);
    }

    @Override
    public long capacity() {
        return AcervusConfig.FLUID_CAPACITY.get();
    }

    @Override
    public boolean isEmpty() {
        return (sample().isEmpty() && !unreadable()) || amount() <= 0;
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
    public Component contentName() {
        if (unreadable()) {
            return Component.literal(unreadableId());
        }
        FluidStack sample = sample();
        return sample.isEmpty() ? Component.empty() : sample.getHoverName();
    }

    @Override
    public String brief(long value) {
        return Counts.buckets(value);
    }

    @Override
    public String exact(long value) {
        return Counts.exactBuckets(value);
    }

    @Override
    public String power(long value) {
        return Counts.powerBuckets(value);
    }

    @Override
    public int tint() {
        return HeapColors.FLUID;
    }

    public int insert(FluidStack stack, boolean simulate) {
        FluidStack sample = sample();
        if (stack.isEmpty() || unreadable() || (!sample.isEmpty() && !FluidStack.isSameFluidSameComponents(sample, stack))) {
            return 0;
        }
        int taken = (int) Math.min(room(), stack.getAmount());
        if (taken <= 0 || simulate) {
            return Math.max(taken, 0);
        }

        CompoundTag tag = tag();
        long held = sample.isEmpty() ? 0L : amount();
        if (sample.isEmpty()) {
            tag.put(SAMPLE, stack.copyWithAmount(1).save(registries));
        }
        tag.putLong(AMOUNT, held + taken);
        write(tag, false);
        return taken;
    }

    public FluidStack extract(int wanted, boolean simulate) {
        if (!gives() || unreadable() || isEmpty() || wanted <= 0) {
            return FluidStack.EMPTY;
        }
        int taken = (int) Math.min(Math.min(wanted, amount()), Integer.MAX_VALUE);
        FluidStack out = sample().copyWithAmount(taken);
        if (simulate) {
            return out;
        }

        CompoundTag tag = tag();
        long left = amount() - taken;
        if (left <= 0) {
            emptied(tag);
            left = 0;
        }
        tag.putLong(AMOUNT, left);
        write(tag, left <= 0);
        return out;
    }

    @Override
    protected net.minecraft.world.level.block.entity.BlockEntityType<?> type() {
        return AcervusRegistry.FLUID_HEAP_ENTITY.get();
    }

    @Override
    public boolean emptyContainer(ItemStack stack) {
        return FluidHeapBlockEntity.isEmptyContainer(stack);
    }

    @Override
    public void draw(Vessel vessel) {
        IFluidHandlerItem container = vessel.held().getCapability(Capabilities.FluidHandler.ITEM);
        if (container == null) {
            return;
        }
        FluidStack offered = container.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        int taken = offered.isEmpty() ? 0 : insert(offered, false);
        if (taken <= 0) {
            return;
        }
        container.drain(offered.copyWithAmount(taken), IFluidHandler.FluidAction.EXECUTE);
        vessel.hold(container.getContainer());
    }
}
