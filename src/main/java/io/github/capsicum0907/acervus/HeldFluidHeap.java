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

/** A fluid heap being carried. See {@link Held} for the rule it follows. */
public final class HeldFluidHeap extends Held {
    private HeldFluidHeap(HolderLookup.Provider registries, java.util.function.Supplier<ItemStack> where,
            boolean gives) {
        super(registries, where, gives);
    }

    /** A heap being carried: it takes and does not give. */
    public static HeldFluidHeap of(HolderLookup.Provider registries, ItemStack stack) {
        return new HeldFluidHeap(registries, () -> stack, false);
    }

    /** The one in that hand, whatever it is at the moment of asking. */
    public static HeldFluidHeap inHand(Player player, InteractionHand hand) {
        return new HeldFluidHeap(player.level().registryAccess(), () -> player.getItemInHand(hand), false);
    }

    /** A heap slotted into a controller, which is a placed block, so it gives. */
    public static HeldFluidHeap stored(HolderLookup.Provider registries, ItemStack stack) {
        return new HeldFluidHeap(registries, () -> stack, true);
    }

    /** Identity only: the fluid and its components, always with an amount of one. */
    public FluidStack sample() {
        CompoundTag tag = tag();
        return tag.contains(SAMPLE)
                ? FluidStack.parse(registries, tag.getCompound(SAMPLE)).orElse(FluidStack.EMPTY)
                : FluidStack.EMPTY;
    }

    @Override
    public long amount() {
        return tag().getLong(AMOUNT);
    }

    @Override
    public long capacity() {
        return AcervusConfig.FLUID_CAPACITY.get();
    }

    @Override
    public boolean isEmpty() {
        return sample().isEmpty() || amount() <= 0;
    }

    @Override
    public Component contentName() {
        FluidStack sample = sample();
        return sample.isEmpty() ? Component.empty() : sample.getHoverName();
    }

    /** Buckets, because millibuckets cost three digits of every number for nothing. */
    @Override
    public String brief(long value) {
        return Counts.buckets(value);
    }

    @Override
    public String exact(long value) {
        return Counts.exactBuckets(value);
    }

    @Override
    public int tint() {
        return 0xFF7FB8C8;
    }

    /** @return how much of the offered fluid was taken, which may be none */
    public int insert(FluidStack stack, boolean simulate) {
        FluidStack sample = sample();
        if (stack.isEmpty() || (!sample.isEmpty() && !FluidStack.isSameFluidSameComponents(sample, stack))) {
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

    /**
     * @return what was taken out, which is nothing at all unless this heap
     *         {@link #gives()} — a carried one never does
     */
    public FluidStack extract(int wanted, boolean simulate) {
        if (!gives() || isEmpty() || wanted <= 0) {
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
            // Forgotten with the last drop, here as on the block: one that remembered
            // would refuse the next thing poured in with nothing to say why.
            tag.remove(SAMPLE);
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

    /**
     * Whatever is in the container goes in — not only a full one, which is the rule the
     * block needs because the block can also pour back out and has to be told which way
     * a half-empty bucket was meant to go. Here there is only one way.
     */
    @Override
    public void draw(Vessel vessel) {
        IFluidHandlerItem container = vessel.held().getCapability(Capabilities.FluidHandler.ITEM);
        if (container == null) {
            vessel.done();
            return;
        }
        FluidStack offered = container.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        int taken = offered.isEmpty() ? 0 : insert(offered, false);
        if (taken <= 0) {
            vessel.done();
            return;
        }
        container.drain(offered.copyWithAmount(taken), IFluidHandler.FluidAction.EXECUTE);
        // A bucket becomes an empty bucket: the container the capability hands back is
        // a different item from the one that went in.
        vessel.replace(container.getContainer());
        vessel.decide(Vessel.Flow.IN);
    }
}
