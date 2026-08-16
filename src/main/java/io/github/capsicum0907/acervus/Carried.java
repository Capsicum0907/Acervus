package io.github.capsicum0907.acervus;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * A heap that is being carried collects.
 *
 * <p>Two ways in, because items reach a player two ways. One is walked over and
 * intercepted before the inventory ever sees it; the other is already in a slot —
 * from {@code /give}, from a crafting result, from a chest — and is swept up
 * afterwards. Together they mean the same thing to a player: <b>what a heap holds
 * does not take up slots any more.</b>
 *
 * <p>Nothing comes back out. {@link CarriedHeap} is where that rule lives and why.
 *
 * <p>Only a heap that <em>already holds</em> something takes anything, the same
 * distinction {@link HeapBlockEntity#holds} draws against {@code accepts}: an empty
 * heap would otherwise commit itself to whatever was walked over first, which is a
 * decision made by accident. Committing an empty one is what its screen is for.
 */
public final class Carried {
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
     * Everything that reached a slot some other way — {@code /give}, a crafting
     * result, a shift-click out of a chest — swept into the heaps carrying it.
     *
     * <p>Not while a container is open. The player is moving things about on purpose
     * then, and one of the things they may be moving them out of is a heap; a sweep
     * running underneath would put it straight back.
     */
    public static void onTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || !AcervusConfig.ABSORBS_WHEN_CARRIED.get()
                || player.containerMenu != player.inventoryMenu) {
            return;
        }
        sweep(player);
    }

    /**
     * Absorbs what is in the player's own storage slots.
     *
     * <p><b>Not what is in their hand.</b> That is the one place to keep something a
     * heap would otherwise claim, and it needs to exist: without it, carrying a heap
     * of cobblestone would mean never being able to hold a cobblestone. Worn armour is
     * left alone for the same reason.
     */
    public static void sweep(Player player) {
        if (!carriesAHeap(player)) {
            return;
        }
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            if (slot == inventory.selected) {
                continue;
            }
            ItemStack stack = inventory.getItem(slot);
            int taken = absorb(player, stack);
            if (taken > 0) {
                stack.shrink(taken);
                inventory.setChanged();
            }
        }
    }

    /**
     * Puts as much of {@code incoming} as will fit into the heaps this player is
     * carrying, without taking it out of the stack.
     *
     * <p>Every slot is searched for a heap, the hands and the armour included — a heap
     * held in the hand collecting is what anyone would expect of one. Which slots are
     * looked at as a <em>source</em> is a separate question, answered in
     * {@link #sweep}.
     *
     * @return how many were taken, which is none unless some heap already holds them
     */
    public static int absorb(Player player, ItemStack incoming) {
        if (incoming.isEmpty()) {
            return 0;
        }
        Inventory inventory = player.getInventory();

        int absorbed = 0;
        int left = incoming.getCount();
        for (int slot = 0; slot < inventory.getContainerSize() && left > 0; slot++) {
            int taken = absorb(player, inventory.getItem(slot), incoming, left);
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
    private static int absorb(Player player, ItemStack heap, ItemStack incoming, int wanted) {
        // One heap, not a stack of them. Several heaps in a slot are one set of
        // components between them, so adding to "the" heap would add to all of them —
        // the same duplication a shulker box avoids by not stacking at all.
        if (!isHeap(heap) || !ContentsBlockItem.alone(heap)) {
            return 0;
        }
        CarriedHeap pile = CarriedHeap.of(player, heap);
        if (!pile.holds(incoming)) {
            return 0;
        }
        return pile.insert(incoming.copyWithCount(wanted), false);
    }

    /**
     * The gesture that opens a held heap: sneak and right-click the air.
     *
     * <p>The air, because right-clicking a block is how a heap is placed and that must
     * keep working; {@code useOn} runs first and only a miss reaches here. Sneaking,
     * because a plain right-click with a heap in hand already means something on the
     * heap in front of you.
     *
     * <p>All four heaps do this and only the menu differs, so the gesture is written
     * once. What opens is the block's own screen, with one difference the screen states
     * outright: nothing comes out.
     *
     * <p><b>One at a time.</b> A screen onto a stack of empty heaps is a screen onto all
     * of them — whatever went in would go into every one, and taking them apart
     * afterwards would copy it. This is the same guard {@link #absorb} makes, in the
     * other way in.
     */
    public static InteractionResultHolder<ItemStack> open(Player player, InteractionHand hand,
            MenuConstructor menu) {
        ItemStack held = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(held);
        }
        if (!ContentsBlockItem.alone(held)) {
            player.displayClientMessage(Component.translatable("acervus.stacked.open"), true);
            return InteractionResultHolder.fail(held);
        }
        if (player instanceof ServerPlayer server) {
            server.openMenu(new SimpleMenuProvider(menu, held.getHoverName()),
                    buffer -> buffer.writeEnum(hand));
        }
        return InteractionResultHolder.sidedSuccess(held, player.level().isClientSide());
    }

    public static boolean isHeap(ItemStack stack) {
        return stack.getItem() == AcervusRegistry.HEAP_ITEM.get();
    }

    /** One pass before the nested one, so a player carrying no heap costs almost nothing. */
    private static boolean carriesAHeap(Player player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (isHeap(inventory.getItem(slot))) {
                return true;
            }
        }
        return false;
    }

}
