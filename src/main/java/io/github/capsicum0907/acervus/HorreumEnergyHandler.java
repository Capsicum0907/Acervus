package io.github.capsicum0907.acervus;

import java.util.List;

import net.neoforged.neoforge.energy.IEnergyStorage;

public class HorreumEnergyHandler implements IEnergyStorage {
    private final HorreumBlockEntity rack;

    public HorreumEnergyHandler(HorreumBlockEntity rack) {
        this.rack = rack;
    }

    private List<HeldEnergyHeap> cells() {
        return rack.readAll(AcervusRegistry.ENERGY_HEAP_ITEM.get(), HeldEnergyHeap::stored);
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        if (toReceive <= 0) {
            return 0;
        }
        int taken = 0;
        for (HeldEnergyHeap cell : cells()) {
            if (taken >= toReceive) {
                break;
            }
            int room = (int) Math.min(cell.room(), toReceive - taken);
            taken += simulate ? room : cell.receive(room);
        }
        if (taken > 0 && !simulate) {
            rack.changed();
        }
        return taken;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        if (toExtract <= 0) {
            return 0;
        }
        int given = 0;
        for (HeldEnergyHeap cell : cells()) {
            if (given >= toExtract) {
                break;
            }
            int available = (int) Math.min(cell.amount(), toExtract - given);
            given += simulate ? available : cell.give(available);
        }
        if (given > 0 && !simulate) {
            rack.changed();
        }
        return given;
    }

    @Override
    public int getEnergyStored() {
        return saturated(HeldEnergyHeap::amount);
    }

    @Override
    public int getMaxEnergyStored() {
        return saturated(HeldEnergyHeap::capacity);
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return true;
    }

    private int saturated(java.util.function.ToLongFunction<HeldEnergyHeap> of) {
        long total = 0;
        for (HeldEnergyHeap cell : cells()) {
            total += of.applyAsLong(cell);
            if (total >= Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
        }
        return (int) total;
    }
}
