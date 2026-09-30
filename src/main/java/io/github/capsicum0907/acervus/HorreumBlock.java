package io.github.capsicum0907.acervus;

import java.util.List;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The rack. Right-click it to put heaps in and take them out.
 *
 * <p>Nothing else: it has no gestures of its own, because everything it does is done
 * through the heaps inside it and they already know how. What the block adds is a
 * single place for a pipe to reach all of them.
 */
public class HorreumBlock extends BaseEntityBlock {
    public static final MapCodec<HorreumBlock> CODEC = simpleCodec(HorreumBlock::new);

    public HorreumBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HorreumBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof HorreumBlockEntity)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer server) {
            server.openMenu(new SimpleMenuProvider(
                            (id, inventory, viewer) -> HorreumMenu.at(id, inventory, pos),
                            state.getBlock().getName()),
                    buffer -> buffer.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * The heaps ride on the dropped rack, and for a harder reason than usual.
     *
     * <p>Spilling would drop nine heap items on the floor, which sounds harmless
     * until one remembers what a heap holds: nine stacks of two billion, in a pile
     * of entities that can be walked away from, burned, or picked up by the wrong
     * hopper. A heap is a thing you move, and so is a rack of them.
     */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack dropped = new ItemStack(this);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof HorreumBlockEntity rack
                && !rack.isEmpty()) {
            rack.saveToItem(dropped, params.getLevel().registryAccess());
        }
        return List.of(dropped);
    }

    /** Middle-clicking one gives back what is in it, the same as a heap. */
    @Override
    public ItemStack getCloneItemStack(BlockState state, net.minecraft.world.phys.HitResult target,
            net.minecraft.world.level.LevelReader level, BlockPos pos, Player player) {
        ItemStack picked = new ItemStack(this);
        if (level.getBlockEntity(pos) instanceof HorreumBlockEntity rack && !rack.isEmpty()) {
            rack.saveToItem(picked, level.registryAccess());
        }
        return picked;
    }
}
