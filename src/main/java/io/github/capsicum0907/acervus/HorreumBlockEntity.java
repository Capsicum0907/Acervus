package io.github.capsicum0907.acervus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class HorreumBlockEntity extends BlockEntity {
    public static final int SLOTS = 9;

    private static final String HEAPS = "Heaps";
    private static final String ITEMS = "Items";
    private static final String SLOT = "Slot";

    private final NonNullList<ItemStack> heaps = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private final List<ItemStack> waiting = new ArrayList<>();

    private final HorreumItemHandler items = new HorreumItemHandler(this);
    private final HorreumFluidHandler fluids = new HorreumFluidHandler(this);
    private final HorreumEnergyHandler energy = new HorreumEnergyHandler(this);

    public HorreumBlockEntity(BlockPos pos, BlockState state) {
        super(AcervusRegistry.HORREUM_ENTITY.get(), pos, state);
    }

    public NonNullList<ItemStack> heaps() {
        return heaps;
    }

    public ItemStack heap(int slot) {
        return slot < 0 || slot >= SLOTS ? ItemStack.EMPTY : heaps.get(slot);
    }

    public HorreumItemHandler items() {
        return items;
    }

    public HorreumFluidHandler fluids() {
        return fluids;
    }

    public HorreumEnergyHandler energy() {
        return energy;
    }

    public <T> T read(int slot, net.minecraft.world.item.Item kind,
            BiFunction<HolderLookup.Provider, ItemStack, T> reader) {
        ItemStack stack = heap(slot);
        if (level == null || stack.isEmpty() || stack.getItem() != kind) {
            return null;
        }
        return reader.apply(level.registryAccess(), stack);
    }

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

    public static NonNullList<ItemStack> readHeaps(ItemStack rack, HolderLookup.Provider registries) {
        NonNullList<ItemStack> heaps = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        load(onItem(rack), heaps, new ArrayList<>(), registries);
        return heaps;
    }

    public static void writeHeaps(ItemStack rack, NonNullList<ItemStack> heaps,
            HolderLookup.Provider registries) {
        List<ItemStack> waiting = new ArrayList<>();
        load(onItem(rack), NonNullList.withSize(SLOTS, ItemStack.EMPTY), waiting, registries);
        settle(heaps, waiting);
        CompoundTag tag = new CompoundTag();
        tag.put(HEAPS, saved(heaps, waiting, registries));
        BlockItem.setBlockEntityData(rack, AcervusRegistry.HORREUM_ENTITY.get(), tag);
    }

    private static CompoundTag onItem(ItemStack rack) {
        return rack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).copyTag().getCompound(HEAPS);
    }

    private static void load(CompoundTag inside, NonNullList<ItemStack> heaps, List<ItemStack> waiting,
            HolderLookup.Provider registries) {
        heaps.clear();
        waiting.clear();
        ListTag list = inside.getList(ITEMS, Tag.TAG_COMPOUND);
        for (int entry = 0; entry < list.size(); entry++) {
            CompoundTag one = list.getCompound(entry);
            int slot = one.getByte(SLOT) & 0xFF;
            ItemStack stack = ItemStack.parse(registries, one).orElse(ItemStack.EMPTY);
            if (stack.isEmpty()) {
                continue;
            }
            if (slot < SLOTS) {
                heaps.set(slot, stack);
            } else {
                waiting.add(stack);
            }
        }
        settle(heaps, waiting);
    }

    private static void settle(NonNullList<ItemStack> heaps, List<ItemStack> waiting) {
        for (int slot = 0; slot < SLOTS && !waiting.isEmpty(); slot++) {
            if (heaps.get(slot).isEmpty()) {
                heaps.set(slot, waiting.remove(0));
            }
        }
    }

    private static CompoundTag saved(NonNullList<ItemStack> heaps, List<ItemStack> waiting,
            HolderLookup.Provider registries) {
        CompoundTag inside = new CompoundTag();
        ContainerHelper.saveAllItems(inside, heaps, true, registries);
        ListTag list = inside.getList(ITEMS, Tag.TAG_COMPOUND);
        for (int index = 0; index < waiting.size(); index++) {
            CompoundTag one = (CompoundTag) waiting.get(index).save(registries, new CompoundTag());
            one.putByte(SLOT, (byte) (SLOTS + index));
            list.add(one);
        }
        inside.put(ITEMS, list);
        return inside;
    }

    public int waiting() {
        return waiting.size();
    }

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
        if (!waiting.isEmpty()) {
            return false;
        }
        for (ItemStack stack : heaps) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public void changed() {
        settle(heaps, waiting);
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(HEAPS, saved(heaps, waiting, registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        load(tag.getCompound(HEAPS), heaps, waiting, registries);
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
