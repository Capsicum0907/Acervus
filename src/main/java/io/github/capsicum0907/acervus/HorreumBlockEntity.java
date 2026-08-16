package io.github.capsicum0907.acervus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A rack of heaps, and one place to reach all of them.
 *
 * <p>A heap already carries its contents as an item, so a rack of heaps needs no
 * storage of its own: it is a row of slots holding heap items, and every window it
 * offers the outside world is a view over what is in those slots. Nothing is copied
 * in, nothing has to be kept in step, and pulling a heap out takes its contents with
 * it because they were never anywhere else.
 *
 * <p><b>The heaps inside give.</b> A heap in a pocket does not, and the reason was
 * never the item — it was that the price of drawing from a heap is putting a block
 * down. This is that block. See {@link Held#gives()}.
 *
 * <p>Mixed on purpose: one rack holds item, fluid, energy and gas heaps side by side,
 * and offers the matching window for each kind. Sorting them into four different
 * racks would be four blocks and four sets of pipes to say one thing.
 *
 * <p><b>This must never implement {@link net.minecraft.world.Container}.</b> The same
 * reason as {@link HeapBlockEntity}: a hopper prefers the container path, and what it
 * would find there is heap <em>items</em> — it would carry the heaps away rather than
 * their contents, which is a very expensive misunderstanding.
 */
public class HorreumBlockEntity extends BlockEntity {
    /**
     * How many heaps a rack holds.
     *
     * <p>Not a setting. The screen is a picture with twelve slots drawn on it, and a
     * number that could change would have to be a number the picture could draw.
     */
    public static final int SLOTS = 12;

    private static final String HEAPS = "Heaps";

    private final NonNullList<ItemStack> heaps = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    private final HorreumItemHandler items = new HorreumItemHandler(this);
    private final HorreumFluidHandler fluids = new HorreumFluidHandler(this);
    private final HorreumEnergyHandler energy = new HorreumEnergyHandler(this);

    public HorreumBlockEntity(BlockPos pos, BlockState state) {
        super(AcervusRegistry.HORREUM_ENTITY.get(), pos, state);
    }

    /** The heap items themselves, to be read and written in place. */
    public NonNullList<ItemStack> heaps() {
        return heaps;
    }

    public ItemStack heap(int slot) {
        return slot < 0 || slot >= SLOTS ? ItemStack.EMPTY : heaps.get(slot);
    }

    /** One handler each, for the whole block; see {@link HeapBlockEntity#handler}. */
    public HorreumItemHandler items() {
        return items;
    }

    public HorreumFluidHandler fluids() {
        return fluids;
    }

    public HorreumEnergyHandler energy() {
        return energy;
    }

    /**
     * The heap in that slot read as the kind asked for, or null when the slot holds
     * something else — an empty slot, or a heap of a different kind.
     *
     * <p>Every window is built out of this, which is why it takes the reader rather
     * than knowing about any of them: the rack does not care what a fluid is.
     */
    public <T> T read(int slot, net.minecraft.world.item.Item kind,
            BiFunction<HolderLookup.Provider, ItemStack, T> reader) {
        ItemStack stack = heap(slot);
        if (level == null || stack.isEmpty() || stack.getItem() != kind) {
            return null;
        }
        return reader.apply(level.registryAccess(), stack);
    }

    /** Every slot holding a heap of that kind, in slot order. */
    public <T> List<T> readAll(net.minecraft.world.item.Item kind,
            BiFunction<HolderLookup.Provider, ItemStack, T> reader) {
        List<T> found = new ArrayList<>();
        for (int slot = 0; slot < SLOTS; slot++) {
            T one = read(slot, kind, reader);
            if (one != null) {
                found.add(one);
            }
        }
        return found;
    }

    /** Whether that item may go in a rack slot at all. */
    public static boolean isHeap(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        net.minecraft.world.item.Item item = stack.getItem();
        return item == AcervusRegistry.HEAP_ITEM.get()
                || item == AcervusRegistry.FLUID_HEAP_ITEM.get()
                || item == AcervusRegistry.ENERGY_HEAP_ITEM.get()
                || (Mods.mekanism()
                        && item == io.github.capsicum0907.acervus.gas.GasHeap.ITEM.get());
    }

    public boolean isEmpty() {
        for (ItemStack stack : heaps) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Something inside changed. The heaps are drawn in the screen and their amounts
     * are read from the items themselves, so a client that is not told keeps showing
     * what it last saw.
     */
    public void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // Written even when every slot is empty, for the reason given in
        // HeapBlockEntity#saveAdditional: an empty update tag is discarded before it
        // reaches the client, so emptying would never be heard about.
        CompoundTag inside = new CompoundTag();
        ContainerHelper.saveAllItems(inside, heaps, true, registries);
        tag.put(HEAPS, inside);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        heaps.clear();
        ContainerHelper.loadAllItems(tag.getCompound(HEAPS), heaps, registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
