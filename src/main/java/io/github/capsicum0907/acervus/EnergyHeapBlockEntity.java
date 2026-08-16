package io.github.capsicum0907.acervus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

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
