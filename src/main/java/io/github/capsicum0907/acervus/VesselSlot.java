package io.github.capsicum0907.acervus;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The slot over a heap's {@link Vessel}: a plain one-item slot, held somewhere other
 * than a container.
 *
 * <p>One item, not a stack. Filling one bucket of a stack of sixteen and leaving the
 * other fifteen empty is a question with no good answer, and refusing to take the
 * stack in the first place is a clearer one.
 */
public class VesselSlot extends Slot {
    private static final SimpleContainer UNUSED = new SimpleContainer(0);

    private final Vessel vessel;
    private final Runnable changed;

    public VesselSlot(Vessel vessel, int x, int y, Runnable changed) {
        super(UNUSED, 0, x, y);
        this.vessel = vessel;
        this.changed = changed;
    }

    public Vessel vessel() {
        return vessel;
    }

    @Override
    public ItemStack getItem() {
        return vessel.held();
    }

    @Override
    public boolean hasItem() {
        return !vessel.isEmpty();
    }

    @Override
    public void set(ItemStack stack) {
        vessel.hold(stack);
        setChanged();
    }

    @Override
    public ItemStack remove(int amount) {
        ItemStack held = vessel.held();
        if (held.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack taken = held.split(amount);
        if (held.isEmpty()) {
            vessel.hold(ItemStack.EMPTY);
        }
        setChanged();
        return taken;
    }

    @Override
    public void setChanged() {
        super.setChanged();
        changed.run();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return 1;
    }
}
