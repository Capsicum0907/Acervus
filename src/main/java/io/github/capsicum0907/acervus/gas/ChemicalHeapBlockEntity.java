package io.github.capsicum0907.acervus.gas;

import io.github.capsicum0907.acervus.AcervusConfig;

import mekanism.api.chemical.ChemicalStack;

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
 * What a chemical heap holds: one chemical, and how much of it.
 *
 * <p><b>The only one of the four that can tell the whole truth.</b> Mekanism counts
 * chemicals in longs — {@code getChemicalTankCapacity} returns one, and so does the
 * amount on a stack — so nothing here has to saturate. An item heap, a fluid heap
 * and an energy heap all hold a long internally and then say the largest int they
 * can; this one says what it holds.
 *
 * <p>Everything in this package exists only when Mekanism does, which is why it is
 * in a package of its own: nothing outside it mentions a chemical, so a game without
 * Mekanism never loads a class that would be missing one.
 */
public class ChemicalHeapBlockEntity extends BlockEntity implements io.github.capsicum0907.acervus.Heaped, io.github.capsicum0907.acervus.HasVessel {
    private static final String SAMPLE = "Sample";
    private static final String AMOUNT = "Amount";

    /** Identity only: the chemical, always with an amount of one. */
    private ChemicalStack sample = ChemicalStack.EMPTY;
    private long amount;

    private final ChemicalHeapHandler handler = new ChemicalHeapHandler(this);

    public ChemicalHeapBlockEntity(BlockPos pos, BlockState state) {
        super(GasHeap.BLOCK_ENTITY.get(), pos, state);
    }

    public boolean isEmpty() {
        return sample.isEmpty() || amount <= 0;
    }

    public ChemicalStack sample() {
        return sample.copy();
    }

    public long amount() {
        return amount;
    }

    public long capacity() {
        return AcervusConfig.CHEMICAL_CAPACITY.get();
    }

    public long room() {
        return Math.max(0L, capacity() - amount);
    }

    public ChemicalHeapHandler handler() {
        return handler;
    }

    /** All of it, with no clamp anywhere: the amount is a long at both ends. */
    public ChemicalStack contents() {
        return isEmpty() ? ChemicalStack.EMPTY : sample.copyWithAmount(amount);
    }

    public boolean accepts(ChemicalStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return isEmpty() || ChemicalStack.isSameChemical(sample, stack);
    }

    public boolean holds(ChemicalStack stack) {
        return !isEmpty() && !stack.isEmpty() && ChemicalStack.isSameChemical(sample, stack);
    }

    /** @return how much of the offer was taken */
    public long insert(ChemicalStack stack, boolean simulate) {
        if (!accepts(stack)) {
            return 0L;
        }
        long taken = Math.min(room(), stack.getAmount());
        if (taken <= 0 || simulate) {
            return Math.max(taken, 0L);
        }

        if (isEmpty()) {
            sample = stack.copyWithAmount(1);
            amount = 0;
        }
        amount += taken;
        changed();
        return taken;
    }

    /** @return what was taken out: as much as was asked for, if there is that much */
    public ChemicalStack extract(long wanted, boolean simulate) {
        if (isEmpty() || wanted <= 0) {
            return ChemicalStack.EMPTY;
        }
        long taken = Math.min(wanted, amount);
        ChemicalStack out = sample.copyWithAmount(taken);
        if (simulate) {
            return out;
        }

        amount -= taken;
        if (amount <= 0) {
            sample = ChemicalStack.EMPTY;
            amount = 0;
        }
        changed();
        return out;
    }

    private final io.github.capsicum0907.acervus.Vessel vessel = new io.github.capsicum0907.acervus.Vessel();

    @Override
    public io.github.capsicum0907.acervus.Vessel vessel() {
        return vessel;
    }

    /** A tank in the slot is filled from the heap, or emptied into it. */
    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state,
            ChemicalHeapBlockEntity heap) {
        if (level.isClientSide || heap.vessel.isEmpty()) {
            return;
        }
        mekanism.api.chemical.IChemicalHandler container =
                heap.vessel.held().getCapability(GasHeap.CHEMICAL_ITEM);
        if (container == null) {
            return;
        }

        for (int tank = 0; tank < container.getChemicalTanks(); tank++) {
            ChemicalStack inside = container.getChemicalInTank(tank);
            if (!inside.isEmpty() && heap.accepts(inside)) {
                long moved = heap.insert(inside, false);
                if (moved > 0) {
                    container.extractChemical(tank, moved, mekanism.api.Action.EXECUTE);
                    return;
                }
            }
            if (!heap.isEmpty()) {
                ChemicalStack offer = heap.extract(container.getChemicalTankCapacity(tank), true);
                ChemicalStack left = container.insertChemical(tank, offer, mekanism.api.Action.EXECUTE);
                long given = offer.getAmount() - left.getAmount();
                if (given > 0) {
                    heap.extract(given, false);
                    return;
                }
            }
        }
    }

    @Override
    public net.minecraft.network.chat.Component contentName() {
        return sample.isEmpty() ? net.minecraft.network.chat.Component.empty()
                : sample.getChemical().getTextComponent();
    }

    @Override
    public String brief(long value) {
        return io.github.capsicum0907.acervus.Counts.brief(value);
    }

    @Override
    public String exact(long value) {
        return io.github.capsicum0907.acervus.Counts.exact(value);
    }

    @Override
    public int tint() {
        return 0xFF8FCF8A;
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
        tag.putLong(AMOUNT, amount);
        vessel.save(tag, registries);
        if (!sample.isEmpty()) {
            tag.put(SAMPLE, sample.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        sample = tag.contains(SAMPLE)
                ? ChemicalStack.parseOptional(registries, tag.getCompound(SAMPLE))
                : ChemicalStack.EMPTY;
        amount = sample.isEmpty() ? 0L : tag.getLong(AMOUNT);
        vessel.load(tag, registries);
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
