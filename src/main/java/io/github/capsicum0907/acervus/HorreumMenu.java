package io.github.capsicum0907.acervus;

import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The rack's screen: twelve slots that take heaps, and nothing else.
 *
 * <p>There is no readout. Every heap item already says what is inside it on its own
 * tooltip — that line exists because a full heap and an empty one look identical —
 * so a rack that repeated it beside each slot would be saying the same thing twice
 * and could disagree with itself.
 */
public class HorreumMenu extends AbstractContainerMenu {
    /** Matches the slot positions in the generated screen texture. */
    private static final int RACK_X = 26;
    private static final int RACK_Y = 22;
    private static final int RACK_COLUMNS = 6;
    private static final int INVENTORY_X = 8;
    private static final int INVENTORY_Y = 84;
    private static final int HOTBAR_Y = 142;
    private static final int SLOT = 18;

    private static final int RACK_FIRST = 0;
    private static final int RACK_LAST = HorreumBlockEntity.SLOTS;
    private static final int PLAYER_LAST = RACK_LAST + 36;

    private final Level level;
    private final BlockPos pos;
    private final ContainerLevelAccess access;

    public HorreumMenu(int id, Inventory inventory, BlockPos pos) {
        super(AcervusRegistry.HORREUM_MENU.get(), id);
        this.level = inventory.player.level();
        this.pos = pos;
        this.access = ContainerLevelAccess.create(level, pos);

        for (int index = 0; index < HorreumBlockEntity.SLOTS; index++) {
            addSlot(new HeapRackSlot(index,
                    RACK_X + (index % RACK_COLUMNS) * SLOT,
                    RACK_Y + (index / RACK_COLUMNS) * SLOT));
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

    /** Null if the block has gone since the screen opened. */
    public HorreumBlockEntity rack() {
        return level.getBlockEntity(pos) instanceof HorreumBlockEntity rack ? rack : null;
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(access, player, AcervusRegistry.HORREUM.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack moving = slot.getItem().copy();

        if (index < RACK_LAST) {
            if (!moveItemStackTo(slot.getItem(), RACK_LAST, PLAYER_LAST, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(slot.getItem(), RACK_FIRST, RACK_LAST, false)) {
            return ItemStack.EMPTY;
        }

        slot.setChanged();
        return moving;
    }

    /**
     * One slot of the rack, reading and writing the block entity's list directly.
     *
     * <p><b>One heap per slot.</b> Two heaps in a slot are one set of components
     * between them, so filling "the" heap would fill both — the duplication a shulker
     * box avoids by not stacking, and the same rule a carried heap follows.
     */
    private class HeapRackSlot extends Slot {
        private static final SimpleContainer UNUSED = new SimpleContainer(0);

        private final int index;

        private HeapRackSlot(int index, int x, int y) {
            super(UNUSED, 0, x, y);
            this.index = index;
        }

        @Override
        public ItemStack getItem() {
            HorreumBlockEntity rack = rack();
            return rack == null ? ItemStack.EMPTY : rack.heap(index);
        }

        @Override
        public boolean hasItem() {
            return !getItem().isEmpty();
        }

        @Override
        public void set(ItemStack stack) {
            HorreumBlockEntity rack = rack();
            if (rack != null) {
                rack.heaps().set(index, stack);
                rack.changed();
            }
        }

        @Override
        public ItemStack remove(int amount) {
            HorreumBlockEntity rack = rack();
            if (rack == null) {
                return ItemStack.EMPTY;
            }
            ItemStack taken = rack.heaps().get(index);
            rack.heaps().set(index, ItemStack.EMPTY);
            rack.changed();
            return taken;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return HorreumBlockEntity.isHeap(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return 1;
        }

        @Override
        public void setChanged() {
            HorreumBlockEntity rack = rack();
            if (rack != null) {
                rack.changed();
            }
        }
    }
}
