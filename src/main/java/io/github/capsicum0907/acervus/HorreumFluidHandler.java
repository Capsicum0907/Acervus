package io.github.capsicum0907.acervus;

import java.util.List;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public class HorreumFluidHandler implements IFluidHandler {
    private final HorreumBlockEntity rack;

    public HorreumFluidHandler(HorreumBlockEntity rack) {
        this.rack = rack;
    }

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
