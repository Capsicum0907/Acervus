package io.github.capsicum0907.acervus.gas.mek;

import io.github.capsicum0907.acervus.AcervusRegistry;
import io.github.capsicum0907.acervus.Vessel;
import io.github.capsicum0907.acervus.gas.ChemicalHeapBlockEntity;
import io.github.capsicum0907.acervus.gas.GasHeap;
import io.github.capsicum0907.acervus.gas.Chemistry;
import io.github.capsicum0907.acervus.gas.HeldChemicalHeap;

import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class MekanismChemistry implements Chemistry {
    public static final Chemistry INSTANCE = new MekanismChemistry();

    private static final String MEKANISM = "mekanism";
    private static final String AMOUNT = "amount";

    public static final BlockCapability<IChemicalHandler, Direction> CHEMICAL_HANDLER = BlockCapability.createSided(
            ResourceLocation.fromNamespaceAndPath(MEKANISM, "chemical_handler"), IChemicalHandler.class);

    public static final ItemCapability<IChemicalHandler, Void> CHEMICAL_ITEM = ItemCapability.createVoid(
            ResourceLocation.fromNamespaceAndPath(MEKANISM, "chemical_handler"), IChemicalHandler.class);

    private MekanismChemistry() {
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(CHEMICAL_HANDLER, GasHeap.BLOCK_ENTITY.get(),
                (heap, side) -> new ChemicalHeapHandler(heap));
        event.registerBlockEntity(CHEMICAL_HANDLER, AcervusRegistry.HORREUM_ENTITY.get(),
                (rack, side) -> new HorreumChemicalHandler(rack));
    }

    public static ChemicalStack stack(CompoundTag kind, long amount, HolderLookup.Provider registries) {
        if (kind == null || amount <= 0L || registries == null) {
            return ChemicalStack.EMPTY;
        }
        ChemicalStack parsed = parse(kind, registries);
        return parsed.isEmpty() ? ChemicalStack.EMPTY : parsed.copyWithAmount(amount);
    }

    public static CompoundTag kind(ChemicalStack stack, HolderLookup.Provider registries) {
        Tag saved = stack.copyWithAmount(1L).save(registries);
        return saved instanceof CompoundTag compound ? compound : new CompoundTag();
    }

    private static ChemicalStack parse(CompoundTag kind, HolderLookup.Provider registries) {
        if (kind == null || registries == null) {
            return ChemicalStack.EMPTY;
        }
        CompoundTag probe = kind.copy();
        if (!probe.contains(AMOUNT)) {
            probe.putLong(AMOUNT, 1L);
        }
        return ChemicalStack.parseOptional(registries, probe);
    }

    @Override
    public boolean readable(CompoundTag kind, HolderLookup.Provider registries) {
        return !parse(kind, registries).isEmpty();
    }

    @Override
    public Component name(CompoundTag kind, HolderLookup.Provider registries) {
        ChemicalStack parsed = parse(kind, registries);
        return parsed.isEmpty() ? Component.literal(kind.getString("id")) : parsed.getChemical().getTextComponent();
    }

    @Override
    public ResourceLocation icon(CompoundTag kind, HolderLookup.Provider registries) {
        ChemicalStack parsed = parse(kind, registries);
        return parsed.isEmpty() ? null : parsed.getChemical().getIcon();
    }

    @Override
    public int tint(CompoundTag kind, HolderLookup.Provider registries) {
        ChemicalStack parsed = parse(kind, registries);
        return parsed.isEmpty() ? 0xFFFFFFFF : 0xFF000000 | parsed.getChemicalTint();
    }

    @Override
    public boolean emptyContainer(ItemStack stack) {
        IChemicalHandler container = stack.getCapability(CHEMICAL_ITEM);
        if (container == null) {
            return false;
        }
        for (int tank = 0; tank < container.getChemicalTanks(); tank++) {
            if (!container.getChemicalInTank(tank).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean tickVessels(ChemicalHeapBlockEntity heap) {
        boolean moved = false;
        IChemicalHandler emptied = container(heap.vessel(Vessel.Flow.IN));
        if (emptied != null) {
            moved = pourIn(heap, emptied);
        }
        IChemicalHandler filled = container(heap.vessel(Vessel.Flow.OUT));
        if (filled != null) {
            moved |= drawOut(heap, filled);
        }
        return moved;
    }

    @Override
    public void draw(HeldChemicalHeap heap, Vessel vessel) {
        IChemicalHandler container = container(vessel);
        if (container == null) {
            return;
        }
        for (int tank = 0; tank < container.getChemicalTanks(); tank++) {
            ChemicalStack inside = container.getChemicalInTank(tank);
            long taken = inside.isEmpty() ? 0L
                    : heap.insertKind(kind(inside, heap.registries()), inside.getAmount(), false);
            if (taken > 0) {
                container.extractChemical(tank, taken, Action.EXECUTE);
                return;
            }
        }
    }

    private static IChemicalHandler container(Vessel vessel) {
        return vessel.isEmpty() ? null : vessel.held().getCapability(CHEMICAL_ITEM);
    }

    private static boolean pourIn(ChemicalHeapBlockEntity heap, IChemicalHandler container) {
        HolderLookup.Provider registries = heap.registries();
        for (int tank = 0; tank < container.getChemicalTanks(); tank++) {
            ChemicalStack inside = container.getChemicalInTank(tank);
            if (inside.isEmpty()) {
                continue;
            }
            long taken = heap.insertKind(kind(inside, registries), inside.getAmount(), false);
            if (taken > 0) {
                container.extractChemical(tank, taken, Action.EXECUTE);
                return true;
            }
        }
        return false;
    }

    private static boolean drawOut(ChemicalHeapBlockEntity heap, IChemicalHandler container) {
        if (heap.isEmpty() || heap.unreadable()) {
            return false;
        }
        HolderLookup.Provider registries = heap.registries();
        for (int tank = 0; tank < container.getChemicalTanks(); tank++) {
            long most = Math.min(heap.amount(), container.getChemicalTankCapacity(tank));
            ChemicalStack offer = stack(heap.kind(), most, registries);
            if (offer.isEmpty()) {
                continue;
            }
            ChemicalStack left = container.insertChemical(tank, offer, Action.EXECUTE);
            long given = offer.getAmount() - left.getAmount();
            if (given > 0) {
                heap.extractAmount(given, false);
                return true;
            }
        }
        return false;
    }
}
