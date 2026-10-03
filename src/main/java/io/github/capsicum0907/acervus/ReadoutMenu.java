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
import net.minecraft.world.level.block.entity.BlockEntity;

public class ReadoutMenu extends AbstractContainerMenu {
    private static final int IN_X = 8;
    private static final int OUT_X = 150;
    private static final int VESSEL_Y = 38;
    private static final int INVENTORY_X = 8;
    private static final int INVENTORY_Y = 84;
    private static final int HOTBAR_Y = 142;
    private static final int SLOT = 18;

    private static final int IN_SLOT = 0;
    private static final int OUT_SLOT = 1;
    private static final int PLAYER_FIRST = 2;
    private static final int PLAYER_LAST = PLAYER_FIRST + 36;

    public interface Source {
        Heaped heap();

        Vessel vessel(Vessel.Flow flow);

        boolean stillValid(Player player);

        default int frozen() {
            return -1;
        }

        default void tick() {
        }

        default void vesselChanged() {
        }

        default boolean emptyContainer(ItemStack stack) {
            return false;
        }

        default boolean lendsTheVessel() {
            return false;
        }
    }

    private final Source source;

    private ReadoutMenu(MenuType<?> type, int id, Inventory inventory, Source source) {
        super(type, id);
        this.source = source;

        addSlot(new VesselSlot(source.vessel(Vessel.Flow.IN), IN_X, VESSEL_Y, source::vesselChanged) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return source.heap().takes() && super.mayPlace(stack);
            }
        });
        addSlot(new VesselSlot(source.vessel(Vessel.Flow.OUT), OUT_X, VESSEL_Y, source::vesselChanged) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return source.heap().gives() && super.mayPlace(stack);
            }
        });

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

    public static ReadoutMenu at(int id, Inventory inventory, BlockPos pos) {
        return new ReadoutMenu(AcervusRegistry.READOUT_MENU.get(), id, inventory,
                new AtBlock(inventory.player.level(), pos));
    }

    public static ReadoutMenu inHand(int id, Inventory inventory, InteractionHand hand) {
        return new ReadoutMenu(AcervusRegistry.CARRIED_READOUT_MENU.get(), id, inventory,
                new InHand(inventory.player, hand));
    }

    public Heaped heap() {
        return source.heap();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != MenuButtons.LOCK) {
            return false;
        }
        Heaped heap = heap();
        heap.lock(!heap.locked());
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return source.stillValid(player);
    }

    @Override
    public void broadcastChanges() {
        source.tick();
        super.broadcastChanges();
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!source.lendsTheVessel() || player.level().isClientSide) {
            return;
        }
        for (Vessel.Flow flow : Vessel.Flow.values()) {
            Vessel vessel = source.vessel(flow);
            if (!vessel.isEmpty()) {
                player.getInventory().placeItemBackInInventory(vessel.held());
                vessel.hold(ItemStack.EMPTY);
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack moving = slot.getItem().copy();

        if (index == IN_SLOT || index == OUT_SLOT) {
            if (!moveItemStackTo(slot.getItem(), PLAYER_FIRST, PLAYER_LAST, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            int target = source.emptyContainer(slot.getItem()) ? OUT_SLOT : IN_SLOT;
            if (!moveItemStackTo(slot.getItem(), target, target + 1, false)) {
                return ItemStack.EMPTY;
            }
        }

        slot.setChanged();
        return moving;
    }

    private record AtBlock(Level level, BlockPos pos, ContainerLevelAccess access, Vessel spareIn, Vessel spareOut)
            implements Source {
        AtBlock(Level level, BlockPos pos) {
            this(level, pos, ContainerLevelAccess.create(level, pos),
                    new Vessel(Vessel.Flow.IN), new Vessel(Vessel.Flow.OUT));
        }

        @Override
        public Heaped heap() {
            return level.getBlockEntity(pos) instanceof Heaped heaped ? heaped : Heaped.NONE;
        }

        @Override
        public Vessel vessel(Vessel.Flow flow) {
            if (level.getBlockEntity(pos) instanceof HasVessel holder) {
                return holder.vessel(flow);
            }
            return flow == Vessel.Flow.IN ? spareIn : spareOut;
        }

        @Override
        public boolean emptyContainer(ItemStack stack) {
            return level.getBlockEntity(pos) instanceof HasVessel holder && holder.emptyContainer(stack);
        }

        @Override
        public void vesselChanged() {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity != null) {
                entity.setChanged();
            }
        }

        @Override
        public boolean stillValid(Player player) {
            return access.evaluate((level, at) -> player.distanceToSqr(
                    at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5) <= 64.0, true);
        }
    }

    private record InHand(Player player, InteractionHand hand, Held held, Vessel in, Vessel out)
            implements Source {
        InHand(Player player, InteractionHand hand) {
            this(player, hand, heldHeap(player, hand), new Vessel(Vessel.Flow.IN), new Vessel(Vessel.Flow.OUT));
        }

        @Override
        public Vessel vessel(Vessel.Flow flow) {
            return flow == Vessel.Flow.IN ? in : out;
        }

        @Override
        public boolean emptyContainer(ItemStack stack) {
            return held != null && held.emptyContainer(stack);
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
            if (held != null && !in.isEmpty() && !player.level().isClientSide) {
                held.draw(in);
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

    private static Held heldHeap(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() == AcervusRegistry.FLUID_HEAP_ITEM.get()) {
            return HeldFluidHeap.inHand(player, hand);
        }
        if (stack.getItem() == AcervusRegistry.ENERGY_HEAP_ITEM.get()) {
            return HeldEnergyHeap.inHand(player, hand);
        }
        if (stack.getItem() == io.github.capsicum0907.acervus.gas.GasHeap.ITEM.get()) {
            return io.github.capsicum0907.acervus.gas.HeldChemicalHeap.inHand(player, hand);
        }
        return null;
    }
}
