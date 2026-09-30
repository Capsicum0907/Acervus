package io.github.capsicum0907.acervus;

import java.util.function.BiPredicate;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
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

public class FluidHeapBlockEntity extends BlockEntity implements Heaped, HasVessel {
    private static final String SAMPLE = "Sample";
    private static final String AMOUNT = "Amount";

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

    public FluidHeapHandler handler() {
        return handler;
    }

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

    public boolean holds(FluidStack stack) {
        return !isEmpty() && !stack.isEmpty() && FluidStack.isSameFluidSameComponents(sample, stack);
    }

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
            sample = FluidStack.EMPTY;
            amount = 0;
        }
        changed();
        return out;
    }

    private final Vessel in = new Vessel(Vessel.Flow.IN);
    private final Vessel out = new Vessel(Vessel.Flow.OUT);

    @Override
    public Vessel vessel(Vessel.Flow flow) {
        return flow == Vessel.Flow.IN ? in : out;
    }

    @Override
    public boolean emptyContainer(ItemStack stack) {
        return isEmptyContainer(stack);
    }

    public static boolean isEmptyContainer(ItemStack stack) {
        IFluidHandlerItem container = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (container == null) {
            return false;
        }
        for (int tank = 0; tank < container.getTanks(); tank++) {
            if (!container.getFluidInTank(tank).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FluidHeapBlockEntity heap) {
        if (level.isClientSide) {
            return;
        }
        boolean moved = heap.tickVessel(heap.in, FluidHeapBlockEntity::pourIn);
        moved |= heap.tickVessel(heap.out, FluidHeapBlockEntity::drawOut);
        if (moved) {
            heap.changed();
        }
    }

    private boolean tickVessel(Vessel vessel, BiPredicate<FluidHeapBlockEntity, IFluidHandlerItem> move) {
        if (vessel.isEmpty()) {
            return false;
        }
        IFluidHandlerItem container = vessel.held().getCapability(Capabilities.FluidHandler.ITEM);
        if (container == null || !move.test(this, container)) {
            return false;
        }
        vessel.hold(container.getContainer());
        return true;
    }

    private static boolean pourIn(FluidHeapBlockEntity heap, IFluidHandlerItem container) {
        FluidStack offered = container.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
        if (offered.isEmpty() || !heap.accepts(offered)) {
            return false;
        }
        int taken = heap.insert(offered, false);
        if (taken <= 0) {
            return false;
        }
        container.drain(offered.copyWithAmount(taken), IFluidHandler.FluidAction.EXECUTE);
        return true;
    }

    private static boolean drawOut(FluidHeapBlockEntity heap, IFluidHandlerItem container) {
        if (heap.isEmpty()) {
            return false;
        }
        FluidStack offer = heap.extract(Integer.MAX_VALUE, true);
        int filled = container.fill(offer, IFluidHandler.FluidAction.EXECUTE);
        if (filled <= 0) {
            return false;
        }
        heap.extract(filled, false);
        return true;
    }

    @Override
    public Component contentName() {
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
        return 0xFF7FB8C8;
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
        in.save(tag, registries);
        out.save(tag, registries);
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
        in.load(tag, registries);
        out.load(tag, registries);
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
