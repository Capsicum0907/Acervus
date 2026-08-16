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

/**
 * What a Refined Storage network sees when its external storage faces a heap or a
 * rack of them.
 *
 * <p><b>This exists for one number.</b> Refined Storage counts in longs everywhere —
 * {@code ResourceAmount} carries one, {@code insert} and {@code extract} take one —
 * and a heap holds a long. The two agree perfectly, and until now they had to speak
 * through {@code IItemHandler}, whose {@code ItemStack} counts in an int. So a network
 * looking at a heap of five billion was told 2,147,483,647 and believed it. Nothing
 * about Refined Storage was the limit; the adapter between them was.
 *
 * <p>Moving is still done in int-sized calls, because that is all a stack can carry,
 * and a network that wants more simply asks again. It is the <em>reading</em> that had
 * no way to be honest, and now does.
 *
 * <p><b>Always non-null, even when the block is not ours.</b> Refined Storage collects
 * providers with {@code .map(factory -> factory.create(…)).toList()} and does not
 * filter what comes back, so a factory that returned null for somebody else's block
 * would put a null in that list. One that answers "nothing here" is the shape the
 * interface actually asks for.
 */
public class HeapStorage implements ExternalStorageProvider {
    private final ServerLevel level;
    private final BlockPos pos;

    public HeapStorage(ServerLevel level, BlockPos pos) {
        this.level = level;
        this.pos = pos;
    }

    /**
     * Every heap behind this face, read fresh.
     *
     * <p>A single heap is one entry; a rack is one per item heap in it. Looked up each
     * time rather than remembered, because the block can be broken, replaced, or have
     * its heaps taken out while the network is still pointing at it.
     */
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
            if (pile.count() > 0) {
                // The whole count, as a long, which is the entire point of this class.
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

    /**
     * One call moves at most what an {@code ItemStack} can <em>count</em> — two billion,
     * not sixty-four. {@code ItemResource.toItemStack(long)} casts rather than clamping
     * to a stack, so the only real ceiling is the int, and clamping first is also what
     * keeps Refined Storage from logging a truncation warning about it.
     *
     * <p>A network wanting more than two billion in one call asks again. Nothing is
     * lost by saying "this much for now"; it was the <em>reading</em> that had no way to
     * be honest.
     */
    private static int atMostAnInt(long amount) {
        return (int) Math.min(amount, Integer.MAX_VALUE);
    }

    /** A heap, wherever it is kept. The two places differ only in who to tell. */
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

    /** A heap in a rack, which has to be told that one of its items changed. */
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
