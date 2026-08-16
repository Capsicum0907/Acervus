package io.github.capsicum0907.acervus;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

/**
 * A heap that is being carried rather than placed.
 *
 * <p>The contents already ride on the item — that is how a heap survives being mined
 * — so a heap in a pocket is a full heap with nothing that reads it. This is that
 * reading, and it is deliberately one-way: <b>a carried heap takes, and does not
 * give</b>.
 *
 * <p>The asymmetry is the whole design. Emptying a pocket into one while mining is
 * what makes carrying it worth a slot; drawing two billion of anything back out of a
 * pocket is a different item entirely, one that makes every other slot pointless.
 * Taking still requires putting the block down, which is a deliberate act in a place.
 *
 * <p>Only a heap that <em>already holds</em> something takes anything, the same
 * distinction {@link HeapBlockEntity#holds} draws against {@code accepts}: an empty
 * heap in a bag would otherwise commit itself to whatever the player happened to walk
 * over first, which is a decision made by accident.
 */
public final class Carried {
    private static final String SAMPLE = "Sample";
    private static final String AMOUNT = "Amount";
    private static final String LEGACY_COUNT = "Count";

    private Carried() {
    }

    /**
     * Takes items out of the air and into a heap the player is already carrying,
     * before the inventory ever sees them.
     *
     * <p>Everything past the absorbing is what vanilla would have done: the take
     * animation and its sound, the statistic, and the pickup trigger. They are done
     * here because {@code ItemEntity#playerTouch} only does them when
     * {@code Inventory#add} accepted something, and the whole point of this is that
     * the inventory never did.
     */
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        if (!AcervusConfig.ABSORBS_WHEN_CARRIED.get() || event.canPickup().isFalse()) {
            return;
        }
        Player player = event.getPlayer();
        ItemEntity entity = event.getItemEntity();
        // The same two conditions vanilla checks after this event, checked before it:
        // an item just thrown, or one being held for whoever dropped it, is not free
        // to take yet. TRUE means another listener has already waived both.
        if (!event.canPickup().isTrue()
                && (entity.hasPickUpDelay()
                        || (entity.getTarget() != null && !entity.getTarget().equals(player.getUUID())))) {
            return;
        }

        ItemStack stack = entity.getItem();
        int absorbed = absorb(player, stack);
        if (absorbed <= 0) {
            return;
        }

        // Read before the shrink. An emptied stack answers Items.AIR, so asking it
        // afterwards would quietly award every full pickup to air — which is why
        // ItemEntity#playerTouch takes the item at the top and not where it is used.
        Item taken = stack.getItem();
        player.take(entity, absorbed);
        stack.shrink(absorbed);
        player.awardStat(Stats.ITEM_PICKED_UP.get(taken), absorbed);
        player.onItemPickup(entity);
        if (stack.isEmpty()) {
            // Nothing is left for the inventory, and an item entity holding an empty
            // stack would sit there being collided with forever.
            entity.discard();
            event.setCanPickup(TriState.FALSE);
        }
    }

    /**
     * Puts as much of {@code incoming} as will fit into the heaps this player is
     * carrying, without taking it out of the stack.
     *
     * @return how many were taken, which is none unless some heap already holds them
     */
    public static int absorb(Player player, ItemStack incoming) {
        if (incoming.isEmpty()) {
            return 0;
        }
        HolderLookup.Provider registries = player.level().registryAccess();
        Inventory inventory = player.getInventory();

        int absorbed = 0;
        int left = incoming.getCount();
        for (int slot = 0; slot < inventory.getContainerSize() && left > 0; slot++) {
            int taken = absorb(inventory.getItem(slot), incoming, left, registries);
            absorbed += taken;
            left -= taken;
        }
        return absorbed;
    }

    /**
     * @param wanted at most this many, so a caller filling several heaps from one stack
     *               cannot promise the same items twice
     * @return how many this heap took
     */
    private static int absorb(ItemStack heap, ItemStack incoming, int wanted, HolderLookup.Provider registries) {
        // One heap, not a stack of them. Several heaps in a slot are one set of
        // components between them, so adding to "the" heap would add to all of them —
        // the same duplication a shulker box avoids by not stacking at all.
        if (!isHeap(heap) || heap.getCount() != 1) {
            return 0;
        }
        ItemStack sample = sample(heap, registries);
        if (sample.isEmpty() || !ItemStack.isSameItemSameComponents(sample, incoming)) {
            return 0;
        }

        long held = amount(heap);
        int taken = (int) Math.min(Math.max(0L, AcervusConfig.CAPACITY.get() - held), wanted);
        if (taken <= 0) {
            return 0;
        }

        CompoundTag tag = data(heap);
        tag.putLong(AMOUNT, held + taken);
        tag.remove(LEGACY_COUNT);
        heap.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(tag));
        return taken;
    }

    public static boolean isHeap(ItemStack stack) {
        return stack.getItem() == AcervusRegistry.HEAP_ITEM.get();
    }

    /** What this carried heap holds, with a count of one, or empty. */
    public static ItemStack sample(ItemStack heap, HolderLookup.Provider registries) {
        CompoundTag tag = data(heap);
        if (!tag.contains(SAMPLE)) {
            return ItemStack.EMPTY;
        }
        return ItemStack.parse(registries, tag.getCompound(SAMPLE)).orElse(ItemStack.EMPTY);
    }

    /** How much it holds. The old key is read when the new one is absent, as everywhere else. */
    public static long amount(ItemStack heap) {
        CompoundTag tag = data(heap);
        return tag.contains(AMOUNT) ? tag.getLong(AMOUNT) : tag.getLong(LEGACY_COUNT);
    }

    private static CompoundTag data(ItemStack heap) {
        return heap.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).copyTag();
    }
}
