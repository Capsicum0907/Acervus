package io.github.capsicum0907.acervus;

import java.util.List;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * The window a pipe or a pump sees onto a rack of fluid heaps.
 *
 * <p><b>One tank per fluid heap actually in the rack, not one per rack slot.</b> That
 * is a reversal, and the interface is what settles it: {@code fill} and {@code drain}
 * take no tank index at all — the routing below is this class's own — so an index is
 * only ever used to <em>read</em> a tank, within the tick it was asked for. Nothing
 * holds one between ticks, so nothing is broken by the numbering changing when a heap
 * is put in or taken out.
 *
 * <p>Twelve fixed tanks was the cautious answer and it had a visible price: anything
 * that lists a block's tanks — Jade, most obviously — drew twelve bars for a rack with
 * one fluid heap in it, eleven of them saying Empty forever. The item side keeps its
 * fixed slots, because {@code IItemHandler} really does take an index when it inserts
 * and extracts. See {@link HorreumItemHandler}.
 *
 * <p>Filling routes to <b>a tank that already holds that fluid before an empty one</b>.
 * The other way round, a bucket of water would claim whichever empty heap came first
 * and the rack would fill up with half-used tanks of the same thing.
 */
public class HorreumFluidHandler implements IFluidHandler {
    private final HorreumBlockEntity rack;

    public HorreumFluidHandler(HorreumBlockEntity rack) {
        this.rack = rack;
    }

    /** The fluid heaps in the rack, in slot order. Read fresh: the slots change. */
    private List<HeldFluidHeap> tanks() {
        return rack.readAll(AcervusRegistry.FLUID_HEAP_ITEM.get(), HeldFluidHeap::stored);
    }

    private HeldFluidHeap at(int tank) {
        List<HeldFluidHeap> tanks = tanks();
        return tank < 0 || tank >= tanks.size() ? null : tanks.get(tank);
    }

    @Override
    public int getTanks() {
        return tanks().size();
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
        // Twice over the tanks: everything that already holds this fluid, and only then
        // the empty ones. One pass would commit an empty heap while a matching one stood
        // half full beside it.
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
        for (HeldFluidHeap heap : tanks()) {
            if (filled >= resource.getAmount()) {
                break;
            }
            if (matchingOnly == heap.isEmpty()) {
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
        for (HeldFluidHeap heap : tanks()) {
            if (heap.isEmpty() || !FluidStack.isSameFluidSameComponents(heap.sample(), resource)) {
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
        for (HeldFluidHeap heap : tanks()) {
            if (heap.isEmpty()) {
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
