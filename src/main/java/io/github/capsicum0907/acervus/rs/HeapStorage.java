package io.github.capsicum0907.acervus.rs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.refinedmods.refinedstorage.api.core.Action;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.resource.ResourceKey;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.api.storage.external.ExternalStorageProvider;
import com.refinedmods.refinedstorage.common.support.resource.FluidResource;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;

import io.github.capsicum0907.acervus.AcervusRegistry;
import io.github.capsicum0907.acervus.CarriedHeap;
import io.github.capsicum0907.acervus.Counts;
import io.github.capsicum0907.acervus.FluidHeapBlockEntity;
import io.github.capsicum0907.acervus.HeapBlockEntity;
import io.github.capsicum0907.acervus.HeldFluidHeap;
import io.github.capsicum0907.acervus.HorreumBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

public class HeapStorage implements ExternalStorageProvider {
    private final ServerLevel level;
    private final BlockPos pos;

    public HeapStorage(ServerLevel level, BlockPos pos) {
        this.level = level;
        this.pos = pos;
    }

    private List<Pile> piles() {
        return switch (level.getBlockEntity(pos)) {
            case HeapBlockEntity heap -> List.of(new Block(heap));
            case HorreumBlockEntity rack -> racked(rack);
            case null, default -> List.of();
        };
    }

    private List<Tank> tanks() {
        return switch (level.getBlockEntity(pos)) {
            case FluidHeapBlockEntity heap -> List.of(new PlacedTank(heap));
            case HorreumBlockEntity rack -> rackedTanks(rack);
            case null, default -> List.of();
        };
    }

    private static List<Pile> racked(HorreumBlockEntity rack) {
        List<Pile> piles = new ArrayList<>();
        for (int slot = 0; slot < HorreumBlockEntity.SLOTS; slot++) {
            ItemStack heap = rack.heap(slot);
            if (heap.getItem() == AcervusRegistry.HEAP_ITEM.get()) {
                piles.add(new Racked(rack, CarriedHeap.stored(rack.getLevel().registryAccess(), heap)));
            }
        }
        return piles;
    }

    private static List<Tank> rackedTanks(HorreumBlockEntity rack) {
        List<Tank> tanks = new ArrayList<>();
        for (int slot = 0; slot < HorreumBlockEntity.SLOTS; slot++) {
            ItemStack heap = rack.heap(slot);
            if (heap.getItem() == AcervusRegistry.FLUID_HEAP_ITEM.get()) {
                tanks.add(new RackedTank(rack, HeldFluidHeap.stored(rack.getLevel().registryAccess(), heap)));
            }
        }
        return tanks;
    }

    @Override
    public Iterator<ResourceAmount> iterator() {
        List<ResourceAmount> amounts = new ArrayList<>();
        for (Pile pile : piles()) {
            long beyond = Counts.beyondAnInt(pile.count(), pile.room() > 0);
            if (beyond > 0 && !pile.sample().isEmpty()) {
                amounts.add(new ResourceAmount(ItemResource.ofItemStack(pile.sample()), beyond));
            }
        }
        for (Tank tank : tanks()) {
            long beyond = Counts.beyondAnInt(tank.amount());
            FluidStack sample = tank.sample();
            if (beyond > 0 && !sample.isEmpty()) {
                amounts.add(new ResourceAmount(new FluidResource(sample.getFluid(), sample.getComponentsPatch()), beyond));
            }
        }
        return Collections.unmodifiableList(amounts).iterator();
    }

    @Override
    public long insert(ResourceKey resource, long amount, Action action, Actor actor) {
        if (amount <= 0) {
            return 0L;
        }
        boolean simulate = action == Action.SIMULATE;
        if (resource instanceof FluidResource fluid) {
            FluidStack offered = new FluidStack(fluid.fluid().builtInRegistryHolder(), atMostAnInt(amount),
                    fluid.components());
            long taken = 0L;
            for (Tank tank : tanks()) {
                if (taken >= amount) {
                    break;
                }
                taken += tank.insert(offered.copyWithAmount(atMostAnInt(amount - taken)), simulate);
            }
            return taken;
        }
        if (!(resource instanceof ItemResource item)) {
            return 0L;
        }
        ItemStack offered = item.toItemStack(atMostAnInt(amount));

        long taken = 0L;
        for (Pile pile : piles()) {
            if (taken >= amount) {
                break;
            }
            taken += pile.insert(offered, simulate);
        }
        return taken;
    }

