package io.github.capsicum0907.acervus;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public class FluidHeapHandler implements IFluidHandler {
    private static final int TANK = 0;

    private final FluidHeapBlockEntity heap;

    public FluidHeapHandler(FluidHeapBlockEntity heap) {
        this.heap = heap;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return tank == TANK ? heap.contents() : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return tank == TANK ? (int) Math.min(heap.capacity(), Integer.MAX_VALUE) : 0;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return tank == TANK && heap.accepts(stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return heap.insert(resource, action.simulate());
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return heap.holds(resource) ? heap.extract(resource.getAmount(), action.simulate())
                : FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return heap.extract(maxDrain, action.simulate());
    }
}
