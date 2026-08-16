package io.github.capsicum0907.acervus;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * What a fluid heap holds: one kind of fluid, and how much of it in millibuckets.
 *
 * <p>The same shape as {@link HeapBlockEntity} and for the same reasons — a long
 * amount that the block keeps, and a sample carrying only the identity of what is
 * stored. The two are not yet one class on purpose: a third resource is what will
 * say which parts are really shared and which only look alike, and guessing that
 * from two would be guessing.
 *
 * <p>The ceiling at the edge is lower here than it looks. A {@code FluidStack}
 * counts in an int and so does every method of {@code IFluidHandler}, so what a
 * fluid heap can <em>say</em> stops at about two million buckets however much it
 * holds. As with items, the answer is to saturate rather than to wrap.
 */
public class FluidHeapBlockEntity extends BlockEntity implements Heaped, HasVessel {
    private static final String SAMPLE = "Sample";
    private static final String AMOUNT = "Amount";

    /** Identity only: the fluid and its components, always with an amount of one. */
    private FluidStack sample = FluidStack.EMPTY;
    private long amount;

    private final FluidHeapHandler handler = new FluidHeapHandler(this);

    public FluidHeapBlockEntity(BlockPos pos, BlockState state) {
        super(AcervusRegistry.FLUID_HEAP_ENTITY.get(), pos, state);
    }

    public boolean isEmpty() {
        return sample.isEmpty() || amount <= 0;
    }

    public FluidStack sample() {
        return sample.copy();
    }

    public long amount() {
        return amount;
    }

    public long capacity() {
        return AcervusConfig.FLUID_CAPACITY.get();
    }

    public long room() {
        return Math.max(0L, capacity() - amount);
    }

    /** One handler for the whole block, handed to every side. */
    public FluidHeapHandler handler() {
        return handler;
    }

    /** Everything inside, saturated at what a fluid stack can count. */
    public FluidStack contents() {
        if (isEmpty()) {
            return FluidStack.EMPTY;
        }
        return sample.copyWithAmount((int) Math.min(amount, Integer.MAX_VALUE));
    }

    public boolean accepts(FluidStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return isEmpty() || FluidStack.isSameFluidSameComponents(sample, stack);
    }

    /** Whether this is already what the heap holds — the question a person is asked. */
    public boolean holds(FluidStack stack) {
        return !isEmpty() && !stack.isEmpty() && FluidStack.isSameFluidSameComponents(sample, stack);
    }

    /** @return how much of the offered fluid was taken, which may be none */
    public int insert(FluidStack stack, boolean simulate) {
        if (!accepts(stack)) {
            return 0;
        }
        int taken = (int) Math.min(room(), stack.getAmount());
        if (taken <= 0 || simulate) {
            return Math.max(taken, 0);
        }

        if (isEmpty()) {
            sample = stack.copyWithAmount(1);
            amount = 0;
        }
        amount += taken;
        changed();
        return taken;
    }

    /** @return what was taken out: as much as was asked for, if there is that much */
    public FluidStack extract(int wanted, boolean simulate) {
        if (isEmpty() || wanted <= 0) {
            return FluidStack.EMPTY;
        }
        int taken = (int) Math.min(Math.min(wanted, amount), Integer.MAX_VALUE);
        FluidStack out = sample.copyWithAmount(taken);
        if (simulate) {
            return out;
        }

        amount -= taken;
        if (amount <= 0) {
            // Forgotten with the last drop, so the next thing poured in is not refused
            // for a reason nothing on the block explains.
            sample = FluidStack.EMPTY;
            amount = 0;
        }
        changed();
        return out;
    }

    private final Vessel vessel = new Vessel();

    @Override
    public Vessel vessel() {
        return vessel;
    }

    /**
     * Whatever is in the vessel is filled or emptied, once a tick.
     *
     * <p>Which way round is not asked: a container with something in it is poured in,
     * and one without is filled. That covers a bucket both ways without the player
     * having to say which they meant.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, FluidHeapBlockEntity heap) {
        if (level.isClientSide || heap.vessel.isEmpty()) {
            return;
        }
        ItemStack container = heap.vessel.held();

        FluidActionResult poured = FluidUtil.tryEmptyContainer(
                container, heap.handler(), Integer.MAX_VALUE, null, true);
        if (poured.isSuccess()) {
            heap.vessel.hold(poured.getResult());
            heap.changed();
            return;
        }

        FluidActionResult drawn = FluidUtil.tryFillContainer(
                container, heap.handler(), Integer.MAX_VALUE, null, true);
        if (drawn.isSuccess()) {
            heap.vessel.hold(drawn.getResult());
            heap.changed();
        }
    }

    @Override
    public Component contentName() {
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

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    /** The amount is written even when it is zero; see {@link HeapBlockEntity#saveAdditional}. */
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong(AMOUNT, amount);
        vessel.save(tag, registries);
        if (!sample.isEmpty()) {
            tag.put(SAMPLE, sample.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        sample = tag.contains(SAMPLE)
                ? FluidStack.parse(registries, tag.getCompound(SAMPLE)).orElse(FluidStack.EMPTY)
                : FluidStack.EMPTY;
        amount = sample.isEmpty() ? 0L : tag.getLong(AMOUNT);
        vessel.load(tag, registries);
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
