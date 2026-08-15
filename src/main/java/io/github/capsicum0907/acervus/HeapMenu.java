package io.github.capsicum0907.acervus;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * The screen's half on the server.
 *
 * <p><b>The heap is not a slot.</b> A slot is a place that holds up to a stack and
 * can be assigned to; a heap holds billions and can only be added to or taken from.
 * Putting it behind a slot would mean answering what "set this slot to sixty-four"
 * does to a heap of five thousand, and there is no answer that is not a bug. So the
 * menu carries only the player's own slots, and what is in the heap is drawn rather
 * than held — the two things that would have been slot clicks are buttons instead.
 *
 * <p>Buttons ride on {@code clickMenuButton}, which is a packet the game already
 * has, so this mod sends nothing of its own.
 *
 * <p>The count is not sent as menu data either. Menu data fields are shorts on the
 * wire, which would cap what a heap can say at 32767; the block entity is already
 * synchronised to the client for drawing, so the screen reads it from there.
 */
public class HeapMenu extends AbstractContainerMenu {
    public static final int TAKE_STACK = 0;
    public static final int TAKE_ONE = 1;

    /** Matches the slot positions in the generated screen texture. */
    private static final int INVENTORY_X = 8;
    private static final int INVENTORY_Y = 100;
    private static final int HOTBAR_Y = 160;
    private static final int SLOT = 18;

    private final ContainerLevelAccess access;
    private final BlockPos pos;

    public HeapMenu(int id, Inventory inventory, BlockPos pos) {
        super(AcervusRegistry.HEAP_MENU.get(), id);
        this.pos = pos;
        this.access = ContainerLevelAccess.create(inventory.player.level(), pos);

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

    /** Shift-clicking one of the player's own stacks puts it in. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        HeapBlockEntity heap = heap(player);
        if (heap == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        int taken = heap.insert(stack, false);
        if (taken == 0) {
            return ItemStack.EMPTY;
        }
        stack.shrink(taken);
        slot.setChanged();
        // EMPTY rather than the remainder: the caller repeats until it is told to
        // stop, and a heap that still has room would never say stop.
        return ItemStack.EMPTY;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player.level().isClientSide) {
            return true;
        }
        HeapBlockEntity heap = heap(player);
        if (heap == null) {
            return false;
        }

        ItemStack out = switch (id) {
            // A stack means whatever a stack of that item is. Mods that change stack
            // sizes are common, and a literal sixty-four would be wrong under them.
            case TAKE_STACK -> heap.extract(Integer.MAX_VALUE, false);
            case TAKE_ONE -> heap.extract(1, false);
            default -> ItemStack.EMPTY;
        };
        if (out.isEmpty()) {
            return false;
        }
        ItemHandlerHelper.giveItemToPlayer(player, out);
        return true;
    }
}
