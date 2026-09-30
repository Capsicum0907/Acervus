package io.github.capsicum0907.acervus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * What an energy heap holds: a number, and nothing else.
 *
 * <p>The simplest of the three, and the difference is worth naming: energy has no
 * identity. An item heap and a fluid heap both keep a sample beside the amount,
 * because "five thousand" means nothing without "of what" — and both therefore have
 * to answer what happens when something else is offered. Energy is energy, so this
 * one has no sample, cannot be locked to a kind, and never refuses anything except
 * for being full.
 */
public class EnergyHeapBlockEntity extends BlockEntity implements Heaped, HasVessel {
    /** The same name every heap uses; see {@link HeapBlockEntity} for why. */
    private static final String AMOUNT = "Amount";
    private static final String LEGACY_STORED = "Stored";

    private long stored;

    private final EnergyHeapHandler handler = new EnergyHeapHandler(this);

    public EnergyHeapBlockEntity(BlockPos pos, BlockState state) {
        super(AcervusRegistry.ENERGY_HEAP_ENTITY.get(), pos, state);
    }

    public boolean isEmpty() {
        return stored <= 0;
    }

    public long stored() {
        return stored;
    }

    public long capacity() {
        return AcervusConfig.ENERGY_CAPACITY.get();
    }

    public long room() {
        return Math.max(0L, capacity() - stored);
    }

    /** One handler for the whole block, handed to every side. */
    public EnergyHeapHandler handler() {
        return handler;
    }

    /** @return how much was taken in, which is never more than was offered */
    public int receive(int offered, boolean simulate) {
        if (offered <= 0) {
            return 0;
        }
        int taken = (int) Math.min(room(), offered);
        if (taken > 0 && !simulate) {
            stored += taken;
            changed();
        }
        return Math.max(taken, 0);
    }

    /** @return how much was handed out, which is never more than was asked for */
    public int give(int wanted, boolean simulate) {
        if (wanted <= 0 || isEmpty()) {
            return 0;
        }
        int given = (int) Math.min(Math.min(wanted, stored), Integer.MAX_VALUE);
        if (given > 0 && !simulate) {
            stored -= given;
            changed();
        }
        return Math.max(given, 0);
    }

    @Override
    public boolean hasKinds() {
        return false;
    }

    /** Energy has no kinds, so there is nothing to name. */
    @Override
    public Component contentName() {
        return Component.empty();
    }

    @Override
    public long amount() {
        return stored;
    }

    @Override
    public String brief(long value) {
        return Counts.brief(value) + " FE";
    }

    @Override
    public String exact(long value) {
        return Counts.exact(value) + " FE";
    }

    @Override
    public String power(long value) {
        return Counts.power(value) + " FE";
    }

    @Override
    public int tint() {
        return 0xFFD8A24A;
    }

    private final Vessel in = new Vessel(Vessel.Flow.IN);
    private final Vessel out = new Vessel(Vessel.Flow.OUT);

    @Override
    public Vessel vessel(Vessel.Flow flow) {
        return flow == Vessel.Flow.IN ? in : out;
    }

    @Override
    public boolean emptyContainer(net.minecraft.world.item.ItemStack stack) {
        return isEmptyContainer(stack);
    }

    public static boolean isEmptyContainer(net.minecraft.world.item.ItemStack stack) {
        IEnergyStorage container = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        return container != null && container.getEnergyStored() <= 0;
    }

    private void tickVessels() {
        int rate = (int) Math.min(AcervusConfig.ENERGY_PUSH_RATE.get(), Integer.MAX_VALUE);
        IEnergyStorage emptying = battery(in);
        if (emptying != null) {
            int taken = emptying.extractEnergy((int) Math.min(rate, room()), false);
            if (taken > 0) {
                receive(taken, false);
            }
        }
        IEnergyStorage filling = battery(out);
        if (filling != null) {
            int given = filling.receiveEnergy((int) Math.min(rate, stored), false);
            if (given > 0) {
                give(given, false);
            }
        }
    }

    private static IEnergyStorage battery(Vessel vessel) {
        return vessel.isEmpty() ? null : vessel.held().getCapability(Capabilities.EnergyStorage.ITEM);
    }

    /**
     * Offering what it holds to whatever is touching it, once a tick.
     *
     * <p>An energy heap pushes; the item and fluid heaps do not. That is not an
     * inconsistency, it is the ecosystem: a hopper comes and takes items, a pump comes
     * and takes fluid, and a machine that wants power sits there waiting to be given
     * some. A store that only answered when asked would sit full beside a furnace that
     * never asked — which is exactly what this one did until it was watched.
     *
     * <p>The neighbours are looked up through a cache rather than every tick, because
     * a capability lookup is a map search and this happens twenty times a second per
     * heap.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, EnergyHeapBlockEntity heap) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        // The vessel first, and unconditionally: a battery in the slot is a person
        // asking, and turning pushing off is about cables rather than about them.
        heap.tickVessels();
        heap.showLamps();
        if (!AcervusConfig.ENERGY_PUSHES.get() || heap.isEmpty()) {
            return;
        }

        int rate = (int) Math.min(AcervusConfig.ENERGY_PUSH_RATE.get(), Integer.MAX_VALUE);
        for (Direction side : Direction.values()) {
            IEnergyStorage neighbour = heap.neighbour(server, side);
            if (neighbour == null || !neighbour.canReceive()) {
                continue;
            }
            int offered = (int) Math.min(Math.min(rate, heap.stored), Integer.MAX_VALUE);
            int taken = neighbour.receiveEnergy(offered, false);
            if (taken > 0) {
                heap.give(taken, false);
            }
            if (heap.isEmpty()) {
                return;
            }
        }
    }

    private void showLamps() {
        BlockState state = getBlockState();
        if (level == null || !state.hasProperty(EnergyHeapBlock.LAMPS)) {
            return;
        }
        int lit = EnergyHeapBlock.lamps(stored, capacity());
        if (state.getValue(EnergyHeapBlock.LAMPS) != lit) {
            level.setBlock(getBlockPos(), state.setValue(EnergyHeapBlock.LAMPS, lit), Block.UPDATE_CLIENTS);
        }
    }

    private IEnergyStorage neighbour(ServerLevel level, Direction side) {
        BlockCapabilityCache<IEnergyStorage, Direction> cache = neighbours[side.ordinal()];
        if (cache == null) {
            cache = BlockCapabilityCache.create(Capabilities.EnergyStorage.BLOCK, level,
                    getBlockPos().relative(side), side.getOpposite());
            neighbours[side.ordinal()] = cache;
        }
        return cache.getCapability();
    }

    @SuppressWarnings("unchecked")
    private final BlockCapabilityCache<IEnergyStorage, Direction>[] neighbours =
            new BlockCapabilityCache[Direction.values().length];

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong(AMOUNT, stored);
        in.save(tag, registries);
        out.save(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        stored = tag.contains(AMOUNT) ? tag.getLong(AMOUNT) : tag.getLong(LEGACY_STORED);
        in.load(tag, registries);
        out.load(tag, registries);
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
