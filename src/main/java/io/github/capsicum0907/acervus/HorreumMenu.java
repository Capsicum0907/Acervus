package io.github.capsicum0907.acervus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
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

    /**
     * Where the rack is: standing in the world, or held in a hand.
     *
     * <p>The same shape as {@link HeapMenu.Source} and for the same reason - the
     * slots and the screen do not care which, so only the finding differs.
     */
    public interface Source {
        /** The twelve slots, to read and write in place. Never null. */
        NonNullList<ItemStack> heaps();

        boolean stillValid(Player player);

        /** Something inside changed and has to be written down. */
        void changed();

        /** The hotbar slot frozen while this screen is open, or -1. */
        default int frozen() {
            return -1;
        }
    }

    private final Source source;

    private HorreumMenu(MenuType<?> type, int id, Inventory inventory, Source source) {
        super(type, id);
        this.source = source;

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
            int x = INVENTORY_X + column * SLOT;
            addSlot(column == source.frozen()
                    ? new HeapMenu.Frozen(inventory, column, x, HOTBAR_Y)
                    : new Slot(inventory, column, x, HOTBAR_Y));
        }
    }

    /** The screen over a rack standing in the world. */
    public static HorreumMenu at(int id, Inventory inventory, BlockPos pos) {
        return new HorreumMenu(AcervusRegistry.HORREUM_MENU.get(), id, inventory,
                new AtBlock(inventory.player.level(), pos));
    }

    /**
     * The screen over a rack being carried, opened by sneaking and right-clicking the
     * air.
     *
     * <p>The heaps inside can be moved about; what is inside <em>them</em> stays out of
     * reach, because nothing carried offers a window onto its contents. Taking a heap
     * out of a bag is moving an item, not drawing from a store.
     */
    public static HorreumMenu inHand(int id, Inventory inventory, InteractionHand hand) {
        return new HorreumMenu(AcervusRegistry.CARRIED_HORREUM_MENU.get(), id, inventory,
                new InHand(inventory.player, hand));
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

    /** A rack standing in the world: valid while the player is near it. */
    private record AtBlock(Level level, BlockPos pos, ContainerLevelAccess access,
            NonNullList<ItemStack> spare) implements Source {
        AtBlock(Level level, BlockPos pos) {
            this(level, pos, ContainerLevelAccess.create(level, pos),
                    NonNullList.withSize(HorreumBlockEntity.SLOTS, ItemStack.EMPTY));
        }

        private HorreumBlockEntity rack() {
            return level.getBlockEntity(pos) instanceof HorreumBlockEntity rack ? rack : null;
        }

        /**
         * A list nobody else holds, for the moment between the block going and the
         * screen noticing. Writing into it changes nothing, which is exactly right.
         */
        @Override
        public NonNullList<ItemStack> heaps() {
            HorreumBlockEntity rack = rack();
            return rack == null ? spare : rack.heaps();
        }

        @Override
        public boolean stillValid(Player player) {
            return AbstractContainerMenu.stillValid(access, player, AcervusRegistry.HORREUM.get());
        }

        @Override
        public void changed() {
            HorreumBlockEntity rack = rack();
            if (rack != null) {
                rack.changed();
            }
        }
    }

    /**
     * A rack being carried: valid while that hand still holds one.
     *
     * <p>The heaps are read out of the item once and written back on every change. The
     * item is the thing that survives being dropped, so it is the copy that counts.
     */
    private record InHand(Player player, InteractionHand hand,
            NonNullList<ItemStack> heaps) implements Source {
        InHand(Player player, InteractionHand hand) {
            this(player, hand, read(player, hand));
        }

        private static NonNullList<ItemStack> read(Player player, InteractionHand hand) {
            NonNullList<ItemStack> heaps =
                    NonNullList.withSize(HorreumBlockEntity.SLOTS, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(
                    player.getItemInHand(hand)
                            .getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY)
                            .copyTag().getCompound("Heaps"),
                    heaps, player.level().registryAccess());
            return heaps;
        }

        @Override
        public boolean stillValid(Player viewer) {
            return player.getItemInHand(hand).getItem() == AcervusRegistry.HORREUM_ITEM.get();
        }

        /**
         * Through {@code setBlockEntityData}, which names the block entity in the tag -
         * see {@link Held#write}. Writing it any other way takes the world down at the
         * next autosave.
         */
        @Override
        public void changed() {
            if (player.level().isClientSide) {
                return;
            }
            net.minecraft.nbt.CompoundTag inside = new net.minecraft.nbt.CompoundTag();
            ContainerHelper.saveAllItems(inside, heaps, true, player.level().registryAccess());
            BlockItem.setBlockEntityData(player.getItemInHand(hand),
                    AcervusRegistry.HORREUM_ENTITY.get(), inside);
        }

        @Override
        public int frozen() {
            return hand == InteractionHand.MAIN_HAND ? player.getInventory().selected : -1;
        }
    }

    /**
     * One slot of the rack, reading and writing the source's list directly.
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
            return source.heaps().get(index);
        }

        @Override
        public boolean hasItem() {
            return !getItem().isEmpty();
        }

        @Override
        public void set(ItemStack stack) {
            source.heaps().set(index, stack);
            source.changed();
        }

        @Override
        public ItemStack remove(int amount) {
            ItemStack taken = source.heaps().get(index);
            source.heaps().set(index, ItemStack.EMPTY);
            source.changed();
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
            source.changed();
        }
    }
}
