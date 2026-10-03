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

public class HeapMenu extends AbstractContainerMenu {
    private static final int IN_X = 8;
    private static final int OUT_X = 150;
    private static final int HEAP_Y = 38;
    private static final int INVENTORY_X = 8;
    private static final int INVENTORY_Y = 84;
    private static final int HOTBAR_Y = 142;
    private static final int SLOT = 18;

    private static final int IN_SLOT = 0;
    private static final int OUT_SLOT = 1;
    private static final int PLAYER_FIRST = 2;
    private static final int PLAYER_LAST = PLAYER_FIRST + 36;

    public interface Source {
        Pile pile();

        boolean stillValid(Player player);

        default int frozen() {
            return -1;
        }
    }

    private final Source source;
    private boolean smallest;

    private HeapMenu(MenuType<?> type, int id, Inventory inventory, Source source) {
        super(type, id);
        this.source = source;

        addSlot(new IntakeSlot(source::pile, IN_X, HEAP_Y));
        addSlot(new HeapSlot(source::pile, () -> smallest, OUT_X, HEAP_Y));

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

    public static HeapMenu at(int id, Inventory inventory, BlockPos pos) {
        HeapMenu menu = new HeapMenu(AcervusRegistry.HEAP_MENU.get(), id, inventory,
                new AtBlock(inventory.player.level(), pos));
        if (!inventory.player.level().isClientSide) {
            menu.pile().settle();
        }
        return menu;
    }

    public static HeapMenu inHand(int id, Inventory inventory, InteractionHand hand) {
        return new HeapMenu(AcervusRegistry.CARRIED_HEAP_MENU.get(), id, inventory,
                new InHand(inventory.player, hand));
    }

    public Pile pile() {
        return source.pile();
    }

    public boolean smallest() {
        return smallest;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == MenuButtons.FORM) {
            smallest = !smallest;
            return true;
        }
        if (id != MenuButtons.LOCK) {
            return false;
        }
        Pile heap = pile();
        heap.lock(!heap.locked());
        return true;
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
        if (!slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }

        Pile heap = pile();
        if (index == IN_SLOT) {
            return ItemStack.EMPTY;
        }
        return index == OUT_SLOT ? outward(heap) : inward(heap, slot);
    }

    private ItemStack outward(Pile heap) {
        ItemStack taken = heap.extract(slots.get(OUT_SLOT).getMaxStackSize(), false, smallest);
        if (taken.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack moved = taken.copy();
        boolean any = moveItemStackTo(taken, PLAYER_FIRST, PLAYER_LAST, true);
        if (!taken.isEmpty()) {
            heap.insert(taken, false);
        }
        return any ? moved : ItemStack.EMPTY;
    }

    private ItemStack inward(Pile heap, Slot slot) {
        ItemStack stack = slot.getItem();
        int taken = heap.intake(stack, false);
        if (taken == 0) {
            return ItemStack.EMPTY;
        }
        stack.shrink(taken);
        slot.setChanged();
        return ItemStack.EMPTY;
    }

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

    static final class Frozen extends Slot {
        Frozen(Inventory inventory, int index, int x, int y) {
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
