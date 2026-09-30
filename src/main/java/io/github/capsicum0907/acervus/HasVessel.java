package io.github.capsicum0907.acervus;

public interface HasVessel {
    Vessel vessel(Vessel.Flow flow);

    boolean emptyContainer(net.minecraft.world.item.ItemStack stack);
}
