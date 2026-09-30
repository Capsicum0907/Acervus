package io.github.capsicum0907.acervus;

import java.util.List;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public class HeapBlock extends BaseEntityBlock {
    public static final MapCodec<HeapBlock> CODEC = simpleCodec(HeapBlock::new);

    public HeapBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HeapBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof HeapBlockEntity heap)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!heap.holds(held)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        int taken = player.isSecondaryUseActive()
                ? insertEveryMatch(player, heap)
                : insertOne(held, heap);
        if (taken == 0) {
            return ItemInteractionResult.CONSUME;
        }
        say(level, pos, SoundEvents.ITEM_PICKUP, 0.9F);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof HeapBlockEntity)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer server) {
            server.openMenu(new SimpleMenuProvider(
                            (id, inventory, viewer) -> HeapMenu.at(id, inventory, pos),
                            state.getBlock().getName()),
                    buffer -> buffer.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }

    private static int insertOne(ItemStack held, HeapBlockEntity heap) {
        int taken = heap.insert(held, false);
        held.shrink(taken);
        return taken;
    }

    private static int insertEveryMatch(Player player, HeapBlockEntity heap) {
        int total = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (heap.holds(stack)) {
                total += insertOne(stack, heap);
            }
        }
        return total;
    }

    private static void say(Level level, BlockPos pos, net.minecraft.sounds.SoundEvent sound, float pitch) {
        level.playSound(null, pos, sound, SoundSource.BLOCKS, 0.4F, pitch);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack dropped = new ItemStack(this);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof HeapBlockEntity heap
                && (!heap.isEmpty() || heap.locked())) {
            heap.saveToItem(dropped, params.getLevel().registryAccess());
        }
        return List.of(dropped);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult target,
            net.minecraft.world.level.LevelReader level, BlockPos pos, Player player) {
        ItemStack picked = new ItemStack(this);
        if (level.getBlockEntity(pos) instanceof HeapBlockEntity heap && (!heap.isEmpty() || heap.locked())) {
            heap.saveToItem(picked, level.registryAccess());
        }
        return picked;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}
