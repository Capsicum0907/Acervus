package io.github.capsicum0907.acervus.gas;

import io.github.capsicum0907.acervus.Mods;
import io.github.capsicum0907.acervus.Vessel;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public interface Chemistry {
    boolean readable(CompoundTag kind, HolderLookup.Provider registries);

    Component name(CompoundTag kind, HolderLookup.Provider registries);

    ResourceLocation icon(CompoundTag kind, HolderLookup.Provider registries);

    int tint(CompoundTag kind, HolderLookup.Provider registries);

    boolean emptyContainer(ItemStack stack);

    boolean tickVessels(ChemicalHeapBlockEntity heap);

    void draw(HeldChemicalHeap heap, Vessel vessel);

    static Chemistry get() {
        return Mods.mekanism() ? io.github.capsicum0907.acervus.gas.mek.MekanismChemistry.INSTANCE : NONE;
    }

    Chemistry NONE = new Chemistry() {
        @Override
        public boolean readable(CompoundTag kind, HolderLookup.Provider registries) {
            return false;
        }

        @Override
        public Component name(CompoundTag kind, HolderLookup.Provider registries) {
            return Component.literal(kind.getString("id"));
        }

        @Override
        public ResourceLocation icon(CompoundTag kind, HolderLookup.Provider registries) {
            return null;
        }

        @Override
        public int tint(CompoundTag kind, HolderLookup.Provider registries) {
            return 0xFFFFFFFF;
        }

        @Override
        public boolean emptyContainer(ItemStack stack) {
            return false;
        }

        @Override
        public boolean tickVessels(ChemicalHeapBlockEntity heap) {
            return false;
        }

        @Override
        public void draw(HeldChemicalHeap heap, Vessel vessel) {
        }
    };
}
