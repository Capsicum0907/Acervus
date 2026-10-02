package io.github.capsicum0907.acervus;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class HeldEnergyHeap extends Held {
    private static final String LEGACY_STORED = "Stored";

    private HeldEnergyHeap(HolderLookup.Provider registries, java.util.function.Supplier<ItemStack> where,
            boolean gives) {
        super(registries, where, gives);
    }

    public static HeldEnergyHeap of(HolderLookup.Provider registries, ItemStack stack) {
        return new HeldEnergyHeap(registries, () -> stack, false);
    }

    public static HeldEnergyHeap inHand(Player player, InteractionHand hand) {
        return new HeldEnergyHeap(player.level().registryAccess(), () -> player.getItemInHand(hand), false);
    }

    public static HeldEnergyHeap stored(HolderLookup.Provider registries, ItemStack stack) {
        return new HeldEnergyHeap(registries, () -> stack, true);
    }

    @Override
    public long amount() {
        CompoundTag tag = tag();
        return Counts.stored(tag, AMOUNT, LEGACY_STORED);
    }

    @Override
    public long capacity() {
        return AcervusConfig.ENERGY_CAPACITY.get();
    }

    @Override
    public boolean isEmpty() {
        return amount() <= 0;
    }

    @Override
    public boolean hasKinds() {
        return false;
    }

    @Override
    public Component contentName() {
        return Component.empty();
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
        return HeapColors.ENERGY;
    }

    public int receive(int offered) {
        if (offered <= 0) {
            return 0;
        }
        int taken = (int) Math.min(room(), offered);
        if (taken <= 0) {
            return 0;
        }
        CompoundTag tag = tag();
        tag.putLong(AMOUNT, amount() + taken);
        tag.remove(LEGACY_STORED);
        write(tag, false);
        return taken;
    }

    public int give(int wanted) {
        if (!gives() || wanted <= 0 || isEmpty()) {
            return 0;
        }
        int given = (int) Math.min(Math.min(wanted, amount()), Integer.MAX_VALUE);
        if (given <= 0) {
            return 0;
        }
        long left = amount() - given;
        CompoundTag tag = tag();
        tag.putLong(AMOUNT, left);
        tag.remove(LEGACY_STORED);
        write(tag, left <= 0);
        return given;
    }

    @Override
    protected net.minecraft.world.level.block.entity.BlockEntityType<?> type() {
        return AcervusRegistry.ENERGY_HEAP_ENTITY.get();
    }

    @Override
    public boolean emptyContainer(net.minecraft.world.item.ItemStack stack) {
        return EnergyHeapBlockEntity.isEmptyContainer(stack);
    }

    @Override
    public void draw(Vessel vessel) {
        IEnergyStorage container = vessel.held().getCapability(Capabilities.EnergyStorage.ITEM);
        if (container == null) {
            return;
        }
        int rate = (int) Math.min(AcervusConfig.ENERGY_PUSH_RATE.get(), Integer.MAX_VALUE);
        int taken = container.extractEnergy((int) Math.min(rate, room()), false);
        if (taken > 0) {
            receive(taken);
        }
    }
}
