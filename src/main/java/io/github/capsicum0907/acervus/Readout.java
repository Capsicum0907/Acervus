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
    public record Line(Icon icon, Component name, String amount) {
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
                        add(lines, carried(rack.heap(slot), registries));
                    }
                }
            }
            default -> {
            }
        }
        return lines;
    }

    public static List<Line> of(ItemStack stack, HolderLookup.Provider registries) {
        List<Line> lines = new ArrayList<>();
        if (stack.getItem() == AcervusRegistry.HORREUM_ITEM.get()) {
            for (ItemStack heap : HorreumBlockEntity.readHeaps(stack, registries)) {
                add(lines, carried(heap, registries));
            }
        } else {
            add(lines, carried(stack, registries));
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
            return new Line(Icon.MISSING, Component.literal(heap.unreadableId()), Counts.brief(heap.count()));
        }
        if (heap.isEmpty()) {
            return null;
        }
        return new Line(Icon.of(heap.sample()), heap.sample().getHoverName(),
                heap.infinite() ? Counts.INFINITE : Counts.brief(heap.count()));
    }

    private static Line line(Heaped heap) {
        if (heap.isEmpty()) {
            return null;
        }
        String amount = heap.infinite() ? Counts.INFINITE : heap.brief(heap.amount());
        if (heap.unreadable()) {
            return new Line(Icon.MISSING, Component.literal(heap.unreadableId()), amount);
        }
        if (!heap.hasKinds()) {
            return new Line(Icon.ENERGY, Component.translatable("gui.acervus.energy"), amount);
        }
        return new Line(icon(heap), heap.contentName(), amount);
    }

    private static Icon icon(Heaped heap) {
        return switch (heap) {
            case FluidHeapBlockEntity fluid -> Icon.of(fluid.sample());
            case HeldFluidHeap fluid -> Icon.of(fluid.sample());
            default -> Icon.sprite(heap.contentTexture(), heap.contentTint());
        };
    }

    private static Line carried(ItemStack heap, HolderLookup.Provider registries) {
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
