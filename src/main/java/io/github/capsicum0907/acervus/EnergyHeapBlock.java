package io.github.capsicum0907.acervus;

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

/**
 * The energy heap.
 *
 * <p>No interaction of its own. There is no bucket of electricity to right-click it
 * with — energy arrives and leaves through cables, and a block that also had a
 * gesture would be inventing one for a thing that already has a way in.
 */
public class EnergyHeapBlock extends BaseEntityBlock {
    public static final MapCodec<EnergyHeapBlock> CODEC = simpleCodec(EnergyHeapBlock::new);

    public EnergyHeapBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyHeapBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** What is inside rides on the dropped block, for the reasons in {@link HeapBlock#getDrops}. */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack dropped = new ItemStack(this);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof EnergyHeapBlockEntity heap
                && !heap.isEmpty()) {
            heap.saveToItem(dropped, params.getLevel().registryAccess());
        }
        return List.of(dropped);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, LevelReader level, BlockPos pos,
            Player player) {
        ItemStack picked = new ItemStack(this);
        if (level.getBlockEntity(pos) instanceof EnergyHeapBlockEntity heap && !heap.isEmpty()) {
            heap.saveToItem(picked, level.registryAccess());
        }
        return picked;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }
}
