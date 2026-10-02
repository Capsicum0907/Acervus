package io.github.capsicum0907.acervus;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

public final class Storable {
    private Storable() {
    }

    public static boolean allowed(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof ContentsBlockItem || !item.canFitInsideContainerItems()
                || item.components().has(DataComponents.BUNDLE_CONTENTS)) {
            return false;
        }
        ItemContainerContents contents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        return contents.nonEmptyStream().findAny().isEmpty() && !stack.has(DataComponents.BLOCK_ENTITY_DATA);
    }
}
