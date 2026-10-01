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
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;

import io.github.capsicum0907.acervus.AcervusRegistry;
import io.github.capsicum0907.acervus.CarriedHeap;
import io.github.capsicum0907.acervus.HeapBlockEntity;
import io.github.capsicum0907.acervus.HorreumBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

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

    @Override
    public Iterator<ResourceAmount> iterator() {
        List<ResourceAmount> amounts = new ArrayList<>();
        for (Pile pile : piles()) {
            if (pile.count() > 0 && !pile.sample().isEmpty()) {
                amounts.add(new ResourceAmount(ItemResource.ofItemStack(pile.sample()), pile.count()));
            }
        }
        return Collections.unmodifiableList(amounts).iterator();
    }

    @Override
    public long insert(ResourceKey resource, long amount, Action action, Actor actor) {
        if (!(resource instanceof ItemResource item) || amount <= 0) {
            return 0L;
        }
        ItemStack offered = item.toItemStack(atMostAnInt(amount));

        long taken = 0L;
        for (Pile pile : piles()) {
            if (taken >= amount) {
                break;
            }
            taken += pile.insert(offered, action == Action.SIMULATE);
        }
        return taken;
    }

    @Override
    public long extract(ResourceKey resource, long amount, Action action, Actor actor) {
        if (!(resource instanceof ItemResource item) || amount <= 0) {
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
            given += pile.extract(atMostAnInt(amount - given), action == Action.SIMULATE);
        }
        return given;
    }

    private static int atMostAnInt(long amount) {
        return (int) Math.min(amount, Integer.MAX_VALUE);
    }

    private interface Pile {
        ItemStack sample();

        long count();

        int insert(ItemStack stack, boolean simulate);

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
}
