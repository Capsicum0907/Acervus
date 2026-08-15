package io.github.capsicum0907.acervus;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The screen's half on the server.
 *
 * <p>The heap is presented as a slot — see {@link HeapSlot} — rather than as
 * buttons. It is an item; it should be handled the way items are handled, with the
 * clicks everybody already knows. A button beside it would be a new thing to learn
 * for something the player can already do.
 *
 * <p>The count is not sent as menu data. Menu data fields are shorts on the wire,
 * which would cap what a heap can say at 32767; the block entity is already
 * synchronised to the client for drawing, so the screen reads it from there.
 */
public class HeapMenu extends AbstractContainerMenu {
    /** Matches the slot positions in the generated screen texture. */
    private static final int HEAP_X = 16;
    private static final int HEAP_Y = 30;
    private static final int INVENTORY_X = 8;
    private static final int INVENTORY_Y = 84;
    private static final int HOTBAR_Y = 142;
    private static final int SLOT = 18;

    private static final int HEAP_SLOT = 0;
    private static final int PLAYER_FIRST = 1;
    private static final int PLAYER_LAST = PLAYER_FIRST + 36;

    private final ContainerLevelAccess access;
    private final BlockPos pos;

    public HeapMenu(int id, Inventory inventory, BlockPos pos) {
        super(AcervusRegistry.HEAP_MENU.get(), id);
        this.pos = pos;
        this.access = ContainerLevelAccess.create(inventory.player.level(), pos);

        HeapBlockEntity heap = heap(inventory.player);
        if (heap != null) {
            addSlot(new HeapSlot(heap, HEAP_X, HEAP_Y));
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, 9 + row * 9 + column,
                        INVENTORY_X + column * SLOT, INVENTORY_Y + row * SLOT));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, INVENTORY_X + column * SLOT, HOTBAR_Y));
        }
    }

    public BlockPos pos() {
        return pos;
    }

    /** Null if the block has gone since the screen opened. */
    public HeapBlockEntity heap(Player player) {
        return player.level().getBlockEntity(pos) instanceof HeapBlockEntity heap ? heap : null;
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(access, player, AcervusRegistry.HEAP.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        HeapBlockEntity heap = heap(player);
        Slot slot = slots.get(index);
        if (heap == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        if (index == HEAP_SLOT) {
            return outward(heap);
        }
        return inward(heap, slot);
    }

    /**
     * Out of the heap and into the player. A stack at a time, and non-empty so that
     * the game asks again — which is how shift-clicking keeps going until either the
     * heap or the room runs out.
     */
    private ItemStack outward(HeapBlockEntity heap) {
        // A stack's worth, explicitly: extraction is no longer clamped to one, so
        // asking for everything here would empty the heap into a full inventory.
        ItemStack taken = heap.extract(heap.sample().getMaxStackSize(), false);
        if (taken.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack moved = taken.copy();
        boolean any = moveItemStackTo(taken, PLAYER_FIRST, PLAYER_LAST, true);
        if (!taken.isEmpty()) {
            heap.insert(taken, false); // whatever would not fit goes back where it was
        }
        return any ? moved : ItemStack.EMPTY;
    }

    /** Into the heap, all of it at once: there is no reason to make this take turns. */
    private ItemStack inward(HeapBlockEntity heap, Slot slot) {
        ItemStack stack = slot.getItem();
        int taken = heap.insert(stack, false);
        if (taken == 0) {
            return ItemStack.EMPTY;
        }
        stack.shrink(taken);
        slot.setChanged();
        return ItemStack.EMPTY;
    }
}
