package io.github.capsicum0907.acervus;

import java.util.List;

import io.github.capsicum0907.acervus.data.TestStructures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * What a heap holds, and — the part that actually breaks in blocks like this — what
 * it lets out through the item handler that hoppers and pipes talk to.
 *
 * <p>Run with {@code gradlew runGameTestServer}.
 */
@GameTestHolder(Acervus.MODID)
@PrefixGameTestTemplate(false)
public final class AcervusTests {
    private static final BlockPos WHERE = new BlockPos(2, 1, 2);

    /** Far past a stack, and past a shulker box, so nothing accidental can pass. */
    private static final int MANY = 5_000;

    private AcervusTests() {
    }

    @GameTest(template = TestStructures.FLOOR)
    public static void holdsFarMoreThanAStack(GameTestHelper helper) {
        HeapBlockEntity heap = place(helper);

        int taken = heap.insert(new ItemStack(Items.DIAMOND, MANY), false);

        check(taken == MANY, "a heap should have taken all " + MANY + ", not " + taken);
        check(heap.count() == MANY, "a heap should be holding " + MANY + ", not " + heap.count());
        helper.succeed();
    }

    /**
     * The number inside must never leave. Code on the far side of an item handler was
     * written for stacks, and hands a count larger than one straight back to something
     * that rounds it down — which is where storage blocks of this kind lose items.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void neverHandsOutMoreThanAStack(GameTestHelper helper) {
        HeapBlockEntity heap = place(helper);
        heap.insert(new ItemStack(Items.DIAMOND, MANY), false);
        IItemHandler handler = handler(helper);

        check(handler.getStackInSlot(0).getCount() <= Items.DIAMOND.getDefaultMaxStackSize(),
                "the window should show at most a stack, not " + handler.getStackInSlot(0).getCount());
        check(handler.getSlotLimit(0) <= Items.DIAMOND.getDefaultMaxStackSize(),
                "the slot limit should be a stack, not " + handler.getSlotLimit(0));
        check(handler.extractItem(0, Integer.MAX_VALUE, false).getCount()
                        <= Items.DIAMOND.getDefaultMaxStackSize(),
                "asking for everything should still give back one stack");
        helper.succeed();
    }

    /** The path a hopper and every mod's pipes actually take. */
    @GameTest(template = TestStructures.FLOOR)
    public static void fillsAndDrainsThroughTheHandler(GameTestHelper helper) {
        place(helper);
        IItemHandler handler = handler(helper);

        ItemStack left = handler.insertItem(0, new ItemStack(Items.DIAMOND, 64), false);
        check(left.isEmpty(), "the handler should have taken the whole stack");

        ItemStack out = handler.extractItem(0, 64, false);
        check(out.getCount() == 64, "the handler should have given back 64, not " + out.getCount());
        check(out.is(Items.DIAMOND), "the handler should have given back what went in");
        helper.succeed();
    }

    @GameTest(template = TestStructures.FLOOR)
    public static void refusesASecondKind(GameTestHelper helper) {
        HeapBlockEntity heap = place(helper);
        heap.insert(new ItemStack(Items.DIAMOND, 10), false);

        check(heap.insert(new ItemStack(Items.GOLD_INGOT, 10), false) == 0,
                "a heap holding diamonds should refuse gold");
        check(!handler(helper).isItemValid(0, new ItemStack(Items.GOLD_INGOT)),
                "and should say so before being asked to take it");
        helper.succeed();
    }

    /**
     * An emptied heap forgets what it held. One that remembered would refuse the next
     * thing put into it, with nothing on the block to say why.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void emptyForgetsItsKind(GameTestHelper helper) {
        HeapBlockEntity heap = place(helper);
        heap.insert(new ItemStack(Items.DIAMOND, 10), false);
        heap.extract(10, false);

        check(heap.isEmpty(), "a heap that gave back everything should be empty");
        check(heap.insert(new ItemStack(Items.GOLD_INGOT, 10), false) == 10,
                "and should then take a different kind");
        helper.succeed();
    }

    /**
     * Breaking a heap must not spill what is inside: at capacity that would be tens of
     * millions of item entities. What it holds rides on the dropped block instead.
     */
    @GameTest(template = TestStructures.FLOOR)
    public static void carriesItsContentsWhenBroken(GameTestHelper helper) {
        HeapBlockEntity heap = place(helper);
        heap.insert(new ItemStack(Items.DIAMOND, MANY), false);

        BlockPos pos = helper.absolutePos(WHERE);
        List<ItemStack> drops = helper.getLevel().getBlockState(pos).getDrops(
                new LootParams.Builder(helper.getLevel())
                        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                        .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                        .withOptionalParameter(LootContextParams.BLOCK_ENTITY, heap));

        check(drops.size() == 1, "a heap should drop exactly one thing, not " + drops.size());
        check(drops.get(0).has(DataComponents.BLOCK_ENTITY_DATA),
                "the dropped heap should be carrying its contents");
        helper.succeed();
    }

    private static HeapBlockEntity place(GameTestHelper helper) {
        helper.setBlock(WHERE, AcervusRegistry.HEAP.get());
        if (helper.getBlockEntity(WHERE) instanceof HeapBlockEntity heap) {
            return heap;
        }
        throw new GameTestAssertException("placing a heap should have made a heap block entity");
    }

    private static IItemHandler handler(GameTestHelper helper) {
        IItemHandler handler = helper.getLevel().getCapability(
                Capabilities.ItemHandler.BLOCK, helper.absolutePos(WHERE), null);
        if (handler == null) {
            throw new GameTestAssertException("a heap should offer an item handler to hoppers and pipes");
        }
        return handler;
    }

    private static void check(boolean condition, String expectation) {
        if (!condition) {
            throw new GameTestAssertException(expectation);
        }
    }
}
