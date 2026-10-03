package io.github.capsicum0907.acervus;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

public abstract class ContentsBlockItem extends BlockItem {
    protected ContentsBlockItem(Block block, Properties properties) {
        super(block, properties.fireResistant());
    }

    @Override
    public boolean canBeHurtBy(ItemStack stack, DamageSource source) {
        return !source.is(DamageTypeTags.IS_EXPLOSION) && super.canBeHurtBy(stack, source);
    }

    public static boolean holdsSomething(ItemStack stack) {
        return stack.has(DataComponents.BLOCK_ENTITY_DATA);
    }

    public static boolean alone(ItemStack stack) {
        return stack.getCount() == 1;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return holdsSomething(stack) ? 1 : super.getMaxStackSize(stack);
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        ItemStack stack = context.getItemInHand();
        if (holdsSomething(stack) && !alone(stack)) {
            Player player = context.getPlayer();
            if (player != null) {
                player.displayClientMessage(Component.translatable("acervus.stacked.place", stack.getHoverName()), true);
            }
            return InteractionResult.FAIL;
        }
        return super.place(context);
    }
}
