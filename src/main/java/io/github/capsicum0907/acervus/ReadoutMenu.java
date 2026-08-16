package io.github.capsicum0907.acervus;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The screen behind a fluid, energy or gas heap.
 *
 * <p>One menu for all three, because what they have in common is exactly what a
 * screen needs: an amount, a capacity, a name and a slot to put a container in.
 * The item heap keeps its own — there the contents <em>are</em> a slot, and that is
 * worth more than sharing this.
 *
 * <p>Nothing about the block is sent through the menu. The block entity is already
 * synchronised for drawing, so the screen reads it from the level — which also
 * avoids menu data fields, whose sixteen bits would cap a heap at 32767 of anything.
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

    private final ContainerLevelAccess access;
    private final BlockPos pos;

    public ReadoutMenu(int id, Inventory inventory, BlockPos pos) {
        super(AcervusRegistry.READOUT_MENU.get(), id);
        this.pos = pos;
        this.access = ContainerLevelAccess.create(inventory.player.level(), pos);

        Vessel vessel = vessel(inventory.player);
        if (vessel != null) {
            addSlot(new VesselSlot(vessel, VESSEL_X, VESSEL_Y));
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

    /** The heap as something a readout can read, or null if the block has gone. */
    public Heaped heap(Player player) {
        return player.level().getBlockEntity(pos) instanceof Heaped heaped ? heaped : null;
    }

    /** Which way the container in the slot is going, for the screen to say. */
    public Vessel.Flow flow() {
        Vessel vessel = slots.isEmpty() || !(slots.get(0) instanceof VesselSlot slot) ? null : slot.vessel();
        return vessel == null ? Vessel.Flow.NONE : vessel.flow();
    }

    private Vessel vessel(Player player) {
        return player.level().getBlockEntity(pos) instanceof HasVessel holder ? holder.vessel() : null;
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((level, at) -> player.distanceToSqr(
                at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5) <= 64.0, true);
    }

    /** Shift-clicking moves a container into the slot, or back out of it. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
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
}
