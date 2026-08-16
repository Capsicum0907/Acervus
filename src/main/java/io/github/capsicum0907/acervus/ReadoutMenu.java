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
 * The screen behind a fluid, energy or gas heap, standing in the world or in a hand.
 *
 * <p>One menu for all three resources, because what they have in common is exactly
 * what a screen needs: an amount, a capacity, a name and a slot to put a container
 * in. The item heap keeps its own — there the contents <em>are</em> a slot, and that
 * is worth more than sharing this.
 *
 * <p>Nothing about the heap is sent through the menu. A block is already synchronised
 * for drawing and a held item is already synchronised as part of the inventory, so
 * the screen reads the heap where it lives — which also avoids menu data fields,
 * whose sixteen bits would cap a heap at 32767 of anything.
 *
 * <p>What differs between the two places is gathered in {@link Source}.
 */
public class ReadoutMenu extends AbstractContainerMenu {
    /** Matches the slot positions in the generated screen texture. */
    private static final int VESSEL_X = 150;
    private static final int VESSEL_Y = 38;
    private static final int INVENTORY_X = 8;
    private static final int INVENTORY_Y = 84;
    private static final int HOTBAR_Y = 142;
    private static final int SLOT = 18;

    private static final int VESSEL_SLOT = 0;
    private static final int PLAYER_FIRST = 1;
    private static final int PLAYER_LAST = PLAYER_FIRST + 36;

    /** Where the heap is kept, and what that means for the screen over it. */
    public interface Source {
        /** Never null: a heap that has gone answers {@link Heaped#NONE}. */
        Heaped heap();

        /** Never null either; a heap that has gone gets a spare nothing ever looks at. */
        Vessel vessel();

        boolean stillValid(Player player);

        /** The hotbar slot frozen while this screen is open, or -1. See {@link HeapMenu.Source}. */
        default int frozen() {
            return -1;
        }

        /**
         * A moment of moving whatever is in the vessel.
         *
         * <p>Nothing for a block: it is ticking already, and doing it twice would move
         * things at twice the rate the block advertises. A held heap has no block entity
         * to tick, so the menu is the only clock it has.
         */
        default void tick() {
        }

        /** Whether the vessel belongs to the screen and must be handed back when it closes. */
        default boolean lendsTheVessel() {
            return false;
        }
    }

    private final Source source;

