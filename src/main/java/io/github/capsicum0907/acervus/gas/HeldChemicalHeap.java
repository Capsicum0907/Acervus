package io.github.capsicum0907.acervus.gas;

import io.github.capsicum0907.acervus.AcervusConfig;
import io.github.capsicum0907.acervus.Counts;
import io.github.capsicum0907.acervus.HeapColors;
import io.github.capsicum0907.acervus.Held;
import io.github.capsicum0907.acervus.Vessel;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class HeldChemicalHeap extends Held {
    private static final String ID = "id";

    private HeldChemicalHeap(HolderLookup.Provider registries, java.util.function.Supplier<ItemStack> where,
            boolean gives) {
        super(registries, where, gives);
    }

    public static HeldChemicalHeap of(HolderLookup.Provider registries, ItemStack stack) {
        return new HeldChemicalHeap(registries, () -> stack, false);
    }

    public static HeldChemicalHeap inHand(Player player, InteractionHand hand) {
        return new HeldChemicalHeap(player.level().registryAccess(), () -> player.getItemInHand(hand), false);
    }

    public static HeldChemicalHeap stored(HolderLookup.Provider registries, ItemStack stack) {
        return new HeldChemicalHeap(registries, () -> stack, true);
    }

    public HolderLookup.Provider registries() {
        return registries;
    }

    public CompoundTag kind() {
        CompoundTag tag = tag();
        return tag.contains(SAMPLE) ? tag.getCompound(SAMPLE) : null;
    }

    @Override
    public long amount() {
        return Counts.stored(tag(), AMOUNT);
    }

    @Override
    public long capacity() {
        return AcervusConfig.CHEMICAL_CAPACITY.get();
    }

    @Override
    public boolean isEmpty() {
        return kind() == null || amount() <= 0;
    }

    @Override
    public boolean unreadable() {
        CompoundTag kind = kind();
        return kind != null && registries != null && !Chemistry.get().readable(kind, registries);
    }

    @Override
    public String unreadableId() {
        return unreadable() ? kind().getString(ID) : "";
    }

    @Override
    public Component contentName() {
        CompoundTag kind = kind();
        if (kind == null) {
            return Component.empty();
        }
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
    public int tint() {
        return HeapColors.CHEMICAL;
    }

    @Override
    public ResourceLocation contentTexture() {
        CompoundTag kind = kind();
        return kind == null || registries == null ? null : Chemistry.get().icon(kind, registries);
    }

    @Override
    public int contentTint() {
        CompoundTag kind = kind();
        return kind == null || registries == null ? 0xFFFFFFFF : Chemistry.get().tint(kind, registries);
    }

    public boolean accepts(CompoundTag offered) {
        if (offered == null || unreadable()) {
            return false;
        }
        CompoundTag kind = kind();
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
        CompoundTag tag = tag();
        boolean first = !tag.contains(SAMPLE);
        long held = first ? 0L : amount();
        if (first) {
            tag.put(SAMPLE, offered.copy());
        }
        tag.putLong(AMOUNT, held + taken);
        write(tag, false);
        return taken;
    }

    public long extractAmount(long wanted, boolean simulate) {
        if (!gives() || unreadable() || isEmpty() || wanted <= 0L) {
            return 0L;
        }
        long taken = Math.min(wanted, amount());
        if (simulate) {
            return taken;
        }
        CompoundTag tag = tag();
        long left = amount() - taken;
        if (left <= 0L) {
            emptied(tag);
            left = 0L;
        }
        tag.putLong(AMOUNT, left);
        write(tag, left <= 0L);
        return taken;
    }

    @Override
    protected BlockEntityType<?> type() {
        return GasHeap.BLOCK_ENTITY.get();
    }

    @Override
    public boolean emptyContainer(ItemStack stack) {
        return Chemistry.get().emptyContainer(stack);
    }

    @Override
    public void draw(Vessel vessel) {
        Chemistry.get().draw(this, vessel);
    }
}
