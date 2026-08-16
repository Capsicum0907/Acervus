package io.github.capsicum0907.acervus;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * The window a pipe or a pump sees onto a rack of fluid heaps.
 *
 * <p>One tank per rack slot, always twelve — see {@link HorreumItemHandler} for why
 * the indices must not move. A slot holding anything but a fluid heap reads as an
 * empty tank of no capacity.
 *
 * <p>Filling and draining have no tank index in this interface, so they are routed:
 * <b>a fluid goes to a tank that already holds it before it goes to an empty one.</b>
 * The other way round, a bucket of water would claim whichever empty heap came first
 * and the rack would fill up with half-used tanks of the same thing.
 */
public class HorreumFluidHandler implements IFluidHandler {
    private final HorreumBlockEntity rack;

    public HorreumFluidHandler(HorreumBlockEntity rack) {
        this.rack = rack;
    }

    private HeldFluidHeap at(int tank) {
        return rack.read(tank, AcervusRegistry.FLUID_HEAP_ITEM.get(), HeldFluidHeap::stored);
    }

    @Override
    public int getTanks() {
        return HorreumBlockEntity.SLOTS;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        HeldFluidHeap heap = at(tank);
        if (heap == null || heap.isEmpty()) {
            return FluidStack.EMPTY;
        }
        return heap.sample().copyWithAmount((int) Math.min(heap.amount(), Integer.MAX_VALUE));
    }

    @Override
    public int getTankCapacity(int tank) {
        HeldFluidHeap heap = at(tank);
        return heap == null ? 0 : (int) Math.min(heap.capacity(), Integer.MAX_VALUE);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        HeldFluidHeap heap = at(tank);
        return heap != null && heap.insert(stack, true) > 0;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) {
            return 0;
        }
        // Twice over the slots: everything that already holds this fluid, and only
        // then the empty ones. One pass would commit an empty heap while a matching
        // one stood half full beside it.
        int filled = pour(resource, action, true);
        if (filled < resource.getAmount()) {
            filled += pour(resource.copyWithAmount(resource.getAmount() - filled), action, false);
        }
        if (filled > 0 && action.execute()) {
            rack.changed();
        }
        return filled;
    }

    private int pour(FluidStack resource, FluidAction action, boolean matchingOnly) {
        int filled = 0;
        for (int tank = 0; tank < HorreumBlockEntity.SLOTS && filled < resource.getAmount(); tank++) {
            HeldFluidHeap heap = at(tank);
            if (heap == null || (matchingOnly ? heap.isEmpty() : !heap.isEmpty())) {
                continue;
            }
            filled += heap.insert(resource.copyWithAmount(resource.getAmount() - filled), action.simulate());
        }
        return filled;
    }

    /** Draining by kind: only a heap holding that fluid answers. */
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) {
            return FluidStack.EMPTY;
        }
        for (int tank = 0; tank < HorreumBlockEntity.SLOTS; tank++) {
            HeldFluidHeap heap = at(tank);
            if (heap == null || heap.isEmpty()
                    || !FluidStack.isSameFluidSameComponents(heap.sample(), resource)) {
                continue;
            }
            FluidStack out = heap.extract(resource.getAmount(), action.simulate());
            if (!out.isEmpty()) {
                if (action.execute()) {
                    rack.changed();
                }
                return out;
            }
        }
        return FluidStack.EMPTY;
    }

    /**
     * Draining by amount alone: the first heap with anything in it answers, and the
     * whole of what comes out is one fluid. Mixing two heaps into one drain would mean
     * inventing a stack that is half lava, which no fluid stack can be.
     */
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        for (int tank = 0; tank < HorreumBlockEntity.SLOTS; tank++) {
            HeldFluidHeap heap = at(tank);
            if (heap == null || heap.isEmpty()) {
                continue;
            }
            FluidStack out = heap.extract(maxDrain, action.simulate());
            if (!out.isEmpty()) {
                if (action.execute()) {
                    rack.changed();
                }
                return out;
            }
        }
        return FluidStack.EMPTY;
    }
}
