package io.github.capsicum0907.acervus;

import java.util.ArrayList;
import java.util.List;

import io.github.capsicum0907.acervus.gas.GasHeap;
import io.github.capsicum0907.acervus.gas.HeldChemicalHeap;

import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class Readout {
    public record Line(Component name, String amount) {
        public Component text() {
            return name.copy().append(" ").append(amount);
        }
    }

    private Readout() {
    }

    public static boolean covers(BlockEntity entity) {
        return entity instanceof HeapBlockEntity || entity instanceof Heaped || entity instanceof HorreumBlockEntity;
    }

    public static List<Line> of(BlockEntity entity) {
        List<Line> lines = new ArrayList<>();
        HolderLookup.Provider registries = entity.getLevel() == null ? null : entity.getLevel().registryAccess();
        switch (entity) {
            case HeapBlockEntity heap -> add(lines, pile(heap));
            case Heaped heap -> add(lines, line(heap));
            case HorreumBlockEntity rack -> {
                if (registries != null) {
                    for (int slot = 0; slot < HorreumBlockEntity.SLOTS; slot++) {
                        add(lines, racked(rack.heap(slot), registries));
                    }
                }
            }
            default -> {
            }
        }
        return lines;
    }

    private static void add(List<Line> lines, Line line) {
        if (line != null) {
            lines.add(line);
        }
    }

    private static Line pile(Pile heap) {
        if (heap.unreadable()) {
            return new Line(Component.literal(heap.unreadableId()), Counts.brief(heap.count()));
        }
        if (heap.isEmpty()) {
            return null;
        }
        return new Line(heap.sample().getHoverName(), heap.infinite() ? Counts.INFINITE : Counts.brief(heap.count()));
    }

    private static Line line(Heaped heap) {
        if (heap.isEmpty()) {
            return null;
        }
        Component name = heap.unreadable() ? Component.literal(heap.unreadableId())
                : heap.hasKinds() ? heap.contentName() : Component.translatable("gui.acervus.energy");
        return new Line(name, heap.infinite() ? Counts.INFINITE : heap.brief(heap.amount()));
    }

    private static Line racked(ItemStack heap, HolderLookup.Provider registries) {
        Item kind = heap.getItem();
        if (kind == AcervusRegistry.HEAP_ITEM.get()) {
            return pile(CarriedHeap.of(registries, heap));
        }
        if (kind == AcervusRegistry.FLUID_HEAP_ITEM.get()) {
            return line(HeldFluidHeap.of(registries, heap));
        }
        if (kind == AcervusRegistry.ENERGY_HEAP_ITEM.get()) {
            return line(HeldEnergyHeap.of(registries, heap));
        }
        if (kind == GasHeap.ITEM.get()) {
            return line(HeldChemicalHeap.of(registries, heap));
        }
        return null;
    }
}
