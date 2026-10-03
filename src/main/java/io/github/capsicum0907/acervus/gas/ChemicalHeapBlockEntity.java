package io.github.capsicum0907.acervus.gas;

import io.github.capsicum0907.acervus.AcervusConfig;
import io.github.capsicum0907.acervus.Counts;
import io.github.capsicum0907.acervus.Creative;
import io.github.capsicum0907.acervus.HasVessel;
import io.github.capsicum0907.acervus.HeapColors;
import io.github.capsicum0907.acervus.Heaped;
import io.github.capsicum0907.acervus.Vessel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ChemicalHeapBlockEntity extends BlockEntity implements Heaped, HasVessel {
    private static final String SAMPLE = "Sample";
    private static final String LOCKED = "Locked";
    private static final String AMOUNT = "Amount";
    private static final String ID = "id";

    private CompoundTag kind;
    private long amount;
    private boolean locked;

    private final Vessel in = new Vessel(Vessel.Flow.IN);
    private final Vessel out = new Vessel(Vessel.Flow.OUT);

    public ChemicalHeapBlockEntity(BlockPos pos, BlockState state) {
        super(GasHeap.BLOCK_ENTITY.get(), pos, state);
    }

    public HolderLookup.Provider registries() {
        return level == null ? null : level.registryAccess();
    }

    public CompoundTag kind() {
        return kind == null ? null : kind.copy();
    }

    @Override
    public boolean isEmpty() {
        return kind == null || (amount <= 0 && !infinite());
    }

    @Override
    public boolean infinite() {
        return Creative.is(this);
    }

    @Override
    public boolean unreadable() {
        HolderLookup.Provider registries = registries();
        return kind != null && registries != null && !Chemistry.get().readable(kind, registries);
    }

    @Override
    public String unreadableId() {
        return unreadable() ? kind.getString(ID) : "";
    }

    @Override
    public long amount() {
        if (infinite()) {
            return kind == null ? 0L : Long.MAX_VALUE;
        }
        return amount;
    }

    @Override
    public long capacity() {
        return infinite() ? Long.MAX_VALUE : AcervusConfig.CHEMICAL_CAPACITY.get();
    }

    @Override
    public long room() {
        return infinite() ? 0L : Math.max(0L, capacity() - amount);
    }

    public boolean accepts(CompoundTag offered) {
        if (infinite() || offered == null || unreadable()) {
            return false;
        }
        return kind == null || kind.getString(ID).equals(offered.getString(ID));
    }

    public long insertKind(CompoundTag offered, long wanted, boolean simulate) {
        if (wanted <= 0L || !accepts(offered)) {
            return 0L;
        }
        long taken = Math.min(room(), wanted);
        if (taken <= 0L || simulate) {
            return Math.max(taken, 0L);
        }
        if (kind == null) {
            kind = offered.copy();
            amount = 0L;
        }
        amount += taken;
        changed();
        return taken;
    }

    public long intakeKind(CompoundTag offered, long wanted) {
        if (!infinite()) {
            return insertKind(offered, wanted, false);
        }
        if (offered == null || wanted <= 0L) {
            return 0L;
        }
        kind = offered.copy();
        changed();
        return wanted;
    }

    public long extractAmount(long wanted, boolean simulate) {
        if (unreadable() || isEmpty() || wanted <= 0L) {
            return 0L;
        }
        if (infinite()) {
            return wanted;
        }
        long taken = Math.min(wanted, amount);
        if (simulate) {
            return taken;
        }
        amount -= taken;
        if (amount <= 0L) {
            if (!locked) {
                kind = null;
            }
            amount = 0L;
        }
        changed();
        return taken;
    }

    @Override
    public Vessel vessel(Vessel.Flow flow) {
        return flow == Vessel.Flow.IN ? in : out;
    }

    @Override
    public boolean emptyContainer(ItemStack stack) {
        return Chemistry.get().emptyContainer(stack);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ChemicalHeapBlockEntity heap) {
        if (level.isClientSide) {
            return;
        }
        if (Chemistry.get().tickVessels(heap)) {
            heap.changed();
        }
    }

    @Override
    public Component contentName() {
        if (kind == null) {
            return Component.empty();
        }
        HolderLookup.Provider registries = registries();
        return registries == null ? Component.literal(kind.getString(ID)) : Chemistry.get().name(kind, registries);
    }

    @Override
    public String brief(long value) {
        return Counts.brief(value);
    }

    @Override
    public String exact(long value) {
        return Counts.exact(value);
    }

    @Override
    public ResourceLocation contentTexture() {
        HolderLookup.Provider registries = registries();
        return kind == null || registries == null ? null : Chemistry.get().icon(kind, registries);
    }

    @Override
    public int contentTint() {
        HolderLookup.Provider registries = registries();
        return kind == null || registries == null ? 0xFFFFFFFF : Chemistry.get().tint(kind, registries);
    }

    @Override
    public int tint() {
        return HeapColors.CHEMICAL;
    }

    @Override
    public boolean locked() {
        return locked;
    }

    @Override
    public boolean canLock() {
        return !infinite() && !isEmpty() && !unreadable();
    }

    @Override
    public void lock(boolean on) {
        if (unreadable() || on == locked || (on && !canLock())) {
            return;
        }
        locked = on;
        if (!on && amount <= 0L) {
            kind = null;
        }
        changed();
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
        tag.putBoolean(LOCKED, locked);
        in.save(tag, registries);
        out.save(tag, registries);
        if (kind != null) {
            tag.put(SAMPLE, kind.copy());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        kind = tag.contains(SAMPLE) ? tag.getCompound(SAMPLE).copy() : null;
        locked = tag.getBoolean(LOCKED) && kind != null;
        amount = kind == null ? 0L : Counts.stored(tag, AMOUNT);
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
