package io.github.capsicum0907.acervus;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class Vessel {
    private static final String LEGACY_HELD = "Vessel";
    private static final String LEGACY_FLOW = "VesselFlow";

    public enum Flow {
        IN("VesselIn"),
        OUT("VesselOut");

        private final String key;

        Flow(String key) {
            this.key = key;
        }
    }

    private final Flow flow;
    private ItemStack held = ItemStack.EMPTY;

    public Vessel(Flow flow) {
        this.flow = flow;
    }

    public Flow flow() {
        return flow;
    }

    public ItemStack held() {
        return held;
    }

    public boolean isEmpty() {
        return held.isEmpty();
    }

    public void hold(ItemStack stack) {
        held = stack;
    }

    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        if (!held.isEmpty()) {
            tag.put(flow.key, held.save(registries));
        }
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        String key = tag.contains(flow.key) ? flow.key : claimsLegacy(tag) ? LEGACY_HELD : null;
        held = key == null
                ? ItemStack.EMPTY
                : ItemStack.parse(registries, tag.getCompound(key)).orElse(ItemStack.EMPTY);
    }

    private boolean claimsLegacy(CompoundTag tag) {
        if (!tag.contains(LEGACY_HELD)) {
            return false;
        }
        boolean wentOut = Flow.OUT.name().equals(tag.getString(LEGACY_FLOW));
        return wentOut == (flow == Flow.OUT);
    }
}
