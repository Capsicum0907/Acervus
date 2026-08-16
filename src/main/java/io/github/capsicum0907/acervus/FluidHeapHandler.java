package io.github.capsicum0907.acervus;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * The window a fluid heap shows to pipes and tanks.
 *
 * <p>One tank, telling the truth about what is in it and giving out what it is asked
 * for. The difference from the item side is only where the ceiling is: an item
 * handler is documented as being allowed to report more than a stack, while every
 * number in {@code IFluidHandler} is an int and there is nothing above it to report.
 * So both saturate, but this one saturates because it must rather than because it
 * chose to.
 *
 * <p>Nothing is remembered here. Every method reads the block entity when it is
 * called, for the same reason as the item side: a cached answer is a second copy of
 * the truth, and two of them disagree.
 */
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
        // Asked for a particular fluid: give nothing at all if it is not this one,
        // rather than quietly handing over something else of the same size.
        return heap.holds(resource) ? heap.extract(resource.getAmount(), action.simulate())
                : FluidStack.EMPTY;
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return heap.extract(maxDrain, action.simulate());
    }
}
