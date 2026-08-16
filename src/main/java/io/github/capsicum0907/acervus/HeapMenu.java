package io.github.capsicum0907.acervus;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The screen's half on the server, for a heap in the world or a heap in a hand.
 *
 * <p>The heap is presented as a slot — see {@link HeapSlot} — rather than as buttons.
 * It is an item; it should be handled the way items are handled, with the clicks
 * everybody already knows. A button beside it would be a new thing to learn for
 * something the player can already do.
 *
 * <p>The count is not sent as menu data. Menu data fields are shorts on the wire,
 * which would cap what a heap can say at 32767. The block entity is already
 * synchronised to the client for drawing and the item is already synchronised as part
 * of the inventory, so in both cases the screen reads the heap where it lives.
 *
 * <p>What differs between the two is gathered in {@link Source}: where the heap is,
 * when the screen should close, and — for a heap in a hand — which of the player's own
 * slots must be frozen while its screen is open.
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

    /** Where a heap is kept, and what that means for the screen over it. */
    public interface Source {
        /** Never null: a heap that has gone answers {@link Pile#NONE}. */
        Pile pile();

        boolean stillValid(Player player);

        /**
         * The hotbar slot that must not be moved while this screen is open, or -1.
         *
         * <p>Only a heap held in a hand has one, and it is the heap itself: moving it
         * out from under its own screen would leave the screen writing into a stack
         * that is somewhere else.
         */
        default int frozen() {
            return -1;
        }
    }

    private final Source source;

    private HeapMenu(MenuType<?> type, int id, Inventory inventory, Source source) {
        super(type, id);
        this.source = source;

        // Added unconditionally, even when the heap has gone. The slot indices below
        // are counted from it, so a missing first slot would silently shift the range
        // that shift-clicking moves things into.
        addSlot(new HeapSlot(source::pile, HEAP_X, HEAP_Y));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, 9 + row * 9 + column,
                        INVENTORY_X + column * SLOT, INVENTORY_Y + row * SLOT));
            }
        }
        for (int column = 0; column < 9; column++) {
            int x = INVENTORY_X + column * SLOT;
            addSlot(column == source.frozen()
                    ? new Frozen(inventory, column, x, HOTBAR_Y)
                    : new Slot(inventory, column, x, HOTBAR_Y));
        }
    }

    /** The screen over a heap standing in the world. */
    public static HeapMenu at(int id, Inventory inventory, BlockPos pos) {
        return new HeapMenu(AcervusRegistry.HEAP_MENU.get(), id, inventory,
                new AtBlock(inventory.player.level(), pos));
    }

    /** The screen over a heap being held, opened by sneaking and right-clicking the air. */
    public static HeapMenu inHand(int id, Inventory inventory, InteractionHand hand) {
        return new HeapMenu(AcervusRegistry.CARRIED_HEAP_MENU.get(), id, inventory,
                new InHand(inventory.player, hand));
    }

    public Pile pile() {
        return source.pile();
    }

    @Override
    public boolean stillValid(Player player) {
        return source.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        // The frozen slot is the open heap itself. Shift-clicking it is the first thing
        // anyone tries, and it must not put a heap inside itself.
        if (!slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }

        Pile heap = pile();
        return index == HEAP_SLOT ? outward(heap) : inward(heap, slot);
    }

    /**
     * Out of the heap and into the player. A stack at a time, and non-empty so that
     * the game asks again — which is how shift-clicking keeps going until either the
     * heap or the room runs out.
     */
    private ItemStack outward(Pile heap) {
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
    private ItemStack inward(Pile heap, Slot slot) {
        ItemStack stack = slot.getItem();
        int taken = heap.insert(stack, false);
        if (taken == 0) {
            return ItemStack.EMPTY;
        }
        stack.shrink(taken);
        slot.setChanged();
        return ItemStack.EMPTY;
    }

    /** A heap standing in the world: valid while the player is near the block. */
    private record AtBlock(Level level, BlockPos pos, ContainerLevelAccess access) implements Source {
        AtBlock(Level level, BlockPos pos) {
            this(level, pos, ContainerLevelAccess.create(level, pos));
        }

        @Override
        public Pile pile() {
            return level.getBlockEntity(pos) instanceof HeapBlockEntity heap ? heap : Pile.NONE;
        }

        @Override
        public boolean stillValid(Player player) {
            return AbstractContainerMenu.stillValid(access, player, AcervusRegistry.HEAP.get());
        }
    }

    /** A heap being held: valid while that hand still holds one. */
    private record InHand(Player player, InteractionHand hand, CarriedHeap heap) implements Source {
        InHand(Player player, InteractionHand hand) {
            this(player, hand, CarriedHeap.inHand(player, hand));
        }

        @Override
        public Pile pile() {
            return Carried.isHeap(player.getItemInHand(hand)) ? heap : Pile.NONE;
        }

        @Override
        public boolean stillValid(Player player) {
            return Carried.isHeap(player.getItemInHand(hand));
        }

        @Override
        public int frozen() {
            return hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : -1;
        }
    }

    /**
     * The player's own slot holding the heap whose screen this is.
     *
     * <p>Refusing both directions is what covers the swap click as well — pressing a
     * number key over another slot checks this one before exchanging them.
     */
    private static final class Frozen extends Slot {
        private Frozen(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
