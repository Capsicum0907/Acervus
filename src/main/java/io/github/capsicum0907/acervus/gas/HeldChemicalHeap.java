package io.github.capsicum0907.acervus.gas;

import io.github.capsicum0907.acervus.AcervusConfig;
import io.github.capsicum0907.acervus.Counts;
import io.github.capsicum0907.acervus.Held;
import io.github.capsicum0907.acervus.Vessel;

import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class HeldChemicalHeap extends Held {
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

    public ChemicalStack sample() {
        CompoundTag tag = tag();
        return tag.contains(SAMPLE)
                ? ChemicalStack.parseOptional(registries, tag.getCompound(SAMPLE))
                : ChemicalStack.EMPTY;
    }

    @Override
    public long amount() {
        return tag().getLong(AMOUNT);
    }

    @Override
    public long capacity() {
        return AcervusConfig.CHEMICAL_CAPACITY.get();
    }

    @Override
    public boolean isEmpty() {
        return sample().isEmpty() || amount() <= 0;
    }

    @Override
    public Component contentName() {
        ChemicalStack sample = sample();
        return sample.isEmpty() ? Component.empty() : sample.getChemical().getTextComponent();
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
        return io.github.capsicum0907.acervus.HeapColors.CHEMICAL;
    }

    @Override
    public ResourceLocation contentTexture() {
        ChemicalStack sample = sample();
        return sample.isEmpty() ? null : sample.getChemical().getIcon();
    }

    @Override
    public int contentTint() {
        ChemicalStack sample = sample();
        return sample.isEmpty() ? 0xFFFFFFFF : sample.getChemicalTint();
    }

    public long insert(ChemicalStack stack, boolean simulate) {
        ChemicalStack sample = sample();
        if (stack.isEmpty() || (!sample.isEmpty() && !ChemicalStack.isSameChemical(sample, stack))) {
            return 0L;
        }
        long taken = Math.min(room(), stack.getAmount());
        if (taken <= 0 || simulate) {
            return Math.max(taken, 0L);
        }

        CompoundTag tag = tag();
        long held = sample.isEmpty() ? 0L : amount();
        if (sample.isEmpty()) {
            tag.put(SAMPLE, stack.copyWithAmount(1).save(registries));
        }
        tag.putLong(AMOUNT, held + taken);
        write(tag, false);
        return taken;
    }

    public ChemicalStack extract(long wanted, boolean simulate) {
        if (!gives() || isEmpty() || wanted <= 0) {
            return ChemicalStack.EMPTY;
        }
        long taken = Math.min(wanted, amount());
        ChemicalStack out = sample().copyWithAmount(taken);
        if (simulate) {
            return out;
        }

        CompoundTag tag = tag();
        long left = amount() - taken;
        if (left <= 0) {
            emptied(tag);
            left = 0;
        }
        tag.putLong(AMOUNT, left);
        write(tag, left <= 0);
        return out;
    }

    @Override
    protected net.minecraft.world.level.block.entity.BlockEntityType<?> type() {
        return GasHeap.BLOCK_ENTITY.get();
    }

    @Override
    public boolean emptyContainer(net.minecraft.world.item.ItemStack stack) {
        return ChemicalHeapBlockEntity.isEmptyContainer(stack);
    }

    @Override
    public void draw(Vessel vessel) {
        IChemicalHandler container = vessel.held().getCapability(GasHeap.CHEMICAL_ITEM);
        if (container == null) {
            return;
        }
        for (int tank = 0; tank < container.getChemicalTanks(); tank++) {
            ChemicalStack inside = container.getChemicalInTank(tank);
            long taken = inside.isEmpty() ? 0L : insert(inside, false);
            if (taken > 0) {
                container.extractChemical(tank, taken, Action.EXECUTE);
                return;
            }
        }
    }
}
