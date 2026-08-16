package io.github.capsicum0907.acervus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
public class EnergyHeapBlockEntity extends BlockEntity {
    private static final String STORED = "Stored";

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
        if (!AcervusConfig.ENERGY_PUSHES.get() || heap.isEmpty() || !(level instanceof ServerLevel server)) {
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
        tag.putLong(STORED, stored);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        stored = tag.getLong(STORED);
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