    @Override
    public long extract(ResourceKey resource, long amount, Action action, Actor actor) {
        if (amount <= 0) {
            return 0L;
        }
        boolean simulate = action == Action.SIMULATE;
        if (resource instanceof FluidResource fluid) {
            FluidStack wanted = new FluidStack(fluid.fluid().builtInRegistryHolder(), 1, fluid.components());
            long given = 0L;
            for (Tank tank : tanks()) {
                if (given >= amount) {
                    break;
                }
                if (!FluidStack.isSameFluidSameComponents(tank.sample(), wanted)) {
                    continue;
                }
                given += tank.extract(atMostAnInt(amount - given), simulate);
            }
            return given;
        }
        if (!(resource instanceof ItemResource item)) {
            return 0L;
        }
        ItemStack wanted = item.toItemStack();

        long given = 0L;
        for (Pile pile : piles()) {
            if (given >= amount) {
                break;
            }
            if (!ItemStack.isSameItemSameComponents(pile.sample(), wanted)) {
                continue;
            }
            given += pile.extract(atMostAnInt(amount - given), simulate);
        }
        return given;
    }

    private static int atMostAnInt(long amount) {
        return (int) Math.min(amount, Integer.MAX_VALUE);
    }

    private interface Pile {
        ItemStack sample();

        long count();

        long room();

        int insert(ItemStack stack, boolean simulate);

        int extract(int amount, boolean simulate);
    }

    private interface Tank {
        FluidStack sample();

        long amount();

        int insert(FluidStack stack, boolean simulate);

        int extract(int amount, boolean simulate);
    }

    private record Block(HeapBlockEntity heap) implements Pile {
        @Override
        public ItemStack sample() {
            return heap.sample();
        }

        @Override
        public long count() {
            return heap.count();
        }

        @Override
        public long room() {
            return heap.room();
        }

        @Override
        public int insert(ItemStack stack, boolean simulate) {
            return heap.insert(stack, simulate);
        }

        @Override
        public int extract(int amount, boolean simulate) {
            return heap.extract(amount, simulate).getCount();
        }
    }

    private record Racked(HorreumBlockEntity rack, CarriedHeap heap) implements Pile {
        @Override
        public ItemStack sample() {
            return heap.sample();
        }

        @Override
        public long count() {
            return heap.count();
        }

        @Override
        public long room() {
            return heap.room();
        }

        @Override
        public int insert(ItemStack stack, boolean simulate) {
            int taken = heap.insert(stack, simulate);
            if (taken > 0 && !simulate) {
                rack.changed();
            }
            return taken;
        }

        @Override
        public int extract(int amount, boolean simulate) {
            int given = heap.extract(amount, simulate).getCount();
            if (given > 0 && !simulate) {
                rack.changed();
            }
            return given;
        }
    }

    private record PlacedTank(FluidHeapBlockEntity heap) implements Tank {
        @Override
        public FluidStack sample() {
            return heap.sample();
        }

        @Override
        public long amount() {
            return heap.amount();
        }

        @Override
        public int insert(FluidStack stack, boolean simulate) {
            return heap.insert(stack, simulate);
        }

        @Override
        public int extract(int amount, boolean simulate) {
            return heap.extract(amount, simulate).getAmount();
        }
    }

    private record RackedTank(HorreumBlockEntity rack, HeldFluidHeap heap) implements Tank {
        @Override
        public FluidStack sample() {
            return heap.sample();
        }

        @Override
        public long amount() {
            return heap.amount();
        }

        @Override
        public int insert(FluidStack stack, boolean simulate) {
            int taken = heap.insert(stack, simulate);
            if (taken > 0 && !simulate) {
                rack.changed();
            }
            return taken;
        }

        @Override
        public int extract(int amount, boolean simulate) {
            int given = heap.extract(amount, simulate).getAmount();
            if (given > 0 && !simulate) {
                rack.changed();
            }
            return given;
        }
    }
}
