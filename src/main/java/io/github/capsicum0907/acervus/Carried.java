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

public final class Carried {
    private Carried() {
    }

    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        if (!AcervusConfig.ABSORBS_WHEN_CARRIED.get() || event.canPickup().isFalse()) {
            return;
        }
        Player player = event.getPlayer();
        ItemEntity entity = event.getItemEntity();
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

        Item taken = stack.getItem();
        player.take(entity, absorbed);
        stack.shrink(absorbed);
        player.awardStat(Stats.ITEM_PICKED_UP.get(taken), absorbed);
        player.onItemPickup(entity);
        if (stack.isEmpty()) {
            entity.discard();
            event.setCanPickup(TriState.FALSE);
        }
    }

    public static void onTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || !AcervusConfig.ABSORBS_WHEN_CARRIED.get()
                || player.containerMenu != player.inventoryMenu) {
            return;
        }
        sweep(player);
    }

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

    private static int absorb(Player player, ItemStack heap, ItemStack incoming, int wanted) {
        if (!isHeap(heap) || !ContentsBlockItem.alone(heap)) {
            return 0;
        }
        CarriedHeap pile = CarriedHeap.of(player, heap);
        if (!pile.holds(incoming)) {
            return 0;
        }
        return pile.insert(incoming.copyWithCount(wanted), false);
    }

    public static InteractionResultHolder<ItemStack> open(Player player, InteractionHand hand,
            MenuConstructor menu) {
        ItemStack held = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            return InteractionResultHolder.pass(held);
        }
        if (!ContentsBlockItem.alone(held)) {
            player.displayClientMessage(Component.translatable("acervus.stacked.open", held.getHoverName()), true);
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
