package io.github.capsicum0907.acervus.gas;

import java.util.List;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.HitResult;

public class ChemicalHeapBlock extends BaseEntityBlock {
    public static final MapCodec<ChemicalHeapBlock> CODEC = simpleCodec(ChemicalHeapBlock::new);

    public ChemicalHeapBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChemicalHeapBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public <T extends net.minecraft.world.level.block.entity.BlockEntity>
            net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
                    net.minecraft.world.level.Level level, BlockState state,
                    net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, GasHeap.BLOCK_ENTITY.get(), ChemicalHeapBlockEntity::serverTick);
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,
            net.minecraft.world.level.Level level, BlockPos pos, Player player,
            net.minecraft.world.phys.BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof io.github.capsicum0907.acervus.Heaped)) {
            return net.minecraft.world.InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return net.minecraft.world.InteractionResult.SUCCESS;
        }
        if (player instanceof net.minecraft.server.level.ServerPlayer server) {
            server.openMenu(new net.minecraft.world.SimpleMenuProvider(
                            (id, inventory, viewer) ->
                                    io.github.capsicum0907.acervus.ReadoutMenu.at(id, inventory, pos),
                            state.getBlock().getName()),
                    buffer -> buffer.writeBlockPos(pos));
        }
        return net.minecraft.world.InteractionResult.SUCCESS;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack dropped = new ItemStack(this);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof ChemicalHeapBlockEntity heap
                && (!heap.isEmpty() || heap.locked())) {
            heap.saveToItem(dropped, params.getLevel().registryAccess());
        }
        return List.of(dropped);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos,
            Player player) {
        ItemStack picked = new ItemStack(this);
        if (level.getBlockEntity(pos) instanceof ChemicalHeapBlockEntity heap && (!heap.isEmpty() || heap.locked())) {
            heap.saveToItem(picked, level.registryAccess());
        }
        return picked;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}