    private ReadoutMenu(MenuType<?> type, int id, Inventory inventory, Source source) {
        super(type, id);
        this.source = source;

        // Added unconditionally, even when the heap has gone: the slot indices below are
        // counted from it, so a missing first slot would silently shift the range that
        // shift-clicking moves things into.
        addSlot(new VesselSlot(source.vessel(), VESSEL_X, VESSEL_Y));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, 9 + row * 9 + column,
                        INVENTORY_X + column * SLOT, INVENTORY_Y + row * SLOT));
            }
        }
        for (int column = 0; column < 9; column++) {
            int x = INVENTORY_X + column * SLOT;
            addSlot(column == source.frozen()
                    ? new HeapMenu.Frozen(inventory, column, x, HOTBAR_Y)
                    : new Slot(inventory, column, x, HOTBAR_Y));
        }
    }

    /** The screen over a heap standing in the world. */
    public static ReadoutMenu at(int id, Inventory inventory, BlockPos pos) {
        return new ReadoutMenu(AcervusRegistry.READOUT_MENU.get(), id, inventory,
                new AtBlock(inventory.player.level(), pos));
    }

    /** The screen over a heap being held, opened by sneaking and right-clicking the air. */
    public static ReadoutMenu inHand(int id, Inventory inventory, InteractionHand hand) {
        return new ReadoutMenu(AcervusRegistry.CARRIED_READOUT_MENU.get(), id, inventory,
                new InHand(inventory.player, hand));
    }

    public Heaped heap() {
        return source.heap();
    }

    /** Which way the container in the slot is going, for the screen to say. */
    public Vessel.Flow flow() {
        return source.vessel().flow();
    }

    @Override
    public boolean stillValid(Player player) {
        return source.stillValid(player);
    }

    /**
     * Called once a tick per viewer while the screen is open, which is what a held heap
     * uses as its clock. The vessel first, then the ordinary synchronising.
     */
    @Override
    public void broadcastChanges() {
        source.tick();
        super.broadcastChanges();
    }

    /**
     * A held heap's vessel belongs to the screen, so what is in it comes back when the
     * screen closes. A block's does not — the block keeps it, and it is still there the
     * next time anyone opens it.
     */
    @Override
    public void removed(Player player) {
        super.removed(player);
        Vessel vessel = source.vessel();
        if (source.lendsTheVessel() && !player.level().isClientSide && !vessel.isEmpty()) {
            player.getInventory().placeItemBackInInventory(vessel.held());
            vessel.hold(ItemStack.EMPTY);
        }
    }

    /** Shift-clicking moves a container into the slot, or back out of it. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack moving = slot.getItem().copy();

        if (index == VESSEL_SLOT) {
            if (!moveItemStackTo(slot.getItem(), PLAYER_FIRST, PLAYER_LAST, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(slot.getItem(), VESSEL_SLOT, VESSEL_SLOT + 1, false)) {
            return ItemStack.EMPTY;
        }

        slot.setChanged();
        return moving;
    }

    /** A heap standing in the world: valid while the player is near the block. */
    private record AtBlock(Level level, BlockPos pos, ContainerLevelAccess access, Vessel spare)
            implements Source {
        AtBlock(Level level, BlockPos pos) {
            this(level, pos, ContainerLevelAccess.create(level, pos), new Vessel());
        }

        @Override
        public Heaped heap() {
            return level.getBlockEntity(pos) instanceof Heaped heaped ? heaped : Heaped.NONE;
        }

        @Override
        public Vessel vessel() {
            return level.getBlockEntity(pos) instanceof HasVessel holder ? holder.vessel() : spare;
        }

        @Override
        public boolean stillValid(Player player) {
            return access.evaluate((level, at) -> player.distanceToSqr(
                    at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5) <= 64.0, true);
        }
    }

    /**
     * A heap being held: valid while that hand still holds one.
     *
     * <p>The vessel is the menu's own and lasts as long as the screen does. Saving it
     * onto the item would mean a container could be left inside a heap in a pocket,
     * which is a second kind of storage nobody asked for.
     */
    private record InHand(Player player, InteractionHand hand, Held held, Vessel vessel) implements Source {
        InHand(Player player, InteractionHand hand) {
            this(player, hand, heldHeap(player, hand), new Vessel());
        }

        @Override
        public Heaped heap() {
            return held == null || !holdsAHeap(player, hand) ? Heaped.NONE : held;
        }

        @Override
        public boolean stillValid(Player viewer) {
            return holdsAHeap(player, hand);
        }

        @Override
        public int frozen() {
            return hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : -1;
        }

        @Override
        public void tick() {
            if (held != null && !vessel.isEmpty() && !player.level().isClientSide) {
                held.draw(vessel);
            }
        }

        @Override
        public boolean lendsTheVessel() {
            return true;
        }
    }

    private static boolean holdsAHeap(Player player, InteractionHand hand) {
        return heldHeap(player, hand) != null;
    }

    /**
     * Which of the three the player is holding, or null.
     *
     * <p>The gas one is asked for last and through its own package, so that a game
     * without Mekanism never loads a class that mentions a chemical.
     */
    private static Held heldHeap(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() == AcervusRegistry.FLUID_HEAP_ITEM.get()) {
            return HeldFluidHeap.inHand(player, hand);
        }
        if (stack.getItem() == AcervusRegistry.ENERGY_HEAP_ITEM.get()) {
            return HeldEnergyHeap.inHand(player, hand);
        }
        if (io.github.capsicum0907.acervus.gas.GasHeap.present()
                && stack.getItem() == io.github.capsicum0907.acervus.gas.GasHeap.ITEM.get()) {
            return io.github.capsicum0907.acervus.gas.HeldChemicalHeap.inHand(player, hand);
        }
        return null;
    }
}
